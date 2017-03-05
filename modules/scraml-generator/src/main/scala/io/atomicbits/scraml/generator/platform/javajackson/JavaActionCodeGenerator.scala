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

import java.util.Locale

import io.atomicbits.scraml.generator.codegen.{ ActionCode, GenerationAggr }
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

/**
  * Created by peter on 1/03/17.
  */
object JavaActionCodeGenerator extends ActionCode {

  import Platform._

  implicit val platform: Platform = JavaJackson

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassReference): String = {
    s"""public ${headerSegment.fullyQualifiedName} $contentHeaderMethodName =
          new ${headerSegment.fullyQualifiedName}(this.getRequestBuilder());"""
  }

  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String] = {
    parameters map { parameterDef =>
      val (field, classPtr) = parameterDef
      s"${classPtr.classDefinition} $field"
    }
  }

  def bodyTypes(action: ActionSelection): List[Option[ClassPointer]] =
    action.selectedContentType match {
      case StringContentType(contentTypeHeader) => List(Some(StringClassReference))
      case JsonContentType(contentTypeHeader)   => List(Some(StringClassReference))
      case typedContentType: TypedContentType =>
        List(Some(StringClassReference), Some(JsValueClassReference), Some(typedContentType.actualClassPointer))
      case BinaryContentType(contentTypeHeader) =>
        List(
          Some(StringClassReference),
          Some(FileClassReference),
          Some(InputStreamClassReference),
          Some(ArrayClassReference(arrayType = ByteClassReference))
        )
      case AnyContentType(contentTypeHeader) =>
        List(
          None,
          Some(StringClassReference),
          Some(FileClassReference),
          Some(InputStreamClassReference),
          Some(ArrayClassReference(arrayType = ByteClassReference))
        )
      case NoContentType => List(None)
      case x             => List(Some(StringClassReference))
    }

  def createSegmentType(responseType: ResponseType, optBodyType: Option[ClassPointer], generationAggr: GenerationAggr): String = {

    val bodyType = optBodyType.map(_.classDefinition).getOrElse("String")

    responseType match {
      case BinaryResponseType(acceptHeader)     => s"BinaryMethodSegment<$bodyType>"
      case JsonResponseType(acceptHeader)       => s"StringMethodSegment<$bodyType>"
      case typedResponseType: TypedResponseType => s"TypeMethodSegment<$bodyType, ${typedResponseType.actualClassPointer.classDefinition}>"
      case x                                    => s"StringMethodSegment<$bodyType>"
    }

  }

  def responseClassDefinition(responseType: ResponseType): String = {
    responseType match {
      case BinaryResponseType(acceptHeader)     => "CompletableFuture<Response<BinaryData>>"
      case JsonResponseType(acceptHeader)       => "CompletableFuture<Response<String>>"
      case typedResponseType: TypedResponseType => s"CompletableFuture<Response<${typedResponseType.actualClassPointer.classDefinition}>>"
      case x                                    => "CompletableFuture<Response<String>>"
    }
  }

  def canonicalResponseType(responseType: ResponseType): Option[String] = {
    responseType match {
      case BinaryResponseType(acceptHeader)     => None
      case JsonResponseType(acceptHeader)       => None
      case typedResponseType: TypedResponseType => Some(typedResponseType.actualClassPointer.canonicalName)
      case x                                    => None
    }
  }

  def canonicalContentType(contentType: ContentType): Option[String] = {
    contentType match {
      case JsonContentType(contentTypeHeader)   => None
      case typedResponseType: TypedResponseType => Some(typedResponseType.actualClassPointer.canonicalName)
      case x                                    => None
    }
  }

  def sortQueryOrFormParameters(fieldParams: List[(String, ParsedParameter)]): List[(String, ParsedParameter)] = fieldParams.sortBy(_._1)

  def primitiveTypeToJavaType(primitiveType: PrimitiveType, required: Boolean): String = {
    primitiveType match {
      // The cases below goe wrong when the primitive ends up in a list like List<double> versus List<Double>.
      //      case integerType: IntegerType if required => "long"
      //      case numbertype: NumberType if required   => "double"
      //      case booleanType: BooleanType if required => "boolean"
      case stringtype: ParsedString   => "String"
      case integerType: ParsedInteger => "Long"
      case numbertype: ParsedNumber   => "Double"
      case booleanType: ParsedBoolean => "Boolean"
      case other                      => sys.error(s"RAML type $other is not yet supported.")
    }
  }

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, ParsedParameter), noDefault: Boolean = false): String = {
    val (queryParameterName, parameter) = qParam

    val sanitizedParameterName = CleanNameTools.cleanFieldName(queryParameterName)

    parameter.parameterType.parsed match {
      case primitiveType: PrimitiveType =>
        val primitive = primitiveTypeToJavaType(primitiveType, parameter.repeated)
        if (parameter.required) {
          s"$sanitizedParameterName: $primitive"
        } else {
          val defaultValue = if (noDefault) "" else s"= None"
          s"$sanitizedParameterName: Option[$primitive] $defaultValue"
        }
      case arrayType: ParsedArray =>
        arrayType.items match {
          case primitiveType: PrimitiveType =>
            val primitive    = primitiveTypeToJavaType(primitiveType, parameter.repeated)
            val defaultValue = if (noDefault) "" else s"= List.empty[$primitive]"
            s"$sanitizedParameterName: List[$primitive] $defaultValue"
          case other =>
            sys.error(s"Cannot transform an array of an non-promitive type to a query or form parameter: ${other}")
        }
    }
  }

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, ParsedParameter)): String = {
    val (queryParameterName, parameter) = qParam
    val sanitizedParameterName          = CleanNameTools.cleanFieldName(queryParameterName)

    (parameter.parameterType.parsed, parameter.required) match {
      case primitive: PrimitiveType => s"""params.put("$queryParameterName", new SingleHttpParam($sanitizedParameterName));"""
      case arrayType: ParsedArray   => s"""params.put("$queryParameterName", new RepeatedHttpParam($sanitizedParameterName));"""
    }
  }

  def generateAction(actionSelection: ActionSelection,
                     bodyType: Option[ClassPointer],
                     isBinary: Boolean,
                     actionParameters: List[String]        = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     isTypedBodyParam: Boolean             = false,
                     isMultipartParams: Boolean            = false,
                     isBinaryParam: Boolean                = false,
                     contentType: ContentType,
                     responseType: ResponseType,
                     generationAggr: GenerationAggr): String = {

    val segmentBodyType: Option[ClassPointer] = if (isBinary) None else bodyType
    val segmentType: String                   = createSegmentType(actionSelection.selectedResponsetype, segmentBodyType, generationAggr)

    val actionType               = actionSelection.action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase

    val queryParameterMapEntries = actionSelection.action.queryParameters.valueMap.toList.map(expandQueryOrFormParameterAsMapEntry)

    // The bodyFieldValue is only used for String, JSON and Typed bodies, not for a multipart or binary body
    val bodyFieldValue       = if (isTypedBodyParam) "body" else "null"
    val multipartParamsValue = if (isMultipartParams) "parts" else "null"
    val binaryParamValue     = if (isBinaryParam) "BinaryRequest.create(body)" else "null"

    val expectedAcceptHeader      = actionSelection.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = actionSelection.selectedContentType.contentTypeHeaderOpt

    val acceptHeader  = expectedAcceptHeader.map(acceptH            => s""""${acceptH.value}"""").getOrElse("null")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s""""${contentHeader.value}"""").getOrElse("null")

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
