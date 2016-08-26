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

import io.atomicbits.scraml.ramlparser.model.{Id, IdExtractor, ImplicitId}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

import io.atomicbits.scraml.ramlparser.parser.JsUtils._


/**
  * Created by peter on 1/04/16.
  */
case class BooleanType(id: Id = ImplicitId, required: Option[Boolean] = None) extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}


object BooleanType {

  val value = "boolean"

  def apply(json: JsValue): Try[BooleanType] = {

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    Success(
      BooleanType(
        id = id,
        required = json.fieldBooleanValue("required")
      )
    )
  }


  def unapply(json: JsValue): Option[Try[BooleanType]] = {

    (Type.typeDeclaration(json), json) match {
      case (Some(JsString(BooleanType.value)), _) => Some(BooleanType(json))
      case (_, JsString(BooleanType.value))       => Some(Success(new BooleanType()))
      case _                                      => None
    }

  }

}
