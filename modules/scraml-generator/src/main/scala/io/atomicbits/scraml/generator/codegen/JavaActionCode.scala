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

package io.atomicbits.scraml.generator.codegen

import java.util.Locale

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 30/09/15.
 */
object JavaActionCode extends ActionCode {

  implicit val language: Language = Java


  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String = {
    s"""public ${headerSegment.classRef.fullyQualifiedName} $contentHeaderMethodName =
          new ${headerSegment.classRef.fullyQualifiedName}(this.getRequestBuilder());"""
  }


  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String = {
    s"""
         package ${headerSegmentClassRef.packageName};

         import io.atomicbits.scraml.jdsl.*;
         import java.util.*;
         import java.util.concurrent.CompletableFuture;
         import java.io.*;

         ${imports.mkString(";\n")};


         public class ${headerSegmentClassRef.name} extends HeaderSegment {

           public ${headerSegmentClassRef.name}(RequestBuilder requestBuilder) {
             super(requestBuilder);
           }

           ${methods.mkString("\n")}

         }
       """
  }


  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = {
    parameters map { parameterDef =>
      val (field, classRef) = parameterDef
      s"${classRef.classDefinitionJava} $field"
    }
  }


  def bodyTypes(action: RichAction): List[Option[ClassPointer]] =
    action.selectedContentType match {
      case StringContentType(contentTypeHeader)          => List(Some(StringClassReference()))
      case JsonContentType(contentTypeHeader)            => List(Some(StringClassReference()))
      case TypedContentType(contentTypeHeader, classRef) => List(Some(StringClassReference()), Some(classRef))
      case BinaryContentType(contentTypeHeader)          =>
        List(
          Some(StringClassReference()),
          Some(FileClassReference()),
          Some(InputStreamClassReference()),
          Some(JavaArray(name = "byte", packageParts = List("java", "lang"), predef = true))
        )
      case AnyContentType(contentTypeHeader)             =>
        List(
          None,
          Some(StringClassReference()),
          Some(FileClassReference()),
          Some(InputStreamClassReference()),
          Some(JavaArray(name = "byte", packageParts = List("java", "lang"), predef = true))
        )
      case NoContentType                                 => List(None)
      case x                                             => List(Some(StringClassReference()))
    }


  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String = {
    val bodyType = optBodyType.map(_.classDefinitionJava).getOrElse("String")
    responseType match {
      case BinaryResponseType(acceptHeader)          => s"BinaryMethodSegment<$bodyType>"
      case JsonResponseType(acceptHeader)            => s"StringMethodSegment<$bodyType>"
      case TypedResponseType(acceptHeader, classPtr) => s"TypeMethodSegment<$bodyType, ${classPtr.classDefinitionJava}>"
      case x                                         => s"StringMethodSegment<$bodyType>"
    }
  }


  def responseClassDefinition(responseType: ResponseType): String = {
    responseType match {
      case BinaryResponseType(acceptHeader)          => "CompletableFuture<Response<BinaryData>>"
      case JsonResponseType(acceptHeader)            => "CompletableFuture<Response<String>>"
      case TypedResponseType(acceptHeader, classPtr) => s"CompletableFuture<Response<${classPtr.classDefinitionJava}>>"
      case x                                         => "CompletableFuture<Response<String>>"
    }
  }


  def canonicalResponseType(responseType: ResponseType): Option[String] = {
    responseType match {
      case BinaryResponseType(acceptHeader)          => None
      case JsonResponseType(acceptHeader)            => None
      case TypedResponseType(acceptHeader, classPtr) => Some(classPtr.canonicalNameJava)
      case x                                         => None
    }
  }


  def canonicalContentType(contentType: ContentType): Option[String] = {
    contentType match {
      case JsonContentType(contentTypeHeader)            => None
      case TypedContentType(contentTypeHeader, classPtr) => Some(classPtr.canonicalNameJava)
      case x                                             => None
    }
  }


  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)] = fieldParams.sortBy(_._1)


  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): String = {
    val (queryParameterName, parameter) = qParam

    val nameTermName = queryParameterName
    val typeTypeName = parameter.parameterType match {
      case StringType  => "String"
      case IntegerType => "Long"
      case NumberType  => "Double"
      case BooleanType => "Boolean"
      case FileType    => sys.error(s"RAML type 'FileType' is not yet supported.")
      case DateType    => sys.error(s"RAML type 'DateType' is not yet supported.")
    }

    if (parameter.repeated) {
      s"List<$typeTypeName> $nameTermName"
    } else {
      s"$typeTypeName $nameTermName"
    }
  }


  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    parameter match {
      case Parameter(_, _, true)  => s"""params.put("$queryParameterName", new RepeatedHttpParam($queryParameterName));"""
      case Parameter(_, _, false) => s"""params.put("$queryParameterName", new SingleHttpParam($queryParameterName));"""
    }
  }


  def generateAction(action: RichAction,
                     segmentType: String,
                     actionParameters: List[String] = List.empty,
                     queryParameterMapEntries: List[String] = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     typedBodyParam: Boolean = false,
                     multipartParams: Boolean = false,
                     binaryParam: Boolean = false,
                     contentType: ContentType,
                     responseType: ResponseType): String = {

    val actionType = action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase(Locale.ENGLISH)

    val method = s"Method.${actionType.toString.toUpperCase(Locale.ENGLISH)}"

    val (queryParamMap, queryParams) =
      if (queryParameterMapEntries.nonEmpty) {
        (
          s"""
           Map<String, HttpParam> params = new HashMap<String, HttpParam>();
           ${queryParameterMapEntries.mkString("\n")}
         """,
          "params"
          )
      } else {
        ("", "null")
      }

    val (formParamMap, formParams) =
      if (formParameterMapEntries.nonEmpty) {
        (
          s"""
           Map<String, HttpParam> params = new HashMap<String, HttpParam>();
           ${formParameterMapEntries.mkString("\n")}
         """,
          "params"
          )
      } else {
        ("", "null")
      }

    val bodyFieldValue = if (typedBodyParam) "body" else "null"
    val multipartParamsValue = if (multipartParams) "parts" else "null"
    val binaryParamValue = if (binaryParam) "BinaryRequest.create(body)" else "null"

    val expectedAcceptHeader = action.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = action.selectedContentType.contentTypeHeaderOpt

    val acceptHeader = expectedAcceptHeader.map(acceptH => s""""$acceptH"""").getOrElse("null")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s""""$contentHeader"""").getOrElse("null")

    val canonicalResponseT = canonicalResponseType(responseType).map(quoteString).getOrElse("null")

    val canonicalContentT = canonicalContentType(contentType).map(quoteString).getOrElse("null")

    val callResponseType = responseClassDefinition(responseType)

    s"""
       public $callResponseType $actionTypeMethod(${actionParameters.mkString(", ")}) {

         $queryParamMap

         $formParamMap

         return new $segmentType(
           $method,
           $bodyFieldValue,
           $queryParams,
           $formParams,
           $multipartParamsValue,
           $binaryParamValue,
           $acceptHeader,
           $contentHeader,
           this.getRequestBuilder(),
           $canonicalContentT,
           $canonicalResponseT
         ).call();
       }
     """
  }

}
