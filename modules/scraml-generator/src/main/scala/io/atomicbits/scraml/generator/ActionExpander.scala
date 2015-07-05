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

import io.atomicbits.scraml.jsonschemaparser.{ClassRep, PlainClassRep}
import io.atomicbits.scraml.parser.model._

import scala.reflect.macros.whitebox

/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ActionExpander {

  def expandAction(action: Action, schemaLookup: SchemaLookup, c: whitebox.Context): List[c.universe.Tree] = {

    import c.universe._

    // ToDo: handle different types of ClassReps.

    // We currently only support the first context-type mimeType that we see. We should extend this later on.
    val bodyMimeType = action.body.values.toList.headOption
    val hasBody = bodyMimeType.isDefined
    val maybeBodyRootId = bodyMimeType.flatMap(_.schema).flatMap(schemaLookup.externalSchemaLinks.get)
    val maybeBodyClassRep = maybeBodyRootId.flatMap(schemaLookup.canonicalNames.get)

    val formParameters: Map[String, List[Parameter]] = bodyMimeType.map(_.formParameters).getOrElse(Map.empty)
    val isMultipartFormUpload = bodyMimeType.map(_.mimeType).contains("multipart/form-data")

    // We currently only support the first response mimeType that we see. We should extend this later on.
    val response = action.responses.values.toList.headOption
    // We currently only support the first response body mimeType that we see. We should extend this later on.
    val responseMimeType = response.flatMap(_.body.values.toList.headOption)
    val hasResponse = responseMimeType.isDefined
    val maybeResponseRootId = responseMimeType.flatMap(_.schema).flatMap(schemaLookup.externalSchemaLinks.get)
    val maybeResponseClassRep = maybeResponseRootId.flatMap(schemaLookup.canonicalNames.get)

    val (hasJsonDtoBody, bodyClassRep) = maybeBodyClassRep match {
      case Some(bdClass) => (true, bdClass)
      case None          => (false, PlainClassRep("String"))
    }

    val (hasJsonDtoResponse, responseClassRep) = maybeResponseClassRep match {
      case Some(rsClass) => (true, rsClass)
      case None          => (false, PlainClassRep("String"))
    }


    def expandGetAction(): List[c.universe.Tree] = {

      val queryParameterMethodParameters =
        action.queryParameters.toList.map(param => expandParameterAsMethodParameter(param))
      val queryParameterMapEntries =
        action.queryParameters.toList.map(param => expandParameterAsMapEntry(param))

      List(
        q"""
          def get(..$queryParameterMethodParameters) = new GetSegment(
            queryParams = Map(
              ..$queryParameterMapEntries
            ),
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            req = requestBuilder
          ) {

            ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

          }
       """)
    }

    def expandPutAction(): List[c.universe.Tree] = {
      val defaultActions =
        if (hasBody)
          List(
            q"""
            def put(body: String) = new PutSegment(
              validAcceptHeaders = List(..${validAcceptHeaders()}),
              validContentTypeHeaders = List(..${validContentTypeHeaders()}),
              req = requestBuilder) {

              ..${expandHeadersAndExecution(hasBody = true, PlainClassRep("String"))}

            }
            """,
            q"""
            def put(body: JsValue) = new PutSegment(
              validAcceptHeaders = List(..${validAcceptHeaders()}),
              validContentTypeHeaders = List(..${validContentTypeHeaders()}),
              req = requestBuilder) {

              ..${expandHeadersAndExecution(hasBody = true, PlainClassRep("JsValue"))}

            }
            """
          )
        else
          List(
            q"""
            def put() = new PutSegment(
              validAcceptHeaders = List(..${validAcceptHeaders()}),
              validContentTypeHeaders = List(..${validContentTypeHeaders()}),
              req = requestBuilder) {

              ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

            }
            """
          )

      val additionalAction =
        if (hasJsonDtoBody) {
          val typeTypeName = TypeNameExpander.expand(bodyClassRep, c) // TypeName(bodyClassRep.name)
          val bodyParam = List(q"val body: $typeTypeName")
          List(
            q"""
              def put(..$bodyParam) = new PutSegment(
                validAcceptHeaders = List(..${validAcceptHeaders()}),
                validContentTypeHeaders = List(..${validContentTypeHeaders()}),
                req = requestBuilder) {

                  ..${expandHeadersAndExecution(hasBody = true, bodyClassRep)}

              }
             """
          )
        } else Nil

      defaultActions ++ additionalAction
    }


    def expandPostAction(): List[c.universe.Tree] = {

      def expandRegularPostAction(): List[c.universe.Tree] = {
        // We support a custom body instead.
        val defaultActions =
          if (hasBody)
            List(
              q"""
              def post(body: String) = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(..${validAcceptHeaders()}),
                validContentTypeHeaders = List(..${validContentTypeHeaders()}),
                req = requestBuilder) {

                ..${expandHeadersAndExecution(hasBody = true, PlainClassRep("String"))}

              }
              """,
              q"""
              def post(body: JsValue) = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(..${validAcceptHeaders()}),
                validContentTypeHeaders = List(..${validContentTypeHeaders()}),
                req = requestBuilder) {

                ..${expandHeadersAndExecution(hasBody = true, PlainClassRep("JsValue"))}

              }
              """
            )
          else
            List(
              q"""
              def post() = new PostSegment(
                formParams = Map.empty,
                multipartParams = List.empty,
                validAcceptHeaders = List(..${validAcceptHeaders()}),
                validContentTypeHeaders = List(..${validContentTypeHeaders()}),
                req = requestBuilder) {

                ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

              }
              """
            )

        val additionalAction =
          if (hasJsonDtoBody) {
            val typeTypeName = TypeNameExpander.expand(bodyClassRep, c) // TypeName(bodyClassRep.name)
            val bodyParam = List(q"val body: $typeTypeName")
            List(
              q"""
                  def post(..$bodyParam) = new PostSegment(
                    formParams = Map.empty,
                    multipartParams = List.empty,
                    validAcceptHeaders = List(..${validAcceptHeaders()}),
                    validContentTypeHeaders = List(..${validContentTypeHeaders()}),
                    req = requestBuilder) {

                      ..${expandHeadersAndExecution(hasBody = true, bodyClassRep)}

                  }
                """
            )
          } else Nil

        defaultActions ++ additionalAction

      }

      def expandFormPostAction(): List[c.universe.Tree] = {
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
          q"""
          def post(..$formParameterMethodParameters) = new PostSegment(
            formParams = Map(
              ..$formParameterMapEntries
            ),
            multipartParams = List.empty,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

              ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

          }
         """)
      }

      def expandMultipartFormPostAction(): List[c.universe.Tree] = {
        List(
          q"""
          def post(parts: List[BodyPart]) = new PostSegment(
            formParams = Map.empty,
            multipartParams = parts,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

              ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

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

    def expandDeleteAction(): List[c.universe.Tree] = {
      List(
        q"""
          def delete() = new DeleteSegment(
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ..${expandHeadersAndExecution(hasBody = false, PlainClassRep("String"))}

          }
       """)
    }

    def expandHeadersAndExecution(hasBody: Boolean, bodyClassRep: ClassRep): List[c.universe.Tree] = {
      List(
        q"""
           def headers(headers: (String, String)*) = new HeaderSegment(
             headers = headers.toMap,
             req = requestBuilder
           ) {
             ..${expandExecution(hasBody, bodyClassRep)}
           }
         """
      ) ++ expandExecution(hasBody, bodyClassRep)
    }

    def expandExecution(hasBody: Boolean, bodyClassRep: ClassRep): List[c.universe.Tree] = {
      val bodyTypeName = TypeNameExpander.expand(bodyClassRep, c) // TypeName(bodyClassRep.name)
      val responseTypeName = TypeNameExpander.expand(responseClassRep, c) // TypeName(responseClassRep.name)
      val executeSegment =
        if (hasBody) {
          q"""
           private val executeSegment = new ExecuteSegment[$bodyTypeName, $responseTypeName](requestBuilder, Some(body))
         """
        } else {
          q"""
           private val executeSegment = new ExecuteSegment[$bodyTypeName, $responseTypeName](requestBuilder, None)
         """
        }
      val jsonExecutor =
        if (hasResponse) List( q""" def execToJson() = executeSegment.execToJson() """)
        else Nil
      val jsonDtoExecutor =
        if (hasJsonDtoResponse) List( q""" def execToDto() = executeSegment.execToDto() """)
        else Nil
      List(
        executeSegment,
        q""" def exec() = executeSegment.exec() """,
        q""" def execToResponse() = executeSegment.execToResponse() """
      ) ++ jsonExecutor ++ jsonDtoExecutor
    }

    def needsAcceptHeader: Boolean = {
      action.responses.values.toList.flatMap(_.headers).nonEmpty
    }

    def validAcceptHeaders(): List[c.universe.Tree] = {
      action.responses.values.toList.flatMap(response => response.headers.keys.map(header => q"$header"))
    }

    def needsContentTypeHeader: Boolean = {
      action.body.keys.toList.nonEmpty
    }

    def validContentTypeHeaders(): List[c.universe.Tree] = {
      action.body.keys.toList.map(header => q"$header")
    }

    def expandParameterAsMethodParameter(qParam: (String, Parameter)): c.universe.Tree = {
      val (queryParameterName, parameter) = qParam

      val nameTermName = TermName(queryParameterName)
      val typeTypeName = parameter.parameterType match {
        case StringType  => TypeName("String")
        case IntegerType => TypeName("Int")
        case NumberType  => TypeName("Double")
        case BooleanType => TypeName("Boolean")
        case FileType    => sys.error(s"RAML type 'FileType' is not yet supported.")
        case DateType    => sys.error(s"RAML type 'DateType' is not yet supported.")
      }

      if (parameter.required) {
        q"val $nameTermName: $typeTypeName"
      } else {
        q"val $nameTermName: Option[$typeTypeName]"
      }
    }

    def expandParameterAsMapEntry(qParam: (String, Parameter)): c.universe.Tree = {
      val (queryParameterName, parameter) = qParam
      val nameTermName = TermName(queryParameterName)
      parameter match {
        case Parameter(_, true)  => q"""$queryParameterName -> Option($nameTermName).map(_.toString)"""
        case Parameter(_, false) => q"""$queryParameterName -> $nameTermName.map(_.toString)"""
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

}
