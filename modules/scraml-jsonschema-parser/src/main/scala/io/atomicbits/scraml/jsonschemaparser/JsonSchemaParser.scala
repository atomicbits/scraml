/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.jsonschemaparser

import io.atomicbits.scraml.jsonschemaparser.model._
import play.api.libs.json.{JsObject, Json}

import scala.language.postfixOps

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object JsonSchemaParser {

  /**
   * Features:
   * + expanding all ("$ref") schema references to the absolute schema id's for easy lookup using the lookup table
   * + reverse dereferencing for anonymous object references
   * - --> too complex for now, we detect anonymous ("id"-less) object references in the IdExtractor and fail
   * + schema lookup table by expanding all schema id's to their absolute id
   * + Canonical name generation for each schema
   * + case class generation based on the above schema manipulations and canonical names using inline dereferencing
   *
   * References:
   * + par. 7.2.2 http://json-schema.org/latest/json-schema-core.html
   * + par. 7.2.3 http://json-schema.org/latest/json-schema-core.html
   * + http://tools.ietf.org/html/draft-zyp-json-schema-03
   * + http://spacetelescope.github.io/understanding-json-schema/structuring.html (good fragment dereferencing examples)
   *
   */

  /**
   *
   * @param schemas A map containing the String-representation of JSON schema files as values. The keys are external
   *                links referring to the schema. A single schema may contain nested schemas.
   *                All schemas MUST have an "id" property containing an absolute or relative identification for
   *                the schema, e.g.: { "id": "http://atomicbits.io/schema/user.json#", ... }
   * @return A schema lookup table.
   */
  def parse(schemas: Map[String, String]): SchemaLookup = {
    schemas
      .mapValues(Json.parse)
      .collect { case (id, schema: JsObject) => (id, schema) }
      .mapValues(Schema(_)) // we now have a Map[String, Schema]
      .mapValues(expandRelativeToAbsoluteIds) // we are now sure to have only AbsoluteId references as ids
      .foldLeft(SchemaLookup())(updateLookupTableAndObjectMap)
      .map(CanonicalNameGenerator.deduceCanonicalNames)
  }


  /**
   * Expand all relative ids to absolute ids and register them in the schema lookup and also expand all $ref pointers.
   *
   * @param schema
   * @return
   */
  def expandRelativeToAbsoluteIds(schema: Schema): Schema = {

    def expandWithRootAndPath(schema: Schema, root: RootId, path: List[String] = List.empty): Schema = {

      val expandedId = root.toAbsolute(schema.id, path)

      def expandFragment(fragmentPath: (String, Schema)): (String, Schema) = {
        val (pathPart, subSchema) = fragmentPath
        val updatedSubSchema = expandWithRootAndPath(subSchema, expandedId.rootPart, path :+ pathPart)
        (pathPart, updatedSubSchema)
      }

      val schemaWithUpdatedFragments =
        schema match {
          case objEl: ObjectEl =>
            objEl.copy(
              fragments = objEl.fragments.map(expandFragment),
              properties = objEl.properties.map(expandFragment)
            )
          case frag: Fragment => frag.copy(fragments = frag.fragments.map(expandFragment))
          case arr: ArrayEl =>
            val (_, expanded) = expandFragment(("items", arr.items))
            arr.copy(items = expanded)
          case ref: SchemaReference => ref.copy(refersTo = root.toAbsolute(ref.refersTo, path))
          case _ => schema
        }

      val schemaWithUpdatedProperties =
        schemaWithUpdatedFragments match {
          case objEl: ObjectEl => objEl.copy(properties = objEl.properties.map(expandFragment))
          case _ => schemaWithUpdatedFragments
        }

      schemaWithUpdatedProperties.updated(expandedId)
    }

    schema.id match {
      case rootId: RootId => expandWithRootAndPath(schema, rootId)
      case _ => throw JsonSchemaParseException("We cannot expand the ids in a schema that has no absolute root id.")
    }

  }


  /**
   *
   * @param lookup The schema lookup
   * @param linkedSchema A tuple containing a field name and the schema the field refers to. Nothing is done with the
   *                     field name, it is there to make folding easier on schema fragments and object properties.
   * @return The schema lookup with added object references.
   */
  def updateLookupTableAndObjectMap(lookup: SchemaLookup, linkedSchema: (String, Schema)): SchemaLookup = {


    def updateLookupAndObjectMapInternal(lookup: SchemaLookup, schemaFragment: (String, Schema)): SchemaLookup = {

      val (path, schema) = schemaFragment

      val updatedSchemaLookup =
        schema.id match {
          case rootId: RootId =>
            lookup.copy(lookupTable = lookup.lookupTable + (rootId -> schema))
          case _ => lookup
        }

      val absoluteId = schema.id match {
        case absId: AbsoluteId => absId
        case _ => throw JsonSchemaParseException("All IDs should have been expanded to absolute IDs.")
      }

      schema match {
        case objEl: ObjectEl =>
          val schemaLookupWithObjectFragments =
            objEl.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
          val schemaLookupWithObjectProperties =
            objEl.properties.foldLeft(schemaLookupWithObjectFragments)(updateLookupAndObjectMapInternal)
          schemaLookupWithObjectProperties
            .copy(objectMap = schemaLookupWithObjectProperties.objectMap + (absoluteId -> objEl))
        case fragment: Fragment =>
          fragment.fragments.foldLeft(updatedSchemaLookup)(updateLookupAndObjectMapInternal)
        case enumEl: EnumEl =>
          updatedSchemaLookup.copy(enumMap = updatedSchemaLookup.enumMap + (absoluteId -> enumEl))
        case arr: ArrayEl =>
          updateLookupAndObjectMapInternal(updatedSchemaLookup, ("", arr.items))
        case _ => updatedSchemaLookup
      }

    }


    val (externalLink, schema) = linkedSchema

    val schemaLookupWithUpdatedExternalLinks = schema.id match {
      case id: RootId =>
        lookup.copy(externalSchemaLinks = lookup.externalSchemaLinks + (externalLink -> id))
      case _ => throw JsonSchemaParseException(s"A top-level schema must have a root id (is ${schema.id}).")
    }

    updateLookupAndObjectMapInternal(schemaLookupWithUpdatedExternalLinks, ("", schema))

  }


  def simplify(schemas: List[Schema], lookup: SchemaLookup): List[Schema] = ???

  def generateCanonicalNames = ???


}
