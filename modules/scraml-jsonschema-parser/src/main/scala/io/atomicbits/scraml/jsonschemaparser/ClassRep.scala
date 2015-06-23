package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 23/06/15. 
 */
trait ClassRep {

  def name: String

}

case class PlainClassRep(name: String) extends ClassRep

case class TypeClassRep(name: String, types: List[ClassRep]) extends ClassRep
