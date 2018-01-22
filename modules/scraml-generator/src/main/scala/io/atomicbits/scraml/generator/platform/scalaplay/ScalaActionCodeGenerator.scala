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

import io.atomicbits.scraml.generator.codegen.{ ActionCode, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.{ Parameter, QueryString }
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import TypedRestOps._

/**
  * Created by peter on 20/01/17.
  */
case class ScalaActionCodeGenerator(scalaPlay: ScalaPlay) extends ActionCode {

  import Platform._

  implicit val platform: Platform = scalaPlay

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassReference): String = {
    s"""def $contentHeaderMethodName = new ${headerSegment.fullyQualifiedName}(_requestBuilder)"""
  }

  // ToDo: generate the imports!
  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = {
    parameters map { parameterDef =>
      val (field, classPtr) = parameterDef
      s"$field: ${classPtr.classDefinition}"
    }
  }

  def queryStringType(actionSelection: ActionSelection): Option[ClassPointer] = {
    actionSelection.action.queryString.map(_.classPointer())
  }

  def bodyTypes(action: ActionSelection): List[Option[ClassPointer]] =
    action.selectedContentType match {
      case StringContentType(contentTypeHeader) => List(Some(StringClassPointer))
      case JsonContentType(contentTypeHeader)   => List(Some(StringClassPointer), Some(JsValueClassPointer))
      case typedContentType: TypedContentType =>
        typedContentType.classPointer match {
          case StringClassPointer                         => List(Some(StringClassPointer))
          case JsValueClassPointer | JsObjectClassPointer => List(Some(StringClassPointer), Some(JsValueClassPointer))
          case _ =>
            List(Some(StringClassPointer), Some(JsValueClassPointer), Some(typedContentType.classPointer))
        }
      case BinaryContentType(contentTypeHeader) =>
        List(
          Some(StringClassPointer),
          Some(FileClassPointer),
          Some(InputStreamClassPointer),
          Some(ArrayClassPointer(arrayType = ByteClassPointer))
        )
      case AnyContentType(contentTypeHeader) =>
        List(
          None,
          Some(StringClassPointer),
          Some(FileClassPointer),
          Some(InputStreamClassPointer),
          Some(ArrayClassPointer(arrayType = ByteClassPointer))
        )
      case NoContentType => List(None)
      case x             => List(Some(StringClassPointer))
    }

  def responseTypes(action: ActionSelection): List[Option[ClassPointer]] =
    action.selectedResponseType match {
      case StringResponseType(acceptHeader) => List(Some(StringClassPointer))
      case JsonResponseType(acceptHeader)   => List(Some(StringClassPointer), Some(JsValueClassPointer))
      case BinaryResponseType(acceptHeader) =>
        List(
          Some(StringClassPointer),
          Some(FileClassPointer),
          Some(InputStreamClassPointer),
          Some(ArrayClassPointer(arrayType = ByteClassPointer))
        )
      case typedResponseType: TypedResponseType =>
        List(Some(StringClassPointer), Some(JsValueClassPointer), Some(typedResponseType.classPointer))
      case NoResponseType => List(None)
      case x              => List(Some(StringClassPointer))
    }

  def createSegmentType(responseType: ResponseType, optBodyType: Option[ClassPointer]): String = {

    val bodyType = optBodyType.map(_.classDefinition).getOrElse("String")

    responseType match {
      case BinaryResponseType(acceptHeader) => s"BinaryMethodSegment[$bodyType]"
      case JsonResponseType(acceptHeader)   => s"JsonMethodSegment[$bodyType]"
      case typedResponseType: TypedResponseType =>
        s"TypeMethodSegment[$bodyType, ${typedResponseType.classPointer.classDefinition}]"
      case x => s"StringMethodSegment[$bodyType]"
    }

  }

  def responseClassDefinition(responseType: ResponseType): String = {
    responseType match {
      case BinaryResponseType(acceptHeader)     => "BinaryData"
      case JsonResponseType(acceptHeader)       => "String"
      case typedResponseType: TypedResponseType => typedResponseType.classPointer.classDefinition
      case x                                    => "String"
    }
  }

  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)] = {
    fieldParams.sortBy { t =>
      val (field, param) = t
      (!param.required, field)
    }
  }

  def primitiveTypeToScalaType(primitiveType: PrimitiveType): String = {
    primitiveType match {
      case stringType: ParsedString   => "String"
      case integerType: ParsedInteger => "Long"
      case numbertype: ParsedNumber   => "Double"
      case booleanType: ParsedBoolean => "Boolean"
      case other                      => sys.error(s"RAML type $other is not yet supported.")
    }
  }

  def expandQueryStringAsMethodParameter(queryString: QueryString): SourceCodeFragment = {

    val sanitizedParameterName = CleanNameTools.cleanFieldName("queryString")
    val classPointer           = queryString.classPointer()
    val classDefinition        = classPointer.classDefinition

    val methodParameter = s"$sanitizedParameterName: $classDefinition"
    SourceCodeFragment(imports = Set(classPointer), sourceDefinition = List(methodParameter))
  }

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): SourceCodeFragment = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName          = CleanNameTools.cleanFieldName(queryParameterName)
    val classPointer                    = parameter.classPointer()
    val classDefinition                 = classPointer.classDefinition

    val methodParameter =
      if (parameter.required) {
        s"$sanitizedParameterName: $classDefinition"
      } else {
        val defaultValue = if (noDefault) "" else s"= None"
        s"$sanitizedParameterName: Option[$classDefinition] $defaultValue"
      }

    SourceCodeFragment(imports = Set(classPointer), sourceDefinition = List(methodParameter))
  }

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName          = CleanNameTools.cleanFieldName(queryParameterName)

    val httpParamType: String =
      parameter.classPointer() match {
        case ListClassPointer(typeParamValue: PrimitiveClassPointer) => "RepeatedHttpParam"
        case primitive: PrimitiveClassPointer                        => "SimpleHttpParam"
        case complex                                                 => "ComplexHttpParam"
      }

    if (parameter.required) {
      s""""$queryParameterName" -> Option($sanitizedParameterName).map($httpParamType.create(_))"""
    } else {
      s""""$queryParameterName" -> $sanitizedParameterName.map($httpParamType.create(_))"""
    }
  }

  def generateAction(actionSelection: ActionSelection,
                     bodyType: Option[ClassPointer],
                     queryStringType: Option[ClassPointer],
                     isBinary: Boolean,
                     actionParameters: List[String]        = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     isTypedBodyParam: Boolean             = false,
                     isMultipartParams: Boolean            = false,
                     isBinaryParam: Boolean                = false,
                     contentType: ContentType,
                     responseType: ResponseType): String = {

    val segmentBodyType: Option[ClassPointer] = if (isBinary) None else bodyType
    val segmentType: String                   = createSegmentType(actionSelection.selectedResponseType, segmentBodyType)

    val actionType               = actionSelection.action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase

    val queryParameterMapEntries = actionSelection.action.queryParameters.valueMap.toList.map(expandQueryOrFormParameterAsMapEntry)

    val queryStringValue =
      if (queryStringType.isDefined) "Some(TypedQueryParams.create(queryString))"
      else "None"

    val expectedAcceptHeader      = actionSelection.selectedResponseType.acceptHeaderOpt
    val expectedContentTypeHeader = actionSelection.selectedContentType.contentTypeHeaderOpt

    val acceptHeader  = expectedAcceptHeader.map(acceptH            => s"""Some("${acceptH.value}")""").getOrElse("None")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s"""Some("${contentHeader.value}")""").getOrElse("None")

    // The bodyFieldValue is only used for String, JSON and Typed bodies, not for a multipart or binary body
    val bodyFieldValue       = if (isTypedBodyParam) "Some(body)" else "None"
    val multipartParamsValue = if (isMultipartParams) "parts" else "List.empty"
    val binaryParamValue     = if (isBinaryParam) "Some(BinaryRequest(body))" else "None"

    val primitiveBody: Boolean = hasPrimitiveBody(segmentBodyType)

    s"""
       def $actionTypeMethod(${actionParameters.mkString(", ")}) =
         new $segmentType(
           method = $actionType,
           theBody = $bodyFieldValue,
           primitiveBody = $primitiveBody,
           queryParams = Map(
             ${queryParameterMapEntries.mkString(",")}
           ),
           queryString = $queryStringValue,
           formParams = Map(
             ${formParameterMapEntries.mkString(",")}
           ),
           multipartParams = $multipartParamsValue,
           binaryParam = $binaryParamValue,
           expectedAcceptHeader = $acceptHeader,
           expectedContentTypeHeader = $contentHeader,
           req = _requestBuilder
         ).call()
     """
  }

}
