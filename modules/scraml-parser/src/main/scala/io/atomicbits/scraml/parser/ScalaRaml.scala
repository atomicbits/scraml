package io.atomicbits.scraml.parser

import io.atomicbits.scraml.parser.model.Raml


/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class ScalaRaml(raml: org.raml.model.Raml) {

  def asScala: Raml = Raml(raml)

}
