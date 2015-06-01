package io.atomicbits.scraml.jsonschemaparser

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
sealed trait IdType

case class Root(id: String, anchor: String) extends IdType

case class Relative(id: String) extends IdType

case object NoId extends IdType
