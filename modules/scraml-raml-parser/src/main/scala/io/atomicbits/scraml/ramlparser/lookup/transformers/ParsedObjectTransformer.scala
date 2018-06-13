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
import io.atomicbits.scraml.ramlparser.model.{ ImplicitId, JsonSchemaModel, NativeId, UniqueId }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 22/12/16.
  *
  * Transformes a ParsedObject into an ObjectType and collects all available type information into the CanonicalLookupHelper.
  *
  */
object ParsedObjectTransformer {

  type PropertyAggregator = (Map[String, Property[_ <: GenericReferrable]], CanonicalLookupHelper)

  // format: off
  def unapply(parsedTypeContext: ParsedTypeContext)
             (implicit canonicalNameGenerator: CanonicalNameGenerator): Option[(TypeReference, CanonicalLookupHelper)] = { // format: on

    val parsed: ParsedType                           = parsedTypeContext.parsedType
    val canonicalLookupHelper: CanonicalLookupHelper = parsedTypeContext.canonicalLookupHelper
    val canonicalNameOpt: Option[CanonicalName]      = parsedTypeContext.canonicalNameOpt
    val parentNameOpt: Option[CanonicalName]         = parsedTypeContext.parentNameOpt // This is the optional json-schema parent
    val imposedTypeDiscriminatorOpt: Option[String]  = parsedTypeContext.imposedTypeDiscriminator

    /**
      * Find the type discriminator value if there is one and subtract the type discriminator field from the parsed properties.
      */
    def processJsonSchemaTypeDiscriminator(typeDiscriminatorOpt: Option[String],
                                           props: ParsedProperties): (Option[String], ParsedProperties) = {
      typeDiscriminatorOpt.flatMap { typeDiscriminator =>
        props.get(typeDiscriminator).map(_.propertyType.parsed) collect {
          case parsed: ParsedEnum => (parsed.choices.headOption, props - typeDiscriminator)
        }
      } getOrElse (None, props)
    }

    def registerParsedObject(parsedObject: ParsedObject, canonicalName: CanonicalName): (TypeReference, CanonicalLookupHelper) = {

      val ownTypeDiscriminatorOpt: Option[String] = parsedObject.typeDiscriminator
      val typeDescriminatorOpt                    = List(ownTypeDiscriminatorOpt, imposedTypeDiscriminatorOpt).flatten.headOption

      // Prepare the empty aggregator
      val aggregator: PropertyAggregator = (Map.empty[String, Property[_ <: GenericReferrable]], canonicalLookupHelper)

      // if there is an imposed type discriminator, then we need to find its value and remove the discriminator from the properties.
      val (jsonSchemaTypeDiscriminatorValue, propertiesWithoutTypeDiscriminator) =
        processJsonSchemaTypeDiscriminator(typeDescriminatorOpt, parsedObject.properties)

      val typeDiscriminatorValue = {
        val nativeIdOpt =
          Option(parsedObject.id).collect {
            case NativeId(native) => native
          }
        List(jsonSchemaTypeDiscriminatorValue, parsedObject.typeDiscriminatorValue, nativeIdOpt).flatten.headOption
      }

      // Transform and register all properties of this object
      val (properties, propertyUpdatedCanonicalLH) =
        propertiesWithoutTypeDiscriminator.valueMap.foldLeft(aggregator)(propertyTransformer(parsedObject.requiredProperties))

      // Extract all json-schema children from this parsed object and register them in de canonical lookup helper
      val jsonSchemaChildrenUpdatedCanonicalLH =
        extractJsonSchemaChildren(parsedObject, propertyUpdatedCanonicalLH, canonicalName, typeDescriminatorOpt)

      // Get the RAML 1.0 parent from this parsed object, if any

      val raml10ParentNameOp: Set[NonPrimitiveTypeReference] =
        parsedObject.parents.toSeq // These are the RAML 1.0 parents
          .map { parent =>
            val (genericRef, unusedCanonicalLH) = ParsedToCanonicalTypeTransformer.transform(parent, canonicalLookupHelper)
            genericRef
          }
          .collect {
            case nonPrimitiveTypeReference: NonPrimitiveTypeReference => nonPrimitiveTypeReference
          }
          .toSet

      // Make a flattened list of all found parents
      val parents = parentNameOpt.map(raml10ParentNameOp + NonPrimitiveTypeReference(_)).getOrElse(raml10ParentNameOp)

      val objectType =
        ObjectType(
          canonicalName          = canonicalName,
          properties             = properties,
          parents                = parents.toList, // ToDo: make this a Set in ObjectType as well.
          typeParameters         = parsedObject.typeParameters.map(TypeParameter),
          typeDiscriminator      = List(imposedTypeDiscriminatorOpt, parsedObject.typeDiscriminator).flatten.headOption,
          typeDiscriminatorValue = typeDiscriminatorValue
        )

      val typeReference: TypeReference = NonPrimitiveTypeReference(canonicalName) // We don't 'fill in' type parameter values here.
      (typeReference, jsonSchemaChildrenUpdatedCanonicalLH.addCanonicalType(canonicalName, objectType))
    }

    // Generate the canonical name for this object
    val canonicalName = canonicalNameOpt.getOrElse(canonicalNameGenerator.generate(parsed.id))

    (parsed, canonicalName) match {
      case (parsedObject: ParsedObject, NoName(_))                           => Some((JsonType, canonicalLookupHelper))
      case (parsedMultipleInheritance: ParsedMultipleInheritance, NoName(_)) => Some((JsonType, canonicalLookupHelper))
      case (parsedObject: ParsedObject, _) if parsedObject.isEmpty           => Some((JsonType, canonicalLookupHelper))
      case (parsedObject: ParsedObject, _)                                   => Some(registerParsedObject(parsedObject, canonicalName))
      case (parsedMultipleInheritance: ParsedMultipleInheritance, _) =>
        Some(registerParsedObject(parsedMultipleInheritanceToParsedObject(parsedMultipleInheritance), canonicalName))
      case _ => None
    }
  }

