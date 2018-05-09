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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedType
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{ JsObject, JsValue, Json }

import scala.util.{ Success, Try }

/**
  * Created by peter on 10/02/16.
  */
case class Action(actionType: Method,
                  headers: Parameters,
                  queryParameters: Parameters,
                  body: Body,
                  responses: Responses,
                  queryString: Option[QueryString] = None)

object Action {

  def apply(actionDef: (Method, JsObject))(implicit parseContext: ParseContext): Try[Action] = {
    val (method, jsObj) = actionDef
    createAction(method, jsObj)
  }

  private def createAction(actionType: Method, jsObject: JsObject)(implicit parseContext: ParseContext): Try[Action] = {

    parseContext.traits.applyToAction(jsObject) { json =>
      val tryQueryParameters = Parameters(jsValueOpt = (json \ "queryParameters").toOption)

      val tryQueryString =
        (json \ "queryString").toOption.collect {
          case ParsedType(bType) => bType.map(Option(_))
        } getOrElse Success(None)

      val tryHeaders = Parameters((json \ "headers").toOption)

      val tryBody = Body(json)

      val tryResponses = Responses(json)

      for {
        queryParameters <- tryQueryParameters
        headers <- tryHeaders
        body <- tryBody
        responses <- tryResponses
        queryStringOpt <- tryQueryString
      } yield
        Action(
          actionType      = actionType,
          headers         = headers,
          queryParameters = queryParameters,
          body            = body,
          responses       = responses,
          queryString     = queryStringOpt.map(qs => QueryString(queryStringType = TypeRepresentation(qs)))
        )
    }

  }

}
