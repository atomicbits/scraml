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
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 4/12/16.
  */
case class ParsedProperties(valueMap: Map[String, Property] = Map.empty) {

  def apply(name: String): Property = valueMap(name)

  def get(name: String): Option[Property] = valueMap.get(name)

  def map(f: Property => Property): ParsedProperties = {
    copy(valueMap = valueMap.mapValues(f))
  }

  def asTypeMap: Map[String, ParsedType] = {
    valueMap.mapValues(_.propertyType.parsed)
  }

  val values: List[Property] = valueMap.values.toList

  val types: List[ParsedType] = valueMap.values.map(_.propertyType.parsed).toList

  val isEmpty = valueMap.isEmpty

}


object ParsedProperties {

  def apply(jsValueOpt: Option[JsValue], model: TypeModel)(implicit parseContext: ParseContext): Try[ParsedProperties] = {

    def jsObjectToProperties(jsObject: JsObject): Try[ParsedProperties] = {

      val valueMap: Map[String, Try[Property]] =
        jsObject.value.collect {
          case (name, ParsedType(tryType)) =>
            name -> tryType.map { paramType =>
              Property(
                name = name,
                propertyType = TypeRepresentation(paramType.asTypeModel(model)),
                required = paramType.required.getOrElse(paramType.defaultRequiredValue)
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
