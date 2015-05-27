package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 27/05/15, Atomic BITS (http://atomicbits.io). 
 */
object JsonSchemaParser {

  /**
   * schema lookup table by expanding all schema id's using inline dereferencing
   * reverse dereferencing for anonymous object references
   * Canonical name generation for each schema
   * expanding all ("$ref") schema references to the expanded schema id's for easy lookup using the lookup table
   * case class generation based on the above schema manipulations and canonical names
   */

}
