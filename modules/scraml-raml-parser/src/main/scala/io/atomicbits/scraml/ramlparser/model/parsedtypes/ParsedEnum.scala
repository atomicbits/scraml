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
import play.api.libs.json.{ JsArray, JsObject, JsString, JsValue }

import scala.util.{ Success, Try }

/**
  * Created by peter on 1/04/16.
  */
case class ParsedEnum(id: Id, choices: List[String], required: Option[Boolean] = None, model: TypeModel = RamlModel)
    extends NonPrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedEnum = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedEnum {

  val value = "enum"

  def apply(json: JsValue): Try[ParsedEnum] = {

    val model: TypeModel = TypeModel(json)

    val id = JsonSchemaIdExtractor(json)

    val choices = (json \ "enum").asOpt[List[String]]

    val required = (json \ "required").asOpt[Boolean]

    Success(ParsedEnum(id, choices.getOrElse(List.empty), required, model))
  }

  def unapply(json: JsValue): Option[Try[ParsedEnum]] = {

    (ParsedType.typeDeclaration(json), (json \ ParsedEnum.value).toOption) match {
      case (Some(JsString(ParsedString.value)), Some(JsArray(x))) => Some(ParsedEnum(json))
      case (None, Some(JsArray(x)))                               => Some(ParsedEnum(json))
      case _                                                      => None
    }

  }

}
