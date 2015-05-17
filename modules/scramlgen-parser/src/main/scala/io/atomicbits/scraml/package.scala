package io.atomicbits

import org.raml.model.Raml

import scala.language.implicitConversions

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
package object scraml {

  implicit def toScalaRaml(raml: Raml): ScalaRaml = ScalaRaml(raml)

}
