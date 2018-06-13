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

package io.atomicbits.scraml.ramlparser.parser

import play.api.libs.json.{ JsObject, JsString, JsValue }

/**
  * Created by peter on 26/02/16.
  */
object Sourced {

  val sourcefield = "_source"

  /**
    * Unwraps a JSON object that has a "_source" field into the source value and the original json object.
    */
  def unapply(json: JsValue): Option[(JsValue, String)] = {

    json match {
      case jsObj: JsObject =>
        (jsObj \ sourcefield).toOption.collect {
          case JsString(includeFile) => (jsObj - sourcefield, includeFile)
        }
      case _ => None
    }

  }

}
