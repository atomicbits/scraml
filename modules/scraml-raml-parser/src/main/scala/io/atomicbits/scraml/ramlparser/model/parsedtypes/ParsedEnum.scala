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
import play.api.libs.json.{ JsArray, JsObject, JsString, JsValue }

import scala.util.{ Success, Try }

/**
  * Created by peter on 1/04/16.
  */
case class ParsedEnum(id: Id, choices: List[String], required: Option[Boolean] = None, model: TypeModel = RamlModel)
    extends NonPrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedEnum = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel)

}

object ParsedEnum {

  val value = "enum"

  def apply(json: JsValue): Try[ParsedEnum] = {

    val model: TypeModel = TypeModel(json)

    val id = JsonSchemaIdExtractor(json)

    val choices = (json \ "enum").asOpt[List[String]]

    val required = (json \ "required").asOpt[Boolean]

    Success(ParsedEnum(id, choices.getOrElse(List.empty), required, model))
  }

  def unapply(json: JsValue): Option[Try[ParsedEnum]] = {

    (ParsedType.typeDeclaration(json), (json \ ParsedEnum.value).toOption) match {
      case (Some(JsString(ParsedString.value)), Some(JsArray(x))) => Some(ParsedEnum(json))
      case (None, Some(JsArray(x)))                               => Some(ParsedEnum(json))
      case _                                                      => None
    }

  }

}
