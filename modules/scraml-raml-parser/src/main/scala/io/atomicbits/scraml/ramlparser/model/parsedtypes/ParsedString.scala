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

import io.atomicbits.scraml.ramlparser.model._
import play.api.libs.json.{ JsObject, JsString, JsValue, Json }

import scala.util.{ Success, Try }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 1/04/16.
  */
case class ParsedString(id: Id                    = ImplicitId,
                        format: Option[String]    = None,
                        pattern: Option[String]   = None,
                        minLength: Option[Int]    = None,
                        maxLength: Option[Int]    = None,
                        required: Option[Boolean] = None,
                        model: TypeModel          = RamlModel)
    extends PrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedString = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedString {

  val value = "string"

  def apply(json: JsValue): Try[ParsedString] = {

    val model: TypeModel = TypeModel(json)

    val id = IdExtractor(json)

    Success(
      ParsedString(
        id        = id,
        format    = json.fieldStringValue("pattern"),
        pattern   = json.fieldStringValue("format"),
        minLength = json.fieldIntValue("minLength"),
        maxLength = json.fieldIntValue("maxLength"),
        required  = json.fieldBooleanValue("required"),
        model     = model
      )
    )
  }

  def unapply(json: JsValue): Option[Try[ParsedString]] = {

    (ParsedType.typeDeclaration(json), (json \ ParsedEnum.value).toOption, json) match {
      case (Some(JsString(ParsedString.value)), None, _) => Some(ParsedString(json))
      case (_, _, JsString(ParsedString.value))          => Some(Success(ParsedString()))
      case _                                             => None
    }

  }

}
