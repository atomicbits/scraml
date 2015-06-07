/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.parser.model._

import scala.reflect.macros.whitebox

/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ActionExpander {

  def expandAction(action: Action, c: whitebox.Context): c.universe.Tree = {

    import c.universe._


    def expandGetAction(): c.universe.Tree = {

      val queryParameterMethodParameters =
        action.queryParameters.toList.map(param => expandParameterAsMethodParameter(param))
      val queryParameterMapEntries =
        action.queryParameters.toList.map(param => expandParameterAsMapEntry(param))

      q"""
          def get(..$queryParameterMethodParameters) = new GetSegment(
            queryParams = Map(
              ..$queryParameterMapEntries
            ),
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            req = requestBuilder
          ) {

            ..${expandHeaders()}

          }
       """
    }

    def expandPutAction(): c.universe.Tree = {
      q"""
          def put(body: String) = new PutSegment(
            body = body,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ..${expandHeaders()}

          }
       """
    }

    def expandPostAction(): c.universe.Tree = {

      /**
       * We currently only support the first context-type mimeType that we see in a POST statement. We should
       * extend this later on.
       */
      val mimeType = action.body.values.toList.headOption

      val formParameters: Map[String, List[Parameter]] = mimeType.map(_.formParameters).getOrElse(Map.empty)

      if (formParameters.isEmpty) {
        // We support a custom body instead.
        q"""
          def post(body: String) = new PostSegment(
            formParams = Map.empty,
            body = body,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ..${expandHeaders()}

          }
       """
      } else {
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

        q"""
          def post(..$formParameterMethodParameters) = new PostSegment(
            formParams = Map(
              ..$formParameterMapEntries
            ),
            body = None,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ..${expandHeaders()}

          }
       """
      }

    }

    def expandDeleteAction(): c.universe.Tree = {
      q"""
          def delete(body: Option[String] = None) = new DeleteSegment(
            body = body,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ..${expandHeaders()}

          }
       """
    }

    def expandHeaders(): List[c.universe.Tree] = {
      List(
        q"""
           def headers(headers: (String, String)*) = new HeaderSegment(
             headers = headers.toMap,
             req = requestBuilder
           ) {
             ${expandExecution()}
           }
         """,
        q"""
           ${expandExecution()}
         """
      )
    }

    def expandExecution(): c.universe.Tree = {
      q"""
         def execute() = new ExecuteSegment(requestBuilder).execute()
       """
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
        case StringType => TypeName("String")
        case IntegerType => TypeName("Int")
        case NumberType => TypeName("Double")
        case BooleanType => TypeName("Boolean")
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
        case Parameter(_, true) => q"""$queryParameterName -> Option($nameTermName).map(_.toString)"""
        case Parameter(_, false) => q"""$queryParameterName -> $nameTermName.map(_.toString)"""
      }
    }

    action.actionType match {
      case Get => expandGetAction()
      case Put => expandPutAction()
      case Post => expandPostAction()
      case Delete => expandDeleteAction()
      case unknownAction => sys.error(s"$unknownAction actions are not supported yet.")
    }

  }

}
