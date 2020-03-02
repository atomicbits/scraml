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

import play.api.libs.json.{JsArray, JsObject, JsString, Json}

/**
  * Created by peter on 26/02/16.
  */
object KeyedList {

  /**
    * A keyed list is of the form (in yaml):
    *
    * * traits:
    * *    - secured: !include traits/secured.raml
    * *    - rateLimited: !include traits/rate-limited.raml
    *
    * And its is parsed to JSON as:
    *
    * * "traits": [
    * *   {
    * *     "secured": {
    * *       "!include": "traits/secured.raml"
    * *     }
    * *   },
    * *   {
    * *     "rateLimited": {
    * *       "!include": "traits/rate-limited.raml"
    * *     }
    * *   }
    * * ]
    *
    * toJsObject 'flattens' this to:
    *
    * * "traits":
    * *   {
    * *     "secured": {
    * *       "!include": "traits/secured.raml"
    * *     },
    * *     "rateLimited": {
    * *       "!include": "traits/rate-limited.raml"
    * *     }
    * *   }
    *
    * @param keyedList
    * @return
    */
  def toJsObject(keyedList: JsArray): JsObject = {
    val collected: scala.collection.IndexedSeq[JsObject] =
      keyedList.value.collect {
        case jsObj: JsObject => jsObj
        case JsString(value) => Json.obj() + (value -> Json.obj())
      }
    collected.foldLeft(Json.obj()) {
      case (aggr, js) => aggr ++ js
    }
  }

}
