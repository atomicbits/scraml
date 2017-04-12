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

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedType
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import io.atomicbits.scraml.util.TryUtils._
import play.api.libs.json.{ JsObject, JsValue }

import scala.language.postfixOps
import scala.util.{ Success, Try }

case class Parameters(valueMap: Map[String, Parameter] = Map.empty) {

  def nonEmpty: Boolean = valueMap.nonEmpty

  def byName(name: String): Option[Parameter] = valueMap.get(name)

  val values: List[Parameter] = valueMap.values.toList

  val isEmpty = valueMap.isEmpty

  def mapValues(fn: Parameter => Parameter): Parameters = copy(valueMap = valueMap.mapValues(fn))

}

/**
  * Created by peter on 26/08/16.
  */
object Parameters {

  def apply(jsValueOpt: Option[JsValue])(implicit parseContext: ParseContext): Try[Parameters] = {

    /**
      * @param name The name of the parameter
      * @return A pair whose first element is de actual parameter name and the second element indicates whether or not the
      *         parameter is an optional parameter. None (no indication for optional is given) Some(false) (it is an optional parameter).
      */
    def detectRequiredParameterName(name: String): (String, Option[Boolean]) = {
      if (name.length > 1 && name.endsWith("?")) (name.dropRight(1), Some(false))
      else (name, None)
    }

    def jsObjectToParameters(jsObject: JsObject): Try[Parameters] = {

      val valueMap: Map[String, Try[Parameter]] =
        jsObject.value.collect {
          case (name, ParsedType(tryType)) =>
            val (actualName, requiredProp) = detectRequiredParameterName(name)
            actualName -> tryType.map { paramType =>
              Parameter(
                name          = actualName,
                parameterType = TypeRepresentation(paramType),
                required      = requiredProp.getOrElse(paramType.required.getOrElse(true)) // paramType.defaultRequiredValue
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
