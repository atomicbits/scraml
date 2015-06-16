/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.jsonschemaparser.model

import io.atomicbits.scraml.jsonschemaparser._
import play.api.libs.json.JsObject

import scala.language.postfixOps

/**
 * Created by peter on 7/06/15. 
 */
case class ArrayEl(id: Id, items: Schema, required: Boolean = false) extends Schema with AllowedAsObjectField {

  override def updated(updatedId: Id): Schema = copy(id = updatedId)

}

object ArrayEl {

  def apply(schema: JsObject): ArrayEl = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the items type
    val items =
      schema \ "items" toOption match {
        case Some(obj: JsObject) => Some(Schema(obj))
        case _ => None
      }

    // Process the required field
    val required = (schema \ "required").asOpt[Boolean]

    ArrayEl(
      id = id,
      items = items.getOrElse(
        throw JsonSchemaParseException("An array type must have an 'items' field that refers to a JsObject")
      ),
      required = required.getOrElse(false)
    )

  }

}