  // format: off
  def propertyTransformer(requiredPropertyNames: List[String])
                         (propertyAggregator: PropertyAggregator,
                          propKeyValue: (String, ParsedProperty))
                         (implicit canonicalNameGenerator: CanonicalNameGenerator): PropertyAggregator = { // format: on

    val (currentProperties, currentCanonicalLH) = propertyAggregator
    val (propName, propValue)                   = propKeyValue
    val (typeReference, updatedCanonicalLH) =
      ParsedToCanonicalTypeTransformer.transform(propValue.propertyType.parsed, currentCanonicalLH)
    val required = requiredPropertyNames.contains(propName) || propValue.required
    val property =
      Property(
        name     = propName,
        ttype    = typeReference,
        required = required
        // ToDo: process and add the typeConstraints
      )
    val updatedProperties = currentProperties + (propName -> property)
    (updatedProperties, updatedCanonicalLH)
  }

  // format: off
  /**
    * Register the children (if any) of the given parsedObject and return the updated canonical lookup helper.
    */
  private def extractJsonSchemaChildren(parsedObject: ParsedObject,
                                        canonicalLookupHelper: CanonicalLookupHelper,
                                        parentName: CanonicalName,
                                        typeDiscriminatorOpt: Option[String])
                                       (implicit canonicalNameGenerator: CanonicalNameGenerator): CanonicalLookupHelper = { // format: on

    val typeDiscriminator = typeDiscriminatorOpt.getOrElse("type")

    def registerObject(canonicalLH: CanonicalLookupHelper, parsedChild: ParsedType): CanonicalLookupHelper = {

      ParsedTypeContext(parsedChild, canonicalLH, None, Some(parentName), Some(typeDiscriminator)) match {
        case ParsedObjectTransformer(typeReference, updatedCanonicalLH) => updatedCanonicalLH
        case _                                                          => canonicalLH
      }

    }

    parsedObject.selection match {
      case Some(OneOf(selection)) =>
        selection.foldLeft(canonicalLookupHelper)(registerObject)
      case _ => canonicalLookupHelper
    }
  }

  def parsedMultipleInheritanceToParsedObject(parsedMultipleInheritance: ParsedMultipleInheritance): ParsedObject = {

    ParsedObject(
      id                     = parsedMultipleInheritance.id,
      properties             = parsedMultipleInheritance.properties,
      required               = parsedMultipleInheritance.required,
      requiredProperties     = parsedMultipleInheritance.requiredProperties,
      parents                = parsedMultipleInheritance.parents,
      typeParameters         = parsedMultipleInheritance.typeParameters,
      typeDiscriminator      = parsedMultipleInheritance.typeDiscriminator,
      typeDiscriminatorValue = parsedMultipleInheritance.typeDiscriminatorValue,
      model                  = parsedMultipleInheritance.model
    )

  }

}
