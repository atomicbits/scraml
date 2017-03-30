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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.{ ActionFunctionResult, ActionGenerator, GenerationAggr }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ ResourceClassDefinition, SourceFile }
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.generator.util.CleanNameUtil

/**
  * Created by peter on 1/03/17.
  */
object ResourceClassGenerator extends SourceGenerator {

  implicit val platform: Platform = JavaJackson

  def generate(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition): GenerationAggr = {

    val classDefinition        = generateClassDefinition(resourceClassDefinition)
    val resourceClassReference = resourceClassDefinition.classReference

    val dslFields = resourceClassDefinition.childResourceDefinitions.map(generateResourceDslField)

    val ActionFunctionResult(actionImports, actionFunctions, headerPathSourceDefs) =
      ActionGenerator(JavaActionCodeGenerator).generateActionFunctions(resourceClassDefinition)

    val imports = platform.importStatements(resourceClassReference, actionImports)

    val resourceConstructors = generateResourceConstructors(resourceClassDefinition)

    val addHeaderConstructorArgs = generateAddHeaderConstructorArguments(resourceClassDefinition)
    val setHeaderConstructorArgs = generateSetHeaderConstructorArguments(resourceClassDefinition)

    val className      = resourceClassReference.name
    val classNameCamel = CleanNameUtil.camelCased(className)

    val sourcecode =
      s"""
           package ${resourceClassReference.packageName};

           import io.atomicbits.scraml.jdsl.*;
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
    val cleanUrlSegment  = JavaJackson.escapeJavaKeyword(CleanNameTools.cleanMethodName(resource.urlSegment))
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
