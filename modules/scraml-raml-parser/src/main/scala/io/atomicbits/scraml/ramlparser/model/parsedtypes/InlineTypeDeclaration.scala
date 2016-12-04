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

import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.JsValue

import scala.util.Try

/**
  * Created by peter on 1/09/16.
  */
object InlineTypeDeclaration {

  /**
    * An inline type declaration is of the form:
    *
    * . /activate:
    * .  put:
    * .    body:
    * .      application/vnd-v1.0+json:
    * .        schema: |
    * .          {
    * .            "type": "array",
    * .              "items": {
    * .                "$ref": "user.json"
    * .              }
    * .          }
    *
    * Here, the 'schema' (or 'type') field does not refer to the type but to an object that defines the type.
    *
    */
  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedType]] = {

    ParsedType.typeDeclaration(json) match {
      case Some(ParsedType(tryType)) => Some(tryType)
      case _                         => None
    }

  }

}
