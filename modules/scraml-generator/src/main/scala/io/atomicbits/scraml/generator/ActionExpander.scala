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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.lookup.SchemaLookup
import io.atomicbits.scraml.jsonschemaparser.{ClassRep, PlainClassRep}
import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ActionExpander {

  def expandAction(action: Action, schemaLookup: SchemaLookup): List[String] = {

    // ToDo: handle different types of ClassReps.

    // We currently only support the first context-type mimeType that we see. We should extend this later on.
    val bodyMimeType = action.body.values.toList.headOption
    val hasBody = bodyMimeType.isDefined
    val maybeBodySchema = bodyMimeType.flatMap(_.schema).flatMap(schemaLookup.externalSchemaLinks.get).map(schemaLookup.lookupSchema)
    val maybeBodyClassRep = maybeBodySchema.flatMap(TypeGenerator.schemaAsClassRep(_, schemaLookup))

    val formParameters: Map[String, List[Parameter]] = bodyMimeType.map(_.formParameters).getOrElse(Map.empty)
    val isMultipartFormUpload = bodyMimeType.map(_.mimeType).exists(_ == "multipart/form-data")

    // We currently only support the first response mimeType that we see. We should extend this later on.
    val response = action.responses.values.toList.headOption
    // We currently only support the first response body mimeType that we see. We should extend this later on.
    val responseMimeType = response.flatMap(_.body.values.toList.headOption)
    val hasResponse = responseMimeType.isDefined
    val hasJsonResponse = responseMimeType.exists(_.mimeType.toLowerCase.contains("json"))
    val maybeResponseSchema = responseMimeType.flatMap(_.schema).flatMap(schemaLookup.externalSchemaLinks.get).map(schemaLookup.lookupSchema)
    val maybeResponseClassRep = maybeResponseSchema.flatMap(TypeGenerator.schemaAsClassRep(_, schemaLookup))

    val (hasTypedBody, bodyClassRep) = maybeBodyClassRep match {
      case Some(bdClass) => (true, bdClass)
      case None          => (false, PlainClassRep("String"))
    }

    val (hasTypedResponse, responseClassRep) = maybeResponseClassRep match {
      case Some(rsClass) => (true, rsClass)
      case None          => (false, PlainClassRep("String"))
    }


    def expandGetAction(): List[String] = {

      val queryParameterMethodParameters =
        action.queryParameters.toList.map(param => expandParameterAsMethodParameter(param))
      val queryParameterMapEntries =
        action.queryParameters.toList.map(param => expandParameterAsMapEntry(param))

      List(
        s"""
          def get(${queryParameterMethodParameters.mkString(",")}) = new GetSegment(
            queryParams = Map(
              ${queryParameterMapEntries.mkString(",")}
            ),
            validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
            req = requestBuilder
          ) {

            ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

          }
       """)
    }

    def expandPutAction(): List[String] = {
      val defaultActions =
        if (hasBody)
          List(
            s"""
            def put(body: String) = new PutSegment(
              validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
              validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
              req = requestBuilder) {

              ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

            }
            """,
            s"""
            def put(body: JsValue) = new PutSegment(
              validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
              validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
              req = requestBuilder) {

              ${expandHeadersAndExecution(hasBody = true, PlainClassRep("JsValue")).mkString("\n")}

            }
            """
          )
        else
          List(
            s"""
            def put() = new PutSegment(
              validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
              validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
              req = requestBuilder) {

              ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

            }
            """
          )

      val additionalAction =
        if (hasTypedBody) {
          val typeTypeName = TypeGenerator.classRepAsType(bodyClassRep)
          val bodyParam = List(s"body: $typeTypeName")
          List(
            s"""
              def put(${bodyParam.mkString(",")}) = new PutSegment(
                validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
                validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
                req = requestBuilder) {

                  ${expandHeadersAndExecution(hasBody = true, bodyClassRep).mkString("\n")}

              }
             """
          )
        } else Nil

      defaultActions ++ additionalAction
    }


    def expandPostAction(): List[String] = {

      def expandRegularPostAction(): List[String] = {
        // We support a custom body instead.
        val defaultActions =
          if (hasBody)
            List(
              s"""
              def post(body: String) = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
                validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
                req = requestBuilder) {

                ${expandHeadersAndExecution(hasBody = true, PlainClassRep("String")).mkString("\n")}

              }
              """,
              s"""
              def post(body: JsValue) = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
                validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
                req = requestBuilder) {

                ${expandHeadersAndExecution(hasBody = true, PlainClassRep("JsValue")).mkString("\n")}

              }
              """
            )
          else
            List(
              s"""
              def post() = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
                validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
                req = requestBuilder) {

                ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

              }
              """
            )

        val additionalAction =
          if (hasTypedBody) {
            val typeTypeName = TypeGenerator.classRepAsType(bodyClassRep) // TypeName(bodyClassRep.name)
            val bodyParam = List(s"val body: $typeTypeName")
            List(
              s"""
                  def post(${bodyParam.mkString(",")}) = new PostSegment(
                    formParams = Map.empty,
                    multipartParams = List.empty,
                    validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
                    validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
                    req = requestBuilder) {

                      ${expandHeadersAndExecution(hasBody = true, bodyClassRep).mkString("\n")}

                  }
                """
            )
          } else Nil

        defaultActions ++ additionalAction

      }

      def expandFormPostAction(): List[String] = {
        // We support the given form parameters.
        val formParameterMethodParameters =
          formParameters.toList.map { paramPair =>
            val (name, paramList) = paramPair
            if (paramList.isEmpty) sys.error(s"Form parameter $name has no valid type definition.")
            expandParameterAsMethodParameter((name, paramList.head))
            // We still don't understand why the form parameters are represented as a Map[String, List[Parameter]]
            // instead of just a Map[String, Parameter] in the Java Raml model. Here, we just use the first element
            // of the parameter list.
          }

        val formParameterMapEntries =
          formParameters.toList.map { paramPair =>
            val (name, paramList) = paramPair
            expandParameterAsMapEntry((name, paramList.head))
          }

        List(
          s"""
          def post(${formParameterMethodParameters.mkString(",")}) = new PostSegment(
            formParams = Map(
              ${formParameterMapEntries.mkString(",")}
            ),
            multipartParams = List.empty,
            validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
            validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
            req = requestBuilder) {

              ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

          }
         """)
      }

      def expandMultipartFormPostAction(): List[String] = {
        List(
          s"""
          def post(parts: List[BodyPart]) = new PostSegment(
            formParams = Map.empty,
            multipartParams = parts,
            validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
            validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
            req = requestBuilder) {

              ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

          }
         """)
      }

      if (formParameters.nonEmpty) {
        expandFormPostAction()
      } else if (isMultipartFormUpload) {
        expandMultipartFormPostAction()
      } else {
        expandRegularPostAction()
      }

    }

    def expandDeleteAction(): List[String] = {
      List(
        s"""
          def delete() = new DeleteSegment(
            validAcceptHeaders = List(${validAcceptHeaders().mkString(",")}),
            validContentTypeHeaders = List(${validContentTypeHeaders().mkString(",")}),
            req = requestBuilder) {

            ${expandHeadersAndExecution(hasBody = false, PlainClassRep("String")).mkString("\n")}

          }
       """)
    }

    def expandHeadersAndExecution(hasBody: Boolean, bodyClassRep: ClassRep): List[String] = {
      List(
        s"""
           def headers(headers: (String, String)*) = new HeaderSegment(
             headers = headers.toMap,
             req = requestBuilder
           ) {
             ${expandExecution(hasBody, bodyClassRep).mkString("\n")}
           }
         """
      ) ++ expandExecution(hasBody, bodyClassRep)
    }

    def expandExecution(hasBody: Boolean, bodyClassRep: ClassRep): List[String] = {
      val bodyTypeName = TypeGenerator.classRepAsType(bodyClassRep)
      val responseTypeName = TypeGenerator.classRepAsType(responseClassRep)
      val executeSegment =
        if (hasBody) {
          List(s"""
           private val executeSegment = new ExecuteSegment[$bodyTypeName, $responseTypeName](requestBuilder, Some(body))
         """)
        } else {
          List(s"""
           private val executeSegment = new ExecuteSegment[$bodyTypeName, $responseTypeName](requestBuilder, None)
         """)
        }
      val stringExecutor = List( s""" def call() = executeSegment.callToStringResponse() """)
      val jsonExecutor = List( s""" def call() = executeSegment.callToJsonResponse() """)
      val jsonDtoExecutor = List( s""" def call() = executeSegment.callToTypeResponse() """)
      if (hasTypedResponse) executeSegment ++ jsonDtoExecutor
      else if(hasJsonResponse) executeSegment ++ jsonExecutor
      else executeSegment ++ stringExecutor
    }

    def needsAcceptHeader: Boolean = {
      action.responses.values.toList.flatMap(_.headers).nonEmpty
    }

    def validAcceptHeaders(): List[String] = {
      action.responses.values.toList.flatMap(response => response.headers.keys).map(quoteString)
    }

    def needsContentTypeHeader: Boolean = {
      action.body.keys.toList.nonEmpty
    }

    def validContentTypeHeaders(): List[String] = {
      action.body.keys.toList.map(quoteString)
    }

    def expandParameterAsMethodParameter(qParam: (String, Parameter)): String = {
      val (queryParameterName, parameter) = qParam

      val nameTermName = queryParameterName
      val typeTypeName = parameter.parameterType match {
        case StringType  => "String"
        case IntegerType => "Int"
        case NumberType  => "Double"
        case BooleanType => "Boolean"
        case FileType    => sys.error(s"RAML type 'FileType' is not yet supported.")
        case DateType    => sys.error(s"RAML type 'DateType' is not yet supported.")
      }

      if (parameter.repeated) {
        s"$nameTermName: List[$typeTypeName]"
      } else {
        if (parameter.required) {
          s"$nameTermName: $typeTypeName"
        } else {
          s"$nameTermName: Option[$typeTypeName]"
        }
      }
    }

    def expandParameterAsMapEntry(qParam: (String, Parameter)): String = {
      val (queryParameterName, parameter) = qParam
      val nameTermName = queryParameterName
      parameter match {
        case Parameter(_, _, true)  => s"""${quoteString(queryParameterName)} -> Option($nameTermName).map(HttpParam(_))"""
        case Parameter(_, true, false)  => s"""${quoteString(queryParameterName)} -> Option($nameTermName).map(HttpParam(_))"""
        case Parameter(_, false, false) => s"""${quoteString(queryParameterName)} -> $nameTermName.map(HttpParam(_))"""
      }
    }


    action.actionType match {
      case Get           => expandGetAction()
      case Put           => expandPutAction()
      case Post          => expandPostAction()
      case Delete        => expandDeleteAction()
      case unknownAction => sys.error(s"$unknownAction actions are not supported yet.")
    }

  }

  private def quoteString(text: String): String = s""""$text""""

}
