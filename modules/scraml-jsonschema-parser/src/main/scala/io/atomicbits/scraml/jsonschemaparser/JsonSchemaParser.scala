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

import io.atomicbits.scraml.jsonschemaparser.model.Schema
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

    //      .mapValues(expandToAbsoluteRefs)
    //      .foldLeft(SchemaLookup())(registerAbsoluteSchemaIds)
    //      .map(CanonicalNameGenerator.deduceCanonicalNames)
    SchemaLookup() // ToDo: fix
  }

  def parseRawSchemas(schemas: Map[String, String]): Map[String, Schema] = {
    schemas
      .mapValues(Json.parse)
      .collect { case (id, schema: JsObject) => (id, schema) }
      .mapValues(Schema(_))
  }


  private[jsonschemaparser] def expandToAbsoluteRefs(schema: JsObject): JsObject = {

    def expandRefsFromRoot(schema: JsObject, root: AbsoluteId): JsObject = {

      def childObjectsFieldMap(schema: JsObject) = {
        schema.fields.collect { case (fieldName, jsObj: JsObject) => (fieldName, jsObj) }
      }

      val currentRoot =
        schema match {
          case IdExtractor(AbsoluteId(id)) => AbsoluteId(id)
          case _ => root
        }

      val schemaWithUpdatedRef =
        (schema \ "$ref").asOpt[String]
          .map(currentRoot.expandRef)
          .map(expanded => schema ++ Json.obj("$ref" -> expanded))
          .getOrElse(schema)

      val childObjects: Seq[(String, JsObject)] = childObjectsFieldMap(schemaWithUpdatedRef)

      childObjects.foldLeft(schemaWithUpdatedRef) { (updatedSchema, childObjectWithField) =>
        val (fieldName, childObject) = childObjectWithField
        updatedSchema ++ Json.obj(fieldName -> expandRefsFromRoot(childObject, currentRoot))
      }

    }

    schema match {
      case IdExtractor(AbsoluteId(id)) =>
        // This is just an initial check to see if all given schema's have an id that is a schema root.
        expandRefsFromRoot(schema, AbsoluteId(id))

      case _ => throw new IllegalArgumentException("A top-level schema should have a root id.")

    }

  }


  private[jsonschemaparser] def registerAbsoluteSchemaIds(schemaLookup: SchemaLookup,
                                                          linkedSchema: (String, JsObject)): SchemaLookup = {

    def registerIds(schema: JsObject, root: AbsoluteId, schemaLookup: SchemaLookup): SchemaLookup = {

      def childObjects(schema: JsObject) = {
        schema.values.collect { case jsObj: JsObject => jsObj }
      }

      schema match {
        case IdExtractor(AbsoluteId(id)) =>
          val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (AbsoluteId(id) -> schema))
          childObjects(schema).foldLeft(updatedSchemaLookup) { (lookup, childObject) =>
            registerIds(childObject, AbsoluteId(id), lookup)
          }

        case IdExtractor(RelativeId(id)) =>
          val absoluteId = root.toAbsolute(RelativeId(id))
          val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (absoluteId -> schema))
          childObjects(schema).foldLeft(updatedSchemaLookup) { (lookup, childObject) =>
            registerIds(childObject, root, lookup)
          }

        case IdExtractor(FragmentId(id)) =>
          childObjects(schema).foldLeft(schemaLookup) { (lookup, childObject) =>
            registerIds(childObject, root, lookup)
          }
        // We do not need to register fragmented ids because they are resolved through their base URL.
        //          val absoluteId = root.rootFromFragment(Fragment(id))
        //          val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (absoluteId.id -> schema))
        //          childObjects(schema).foldLeft(updatedSchemaLookup) { (lookup, childObject) =>
        //            registerIds(childObject, root, lookup)
        //          }

        case IdExtractor(ImplicitId) =>
          childObjects(schema).foldLeft(schemaLookup) { (lookup, childObject) =>
            registerIds(childObject, root, lookup)
          }

      }

    }

    val (link, schema) = linkedSchema

    schema match {
      case IdExtractor(AbsoluteId(id)) =>
        // This is just an initial check to see if all given schema's have an id that is a schema root.
        val updatedSchemaLookup =
          schemaLookup.copy(externalSchemaLinks = schemaLookup.externalSchemaLinks + (link -> AbsoluteId(id)))
        registerIds(schema, AbsoluteId(id), updatedSchemaLookup)

      case _ => throw new IllegalArgumentException("A top-level schema should have a root id.")

    }

  }

}
