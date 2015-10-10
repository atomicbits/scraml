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


  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String = {
    s"""public ${headerSegment.classRef.fullyQualifiedName} $contentHeaderMethodName =
          new ${headerSegment.classRef.fullyQualifiedName}(this.getRequestBuilder());"""
  }


  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String = {
    s"""
         package ${headerSegmentClassRef.packageName};

         import io.atomicbits.scraml.dsl.java.*;
         import java.util.*;

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
      case NoContentType                                 => List(None, Some(StringClassReference()))
      case x                                             => List(Some(StringClassReference()))
    }


  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String = {
    val bodyType = optBodyType.map(_.classDefinitionJava).getOrElse("String")
    responseType match {
      case JsonResponseType(acceptHeader)            => s"StringMethodSegment<$bodyType>"
      case TypedResponseType(acceptHeader, classPtr) => s"TypeMethodSegment<$bodyType, ${classPtr.classDefinitionJava}>"
      case x                                         => s"StringMethodSegment<$bodyType>"
    }
  }


  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter)): String = {
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
                     bodyField: Boolean = false,
                     queryParameterMapEntries: List[String] = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     multipartParams: Option[String] = None,
                     canonicalResponseTypeOpt: Option[String] = None): String = {

    val actionType = action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase(Locale.ENGLISH)

    val method = s"Method.${actionType.toString.toUpperCase(Locale.ENGLISH)}"

    val bodyFieldValue = if (bodyField) "body" else "null"

    val (queryParamMap, queryParams) =
      if (queryParameterMapEntries.nonEmpty) {
        (s"""
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
        (s"""
           Map<String, HttpParam> params = new HashMap<String, HttpParam>();
           ${formParameterMapEntries.mkString("\n")}
         """,
          "params"
          )
      } else {
        ("", "null")
      }

    val multipartParamsValue = multipartParams.getOrElse("null")

    val expectedAcceptHeader = action.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = action.selectedContentType.contentTypeHeaderOpt

    val acceptHeader = expectedAcceptHeader.map(acceptH => s""""$acceptH"""").getOrElse("null")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s""""$contentHeader"""").getOrElse("null")

    val canonicalResponseType = canonicalResponseTypeOpt.map(quoteString).getOrElse("null")

    s"""
       public $segmentType $actionTypeMethod(${actionParameters.mkString(", ")}) {

         $queryParamMap

         $formParamMap

         return new $segmentType(
           $method,
           $bodyFieldValue,
           $queryParams,
           $formParams,
           $multipartParamsValue,
           $acceptHeader,
           $contentHeader,
           this.getRequestBuilder(),
           $canonicalResponseType
         );
       }
     """
  }

}
