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
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.Parameter
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 30/09/15.
  */
object ScalaActionCode extends ActionCode {

  implicit val language: Language = Scala


  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String = {
    s"""def $contentHeaderMethodName = new ${headerSegment.classRef.fullyQualifiedName}(_requestBuilder)"""
  }


  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String = {
    s"""
         package ${headerSegmentClassRef.packageName}

         import io.atomicbits.scraml.dsl._
         import play.api.libs.json._
         import java.io._

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
      case BinaryContentType(contentTypeHeader)          =>
        List(
          Some(StringClassReference()),
          Some(FileClassReference()),
          Some(InputStreamClassReference()),
          Some(ArrayClassReference.typed(arrayType = ByteClassReference()))
        )
      case AnyContentType(contentTypeHeader)             =>
        List(
          None,
          Some(StringClassReference()),
          Some(FileClassReference()),
          Some(InputStreamClassReference()),
          Some(ArrayClassReference.typed(arrayType = ByteClassReference()))
        )
      case NoContentType                                 => List(None)
      case x                                             => List(Some(StringClassReference()))
    }


  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String = {
    val bodyType = optBodyType.map(_.classDefinitionScala).getOrElse("String")
    responseType match {
      case BinaryResponseType(acceptHeader)          => s"BinaryMethodSegment[$bodyType]"
      case JsonResponseType(acceptHeader)            => s"JsonMethodSegment[$bodyType]"
      case TypedResponseType(acceptHeader, classPtr) => s"TypeMethodSegment[$bodyType, ${classPtr.classDefinitionScala}]"
      case x                                         => s"StringMethodSegment[$bodyType]"
    }
  }

  def responseClassDefinition(responseType: ResponseType): String = {
    responseType match {
      case BinaryResponseType(acceptHeader)          => "BinaryData"
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


  def primitiveTypeToScalaType(primitiveType: PrimitiveType): String = {
    primitiveType match {
      case stringType: StringType   => "String"
      case integerType: IntegerType => "Long"
      case numbertype: NumberType   => "Double"
      case booleanType: BooleanType => "Boolean"
      case other                    => sys.error(s"RAML type $other is not yet supported.")
    }
  }

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): String = {
    val (queryParameterName, parameter) = qParam

    val sanitizedParameterName = CleanNameUtil.cleanFieldName(queryParameterName)

    parameter.parameterType match {
      case primitiveType: PrimitiveType =>
        val primitive = primitiveTypeToScalaType(primitiveType)
        if (parameter.required) {
          s"$sanitizedParameterName: $primitive"
        } else {
          val defaultValue = if (noDefault) "" else s"= None"
          s"$sanitizedParameterName: Option[$primitive] $defaultValue"
        }
      case arrayType: ArrayType         =>
        arrayType.items match {
          case primitiveType: PrimitiveType =>
            val primitive = primitiveTypeToScalaType(primitiveType)
            val defaultValue = if (noDefault) "" else s"= List.empty[$primitive]"
            s"$sanitizedParameterName: List[$primitive] $defaultValue"
          case other                        =>
            sys.error(s"Cannot transform an array of an non-promitive type to a query or form parameter: ${other}")
        }
    }
  }


  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName = CleanNameUtil.cleanFieldName(queryParameterName)

    (parameter.parameterType, parameter.required) match {
      case (primitive: PrimitiveType, false) => s""""$queryParameterName" -> $sanitizedParameterName.map(HttpParam(_))"""
      case (primitive: PrimitiveType, true)  => s""""$queryParameterName" -> Option($sanitizedParameterName).map(HttpParam(_))"""
      case (arrayType: ArrayType, _)         => s""""$queryParameterName" -> Option($sanitizedParameterName).map(HttpParam(_))"""
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
    val actionTypeMethod: String = actionType.toString.toLowerCase

    val expectedAcceptHeader = action.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = action.selectedContentType.contentTypeHeaderOpt

    val acceptHeader = expectedAcceptHeader.map(acceptH => s"""Some("${acceptH.value}")""").getOrElse("None")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s"""Some("${contentHeader.value}")""").getOrElse("None")

    // The bodyFieldValue is only used for String, JSON and Typed bodies, not for a multipart or binary body
    val bodyFieldValue = if (typedBodyParam) "Some(body)" else "None"
    val multipartParamsValue = if (multipartParams) "parts" else "List.empty"
    val binaryParamValue = if (binaryParam) "Some(BinaryRequest(body))" else "None"

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
           binaryParam = $binaryParamValue,
           expectedAcceptHeader = $acceptHeader,
           expectedContentTypeHeader = $contentHeader,
           req = _requestBuilder
         ).call()
     """
  }

}
