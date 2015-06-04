package io.atomicbits.scraml.generator

import io.atomicbits.scraml.jsonschemaparser.SchemaLookup

import scala.reflect.macros.whitebox

/**
 * Created by peter on 4/06/15, Atomic BITS (http://atomicbits.io). 
 */
object CaseClassGenerator {

  def generateCaseClasses(schemaLookup: SchemaLookup, c: whitebox.Context): c.universe.Tree = {

    // Expand all canonical names into their case class definitions.

//    schemaLookup.can

    ???
  }

}
