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
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 1/04/16.
  */

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
case class GenericObjectType(id: Id,
                             typeVariable: String,
                             required: Option[Boolean] = None,
                             fragments: Fragment = Fragment())
  extends Fragmented with AllowedAsObjectField with NonePrimitiveType {

  override def updated(updatedId: Id): GenericObjectType = copy(id = updatedId)

}


object GenericObjectType {

  val value = "genericType"

  def apply(schema: JsValue)(implicit parseContext: ParseContext): Try[GenericObjectType] = {

    // Process the id
    val id: Id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the required field
    val required = (schema \ "required").asOpt[Boolean]

    val fragments = schema match {
      case Fragment(fragment) => fragment
    }

    val genericType = (schema \ "genericType").asOpt[String].map(Success(_))
      .getOrElse(Failure[String](RamlParseException(s"A generic object must have a 'genericType' field: $id")))

    TryUtils.withSuccess(
      Success(id),
      genericType,
      Success(required),
      fragments
    )(GenericObjectType(_, _, _, _))
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[GenericObjectType]] = {
    (Type.typeDeclaration(json), (json \ "properties").toOption, (json \ "genericType").toOption) match {
      case (Some(JsString(ObjectType.value)), _, Some(JsString(genT))) => Some(GenericObjectType(json))
      case (None, Some(jsObj), Some(JsString(genT)))                   => Some(GenericObjectType(json))
      case _                                                           => None
    }
  }

}
