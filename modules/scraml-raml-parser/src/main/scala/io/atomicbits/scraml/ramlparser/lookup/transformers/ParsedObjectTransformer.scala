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
import io.atomicbits.scraml.ramlparser.model.UniqueId
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 22/12/16.
  *
  * Transformes a ParsedObject into an ObjectType and collects all available type information into the CanonicalLookupHelper.
  *
  */
object ParsedObjectTransformer {

  type PropertyAggregator = (Map[String, Property[_]], CanonicalLookupHelper)

  // format: off
  def unapply(parsedTypeContext: ParsedTypeContext)
             (implicit canonicalNameGenerator: CanonicalNameGenerator): Option[(TypeReference, CanonicalLookupHelper)] = { // format: on

    val parsed: ParsedType                           = parsedTypeContext.parsedType
    val canonicalLookupHelper: CanonicalLookupHelper = parsedTypeContext.canonicalLookupHelper
    val canonicalNameOpt: Option[CanonicalName]      = parsedTypeContext.canonicalNameOpt
    val parentNameOpt: Option[CanonicalName]         = parsedTypeContext.parentNameOpt // This is the optional json-schema parent

    def registerParsedObject(parsedObject: ParsedObject): (TypeReference, CanonicalLookupHelper) = {

      // Prepare the empty aggregator
      val aggregator: PropertyAggregator = (Map.empty[String, Property[_]], canonicalLookupHelper)
      // Transform and register all properties of this object
      val (properies, propertyUpdatedCanonicalLH) =
        parsedObject.properties.valueMap.foldLeft(aggregator)(propertyTransformer)

      // Generate the canonical name for this object
      val canonicalName = canonicalNameOpt.getOrElse(canonicalNameGenerator.generate(parsed.id))

      // Extract all json-schema children from this parsed object and register them in de canonical lookup helper
      val jsonSchemaChildrenUpdatedCanonicalLH = extractJsonSchemaChildren(parsedObject, propertyUpdatedCanonicalLH, canonicalName)

      // Get the RAML 1.0 parent from this parsed object, if any
      val parentId: Option[UniqueId] = parsedObject.parent // This is the RAML 1.0 parent
      val raml10ParentNameOp         = parentId.map(canonicalNameGenerator.generate)

      // Make a flattened list of all found parents
      val parents = List(raml10ParentNameOp, parentNameOpt).flatten.map(NonPrimitiveTypeReference(_))

      val objectType =
        ObjectType(
          canonicalName          = canonicalName,
          properties             = properies,
          parents                = parents,
          typeParameters         = parsedObject.typeParameters.map(TypeParameter),
          typeDiscriminator      = parsedObject.typeDiscriminator,
          typeDiscriminatorValue = parsedObject.typeDiscriminatorValue
        )

      val typeReference: TypeReference = NonPrimitiveTypeReference(canonicalName) // We don't 'fill in' type parameter values here.

      (typeReference, jsonSchemaChildrenUpdatedCanonicalLH.addCanonicalType(canonicalName, objectType))
    }

    parsed match {
      case parsedObject: ParsedObject => Some(registerParsedObject(parsedObject))
      case _                          => None
    }
  }

  // format: off
  def propertyTransformer(propertyAggregator: PropertyAggregator,
                          propKeyValue: (String, ParsedProperty))
                         (implicit canonicalNameGenerator: CanonicalNameGenerator): PropertyAggregator = { // format: on

    val (currentProperties, currentCanonicalLH) = propertyAggregator
    val (propName, propValue)                   = propKeyValue
    val (typeReference, updatedCanonicalLH) =
      ParsedToCanonicalTypeTransformer.transform(propValue.propertyType.parsed, currentCanonicalLH)
    val property =
      Property(
        name     = propName,
        ttype    = typeReference,
        required = propValue.required
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
                                        parentName: CanonicalName)
                                       (implicit canonicalNameGenerator: CanonicalNameGenerator): CanonicalLookupHelper = { // format: on

    def registerObject(canonicalLH: CanonicalLookupHelper, parsedChild: ParsedType): CanonicalLookupHelper = {

      ParsedTypeContext(parsedChild, canonicalLH, None, Some(parentName)) match {
        case ParsedObjectTransformer(typeReference, updatedCanonicalLH)        => updatedCanonicalLH
        case ParsedTypeReferenceTransformer(typeReference, updatedCanonicalLH) => updatedCanonicalLH
        case _                                                                 => canonicalLH
      }

    }

    parsedObject.selection match {
      case Some(OneOf(selection)) => selection.foldLeft(canonicalLookupHelper)(registerObject)
      case _                      => canonicalLookupHelper
    }
  }

}
