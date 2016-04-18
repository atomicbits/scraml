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

import io.atomicbits.scraml.ramlparser.model.{Id, IdExtractor}
import play.api.libs.json.{JsObject, Json}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class StringType(id: Id, format: Option[String] = None, required: Boolean = false)
  extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}

object StringType {

  def apply(schema: JsObject): Try[StringType] = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val format = (schema \ "format").asOpt[String]

    val required = (schema \ "required").asOpt[Boolean]

    Success(StringType(id, format, required.getOrElse(false)))
  }


  def apply(): Try[StringType] = {
    apply(Json.obj())
  }

}
