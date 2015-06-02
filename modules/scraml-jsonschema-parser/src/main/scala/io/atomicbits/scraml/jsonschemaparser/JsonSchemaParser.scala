package io.atomicbits.scraml.jsonschemaparser

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
   * @param schemas A list containing the String-representation of JSON schema files. A single schema may contain
   *                nested schemas.
   *                All schemas MUST have an "id" property containing an absolute or relative identification for
   *                the schema, e.g.: { "id": "http://atomicbits.io/schema/user.json#", ... }
   * @return A schema lookup table.
   */
  def parse(schemas: List[String]): SchemaLookup = {
    schemas
      .map(Json.parse)
      .collect { case schema: JsObject => schema }
      .map(expandToAbsoluteRefs)
      .foldLeft(SchemaLookup())(registerAbsoluteSchemaIds)
  }


  private[jsonschemaparser] def expandToAbsoluteRefs(schema: JsObject): JsObject = {

    schema match {
      case IdExtractor(Root(id)) =>
        // This is just an initial check to see if all given schema's have an id that is a schema root.
        expandRefsFromRoot(schema, Root(id))

      case _ => throw new IllegalArgumentException("A top-level schema should have a root id.")

    }

    def expandRefsFromRoot(schema: JsObject, root: Root): JsObject = {

      def childObjectsFieldMap(schema: JsObject) = {
        schema.fields.collect { case (fieldName, jsObj: JsObject) => (fieldName, jsObj) }
      }

      val currentRoot =
        schema match {
          case IdExtractor(Root(id)) => Root(id)
          case IdExtractor(Relative(id)) => root.rootFromRelative(Relative(id))
          case IdExtractor(NoId) => root
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

  }


  private[jsonschemaparser] def registerAbsoluteSchemaIds(schemaLookup: SchemaLookup,
                                                          schema: JsObject): SchemaLookup = {

    schema match {
      case IdExtractor(Root(id)) =>
        // This is just an initial check to see if all given schema's have an id that is a schema root.
        registerIds(schema, Root(id), schemaLookup)

      case _ => throw new IllegalArgumentException("A top-level schema should have a root id.")

    }

    def registerIds(schema: JsObject, root: Root, schemaLookup: SchemaLookup): SchemaLookup = {

      schema match {
        case IdExtractor(Root(id)) =>
          val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (id -> schema))
          childObjects(schema).foldLeft(updatedSchemaLookup) { (lookup, childObject) =>
            registerIds(childObject, Root(id), lookup)
          }

        case IdExtractor(Relative(id)) =>
          val newRoot = root.rootFromRelative(Relative(id))
          val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (newRoot.id -> schema))
          childObjects(schema).foldLeft(updatedSchemaLookup) { (lookup, childObject) =>
            registerIds(childObject, newRoot, lookup)
          }

        case IdExtractor(NoId) =>
          childObjects(schema).foldLeft(schemaLookup) { (lookup, childObject) =>
            registerIds(childObject, root, lookup)
          }

      }

      def childObjects(schema: JsObject) = {
        schema.values.collect { case jsObj: JsObject => jsObj }
      }

    }

  }


}
