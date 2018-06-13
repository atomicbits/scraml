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

package io.atomicbits.scraml.dsl.scalaplay

import io.atomicbits.scraml.dsl.scalaplay.json.JsonOps
import play.api.libs.json._

/**
  * Created by peter on 27/07/15.
  */
sealed trait HttpParam

case object HttpParam {

  def toFormUrlEncoded(json: JsValue): Map[String, HttpParam] = {

    def flatten(jsObj: JsObject): Map[String, HttpParam] = {
      jsObj.value.collect {
        case (field, JsString(value))  => field -> SimpleHttpParam.create(value)
        case (field, JsNumber(value))  => field -> SimpleHttpParam.create(value)
        case (field, JsBoolean(value)) => field -> SimpleHttpParam.create(value)
        case (field, jsObj: JsObject)  => field -> SimpleHttpParam.create(jsObj)
      }.toMap
    }

    json match {
      case jsObject: JsObject => flatten(jsObject)
      case other              => Map.empty
    }
  }

}

case class SimpleHttpParam(parameter: String) extends HttpParam

object SimpleHttpParam {

  def create(value: Any): SimpleHttpParam = SimpleHttpParam(value.toString)

}

case class ComplexHttpParam(json: String) extends HttpParam

object ComplexHttpParam {

  def create[P](value: P)(implicit formatter: Format[P]): ComplexHttpParam =
    ComplexHttpParam(JsonOps.toString(formatter.writes(value)))

}

case class RepeatedHttpParam(parameters: List[String]) extends HttpParam

object RepeatedHttpParam {

  def create(values: List[Any]): RepeatedHttpParam = RepeatedHttpParam(values.map(_.toString))

}
