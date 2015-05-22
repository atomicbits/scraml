package io.atomicbits.scraml.parser

import org.raml.model.Raml
import org.raml.parser.rule.ValidationResult
import org.raml.parser.visitor.{RamlDocumentBuilder, RamlValidationService}

import scala.collection.JavaConverters._

/**
 * The RAML service is a wrapper around the raml-java-parser, see: https://github.com/raml-org/raml-java-parser
 *
 * Created by peter on 12/05/15, Atomic BITS bvba (http://atomicbits.io).
 */
object RamlParser {

  def buildRaml(path: String): Raml = {
    new RamlDocumentBuilder().build(path)
  }

  def validateRaml(resourceLocation: String): List[ValidationResult] = {
    RamlValidationService.createDefault.validate(resourceLocation).asScala.toList
  }

  def printValidations(validations: List[ValidationResult]): String = {

    def printValidationResult(validationResult: ValidationResult): String = {
      s"""
         |${validationResult.getMessage}
          |level: ${validationResult.getLevel}, line: ${validationResult.getLine} from: ${validationResult.getStartColumn} to: ${validationResult.getEndColumn}
       """.stripMargin
    }

    validations.map(printValidationResult).mkString("\n- - - ", "- - - ", "- - - \n")

  }

}
