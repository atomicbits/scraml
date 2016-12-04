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
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._


/**
  * Created by peter on 1/04/16.
  */
case class ParsedNull(id: Id = ImplicitId,
                      required: Option[Boolean] = None,
                      model: TypeModel = RamlModel) extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedNull = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}


object ParsedNull {

  val value = "null"


  def apply(json: JsValue): Try[ParsedNull] = {

    val model: TypeModel = TypeModel(json)

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    Success(
      ParsedNull(
        id = id,
        required = json.fieldBooleanValue("required"),
        model = model
      )
    )
  }


  def unapply(json: JsValue): Option[Try[ParsedNull]] = {

    (ParsedType.typeDeclaration(json), json) match {
      case (Some(JsString(ParsedNull.value)), _) => Some(ParsedNull(json))
      case (_, JsString(ParsedNull.value))       => Some(Success(new ParsedNull()))
      case _                                     => None
    }

  }

}
