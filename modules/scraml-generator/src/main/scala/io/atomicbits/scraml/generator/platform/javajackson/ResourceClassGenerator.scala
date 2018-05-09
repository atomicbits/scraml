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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.{ ActionGenerator, DslSourceRewriter, GenerationAggr, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.ResourceClassDefinition
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 1/03/17.
  */
case class ResourceClassGenerator(javaJackson: CommonJavaJacksonPlatform) extends SourceGenerator {

  implicit val platform: CommonJavaJacksonPlatform = javaJackson

  def generate(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr = {

    val classDefinition        = generateClassDefinition(resourceClassDefinition)
    val resourceClassReference = resourceClassDefinition.classReference

    val dslFields = resourceClassDefinition.childResourceDefinitions.map(generateResourceDslField)

    val SourceCodeFragment(actionImports, actionFunctions, headerPathSourceDefs) =
      ActionGenerator(new JavaActionCodeGenerator(platform)).generateActionFunctions(resourceClassDefinition)

    val imports = platform.importStatements(resourceClassReference, actionImports)

    val resourceConstructors = generateResourceConstructors(resourceClassDefinition)

    val addHeaderConstructorArgs = generateAddHeaderConstructorArguments(resourceClassDefinition)
    val setHeaderConstructorArgs = generateSetHeaderConstructorArguments(resourceClassDefinition)

    val className      = resourceClassReference.name
    val classNameCamel = CleanNameUtil.camelCased(className)

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val sourcecode =
      s"""
           package ${resourceClassReference.packageName};

           import $dslBasePackage.*;
           import java.util.*;
           import java.util.concurrent.CompletableFuture;
           import java.io.*;

           ${imports.mkString("\n")}

           $classDefinition

             public $className(){
             }

             ${resourceConstructors.mkString("\n\n")}

             public $className addHeader(String key, String value) {
               $className $classNameCamel = new $className(getRequestBuilder(), true);
               $classNameCamel._requestBuilder.addHeader(key, value);
               return $classNameCamel;
             }

             public $className setHeader(String key, String value) {
               $className $classNameCamel = new $className(getRequestBuilder(), true);
               $classNameCamel._requestBuilder.setHeader(key, value);
               return $classNameCamel;
             }

             ${dslFields.mkString("\n\n")}

             ${actionFunctions.mkString("\n\n")}

           }
         """

    generationAggr
      .addSourceDefinitions(headerPathSourceDefs)
      .addSourceFile(SourceFile(filePath = resourceClassReference.toFilePath, content = sourcecode))
  }

  def generateResourceConstructors(resourceClassDefinition: ResourceClassDefinition): List[String] = {

    val resourceClassReference = resourceClassDefinition.classReference
    val resource               = resourceClassDefinition.resource

    resourceClassDefinition.urlParamClassPointer().map(_.native) match {
      case Some(paramClassReference) =>
        List(
          s"""
               public ${resourceClassReference.name}(${paramClassReference.name} value, RequestBuilder requestBuilder) {
                 super(value, requestBuilder);
               }
             """,
          s"""
               public ${resourceClassReference.name}(RequestBuilder requestBuilder, Boolean noPath) {
                 super(requestBuilder);
               }
             """
        )
      case None =>
        List(
          s"""
               public ${resourceClassReference.name}(RequestBuilder requestBuilder) {
                 super("${resource.urlSegment}", requestBuilder);
               }
             """,
          s"""
             public ${resourceClassReference.name}(RequestBuilder requestBuilder, Boolean noPath) {
               super(requestBuilder);
             }
           """
        )
    }
  }

  def generateClassDefinition(resourceClassDefinition: ResourceClassDefinition): String = {

    val resource         = resourceClassDefinition.resource
    val resourceClassRef = resourceClassDefinition.classReference

    resourceClassDefinition.urlParamClassPointer().map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name
        s"""public class ${resourceClassRef.name} extends ParamSegment<$urlParamClassName> { """
      case None =>
        s"""public class ${resourceClassRef.name} extends PlainSegment {"""
    }
  }

  def generateResourceDslField(resourceClassDefinition: ResourceClassDefinition): String = {

    val resource         = resourceClassDefinition.resource
    val cleanUrlSegment  = platform.escapeJavaKeyword(CleanNameTools.cleanMethodName(resource.urlSegment))
    val resourceClassRef = resourceClassDefinition.classReference

    resourceClassDefinition.urlParamClassPointer().map(_.native) match {
      case Some(urlParamClassReference) =>
        val urlParamClassName = urlParamClassReference.name
        s"""
             public ${resourceClassRef.fullyQualifiedName} $cleanUrlSegment($urlParamClassName value) {
               return new ${resourceClassRef.fullyQualifiedName}(value, this.getRequestBuilder());
             }
            """
      case None =>
        s"""
              public ${resourceClassRef.fullyQualifiedName} $cleanUrlSegment =
                new ${resourceClassRef.fullyQualifiedName}(this.getRequestBuilder());
            """

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
