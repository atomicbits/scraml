package io.atomicbits.scraml.jsonschemaparser

import io.atomicbits.scraml.jsonschemaparser.SchemaLookup._
import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */

/**
 * A lookup table to follow schema ids and external links to schema definitions (JsObject) and canonical names.
 *
 * @param lookupTable Maps absolute schema ids (and relative schema ids after they have been expanded to their
 *                    absolute form) to the schema definition. Mind that not all schema definitions represent
 *                    object types, they can represent any type, or even no type (usually when defining nested
 *                    schemas).
 * @param externalSchemaLinks Maps the external schema links to the corresponding schema id. That schema id then
 *                            corresponds with a schema in the lookupTable. That schema should represent an
 *                            actual type (integer, number, string, boolean, object, List[integer], List[number],
 *                            List[string], List[boolean], List[object], or even nested lists).
 */
case class SchemaLookup(lookupTable: Map[AbsoluteId, JsObject] = Map.empty,
                        externalSchemaLinks: Map[Link, Id] = Map.empty) {

  def map(f: SchemaLookup => SchemaLookup): SchemaLookup = f(this)

}

object SchemaLookup {

  type Link = String
  type Id = String

}
