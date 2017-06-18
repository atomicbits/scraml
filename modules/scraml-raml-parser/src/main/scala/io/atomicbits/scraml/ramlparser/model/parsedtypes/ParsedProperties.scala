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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model.{ TypeModel, TypeRepresentation }
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import io.atomicbits.scraml.util.TryUtils._
import play.api.libs.json.{ JsObject, JsValue }

import scala.util.{ Success, Try }

/**
  * Created by peter on 4/12/16.
  */
case class ParsedProperties(valueMap: Map[String, ParsedProperty] = Map.empty) {

  def apply(name: String): ParsedProperty = valueMap(name)

  def get(name: String): Option[ParsedProperty] = valueMap.get(name)

  def -(name: String): ParsedProperties = copy(valueMap = valueMap - name)

  def map(f: ParsedProperty => ParsedProperty): ParsedProperties = {
    copy(valueMap = valueMap.mapValues(f))
  }

  def asTypeMap: Map[String, ParsedType] = {
    valueMap.mapValues(_.propertyType.parsed)
  }

  val values: List[ParsedProperty] = valueMap.values.toList

  val types: List[ParsedType] = valueMap.values.map(_.propertyType.parsed).toList

  val isEmpty = valueMap.isEmpty

}

object ParsedProperties {

  def apply(jsValueOpt: Option[JsValue], model: TypeModel)(implicit parseContext: ParseContext): Try[ParsedProperties] = {

    /**
      * @param name The name of the property
      * @return A pair whose first element is de actual property name and the second element indicates whether or not the
      *         property is an optional property. None (no indication for optional is given) Some(false) (it is an optional property).
      */
    def detectRequiredPropertyName(name: String): (String, Option[Boolean]) = {
      if (name.length > 1 && name.endsWith("?")) (name.dropRight(1), Some(false))
      else (name, None)
    }

    def jsObjectToProperties(jsObject: JsObject): Try[ParsedProperties] = {

      val valueMap: Map[String, Try[ParsedProperty]] =
        jsObject.value
          .mapValues(model.mark)
          .collect {
            case (name, ParsedType(tryType)) =>
              val (actualName, requiredProp) = detectRequiredPropertyName(name)
              actualName -> tryType.map { paramType =>
                val paramTypeWithRightTypeModel = paramType.asTypeModel(model)
                ParsedProperty(
                  name         = actualName,
                  propertyType = TypeRepresentation(paramTypeWithRightTypeModel),
                  required = requiredProp.getOrElse(
                    paramTypeWithRightTypeModel.required.getOrElse(
                      paramTypeWithRightTypeModel.defaultRequiredValue
                  )
                )
              )
            }
        } toMap

      accumulate(valueMap).map(vm => ParsedProperties(vm))
    }

    jsValueOpt.collect {
      case jsObj: JsObject => jsObjectToProperties(jsObj)
    } getOrElse Success(ParsedProperties())

  }

}
