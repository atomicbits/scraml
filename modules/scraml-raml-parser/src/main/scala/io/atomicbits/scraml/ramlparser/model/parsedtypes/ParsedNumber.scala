/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model._
import play.api.libs.json.{ JsObject, JsString, JsValue }

import scala.util.{ Success, Try }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 1/04/16.
  */
case class ParsedNumber(id: Id                    = ImplicitId,
                        format: Option[String]    = None,
                        minimum: Option[Int]      = None,
                        maximum: Option[Int]      = None,
                        multipleOf: Option[Int]   = None,
                        required: Option[Boolean] = None,
                        model: TypeModel          = RamlModel)
    extends PrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedNumber = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedNumber {

  val value = "number"

  def apply(json: JsValue): Try[ParsedNumber] = {

    val model: TypeModel = TypeModel(json)

    val id = JsonSchemaIdExtractor(json)

    Success(
      ParsedNumber(
        id         = id,
        format     = json.fieldStringValue("format"),
        minimum    = json.fieldIntValue("minimum"),
        maximum    = json.fieldIntValue("maximum"),
        multipleOf = json.fieldIntValue("multipleOf"),
        required   = json.fieldBooleanValue("required"),
        model      = model
      )
    )
  }

  def unapply(json: JsValue): Option[Try[ParsedNumber]] = {

    (ParsedType.typeDeclaration(json), json) match {
      case (Some(JsString(ParsedNumber.value)), _) => Some(ParsedNumber(json))
      case (_, JsString(ParsedNumber.value))       => Some(Success(new ParsedNumber()))
      case _                                       => None
    }

  }

}
