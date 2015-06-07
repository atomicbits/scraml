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

import io.atomicbits.scraml.jsonschemaparser.{Id, IdExtractor}
import play.api.libs.json.JsObject

/**
 * Created by peter on 7/06/15. 
 */
/**
 *
 * @param id
 * @param format See http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7
 * @param required
 */
case class StringEl(id: Id, format: Option[String] = None, required: Boolean = false) extends Schema

object StringEl {

  def apply(schema: JsObject): Schema = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val format = (schema \ "format").asOpt[String]

    val required = (schema \ "required").asOpt[Boolean]

    StringEl(id, format, required.getOrElse(false))

  }

}
