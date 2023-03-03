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

import scala.jdk.CollectionConverters._
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
import io.atomicbits.scraml.generator.platform.htmldoc.HtmlDoc
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
  val HTML_DOC: String             = "HtmlDoc".toLowerCase
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
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case SCALA_PLAY =>
        generateFor(
          ScalaPlay(packageNameToPackagParts(apiPackageName)),
          ramlApiPath,
          apiClassName,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case ANDROID_JAVA_JACKSON =>
        generateFor(
          AndroidJavaJackson(packageNameToPackagParts(apiPackageName)),
          ramlApiPath,
          apiClassName,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      case TYPESCRIPT => {
        generateFor(
          TypeScript(),
          ramlApiPath,
          apiClassName,
          thirdPartyClassHeader,
          singleTargeSourceFileName
        )
      }
      case HTML_DOC => {
        generateFor(
          HtmlDoc,
          ramlApiPath,
          apiClassName,
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
                                     thirdPartyClassHeader: String,
                                     singleTargeSourceFileName: String): JMap[String, String] = {

    println(s"Generating client for platform ${platform.name}.")

    implicit val thePlatform = platform

    // We transform the scramlLicenseKey and thirdPartyClassHeader fields to optionals here. We don't take them as optional parameters
    // higher up the chain to maintain a Java-compatible interface for the ScramlGenerator.
    val classHeader: Option[String] =
      if (thirdPartyClassHeader == null || thirdPartyClassHeader.isEmpty) None
      else Some(thirdPartyClassHeader)

    val licenseHeader: String = deferLicenseHeader(classHeader)

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

    tupleList.toMap.asJava
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
      case _                     => content
    }
    sourceFile.copy(content = formattedContent)
  }

  private def deferLicenseHeader(thirdPartyLicenseHeader: Option[String],
                                 commentPrefix: String = "  * ",
                                 headerPrefix: String  = "/**\n",
                                 headerSuffix: String  = "\n  */"): String = {
    thirdPartyLicenseHeader.map { licenseHeader =>
      licenseHeader.split('\n').map(line => s"$commentPrefix${line.trim}").mkString(headerPrefix, "\n", headerSuffix)
    } getOrElse ""
  }

}
