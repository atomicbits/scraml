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

import java.util.Locale

import io.atomicbits.scraml.generator.codegen.{ ActionCode, SourceCodeFragment }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model.{ Parameter, QueryString }
import TypedRestOps._
import io.atomicbits.scraml.generator.platform.androidjavajackson.AndroidJavaJackson
import io.atomicbits.scraml.generator.util.CleanNameUtil

/**
  * Created by peter on 1/03/17.
  */
class JavaActionCodeGenerator(val javaJackson: CommonJavaJacksonPlatform) extends ActionCode {

  import Platform._

  implicit val platform: Platform = javaJackson

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassReference) =
    s"""public ${headerSegment.fullyQualifiedName} $contentHeaderMethodName =
          new ${headerSegment.fullyQualifiedName}(this.getRequestBuilder());"""

  // ToDo: generate the imports!
  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = parameters map { parameterDef =>
    val (field, classPtr) = parameterDef
    s"${classPtr.classDefinition} $field"
  }

  def queryStringType(actionSelection: ActionSelection): Option[ClassPointer] = actionSelection.action.queryString.map(_.classPointer())

  def bodyTypes(actionSelection: ActionSelection): List[Option[ClassPointer]] =
    actionSelection.selectedContentType match {
      case StringContentType(contentTypeHeader) => List(Some(StringClassPointer))
      case JsonContentType(contentTypeHeader)   => List(Some(StringClassPointer))
      case typedContentType: TypedContentType =>
        typedContentType.classPointer match {
          case StringClassPointer | JsValueClassPointer | JsObjectClassPointer => List(Some(StringClassPointer))
          case _ =>
            List(Some(StringClassPointer), Some(typedContentType.classPointer))
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

  def responseTypes(actionSelection: ActionSelection): List[Option[ClassPointer]] =
    actionSelection.selectedResponseType match {
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
      case BinaryResponseType(acceptHeader)     => s"BinaryMethodSegment<$bodyType>"
      case JsonResponseType(acceptHeader)       => s"StringMethodSegment<$bodyType>"
      case typedResponseType: TypedResponseType => s"TypeMethodSegment<$bodyType, ${typedResponseType.classPointer.classDefinition}>"
      case x                                    => s"StringMethodSegment<$bodyType>"
    }

  }

  def responseClassDefinition(responseType: ResponseType): String = responseType match {
    case BinaryResponseType(acceptHeader)     => "CompletableFuture<Response<BinaryData>>"
    case JsonResponseType(acceptHeader)       => "CompletableFuture<Response<String>>"
    case typedResponseType: TypedResponseType => s"CompletableFuture<Response<${typedResponseType.classPointer.classDefinition}>>"
    case x                                    => "CompletableFuture<Response<String>>"
  }

  def canonicalResponseType(responseType: ResponseType): Option[String] = responseType match {
    case BinaryResponseType(acceptHeader)     => None
    case JsonResponseType(acceptHeader)       => None
    case typedResponseType: TypedResponseType => Some(typedResponseType.classPointer.fullyQualifiedClassDefinition)
    case x                                    => None
  }

  def canonicalContentType(contentType: ContentType): Option[String] = contentType match {
    case JsonContentType(contentTypeHeader) => None
    case typedContentType: TypedContentType => Some(typedContentType.classPointer.fullyQualifiedClassDefinition)
    case x                                  => None
  }

  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)] = fieldParams.sortBy(_._1)

  def primitiveTypeToJavaType(primitiveType: PrimitiveType, required: Boolean): String = primitiveType match {
    // The cases below go wrong when the primitive ends up in a list like List<double> versus List<Double>.
    //      case integerType: IntegerType if required => "long"
    //      case numbertype: NumberType if required   => "double"
    //      case booleanType: BooleanType if required => "boolean"
    case stringtype: ParsedString   => "String"
    case integerType: ParsedInteger => "Long"
    case numbertype: ParsedNumber   => "Double"
    case booleanType: ParsedBoolean => "Boolean"
    case other                      => sys.error(s"RAML type $other is not yet supported.")
  }

  def expandQueryStringAsMethodParameter(queryString: QueryString): SourceCodeFragment = {

    val sanitizedParameterName = platform.safeFieldName("queryString")
    val classPointer           = queryString.classPointer()
    val classDefinition        = classPointer.classDefinition

    val methodParameter = s"$classDefinition $sanitizedParameterName"

    SourceCodeFragment(imports = Set(classPointer), sourceDefinition = List(methodParameter))
  }

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): SourceCodeFragment = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName          = platform.safeFieldName(queryParameterName)
    val classPointer                    = parameter.classPointer()
    val classDefinition                 = classPointer.classDefinition

    val methodParameter = s"$classDefinition $sanitizedParameterName"

    SourceCodeFragment(imports = Set(classPointer), sourceDefinition = List(methodParameter))
  }

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName          = platform.safeFieldName(queryParameterName)

    val classPointer = parameter.classPointer()
    val (httpParamType, callParameters) =
      classPointer match {
        case ListClassPointer(typeParamValue: PrimitiveClassPointer) => ("RepeatedHttpParam", List(sanitizedParameterName))
        case primitive: PrimitiveClassPointer                        => ("SimpleHttpParam", List(sanitizedParameterName))
        case complex =>
          ("ComplexHttpParam", List(sanitizedParameterName, CleanNameTools.quoteString(classPointer.fullyQualifiedClassDefinition)))
      }

    s"""params.put("$queryParameterName", new $httpParamType(${callParameters.mkString(", ")}));"""
  }

  def getCallMethod: String =
    platform match {
      case AndroidJavaJackson(_) => ""
      case JavaJackson(_)        => ".call()"
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

    val segmentBodyType = if (isBinary) None else bodyType
    val segmentType     = createSegmentType(actionSelection.selectedResponseType, segmentBodyType)

    val actionType       = actionSelection.action.actionType
    val actionTypeMethod = actionType.toString.toLowerCase

    val queryParameterMapEntries = actionSelection.action.queryParameters.valueMap.toList.map(expandQueryOrFormParameterAsMapEntry)

    // The bodyFieldValue is only used for String, JSON and Typed bodies, not for a multipart or binary body
    val bodyFieldValue       = if (isTypedBodyParam) "body" else "null"
    val multipartParamsValue = if (isMultipartParams) "parts" else "null"
    val binaryParamValue     = if (isBinaryParam) "BinaryRequest.create(body)" else "null"

    val expectedAcceptHeader      = actionSelection.selectedResponseType.acceptHeaderOpt
    val expectedContentTypeHeader = actionSelection.selectedContentType.contentTypeHeaderOpt

    val acceptHeader  = expectedAcceptHeader.map(acceptH            => s""""${acceptH.value}"""").getOrElse("null")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s""""${contentHeader.value}"""").getOrElse("null")

    val method = s"Method.${actionType.toString.toUpperCase(Locale.ENGLISH)}"

    val (queryParamMap, queryParams) =
      if (queryParameterMapEntries.nonEmpty)
        (
          s"""
         Map<String, HttpParam> params = new HashMap<String, HttpParam>();
         ${queryParameterMapEntries.mkString("\n")}
       """,
          "params"
        )
      else ("", "null")

    val (formParamMap, formParams) =
      if (formParameterMapEntries.nonEmpty)
        (
          s"""
         Map<String, HttpParam> params = new HashMap<String, HttpParam>();
         ${formParameterMapEntries.mkString("\n")}
       """,
          "params"
        )
      else ("", "null")

    val queryStringValue =
      if (queryStringType.isDefined) "new TypedQueryParams(queryString)"
      else "null"

    val canonicalResponseT = canonicalResponseType(responseType).map(CleanNameTools.quoteString).getOrElse("null")

    val canonicalContentT = canonicalContentType(contentType).map(CleanNameTools.quoteString).getOrElse("null")

    val callResponseType: String =
      platform match {
        case AndroidJavaJackson(_) => segmentType
        case JavaJackson(_)        => responseClassDefinition(responseType)
      }

    val primitiveBody = hasPrimitiveBody(segmentBodyType)

    val callMethod = getCallMethod

    s"""
       public $callResponseType $actionTypeMethod(${actionParameters.mkString(", ")}) {

         $queryParamMap

         $formParamMap

         return new $segmentType(
           $method,
           $bodyFieldValue,
           $primitiveBody,
           $queryParams,
           $queryStringValue,
           $formParams,
           $multipartParamsValue,
           $binaryParamValue,
           $acceptHeader,
           $contentHeader,
           this.getRequestBuilder(),
           $canonicalContentT,
           $canonicalResponseT
         )$callMethod;
       }
     """

  }

}
