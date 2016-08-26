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
case class NumberType(id: Id = ImplicitId,
                      format: Option[String] = None,
                      minimum: Option[Int] = None,
                      maximum: Option[Int] = None,
                      multipleOf: Option[Int] = None,
                      required: Option[Boolean] = None) extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}

object NumberType {

  val value = "number"


  def apply(json: JsValue): Try[NumberType] = {

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    Success(
      NumberType(
        id = id,
        format = json.fieldStringValue("format"),
        minimum = json.fieldIntValue("minimum"),
        maximum = json.fieldIntValue("maximum"),
        multipleOf = json.fieldIntValue("multipleOf"),
        required = json.fieldBooleanValue("required")
      )
    )
  }


  def unapply(json: JsValue): Option[Try[NumberType]] = {

    (Type.typeDeclaration(json), json) match {
      case (Some(JsString(NumberType.value)), _) => Some(NumberType(json))
      case (_, JsString(NumberType.value))       => Some(Success(new NumberType()))
      case _                                     => None
    }

  }

}
