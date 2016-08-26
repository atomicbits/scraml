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

import io.atomicbits.scraml.ramlparser.model.types.Type
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Failure, Success, Try}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import io.atomicbits.scraml.ramlparser.parser.TryUtils._

import scala.language.postfixOps


case class Parameters(valueMap: Map[String, Parameter]) {

  def byName(name: String): Option[Parameter] = valueMap.get(name)

  val values: List[Parameter] = valueMap.values.toList

}

/**
  * Created by peter on 26/08/16.
  */
object Parameters {

  def apply(jsValue: JsValue)(implicit parseContext: ParseContext): Try[Parameters] = {


    def jsObjectToParameters(jsObject: JsObject): Try[Parameters] = {

      val valueMap =
        jsObject.value.collect {
          case (name, Type(tryType)) =>
            name -> tryType.map { paramType =>
              Parameter(
                name = name,
                parameterType = paramType,
                required = paramType.isRequired,
                repeated = false
              )
            }
        } toMap

      accumulate(valueMap).map(vm => Parameters(vm))
    }

    jsValue match {
      case jsObj: JsObject => jsObjectToParameters(jsObj)
      case x               => Failure(RamlParseException(s"Cannot parse parameters from a non-object field $x in ${parseContext.head}"))
    }
  }


  def unapply(jsValue: JsValue)(implicit parseContext: ParseContext): Option[Try[Parameters]] = {
    jsValue match {
      case jsObj: JsObject => Some(Parameters(jsObj))
      case _               => None
    }
  }

}
