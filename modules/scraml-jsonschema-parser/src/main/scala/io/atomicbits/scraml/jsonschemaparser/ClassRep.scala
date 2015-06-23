package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 23/06/15. 
 */
trait ClassRep

case class PlainClassRep(name: String)

case class TypeClassRep(name: String, types: List[ClassRep])
