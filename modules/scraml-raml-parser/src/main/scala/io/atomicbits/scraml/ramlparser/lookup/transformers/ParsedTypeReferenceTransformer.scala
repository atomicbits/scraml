/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.lookup.transformers

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator, ParsedToCanonicalTypeTransformer }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ PrimitiveType => _, _ }
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

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
          case primitiveType: PrimitiveType =>
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
          case parsedObject: ParsedObject =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedObject.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(
                refers       = canonicalName,
                genericTypes = genericTypes
              )
            (typeReference, canonicalLH)
          case parsedUnionType: ParsedUnionType =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedUnionType.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(
                refers       = canonicalName,
                genericTypes = genericTypes
              )
            (typeReference, canonicalLH)
          case parsedMultipleInheritance: ParsedMultipleInheritance =>
            val canonicalName                     = canonicalNameGenerator.generate(parsedMultipleInheritance.id)
            val (genericTypes, unusedCanonicalLH) = transformGenericTypes(parsedTypeReference.genericTypes, canonicalLH)
            val typeReference =
              NonPrimitiveTypeReference(
                refers       = canonicalName,
                genericTypes = genericTypes
              )
            (typeReference, canonicalLH)
          case parsedTRef: ParsedTypeReference =>
            if (referencesFollowed.contains(parsedTRef))
              sys.error(s"Cyclic reference detected when following $parsedTRef")
            else
              registerParsedTypeReference(parsedTRef, canonicalLH, parsedTypeReference :: referencesFollowed)
          case parsedNull: ParsedNull => (NullType, canonicalLH)
          case unexpected             => sys.error(s"Didn't expect to find a type reference to a $unexpected")
        } getOrElse {
          sys.error(s"The reference ${parsedTypeReference.refersTo} was not found in the parsed type index!")
        }

      typeRefAsGenericReferrable match {
        case typeReference: TypeReference => (typeReference, updatedCanonicalLH)
        case unexpected                   => sys.error(s"Expected $unexpected to be a type referece")
      }

    }

    def transformGenericTypes(parsedGenericTypes: Map[String, ParsedType],
                              canonicalLH: CanonicalLookupHelper): (Map[TypeParameter, GenericReferrable], CanonicalLookupHelper) = {

      val aggregator: (Map[TypeParameter, GenericReferrable], CanonicalLookupHelper) = (Map.empty, canonicalLH)

      parsedGenericTypes.foldLeft(aggregator) { (aggr, parsedGenericType) =>
        val (parsedKey, parsedType)                    = parsedGenericType
        val (canonicalGenericMap, canonicalLookup)     = aggr
        val typeParameter                              = TypeParameter(parsedKey)
        val (genericReferrable, unusedCanonicalLookup) = ParsedToCanonicalTypeTransformer.transform(parsedType, canonicalLookup)
        val updatedCanonicalGenericMap                 = canonicalGenericMap + (typeParameter -> genericReferrable)
        (updatedCanonicalGenericMap, unusedCanonicalLookup)
      }

    }

    parsed match {
      case parsedTypeReference: ParsedTypeReference => Some(registerParsedTypeReference(parsedTypeReference, canonicalLookupHelper))
      case _                                        => None
    }
  }

}