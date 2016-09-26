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
        case JsNumber(value) => value.intValue()
      }
    }

    def fieldDoubleValue(field: String): Option[Double] = {
      (jsValue \ field).toOption.collect {
        case JsNumber(value) => value.doubleValue()
      }
    }

    def fieldBooleanValue(field: String): Option[Boolean] = {
      (jsValue \ field).toOption.collect {
        case JsBoolean(bool) => bool
      }
    }

    def fieldStringListValue(field: String): Option[Seq[String]] = {
      (jsValue \ field).toOption.collect {
        case JsArray(values) => values.collect {
          case JsString(value) => value
        }
      }
    }

  }

}
