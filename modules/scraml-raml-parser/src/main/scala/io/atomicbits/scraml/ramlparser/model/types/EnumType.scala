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

package io.atomicbits.scraml.ramlparser.model.types

import io.atomicbits.scraml.ramlparser.model._
import play.api.libs.json.{JsArray, JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class EnumType(id: Id,
                    choices: List[String],
                    required: Option[Boolean] = None,
                    model: TypeModel = RamlModel) extends NonePrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): EnumType = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): Type = copy(model = typeModel)

}


object EnumType {

  val value = "enum"

  def apply(json: JsValue): Try[EnumType] = {

    val model: TypeModel = TypeModel(json)

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    val choices = (json \ "enum").asOpt[List[String]]

    val required = (json \ "required").asOpt[Boolean]

    Success(EnumType(id, choices.getOrElse(List.empty), required, model))
  }


  def unapply(json: JsValue): Option[Try[EnumType]] = {

    (Type.typeDeclaration(json), (json \ EnumType.value).toOption) match {
      case (Some(JsString(StringType.value)), Some(JsArray(x))) => Some(EnumType(json))
      case (None, Some(JsArray(x)))                             => Some(EnumType(json))
      case _                                                    => None
    }

  }

}
