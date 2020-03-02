/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.lookup.transformers

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator, ParsedToCanonicalTypeTransformer }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ PrimitiveType => ParsedPrimitiveType, _ }

/**
  * Created by peter on 23/12/16.
  */
object ParsedTypeReferenceTransformer {

  // format: off
  def unapply(parsedTypeContext: ParsedTypeContext)
             (implicit canonicalNameGenerator: CanonicalNameGenerator): Option[(TypeReference, CanonicalLookupHelper)] = { // format: on

    val parsed: ParsedType                           = parsedTypeContext.parsedType
    val canonicalLookupHelper: CanonicalLookupHelper = parsedTypeContext.canonicalLookupHelper
    val canonicalNameOpt: Option[CanonicalName]      = parsedTypeContext.canonicalNameOpt
    val parentNameOpt: Option[CanonicalName]         = parsedTypeContext.parentNameOpt

    // If this is a type reference to a (fragmented) json-schema type, then we need to register it as such with the canonicalLookupHelper.
    // In this case, we also need to use the parsedTypeContext.canonicalLookupHelper.declaredTypes map to look up the canonical name
    // as defined by the json-schema id (and not the native id of the RAML definition).

    def registerParsedTypeReference(parsedTypeReference: ParsedTypeReference,
                                    canonicalLH: CanonicalLookupHelper,
                                    referencesFollowed: List[ParsedTypeReference] = List.empty): (TypeReference, CanonicalLookupHelper) = {

      val (typeRefAsGenericReferrable, updatedCanonicalLH) =
        canonicalLookupHelper.getParsedTypeWithProperId(parsedTypeReference.refersTo).map {
          case _: ParsedNull => (NullType, canonicalLH)
          case primitiveType: ParsedPrimitiveType =>
            val (typeRef, unusedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(primitiveType, canonicalLH)
            (typeRef, canonicalLH)
          case parsedDate: ParsedDate =>
            val (typeRef, unusedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(parsedDate, canonicalLH)
            (typeRef, canonicalLH)
          case parsedArray: ParsedArray =>
            val (typeRef, unusedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(parsedArray, canonicalLH)
            (typeRef, canonicalLH)
          case parsedFile: ParsedFile =>
            val (typeRef, unusedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(parsedFile, canonicalLH)
            (typeRef, canonicalLH)
          case parsedEnum: ParsedEnum =>
            val canonicalName = canonicalNameGenerator.generate(parsedEnum.id)
            val typeReference = NonPrimitiveTypeReference(canonicalName)
            (typeReference, canonicalLH)
          case parsedObject: ParsedObject if parsedObject.isEmpty =>
            (JsonType, canonicalLH)
          case parsedObject: ParsedObject =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedObject.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(
                refers                = canonicalName,
                genericTypes          = genericTypes,
                genericTypeParameters = parsedObject.typeParameters.map(TypeParameter)
              )
            (typeReference, canonicalLH)
          case parsedUnionType: ParsedUnionType =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedUnionType.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(refers = canonicalName)
            (typeReference, canonicalLH)
          case parsedMultipleInheritance: ParsedMultipleInheritance =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedMultipleInheritance.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(
                refers                = canonicalName,
                genericTypes          = genericTypes, // Can we have generic types here?
                genericTypeParameters = parsedMultipleInheritance.typeParameters.map(TypeParameter)
              )
            (typeReference, canonicalLH)
          case parsedTRef: ParsedTypeReference =>
            if (referencesFollowed.contains(parsedTRef))
              sys.error(s"Cyclic reference detected when following $parsedTRef")
            else
              registerParsedTypeReference(parsedTRef, canonicalLH, parsedTypeReference :: referencesFollowed)
          case unexpected             => sys.error(s"Didn't expect to find a type reference to a $unexpected in $parsedTypeReference")
        } getOrElse {
          sys.error(
            s"The reference to ${parsedTypeReference.refersTo} was not found in the parsed type index. " +
              s"Is there a typo in one of the type references? (References are case-sensitive!)")
        }

      typeRefAsGenericReferrable match {
        case typeReference: TypeReference => (typeReference, updatedCanonicalLH)
        case unexpected                   => sys.error(s"Expected $unexpected to be a type referece")
      }

    }

    def transformGenericTypes(parsedGenericTypes: List[ParsedType],
                              canonicalLH: CanonicalLookupHelper): (List[GenericReferrable], CanonicalLookupHelper) = {

      val aggregator: (List[GenericReferrable], CanonicalLookupHelper) = (List.empty, canonicalLH)

      parsedGenericTypes.foldLeft(aggregator) { (aggr, parsedGenericType) =>
        val (canonicalGenericList, canonicalLookup)    = aggr
        val (genericReferrable, unusedCanonicalLookup) = ParsedToCanonicalTypeTransformer.transform(parsedGenericType, canonicalLookup)
        val updatedCanonicalGenericList                = canonicalGenericList :+ genericReferrable
        (updatedCanonicalGenericList, unusedCanonicalLookup)
      }

    }

    parsed match {
      case parsedTypeReference: ParsedTypeReference => Some(registerParsedTypeReference(parsedTypeReference, canonicalLookupHelper))
      case _                                        => None
    }
  }

}
