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

import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Success, Try}
import io.atomicbits.scraml.ramlparser.parser.TryUtils._

/**
  * Created by peter on 26/08/16.
  */
case class Responses(responseMap: Map[StatusCode, Response] = Map.empty) {

  val isEmpty = responseMap.isEmpty

}


object Responses {

  def apply(jsValue: JsValue): Try[Responses] = {

    def fromJsObject(json: JsObject): Try[Responses] = {

      val tryResponseMap: Seq[Try[(StatusCode, Response)]] =
        json.value.collect {
          case Response(tryResponse) =>
            tryResponse.map { response =>
              response.status -> response
            }
        } toSeq

      accumulate(tryResponseMap).map(responses => Responses(responses.toMap))
    }

    jsValue match {
      case jsObject: JsObject => fromJsObject(jsObject)
      case _                  => Success(Responses())
    }
  }

}
