/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

import java.io.File

import io.atomicbits.scraml.generator.lookup.{SchemaLookupParser, SchemaLookup}
import io.atomicbits.scraml.generator.model.RichResource
import io.atomicbits.scraml.jsonschemaparser.model.Schema
import io.atomicbits.scraml.jsonschemaparser.JsonSchemaParser
import org.raml.parser.rule.ValidationResult

import io.atomicbits.scraml.parser._
import io.atomicbits.scraml.parser.model._


object ScramlGenerator {


  def generate(ramlApiPath: String, apiPackageName: String, apiClassName: String): Seq[(File, String)] = {

    // Validate RAML spec
    println(s"Running RAML validation on $ramlApiPath: ")
    val validationResults: List[ValidationResult] = RamlParser.validateRaml(ramlApiPath)

    if (validationResults.nonEmpty) {
      sys.error(
        s"""
           |Invalid RAML specification:
           |
           |${RamlParser.printValidations(validationResults)}
            |
            |""".stripMargin
      )
    }
    println("RAML model is valid")

    // Generate the RAML model
    println("Running RAML model generation")
    val raml: Raml = RamlParser.buildRaml(ramlApiPath).asScala
    println(s"RAML model generated")

    val schemas: Map[String, Schema] = JsonSchemaParser.parse(raml.schemas)
    val schemaLookup: SchemaLookup = SchemaLookupParser.parse(schemas)
    println(s"Schema Lookup generated")

    val caseClasses: Seq[ClassRep] = CaseClassGenerator.generateCaseClasses(schemaLookup)
    println(s"Case classes generated")

    val packageBasePath = ramlApiPath.split(".").toList

    val resources: Seq[ClassRep] =
      ResourceClassGenerator.generateResourceClasses(
        apiClassName,
        apiPackageName.split(".").toList,
        raml.resources.map(RichResource(_, packageBasePath, schemaLookup))
      )
    println(s"Resources DSL generated")

    // ToDo: process enumerations
    //    val enumObjects = CaseClassGenerator.generateEnumerationObjects(schemaLookup, c)


    val pathParts: Array[String] = apiPackageName.split('.')
    // It is important to start the foldLeft aggregate with new File(pathParts.head). If you start with new File("") and
    // start iterating from pathParts instead of pathParts.tail, then you'll get the wrong file path on Windows machines.
    val dir = pathParts.tail.foldLeft(new File(pathParts.head))((file, pathPart) => new File(file, pathPart))
    val file = new File(dir, s"$apiClassName.scala")

    (caseClasses ++ resources) map classRepToFileAndContent
  }

  private def classRepToFileAndContent(classRep: ClassRep): (File, String) = {

    val pathParts = classRep.packageParts
    // It is important to start the foldLeft aggregate with new File(pathParts.head). If you start with new File("") and
    // start iterating from pathParts instead of pathParts.tail, then you'll get the wrong file path on Windows machines.
    val dir = pathParts.tail.foldLeft(new File(pathParts.head))((file, pathPart) => new File(file, pathPart))
    val file = new File(dir, s"${classRep.name}.scala")

    (file, classRep.content.getOrElse(s"No content generated for class ${classRep.fullyQualifiedName}"))

  }

}
