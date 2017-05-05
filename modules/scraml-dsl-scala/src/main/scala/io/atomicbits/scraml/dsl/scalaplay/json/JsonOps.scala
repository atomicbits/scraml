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

package io.atomicbits.scraml.dsl.scalaplay.json

import io.atomicbits.scraml.dsl.scalaplay.{ HttpParam, SimpleHttpParam }
import play.api.libs.json._

import scala.language.postfixOps

/**
  * Created by peter on 7/04/17.
  */
object JsonOps {

  def toString(json: JsValue): String = {
    json match {
      case JsString(jsString) => jsString // JsString(jsString).toString would have put quotes around the jsString.
      case other              => other.toString()
    }
  }

  def toFormUrlEncoded(json: JsValue): Map[String, HttpParam] = {

    def flatten(jsObj: JsObject): Map[String, HttpParam] = {
      jsObj.value.collect {
        case (field, JsString(value))  => field -> SimpleHttpParam.create(value)
        case (field, JsNumber(value))  => field -> SimpleHttpParam.create(value)
        case (field, JsBoolean(value)) => field -> SimpleHttpParam.create(value)
        case (field, jsObj: JsObject)  => field -> SimpleHttpParam.create(jsObj)
      } toMap
    }

    json match {
      case JsString(jsString) => Map.empty
      case jsObject: JsObject => flatten(jsObject)
      case other              => Map.empty
    }
  }

}
