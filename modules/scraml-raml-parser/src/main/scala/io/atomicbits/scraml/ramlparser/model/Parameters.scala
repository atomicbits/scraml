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
import io.atomicbits.scraml.ramlparser.parser.{ParseContext}
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Success, Try}
import io.atomicbits.scraml.ramlparser.parser.TryUtils._

import scala.language.postfixOps


case class Parameters(valueMap: Map[String, Parameter] = Map.empty) {

  def byName(name: String): Option[Parameter] = valueMap.get(name)

  val values: List[Parameter] = valueMap.values.toList

  val isEmpty = valueMap.isEmpty

}


/**
  * Created by peter on 26/08/16.
  */
object Parameters {

  def apply(jsValueOpt: Option[JsValue])(implicit parseContext: ParseContext): Try[Parameters] = {


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

    jsValueOpt.collect {
      case jsObj: JsObject => jsObjectToParameters(jsObj)
    } getOrElse Success(Parameters())
  }

}
