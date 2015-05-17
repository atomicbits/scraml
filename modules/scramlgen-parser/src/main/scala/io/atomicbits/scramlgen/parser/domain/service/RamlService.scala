package io.atomicbits.scramlgen.parser.domain.service

import java.io.InputStream
import org.raml.model.Raml
import org.raml.parser.rule.ValidationResult
import org.raml.parser.visitor.{RamlValidationService, RamlDocumentBuilder}
import scala.collection.JavaConverters._

/**
 * The RAML service is a wrapper around the raml-java-parser, see: https://github.com/raml-org/raml-java-parser
 *
 * Created by peter on 12/05/15, Atomic BITS bvba (http://atomicbits.io).
 */
object RamlService {

  def build(path: String): Raml = {
    new RamlDocumentBuilder().build(getInputStream(path), path)
  }

  def validateRaml(resourceLocation: String): List[ValidationResult] = {
    RamlValidationService.createDefault.validate(resourceLocation).asScala.toList
  }

  private def getInputStream(resourceLocation: String): InputStream = {
    Thread.currentThread.getContextClassLoader.getResourceAsStream(resourceLocation)
  }

}
