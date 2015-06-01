package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.{JsObject, Json}

import scala.language.postfixOps

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object JsonSchemaParser {

  /**
   * Features:
   * + schema lookup table by expanding all schema id's using inline dereferencing
   * + reverse dereferencing for anonymous object references
   * + Canonical name generation for each schema
   * + expanding all ("$ref") schema references to the expanded schema id's for easy lookup using the lookup table
   * + case class generation based on the above schema manipulations and canonical names
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
    val jsonSchemas: List[JsObject] = schemas map Json.parse collect { case schema: JsObject => schema }
    val schemaLookup = jsonSchemas.foldLeft(SchemaLookup())(registerExpandedIds)

    ???
  }

  private[jsonschemaparser] def registerExpandedIds(schemaLookup: SchemaLookup, schema: JsObject): SchemaLookup = {

    schema match {
      case IdExtractor(Root(id, anchor)) =>
        val updatedSchemaLookup = schemaLookup.copy(lookupTable = schemaLookup.lookupTable + (id -> schema))
        // for all fields of schema that refer to an object, call expand with the current root and anchor
        // and do that recursively with adjusted root and anchor where necessary
        // ? expand all "$ref" fields already ?
        ???
      case _ => throw new IllegalArgumentException("A top-level schema should have a root id.")
    }

    def registerIds(subSchema: JsObject, root: Root) = {
      ???
    }

  }

}
