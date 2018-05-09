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
import play.api.libs.json.{ JsObject, JsString, JsValue }

import scala.util.{ Success, Try }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

/**
  * Created by peter on 1/04/16.
  */
case class ParsedBoolean(id: Id = ImplicitId, required: Option[Boolean] = None, model: TypeModel = RamlModel)
    extends PrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedBoolean = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedBoolean {

  val value = "boolean"

  def apply(json: JsValue): Try[ParsedBoolean] = {

    val model: TypeModel = TypeModel(json)

    val id = JsonSchemaIdExtractor(json)

    Success(
      ParsedBoolean(
        id       = id,
        required = json.fieldBooleanValue("required"),
        model
      )
    )
  }

  def unapply(json: JsValue): Option[Try[ParsedBoolean]] = {

    (ParsedType.typeDeclaration(json), json) match {
      case (Some(JsString(ParsedBoolean.value)), _) => Some(ParsedBoolean(json))
      case (_, JsString(ParsedBoolean.value))       => Some(Success(new ParsedBoolean()))
      case _                                        => None
    }

  }

}
