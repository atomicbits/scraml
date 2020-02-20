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

  def mapValues(fn: Parameter => Parameter): Parameters = copy(valueMap = valueMap.view.mapValues(fn).toMap)

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
