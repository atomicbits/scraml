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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ ParseContext, RamlParseException }
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json.{ JsObject, JsString, JsValue }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 1/04/16.
  */
/**
  * Created by peter on 16/09/15.
  *
  * Generic Object elements have a 'genericType' field:
  *
  * | {
  * |   "$$schema": "http://json-schema.org/draft-03/schema",
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
case class ParsedGenericObject(id: Id,
                               typeVariable: String,
                               required: Option[Boolean] = None,
                               fragments: Fragments      = Fragments(),
                               model: TypeModel          = RamlModel)
    extends Fragmented
    with AllowedAsObjectField
    with NonPrimitiveType {

  override def updated(updatedId: Id): ParsedGenericObject = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedGenericObject {

  val value = "genericType"

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[ParsedGenericObject] = {

    val model: TypeModel = TypeModel(json)

    // Process the id
    val id: Id = JsonSchemaIdExtractor(json)

    // Process the required field
    val required = (json \ "required").asOpt[Boolean]

    val fragments = json match {
      case Fragments(fragment) => fragment
    }

    val genericType = (json \ "genericType")
      .asOpt[String]
      .map(Success(_))
      .getOrElse(Failure[String](RamlParseException(s"A generic object must have a 'genericType' field: $id")))

    TryUtils.withSuccess(
      Success(id),
      genericType,
      Success(required),
      fragments,
      Success(model)
    )(ParsedGenericObject(_, _, _, _, _))
  }

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedGenericObject]] = {
    (ParsedType.typeDeclaration(json), (json \ "properties").toOption, (json \ "genericType").toOption) match {
      case (Some(JsString(ParsedObject.value)), _, Some(JsString(genT))) => Some(ParsedGenericObject(json))
      case (None, Some(jsObj), Some(JsString(genT)))                     => Some(ParsedGenericObject(json))
      case _                                                             => None
    }
  }

}
