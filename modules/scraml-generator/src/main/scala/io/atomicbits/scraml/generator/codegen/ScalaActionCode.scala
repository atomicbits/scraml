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

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 30/09/15.
 */
object ScalaActionCode extends ActionCode {

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String = {
    s"""def $contentHeaderMethodName = new ${headerSegment.classRef.fullyQualifiedName}(_requestBuilder)"""
  }


  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String = {
    s"""
         package ${headerSegmentClassRef.packageName}

         import io.atomicbits.scraml.dsl._
         import play.api.libs.json._

         ${imports.mkString("\n")}


         class ${headerSegmentClassRef.name}(_req: RequestBuilder) extends HeaderSegment(_req) {

           ${methods.mkString("\n")}

         }
       """
  }


  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = {
    parameters map { parameterDef =>
      val (field, classPtr) = parameterDef
      s"$field: ${classPtr.classDefinitionScala}"
    }
  }


  def bodyTypes(action: RichAction): List[Option[ClassPointer]] =
    action.selectedContentType match {
      case StringContentType(contentTypeHeader)          => List(Some(StringClassReference()))
      case JsonContentType(contentTypeHeader)            => List(Some(StringClassReference()), Some(JsValueClassReference()))
      case TypedContentType(contentTypeHeader, classRef) =>
        List(Some(StringClassReference()), Some(JsValueClassReference()), Some(classRef))
      case NoContentType                                 => List(None)
      case x                                             => List(Some(StringClassReference()))
    }


  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String = {
    val bodyType = optBodyType.map(_.classDefinitionScala).getOrElse("String")
    responseType match {
      case JsonResponseType(acceptHeader)            => s"JsonMethodSegment[$bodyType]"
      case TypedResponseType(acceptHeader, classPtr) => s"TypeMethodSegment[$bodyType, ${classPtr.classDefinitionScala}]"
      case x                                         => s"StringMethodSegment[$bodyType]"
    }
  }

  def responseClassDefinition(responseType: ResponseType): String = {
    responseType match {
      case JsonResponseType(acceptHeader)            => "String"
      case TypedResponseType(acceptHeader, classPtr) => classPtr.classDefinitionScala
      case x                                         => "String"
    }
  }


  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)] = {
    fieldParams.sortBy { t =>
      val (field, param) = t
      (!param.required, !param.repeated, field)
    }
  }


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
      val defaultValue = if (noDefault) "" else s"= List.empty[$typeTypeName]"
      s"$nameTermName: List[$typeTypeName] $defaultValue"
    } else {
      if (parameter.required) {
        s"$nameTermName: $typeTypeName"
      } else {
        val defaultValue = if (noDefault) "" else s"= None"
        s"$nameTermName: Option[$typeTypeName] $defaultValue"
      }
    }
  }


  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    parameter match {
      case Parameter(_, _, true)      => s""""$queryParameterName" -> Option($queryParameterName).map(HttpParam(_))"""
      case Parameter(_, true, false)  => s""""$queryParameterName" -> Option($queryParameterName).map(HttpParam(_))"""
      case Parameter(_, false, false) => s""""$queryParameterName" -> $queryParameterName.map(HttpParam(_))"""
    }
  }


  def generateAction(action: RichAction,
                     segmentType: String,
                     actionParameters: List[String] = List.empty,
                     bodyField: Boolean = false,
                     queryParameterMapEntries: List[String] = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     multipartParams: Option[String] = None,
                     contentType: ContentType,
                     responseType: ResponseType): String = {

    val actionType = action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase

    val expectedAcceptHeader = action.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = action.selectedContentType.contentTypeHeaderOpt

    val acceptHeader = expectedAcceptHeader.map(acceptH => s"""Some("$acceptH")""").getOrElse("None")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s"""Some("$contentHeader")""").getOrElse("None")

    val bodyFieldValue = if (bodyField) "Some(body)" else "None"

    val multipartParamsValue = multipartParams.getOrElse("List.empty")

    s"""
       def $actionTypeMethod(${actionParameters.mkString(", ")}) =
         new $segmentType(
           method = $actionType,
           theBody = $bodyFieldValue,
           queryParams = Map(
             ${queryParameterMapEntries.mkString(",")}
           ),
           formParams = Map(
             ${formParameterMapEntries.mkString(",")}
           ),
           multipartParams = $multipartParamsValue,
           expectedAcceptHeader = $acceptHeader,
           expectedContentTypeHeader = $contentHeader,
           req = _requestBuilder
         ).call()
     """
  }

}
