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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.atomicbits.scraml.generator.oldcodegen.{
  CaseClassGenerator,
  JavaResourceClassGenerator,
  PojoGenerator,
  ScalaResourceClassGenerator
}
import io.atomicbits.scraml.generator.formatting.JavaFormatter
import io.atomicbits.scraml.generator.oldmodel._
import ClassRep.ClassMap

import scala.collection.JavaConversions.mapAsJavaMap
import scala.language.postfixOps
import java.util.{ Map => JMap }

import io.atomicbits.scraml.generator.TypeClassRepAssembler.CanonicalMap
import io.atomicbits.scraml.generator.license.{ LicenseData, LicenseVerifier }
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.model.{ NativeId, Raml, RootId }
import io.atomicbits.scraml.ramlparser.parser.{ RamlParseException, RamlParser }

import scala.util.{ Failure, Success, Try }
import scalariform.formatter.ScalaFormatter
import scalariform.formatter.preferences._
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.platform.javajackson.JavaJackson
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import io.atomicbits.scraml.generator.typemodel.{ SourceDefinition, SourceFile }
import io.atomicbits.scraml.generator.codegen.GenerationAggr

/**
  * The main Scraml generator class.
  * This class is thread-safe and may be used by mutiple threads simultaneously.
  */
object ScramlGenerator {

  def generateScalaCode(ramlApiPath: String,
                        apiPackageName: String,
                        apiClassName: String,
                        licenseKey: String,
                        thirdPartyClassHeader: String): JMap[String, String] =
    generateFor(Scala, ScalaPlay, ramlApiPath, apiPackageName, apiClassName, licenseKey, thirdPartyClassHeader)

  def generateJavaCode(ramlApiPath: String,
                       apiPackageName: String,
                       apiClassName: String,
                       licenseKey: String,
                       thirdPartyClassHeader: String): JMap[String, String] =
    generateFor(Java, JavaJackson, ramlApiPath, apiPackageName, apiClassName, licenseKey, thirdPartyClassHeader)

  private[generator] def generateFor(language: Language,
                                     platform: Platform,
                                     ramlApiPath: String,
                                     apiPackageName: String,
                                     apiClassName: String,
                                     scramlLicenseKey: String,
                                     thirdPartyClassHeader: String): JMap[String, String] = {

    println(s"Generating $language client.")

    // We transform the scramlLicenseKey and thirdPartyClassHeader fields to optionals here. We don't take them as optional parameters
    // higher up the chain to maintain a Java-compatible interface for the ScramlGenerator.
    val licenseKey: Option[String] =
      if (scramlLicenseKey == null || scramlLicenseKey.isEmpty) None
      else Some(scramlLicenseKey)
    val classHeader: Option[String] =
      if (thirdPartyClassHeader == null || thirdPartyClassHeader.isEmpty) None
      else Some(thirdPartyClassHeader)

    val licenseData: Option[LicenseData] = licenseKey.flatMap(LicenseVerifier.validateLicense)

    val licenseHeader: String = deferLicenseHeader(licenseData, classHeader)

    val tupleList =
      generateClassReps(ramlApiPath, apiPackageName, apiClassName, language, platform)
        .collect { case clRep if clRep.content.isDefined => clRep }
        .map(addLicenseAndFormat(_, language, licenseHeader))
        .map(classRepToFilePathAndContent(_, language))

    mapAsJavaMap[String, String](tupleList.toMap)
  }

