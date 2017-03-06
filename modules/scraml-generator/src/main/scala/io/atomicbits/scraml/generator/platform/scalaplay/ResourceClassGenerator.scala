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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.{ ActionFunctionResult, ActionGenerator, GenerationAggr }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ ResourceClassDefinition, SourceFile }
import io.atomicbits.scraml.generator.platform.Platform._

/**
  * Created by peter on 14/01/17.
  */
object ResourceClassGenerator extends SourceGenerator {

  implicit val platform: Platform = ScalaPlay

  def generate(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr = {

    val classDefinition        = generateClassDefinition(resourceClassDefinition)
    val resourceClassReference = resourceClassDefinition.classReference

    val dslFields = resourceClassDefinition.childResourceDefinitions.map(generateResourceDslField)

    val ActionFunctionResult(actionImports, actionFunctions, headerPathSourceDefs) =
      ActionGenerator(ScalaActionCodeGenerator, generationAggr).generateActionFunctions(resourceClassDefinition)

    val imports = platform.importStatements(resourceClassReference, actionImports)

    val addHeaderConstructorArgs = generateAddHeaderConstructorArguments(resourceClassDefinition)
    val setHeaderConstructorArgs = generateSetHeaderConstructorArguments(resourceClassDefinition)

    val sourcecode =
      s"""
           package ${resourceClassReference.packageName}

           import io.atomicbits.scraml.dsl._

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

    resourceClassDefinition.urlParamClassPointer.map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name

        s"""class ${resourceClassRef.name}(_value: $urlParamClassName, _req: RequestBuilder) extends ParamSegment[$urlParamClassName](_value, _req) { """
      case None =>
        s"""class ${resourceClassRef.name}(private val _req: RequestBuilder) extends PlainSegment("${resource.urlSegment}", _req) { """
    }
  }

  def generateResourceDslField(resourceClassDefinition: ResourceClassDefinition): String = {

    val resource         = resourceClassDefinition.resource
    val cleanUrlSegment  = ScalaPlay.escapeScalaKeyword(CleanNameTools.cleanMethodName(resource.urlSegment))
    val resourceClassRef = resourceClassDefinition.classReference

    resourceClassDefinition.urlParamClassPointer.map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name
        s"""def $cleanUrlSegment(value: $urlParamClassName) = new ${resourceClassRef.fullyQualifiedName}(value, _requestBuilder.withAddedPathSegment(value))"""
      case None =>
        s"""def $cleanUrlSegment = new ${resourceClassRef.fullyQualifiedName}(_requestBuilder.withAddedPathSegment("${resource.urlSegment}"))"""
    }
  }

  def generateAddHeaderConstructorArguments(resourceClassDefinition: ResourceClassDefinition): String =
    resourceClassDefinition.urlParamClassPointer match {
      case Some(parameter) => "(_value, _requestBuilder.withAddedHeaders(newHeaders: _*))"
      case None            => "(_requestBuilder.withAddedHeaders(newHeaders: _*))"
    }

  def generateSetHeaderConstructorArguments(resourceClassDefinition: ResourceClassDefinition): String =
    resourceClassDefinition.urlParamClassPointer match {
      case Some(parameter) => "(_value, _requestBuilder.withSetHeaders(newHeaders: _*))"
      case None            => "(_requestBuilder.withSetHeaders(newHeaders: _*))"
    }

}
