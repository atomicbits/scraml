/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.parser

import play.api.libs.json.{ JsArray, JsObject }

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
    keyedList.value.collect {
      case jsObj: JsObject => jsObj
    } reduce (_ ++ _)
  }

}
