/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

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
import io.atomicbits.scraml.generator.platform.androidjavajackson.AndroidJavaJackson
import io.atomicbits.scraml.generator.platform.typescript.TypeScript

/**
  * The main Scraml generator class.
  * This class is thread-safe and may be used by mutiple threads simultaneously.
  */
object ScramlGenerator {

  val JAVA_JACKSON: String         = "JavaJackson".toLowerCase
  val SCALA_PLAY: String           = "ScalaPlay".toLowerCase
  val ANDROID_JAVA_JACKSON: String = "AndroidJavaJackson".toLowerCase
  val TYPESCRIPT: String           = "TypeScript".toLowerCase
  val OSX_SWIFT: String            = "OsxSwift".toLowerCase
  val PYTHON: String               = "Python".toLowerCase
  val CSHARP: String               = "C#".toLowerCase

  /**
    * This is (and must be) a Java-friendly interface!
    */
  def generateScramlCode(platform: String,
                         ramlApiPath: String,
                         apiPackageName: String,
                         apiClassName: String,
                         licenseKey: String,
                         thirdPartyClassHeader: String,
                         singleTargeSourceFileName: String): JMap[String, String] =
    platform.toLowerCase match {
      case JAVA_JACKSON =>
        generateFor(
          JavaJackson(packageNameToPackagParts(apiPackageName)),
          ramlApiPath,
          apiClassName,
          licenseKey,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case SCALA_PLAY =>
        generateFor(
          ScalaPlay(packageNameToPackagParts(apiPackageName)),
          ramlApiPath,
          apiClassName,
          licenseKey,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case ANDROID_JAVA_JACKSON =>
        generateFor(
          AndroidJavaJackson(packageNameToPackagParts(apiPackageName)),
          ramlApiPath,
          apiClassName,
          licenseKey,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case TYPESCRIPT => {
        generateFor(
          TypeScript(),
          ramlApiPath,
          apiClassName,
          licenseKey,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      }
      case OSX_SWIFT => sys.error(s"There is no iOS support yet.")
      case PYTHON    => sys.error(s"There is no Python support yet.")
      case CSHARP    => sys.error(s"There is no C# support yet.")
      case unknown   => sys.error(s"Unknown platform: $unknown")
    }

  private[generator] def generateFor(platform: Platform,
                                     ramlApiPath: String,
                                     apiClassName: String,
                                     scramlLicenseKey: String,
                                     thirdPartyClassHeader: String,
                                     singleTargeSourceFileName: String): JMap[String, String] = {

    println(s"Generating client for platform ${platform.name}.")

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
        .map(DslSourceRewriter.rewrite)

    val singleSourceFile =
      Option(singleTargeSourceFileName).collect {
        case name if name.nonEmpty => name
      }
    val combinedSources = platform.mapSourceFiles((sources ++ dslSources).toSet, singleSourceFile)

    val tupleList =
      combinedSources
        .map(addLicenseAndFormat(_, platform, licenseHeader))
        .map(sourceFile => (sourceFile.filePath.toString, sourceFile.content))

    mapAsJavaMap[String, String](tupleList.toMap)
  }

  def packageNameToPackagParts(packageName: String): List[String] = packageName.split('.').toList.filter(!_.isEmpty)

  private[generator] def buildGenerationAggr(ramlApiPath: String, apiClassName: String, thePlatform: Platform): GenerationAggr = {

    val charsetName = "UTF-8" // ToDo: Get the charset as input parameter.

    // Generate the RAML model
    println("Running RAML model generation")
    val tryRaml: Try[Raml] = RamlParser(ramlApiPath, charsetName).parse
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
      case ScalaPlay(_)          => Try(ScalaFormatter.format(content)).getOrElse(content)
      case JavaJackson(_)        => Try(JavaFormatter.format(content)).getOrElse(content)
      case AndroidJavaJackson(_) => Try(JavaFormatter.format(content)).getOrElse(content)
      case unknown               => content
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
        //        s"""$thirdPartyHeader
        //           |This API client was generated by Scraml (http://scraml.io) for ${licenseData.owner}
        //           |on ${LocalDate.now().format(DateTimeFormatter.ISO_DATE)} using license ${licenseData.licenseId}.
        //         """.stripMargin.trim
        thirdPartyHeader
      } getOrElse agplClassHeader

    classHeader.split('\n').map(line => s" * ${line.trim}").mkString("/**\n", "\n", "\n */")
  }

}
