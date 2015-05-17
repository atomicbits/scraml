package io.atomicbits.scraml

import org.raml.model.Raml

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class ScalaRaml(raml: Raml) {

  def asScala: io.atomicbits.scraml.model.Raml = io.atomicbits.scraml.model.Raml(raml)

}
