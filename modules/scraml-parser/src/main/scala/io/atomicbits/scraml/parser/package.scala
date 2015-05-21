package io.atomicbits.scraml

import org.raml.model.Raml

import scala.language.implicitConversions

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
package object parser {

  implicit def toScalaRaml(raml: Raml): ScalaRaml = ScalaRaml(raml)

}
