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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.{ ActionGenerator, DslSourceRewriter, GenerationAggr, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.ResourceClassDefinition
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 14/01/17.
  */
case class ResourceClassGenerator(scalaPlay: ScalaPlay) extends SourceGenerator {

  implicit val platform: ScalaPlay = scalaPlay

  def generate(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr = {

    val classDefinition        = generateClassDefinition(resourceClassDefinition)
    val resourceClassReference = resourceClassDefinition.classReference

    val dslFields = resourceClassDefinition.childResourceDefinitions.map(generateResourceDslField)

    val SourceCodeFragment(actionImports, actionFunctions, headerPathSourceDefs) =
      ActionGenerator(ScalaActionCodeGenerator(platform)).generateActionFunctions(resourceClassDefinition)

    val imports = platform.importStatements(resourceClassReference, actionImports)

    val addHeaderConstructorArgs = generateAddHeaderConstructorArguments(resourceClassDefinition)
    val setHeaderConstructorArgs = generateSetHeaderConstructorArguments(resourceClassDefinition)

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val sourcecode =
      s"""
           package ${resourceClassReference.packageName}

           import $dslBasePackage._

           import play.api.libs.json._
           import java.io._

           ${imports.mkString("\n")}

           $classDefinition

           /**
            * addHeaders will add the given headers and append the values for existing headers.
            */
           def addHeaders(newHeaders: (String, String)*) =
             new ${resourceClassReference.name}$addHeaderConstructorArgs

           /**
            * setHeaders will add the given headers and set (overwrite) the values for existing headers.
            */
           def setHeaders(newHeaders: (String, String)*) =
             new ${resourceClassReference.name}$setHeaderConstructorArgs

           ${dslFields.mkString("\n\n")}

           ${actionFunctions.mkString("\n\n")}

           }
       """

    generationAggr
      .addSourceDefinitions(headerPathSourceDefs)
      .addSourceFile(SourceFile(filePath = resourceClassReference.toFilePath, content = sourcecode))
  }

  def generateClassDefinition(resourceClassDefinition: ResourceClassDefinition): String = {

    val resource         = resourceClassDefinition.resource
    val resourceClassRef = resourceClassDefinition.classReference

    resourceClassDefinition.urlParamClassPointer().map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name

        s"""class ${resourceClassRef.name}(_value: $urlParamClassName, _req: RequestBuilder) extends ParamSegment[$urlParamClassName](_value, _req) { """
      case None =>
        s"""class ${resourceClassRef.name}(private val _req: RequestBuilder) extends PlainSegment("${resource.urlSegment}", _req) { """
    }
  }

  def generateResourceDslField(resourceClassDefinition: ResourceClassDefinition): String = {

    val resource         = resourceClassDefinition.resource
    val cleanUrlSegment  = platform.escapeScalaKeyword(CleanNameTools.cleanMethodName(resource.urlSegment))
    val resourceClassRef = resourceClassDefinition.classReference

    resourceClassDefinition.urlParamClassPointer().map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name
        s"""def $cleanUrlSegment(value: $urlParamClassName) = new ${resourceClassRef.fullyQualifiedName}(value, _requestBuilder.withAddedPathSegment(value))"""
      case None =>
        s"""def $cleanUrlSegment = new ${resourceClassRef.fullyQualifiedName}(_requestBuilder.withAddedPathSegment("${resource.urlSegment}"))"""
    }
  }

  def generateAddHeaderConstructorArguments(resourceClassDefinition: ResourceClassDefinition): String =
    resourceClassDefinition.urlParamClassPointer() match {
      case Some(parameter) => "(_value, _requestBuilder.withAddedHeaders(newHeaders: _*))"
      case None            => "(_requestBuilder.withAddedHeaders(newHeaders: _*))"
    }

  def generateSetHeaderConstructorArguments(resourceClassDefinition: ResourceClassDefinition): String =
    resourceClassDefinition.urlParamClassPointer() match {
      case Some(parameter) => "(_value, _requestBuilder.withSetHeaders(newHeaders: _*))"
      case None            => "(_requestBuilder.withSetHeaders(newHeaders: _*))"
    }

}
