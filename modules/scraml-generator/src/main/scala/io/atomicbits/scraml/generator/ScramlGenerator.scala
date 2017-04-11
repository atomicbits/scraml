/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.atomicbits.scraml.generator.formatting.JavaFormatter

import scala.collection.JavaConversions.mapAsJavaMap
import scala.language.postfixOps
import java.util.{ Map => JMap }

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
import io.atomicbits.scraml.generator.typemodel.SourceFile
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
    generateFor(ScalaPlay, ramlApiPath, apiPackageName, apiClassName, licenseKey, thirdPartyClassHeader)

  def generateJavaCode(ramlApiPath: String,
                       apiPackageName: String,
                       apiClassName: String,
                       licenseKey: String,
                       thirdPartyClassHeader: String): JMap[String, String] =
    generateFor(JavaJackson, ramlApiPath, apiPackageName, apiClassName, licenseKey, thirdPartyClassHeader)

  private[generator] def generateFor(platform: Platform,
                                     ramlApiPath: String,
                                     apiPackageName: String,
                                     apiClassName: String,
                                     scramlLicenseKey: String,
                                     thirdPartyClassHeader: String): JMap[String, String] = {

    println(s"Generating $language client.")

    implicit val thePlatform = platform

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

    val generationAggregator = buildGenerationAggr(ramlApiPath, apiPackageName, apiClassName, platform)

    val sources: Seq[SourceFile] = generationAggregator.generate.sourceFilesGenerated

    val tupleList =
      sources
        .map(addLicenseAndFormat(_, platform, licenseHeader))
        .map(sourceFile => (sourceFile.filePath, sourceFile.content))

    mapAsJavaMap[String, String](tupleList.toMap)
  }

  private[generator] def buildGenerationAggr(ramlApiPath: String,
                                             apiPackageName: String,
                                             apiClassName: String,
                                             thePlatform: Platform): GenerationAggr = {

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

    val defaultBasePath: List[String] = packageBasePath

    val (ramlExp, canonicalLookup) = raml.collectCanonicals(defaultBasePath) // new version

    val generationAggregator: GenerationAggr =
      GenerationAggr(apiName = apiClassName, apiBasePackage = packageBasePath, raml = ramlExp, canonicalToMap = canonicalLookup.map)

    generationAggregator
  }

  private val formatSettings =
    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(IndentSpaces, 2)

  private def addLicenseAndFormat(sourceFile: SourceFile, platform: Platform, licenseHeader: String): SourceFile = {
    val content = s"$licenseHeader\n${sourceFile.content}"
    val formattedContent = platform match {
      case ScalaPlay   => Try(ScalaFormatter.format(content, formatSettings)).getOrElse(content)
      case JavaJackson => Try(JavaFormatter.format(content)).getOrElse(content)
    }
    sourceFile.copy(content = formattedContent)
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

//  private def classRepToFilePathAndContent(sourceFile: SourceFile): (String, String) = {

//    val classReference = sourceFile.classRef
//    val pathParts      = classReference.safePackageParts
  // It is important to start the foldLeft aggregate with new File(pathParts.head). If you start with new File("") and
  // start iterating from pathParts instead of pathParts.tail, then you'll get the wrong file path on Windows machines.
  //    val dir = pathParts.tail.foldLeft(new File(pathParts.head))((file, pathPart) => new File(file, pathPart))
  //    val file = new File(dir, s"${classRep.name}.scala")

//    val extension = language match {
//      case Scala => "scala"
//      case Java  => "java"
//    }

  // s"${pathParts.mkString(File.separator)}${File.separator}${classReference.name}.$extension"

//    (sourceFile.filePath, sourceFile.content)
//  }

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
