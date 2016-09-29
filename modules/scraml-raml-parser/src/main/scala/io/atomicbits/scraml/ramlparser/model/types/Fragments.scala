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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class Fragments(id: Id = ImplicitId, fragmentMap: Map[String, Type] = Map.empty) extends Type with Fragmented {

  override def updated(updatedId: Id): Fragments = copy(id = updatedId)

  override def model = JsonSchemaModel

  override def asTypeModel(typeModel: TypeModel): Type = this

  def isEmpty: Boolean = fragmentMap.isEmpty

  def fragments: Fragments = this

  def map[T](fn: ((String, Type)) => (String, Type)): Fragments = copy(fragmentMap = fragmentMap.map(fn))

  override def required: Option[Boolean] = None

}


object Fragments {

  def apply(json: JsObject)(implicit parseContext: ParseContext): Try[Fragments] = {

    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](json.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }

    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, Type(fragment))      => (fragmentFieldName, fragment.map(_.asTypeModel(JsonSchemaModel)))
      case (fragmentFieldName, Fragments(fragment)) => (fragmentFieldName, fragment)
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
    "typeDiscriminator"
  )

}
