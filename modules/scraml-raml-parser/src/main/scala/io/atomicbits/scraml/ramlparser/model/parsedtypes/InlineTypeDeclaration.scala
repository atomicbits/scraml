/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
