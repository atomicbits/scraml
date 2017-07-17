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

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import io.atomicbits.scraml.generator.formatting.{ JavaFormatter, ScalaFormatter }

import scala.collection.JavaConversions.mapAsJavaMap
import scala.language.postfixOps
import java.util.{ Map => JMap }

import io.atomicbits.scraml.generator.license.{ LicenseData, LicenseVerifier }
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.parser.{ RamlParseException, RamlParser, SourceFile }

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.platform.javajackson.JavaJackson
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import io.atomicbits.scraml.generator.codegen.{ DslSourceExtractor, DslSourceRewriter, GenerationAggr }

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
    generateFor(ScalaPlay(packageNameToPackagParts(apiPackageName)), ramlApiPath, apiClassName, licenseKey, thirdPartyClassHeader)

  def generateJavaCode(ramlApiPath: String,
                       apiPackageName: String,
                       apiClassName: String,
                       licenseKey: String,
                       thirdPartyClassHeader: String): JMap[String, String] =
    generateFor(JavaJackson(packageNameToPackagParts(apiPackageName)), ramlApiPath, apiClassName, licenseKey, thirdPartyClassHeader)

  private[generator] def generateFor(platform: Platform,
                                     ramlApiPath: String,
                                     apiClassName: String,
                                     scramlLicenseKey: String,
                                     thirdPartyClassHeader: String): JMap[String, String] = {

    println(s"Generating $platform client.")

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

    val generationAggregator = buildGenerationAggr(ramlApiPath, apiClassName, platform)

    val sources: Seq[SourceFile] = generationAggregator.generate.sourceFilesGenerated

    val dslSources: Set[SourceFile] =
      DslSourceExtractor
        .extract()
        .getOrElse(sys.error(s"Could not read the DSL source files!"))
        .map(DslSourceRewriter.rewrite)

    val tupleList =
      (sources ++ dslSources)
        .map(addLicenseAndFormat(_, platform, licenseHeader))
        .map(sourceFile => (sourceFile.filePath.toString, sourceFile.content))

    mapAsJavaMap[String, String](tupleList.toMap)
  }

  def packageNameToPackagParts(packageName: String): List[String] = packageName.split('.').toList.filter(!_.isEmpty)

  private[generator] def buildGenerationAggr(ramlApiPath: String, apiClassName: String, thePlatform: Platform): GenerationAggr = {

    val charsetName = "UTF-8" // ToDo: Get the charset as input parameter.

    // Generate the RAML model
    println("Running RAML model generation")
    val tryRaml: Try[Raml] = RamlParser(ramlApiPath, charsetName, thePlatform.apiBasePackageParts).parse
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

    val host    = thePlatform.apiBasePackageParts.take(2).reverse.mkString(".")
    val urlPath = thePlatform.apiBasePackageParts.drop(2).mkString("/")

    val defaultBasePath: List[String] = thePlatform.apiBasePackageParts

    val (ramlExp, canonicalLookup) = raml.collectCanonicals(defaultBasePath) // new version

    val generationAggregator: GenerationAggr =
      GenerationAggr(apiName        = apiClassName,
                     apiBasePackage = thePlatform.apiBasePackageParts,
                     raml           = ramlExp,
                     canonicalToMap = canonicalLookup.map)

    generationAggregator
  }

  private def addLicenseAndFormat(sourceFile: SourceFile, platform: Platform, licenseHeader: String): SourceFile = {
    val content = s"$licenseHeader\n${sourceFile.content}"
    val formattedContent = platform match {
      case ScalaPlay(_)   => Try(ScalaFormatter.format(content)).getOrElse(content)
      case JavaJackson(_) => Try(JavaFormatter.format(content)).getOrElse(content)
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
