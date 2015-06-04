package io.atomicbits.scraml.jsonschemaparser

import play.api.libs.json.JsObject

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
case class SchemaLookup(lookupTable: Map[String, JsObject] = Map.empty,
                        canonicalNames: Map[String, String] = Map.empty,
                        externalSchemaLinks: Map[String, String] = Map.empty) {

  def map(f: SchemaLookup => SchemaLookup): SchemaLookup = f(this)

}
