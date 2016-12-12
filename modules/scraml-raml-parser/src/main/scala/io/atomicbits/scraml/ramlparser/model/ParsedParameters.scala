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
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Success, Try}
import io.atomicbits.scraml.util.TryUtils._

import scala.language.postfixOps


case class ParsedParameters(valueMap: Map[String, ParsedParameter] = Map.empty) {

  def nonEmpty: Boolean = valueMap.nonEmpty

  def byName(name: String): Option[ParsedParameter] = valueMap.get(name)

  val values: List[ParsedParameter] = valueMap.values.toList

  val isEmpty = valueMap.isEmpty

}


/**
  * Created by peter on 26/08/16.
  */
object ParsedParameters {


  def apply(jsValueOpt: Option[JsValue])(implicit parseContext: ParseContext): Try[ParsedParameters] = apply(jsValueOpt, None)


  def apply(jsValueOpt: Option[JsValue], overrideRequired: Option[Boolean])(implicit parseContext: ParseContext): Try[ParsedParameters] = {

    def jsObjectToParameters(jsObject: JsObject): Try[ParsedParameters] = {

      val valueMap: Map[String, Try[ParsedParameter]] =
        jsObject.value.collect {
          case (name, ParsedType(tryType)) =>
            name -> tryType.map { paramType =>
              ParsedParameter(
                name = name,
                parameterType = TypeRepresentation(paramType),
                required = paramType.required.getOrElse(overrideRequired.getOrElse(paramType.defaultRequiredValue)),
                repeated = false
              )
            }
        } toMap

      accumulate(valueMap).map(vm => ParsedParameters(vm))
    }

    jsValueOpt.collect {
      case jsObj: JsObject => jsObjectToParameters(jsObj)
    } getOrElse Success(ParsedParameters())
  }

}
