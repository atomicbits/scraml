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

import play.api.libs.json._

/**
  * Created by peter on 19/08/16.
  */
object JsUtils {

  implicit class JsOps(val jsValue: JsValue) {

    def fieldStringValue(field: String): Option[String] = {
      (jsValue \ field).toOption.collect {
        case JsString(value) => value
      }
    }

    def fieldIntValue(field: String): Option[Int] = {
      (jsValue \ field).toOption.collect {
        case JsNumber(value) => value.intValue
      }
    }

    def fieldDoubleValue(field: String): Option[Double] = {
      (jsValue \ field).toOption.collect {
        case JsNumber(value) => value.doubleValue
      }
    }

    def fieldBooleanValue(field: String): Option[Boolean] = {
      (jsValue \ field).toOption.collect {
        case JsBoolean(bool) => bool
      }
    }

    def fieldStringListValue(field: String): Option[Seq[String]] = {
      (jsValue \ field).toOption.collect {
        case JsArray(values) =>
          values.collect {
            case JsString(value) => value
          }
      } map(_.toSeq)
    }

  }

}
