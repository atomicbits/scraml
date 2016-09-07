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

import io.atomicbits.scraml.ramlparser.model.{Id, IdExtractor, ImplicitId}
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class Fragment(id: Id = ImplicitId, fragmentMap: Map[String, Type] = Map.empty) extends Type with Fragmented {

  override def updated(updatedId: Id): Fragment = copy(id = updatedId)

  def isEmpty: Boolean = fragmentMap.isEmpty

  def fragments: Fragment = this

  def map[T](fn: ((String, Type)) => (String, Type)): Fragment = copy(fragmentMap = fragmentMap.map(fn))

  override def required: Option[Boolean] = None

}


object Fragment {

  def apply(jsObj: JsObject)(implicit parseContext: ParseContext): Try[Fragment] = {

    val id = jsObj match {
      case IdExtractor(schemaId) => schemaId
    }

    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](jsObj.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }

    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, Type(fragment)) => (fragmentFieldName, fragment)
      case (fragmentFieldName, Fragment(fragment)) => (fragmentFieldName, fragment)
    }

    TryUtils.withSuccess(
      Success(id),
      TryUtils.accumulate(fragments)
    )(Fragment(_, _))
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Fragment]] = {

    json match {
      case jsObject: JsObject => Some(Fragment(jsObject))
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
    "items"
  )

}