  private[generator] def generateClassReps(ramlApiPath: String,
                                           apiPackageName: String,
                                           apiClassName: String,
                                           language: Language,
                                           thePlatform: Platform): Seq[ClassRep] = {

    val packageBasePath = apiPackageName.split('.').toList.filter(!_.isEmpty)
    val charsetName     = "UTF-8" // ToDo: Get the charset as input parameter.

    // Generate the RAML model
    println("Running RAML model generation")
    val tryRaml: Try[Raml] = RamlParser(ramlApiPath, charsetName, packageBasePath).parse
    val raml = tryRaml match {
      case Success(rml) => rml
      case Failure(rpe: RamlParseException) =>
        sys.error(s"""
             |- - - Invalid RAML model: - - -
             |${rpe.messages.mkString("\n")}
             |- - - - - - - - - - - - - - - -
           """.stripMargin)
      case Failure(e) =>
        sys.error(s"""
             |- - - Unexpected parse error: - - -
             |${e.printStackTrace()}
             |- - - - - - - - - - - - - - - - - -
           """.stripMargin)
    }
    println(s"RAML model generated")

    // We need an implicit reference to the language we're generating the DSL for.
    implicit val lang     = language
    implicit val platform = thePlatform

    val host    = packageBasePath.take(2).reverse.mkString(".")
    val urlPath = packageBasePath.drop(2).mkString("/")
    val nativeToRootId: NativeId => RootId = nativeId => RootId(s"http://$host/$urlPath/${nativeId.id}.json")

    val defaultBasePath: List[String] = packageBasePath

    val (ramlExp, canonicalLookup) = raml.collectCanonicals(defaultBasePath) // new version

    val generationAggregator: GenerationAggr =
      GenerationAggr(apiName = apiClassName, apiBasePackage = packageBasePath, raml = ramlExp, canonicalToMap = canonicalLookup.map)
    // val sources: Seq[SourceFile] = generationAggregator.generate.sourceFilesGenerated
    // ToDo... return the sourceFiles and change the method signature's return type

    // Old version from here
    val ramlExpanded    = raml.collectCanonicalTypes(defaultBasePath) // old version
    val typeLookupTable = ramlExpanded.canonicalMap.get

    val canonicalMap: CanonicalMap = new TypeClassRepAssembler(nativeToRootId).deduceClassReps(typeLookupTable)
    println(s"Type Lookup generated")

    val classMap: ClassMap = canonicalMap.values.map(classRep => classRep.classRef -> classRep).toMap
    val richResources      = ramlExpanded.resources.map(RichResource(_, packageBasePath, typeLookupTable, canonicalMap, nativeToRootId))

    language match {
      case Scala =>
        val caseClasses: Seq[ClassRep] = CaseClassGenerator.generateCaseClasses(classMap)
        println(s"Case classes generated")
        val resources: List[ClassRep] = ScalaResourceClassGenerator.generateResourceClasses(apiClassName, packageBasePath, richResources)
        println(s"Resources DSL generated")
        caseClasses ++ resources
      case Java =>
        val pojos: Seq[ClassRep] = PojoGenerator.generatePojos(classMap)
        println(s"POJOs generated")
        val resources: List[ClassRep] = JavaResourceClassGenerator.generateResourceClasses(apiClassName, packageBasePath, richResources)
        println(s"Resources DSL generated")
        pojos ++ resources
    }

  }

  private val formatSettings =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(IndentSpaces, 2)

  private def addLicenseAndFormat(classRep: ClassRep, language: Language, licenseHeader: String): ClassRep = {
    val content = s"$licenseHeader\n${classRep.content.get}"
    val formattedContent = language match {
      case Scala => Try(ScalaFormatter.format(content, formatSettings)).getOrElse(content)
      case Java  => Try(JavaFormatter.format(content)).getOrElse(content)
    }
    classRep.withContent(formattedContent)
  }

  private val agplClassHeader =
    s"""|All rights reserved. This program and the accompanying materials
        |are made available under the terms of the GNU Affero General Public License
        |(AGPL) version 3.0 which accompanies this distribution, and is available in
        |the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
        |
        |This library is distributed in the hope that it will be useful,
        |but WITHOUT ANY WARRANTY; without even the implied warranty of
        |MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
        |Affero General Public License for more details. """.stripMargin

  private def classRepToFilePathAndContent(classRep: ClassRep, language: Language): (String, String) = {

    val classReference = classRep.classRef
    val pathParts      = classReference.safePackageParts
    // It is important to start the foldLeft aggregate with new File(pathParts.head). If you start with new File("") and
    // start iterating from pathParts instead of pathParts.tail, then you'll get the wrong file path on Windows machines.
    //    val dir = pathParts.tail.foldLeft(new File(pathParts.head))((file, pathPart) => new File(file, pathPart))
    //    val file = new File(dir, s"${classRep.name}.scala")

    val extension = language match {
      case Scala => "scala"
      case Java  => "java"
    }

    val filePath = s"${pathParts.mkString(File.separator)}${File.separator}${classReference.name}.$extension"

    (filePath, classRep.content.getOrElse(s"No content generated for class ${classReference.fullyQualifiedName}"))
  }

  private def deferLicenseHeader(licenseKey: Option[LicenseData], thirdPartyLicenseHeader: Option[String]): String = {

    val classHeader =
      licenseKey.map { licenseData =>
        val thirdPartyHeader = thirdPartyLicenseHeader.getOrElse {
          s"""
             | All rights are reserved to ${licenseData.owner}.
           """.stripMargin.trim
        }
        s"""$thirdPartyHeader
           |This API client was generated by Scraml (http://scraml.io) for ${licenseData.owner}
           |on ${LocalDate.now().format(DateTimeFormatter.ISO_DATE)} using license ${licenseData.licenseId}.
         """.stripMargin.trim
      } getOrElse agplClassHeader

    classHeader.split('\n').map(line => s" * ${line.trim}").mkString("/**\n", "\n", "\n */")
  }

}
