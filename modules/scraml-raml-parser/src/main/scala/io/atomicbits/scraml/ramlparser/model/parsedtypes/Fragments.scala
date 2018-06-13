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
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json.{ JsObject, JsString, JsValue }

import scala.util.{ Success, Try }

/**
  * Created by peter on 1/04/16.
  */
case class Fragments(id: Id = ImplicitId, fragmentMap: Map[String, ParsedType] = Map.empty) extends ParsedType with Fragmented {

  override def updated(updatedId: Id): Fragments = copy(id = updatedId)

  override def model = JsonSchemaModel

  override def asTypeModel(typeModel: TypeModel): ParsedType = this

  def isEmpty: Boolean = fragmentMap.isEmpty

  def fragments: Fragments = this

  def map[T](fn: ((String, ParsedType)) => (String, ParsedType)): Fragments = copy(fragmentMap = fragmentMap.map(fn))

  override def required: Option[Boolean] = None

  def values = fragmentMap.values.toList

}

object Fragments {

  def apply(json: JsObject)(implicit parseContext: ParseContext): Try[Fragments] = {

    val id = JsonSchemaIdExtractor(json)

    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](json.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }

    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, ParsedType(fragment)) => (fragmentFieldName, fragment.map(_.asTypeModel(JsonSchemaModel)))
      case (fragmentFieldName, Fragments(fragment))  => (fragmentFieldName, fragment)
    }

    TryUtils.withSuccess(
      Success(id),
      TryUtils.accumulate(fragments)
    )(Fragments(_, _))
  }

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Fragments]] = {

    json match {
      case jsObject: JsObject => Some(Fragments(jsObject))
      case _                  => None
    }

  }

  // Process the fragments and exclude the json-schema fields that we don't need to consider
  // (should be only objects as other fields are ignored as fragments) ToDo: check this
  private val keysToExclude = Seq(
    "id",
    "type",
    "properties",
    "required",
    "oneOf",
    "anyOf",
    "allOf",
    "typeVariables",
    "genericTypes",
    "genericType",
    "$ref",
    "_source",
    "description",
    "$schema",
    "items",
    "formParameters",
    "queryParameters",
    "typeDiscriminator"
  )

}
