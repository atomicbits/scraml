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

import io.atomicbits.scraml.generator.model.{ClassRep, RichResource}
import ClassRep.ClassMap
import io.atomicbits.scraml.generator.lookup.{SchemaLookupParser, SchemaLookup}
import io.atomicbits.scraml.generator.model.RichResource
import io.atomicbits.scraml.jsonschemaparser.model.Schema
import io.atomicbits.scraml.jsonschemaparser.JsonSchemaParser
import org.raml.parser.rule.ValidationResult

import io.atomicbits.scraml.parser._
import io.atomicbits.scraml.parser.model._

import scala.collection.JavaConversions.mapAsJavaMap
import scala.language.postfixOps

import java.util.{Map => JMap}


object ScramlGenerator {


  def generate(ramlApiPath: String, apiPackageName: String, apiClassName: String): JMap[String, String] = {
    val tupleList =
      generateClassReps(ramlApiPath, apiPackageName, apiClassName)
        .collect { case clRep if clRep.content.isDefined => clRep }
        .map(classRepToFilePathAndContent)

    mapAsJavaMap[String, String](tupleList.toMap)
  }

  private[generator] def generateClassReps(ramlApiPath: String, apiPackageName: String, apiClassName: String): Seq[ClassRep] = {
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

    val packageBasePath = apiPackageName.split('.').toList

    val classMap: ClassMap = schemaLookup.classReps.values.map(classRep => classRep.classRef -> classRep).toMap
    val richResources = raml.resources.map(RichResource(_, packageBasePath, schemaLookup))

    // Here's the actual code generation
    val caseClasses: Seq[ClassRep] = CaseClassGenerator.generateCaseClasses(classMap)
    println(s"Case classes generated")
    val resources: Seq[ClassRep] = ResourceClassGenerator.generateResourceClasses(apiClassName, packageBasePath, richResources)
    println(s"Resources DSL generated")

    // ToDo: process enumerations
    //    val enumObjects = CaseClassGenerator.generateEnumerationObjects(schemaLookup, c)

    caseClasses ++ resources
  }

  private def classRepToFilePathAndContent(classRep: ClassRep): (String, String) = {

    val pathParts = classRep.packageParts
    // It is important to start the foldLeft aggregate with new File(pathParts.head). If you start with new File("") and
    // start iterating from pathParts instead of pathParts.tail, then you'll get the wrong file path on Windows machines.
    //    val dir = pathParts.tail.foldLeft(new File(pathParts.head))((file, pathPart) => new File(file, pathPart))
    //    val file = new File(dir, s"${classRep.name}.scala")

    val filePath = s"${pathParts.mkString(File.separator)}${File.separator}${classRep.name}.scala"

    (filePath, classRep.content.getOrElse(s"No content generated for class ${classRep.fullyQualifiedName}"))

  }

}
