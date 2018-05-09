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

import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{ JsObject, JsValue }

import scala.util.{ Success, Try }
import io.atomicbits.scraml.util.TryUtils._

/**
  * Created by peter on 26/08/16.
  */
case class Responses(responseMap: Map[StatusCode, Response] = Map.empty) {

  val isEmpty = responseMap.isEmpty

  def get(statusCode: String): Option[Response] = responseMap.get(StatusCode(statusCode))

}

object Responses {

  def apply(jsValue: JsValue)(implicit parseContext: ParseContext): Try[Responses] = {

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

    (jsValue \ Response.value).toOption match {
      case Some(jsObject: JsObject) => fromJsObject(jsObject)
      case _                        => Success(Responses())
    }
  }

}
