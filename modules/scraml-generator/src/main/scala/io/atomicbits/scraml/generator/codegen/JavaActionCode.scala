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
import io.atomicbits.scraml.generator.util.CleanNameUtil
import io.atomicbits.scraml.ramlparser.model.ParsedParameter
import io.atomicbits.scraml.ramlparser.model.parsedtypes._

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

    val sanitizedParameterName = CleanNameUtil.cleanFieldName(queryParameterName)

    parameter.parameterType.parsed match {
      case primitiveType: PrimitiveType =>
        val primitive = primitiveTypeToJavaType(primitiveType, parameter.repeated)
        s"$primitive $sanitizedParameterName"
      case arrayType: ParsedArray       =>
        arrayType.items match {
          case primitiveType: PrimitiveType =>
            val primitive = primitiveTypeToJavaType(primitiveType, parameter.repeated)
            s"List<$primitive> $sanitizedParameterName"
          case other                        =>
            sys.error(s"Cannot transform an array of an non-promitive type to a query or form parameter: ${other}")
        }
    }
  }


  def expandQueryOrFormParameterAsMapEntry(qParam: (String, ParsedParameter)): String = {
    val (queryParameterName, parameter) = qParam
    val sanitizedQueryParameterName = CleanNameUtil.cleanFieldName(queryParameterName)

    parameter.parameterType.parsed match {
      case primitive: PrimitiveType => s"""params.put("$queryParameterName", new SingleHttpParam($sanitizedQueryParameterName));"""
      case arrayType: ParsedArray   => s"""params.put("$queryParameterName", new RepeatedHttpParam($sanitizedQueryParameterName));"""
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

    val acceptHeader = expectedAcceptHeader.map(acceptH => s""""${acceptH.value}"""").getOrElse("null")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s""""${contentHeader.value}"""").getOrElse("null")

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
