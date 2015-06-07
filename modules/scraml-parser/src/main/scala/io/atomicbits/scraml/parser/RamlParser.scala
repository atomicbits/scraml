/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

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
