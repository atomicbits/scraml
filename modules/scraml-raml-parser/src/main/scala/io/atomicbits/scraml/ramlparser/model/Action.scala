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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Action(actionType: Method,
                  headers: Parameters,
                  queryParameters: Parameters,
                  body: Body,
                  responses: Responses)

object Action {

  def unapply(actionMap: (String, JsValue))(implicit parseContext: ParseContext): Option[Try[Action]] = {

    val actionMapOpt =
      actionMap match {
        case (Method(method), jsObj: JsObject) => Some((method, jsObj))
        case _                                 => None
      }

    actionMapOpt.collect {
      case (method, jsObj) => createAction(method, jsObj)
    }

  }


  private def createAction(actionType: Method, jsObject: JsObject)(implicit parseContext: ParseContext): Try[Action] = {

    parseContext.traits.applyTo(jsObject) { json =>

      val tryQueryParameters = Parameters((json \ "queryParameters").toOption)

      val tryHeaders = Parameters((json \ "headers").toOption)

      val tryBody = Body(json)

      val tryResponses = Responses(json)

      for {
        queryParameters <- tryQueryParameters
        headers <- tryHeaders
        body <- tryBody
        responses <- tryResponses
      } yield Action(
        actionType = actionType,
        headers = headers,
        queryParameters = queryParameters,
        body = body,
        responses = responses
      )
    }

  }

}
