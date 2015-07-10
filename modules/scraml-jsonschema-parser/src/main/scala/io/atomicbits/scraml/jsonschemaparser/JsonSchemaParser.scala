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

package io.atomicbits.scraml.jsonschemaparser

import io.atomicbits.scraml.jsonschemaparser.model._
import play.api.libs.json.{JsObject, Json}

import scala.language.postfixOps

/**
 * Created by peter on 1/06/15, Atomic BITS (http://atomicbits.io). 
 */
object JsonSchemaParser {

  /**
   * References:
   * + par. 7.2.2 http://json-schema.org/latest/json-schema-core.html
   * + par. 7.2.3 http://json-schema.org/latest/json-schema-core.html
   * + http://tools.ietf.org/html/draft-zyp-json-schema-03
   * + http://spacetelescope.github.io/understanding-json-schema/structuring.html (good fragment dereferencing examples)
   *
   */

  /**
   *
   * @param schemas A map containing the String-representation of JSON schema files as values. The keys are external
   *                links referring to the schema. A single schema may contain nested schemas.
   *                All schemas MUST have an "id" property containing an absolute or relative identification for
   *                the schema, e.g.: { "id": "http://atomicbits.io/schema/user.json#", ... }
   * @return A schema lookup table.
   */
  def parse(schemas: Map[String, String]): Map[String, Schema] = {
    schemas
      .mapValues(Json.parse)
      .collect { case (id, schema: JsObject) => (id, schema) }
      .mapValues(Schema(_)) // we now have a Map[String, Schema]
  }

}
