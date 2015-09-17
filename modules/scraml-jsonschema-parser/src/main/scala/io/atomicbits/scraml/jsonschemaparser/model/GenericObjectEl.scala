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

package io.atomicbits.scraml.jsonschemaparser.model

import io.atomicbits.scraml.jsonschemaparser.{IdExtractor, Id}
import play.api.libs.json.JsObject

/**
 * Created by peter on 16/09/15.
 *
 * Generic Object elements have a 'genericType' field:
 *
 * | {
 * |   "$schema": "http://json-schema.org/draft-03/schema",
 * |   "id": "http://atomicbits.io/schema/paged-list.json#",
 * |   "type": "object",
 * |   "typeVariables": ["T", "U"],
 * |   "description": "A paged list with an optional owner of the list",
 * |   "properties": {
 * |     "count": {
 * |       "type": "integer",
 * |       "required": true
 * |     },
 * |     "elements": {
 * |       "required": true,
 * |       "type": "array",
 * |       "items": {
 * |         "type": "object",
 * |         "genericType": "T"
 * |       }
 * |     },
 * |     "owner": {
 * |       "required": false,
 * |       "type": "object",
 * |       "genericType": "U"
 * |     }
 * |   }
 * | }
 *
 *
 */
case class GenericObjectEl(id: Id,
                           required: Boolean,
                           typeVariable: String,
                           fragments: Map[String, Schema] = Map.empty) extends FragmentedSchema with AllowedAsObjectField {

  override def updated(updatedId: Id): Schema = copy(id = updatedId)

}


object GenericObjectEl {

  def apply(schema: JsObject): GenericObjectEl = {

    // Process the id
    val id: Id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the required field
    val required = (schema \ "required").asOpt[Boolean]

    val fragments = Schema.collectFragments(schema)

    val genericType = (schema \ "genericType").asOpt[String].getOrElse(sys.error(s"A generic object must have a 'genericType' field: $id"))

    GenericObjectEl(
      id = id,
      required = required.getOrElse(false),
      typeVariable = genericType,
      fragments = fragments
    )
  }

}
