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

package io.atomicbits.scraml.jsonschemaparser.model

import io.atomicbits.scraml.jsonschemaparser.Id
import play.api.libs.json._

import scala.language.postfixOps

/**
 * Created by peter on 5/06/15, Atomic BITS (http://atomicbits.io).
 *
 * See: http://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7
 *
 */
trait Schema {

  def id: Id

  def updated(id: Id): Schema

}

trait PrimitiveSchema extends Schema

trait FragmentedSchema extends Schema {

  def fragments: Map[String, Schema]

}

trait AllowedAsObjectField {

  def required: Boolean

}

object Schema {

  def apply(schema: JsObject): Schema = {

    val typeOpt = (schema \ "type").asOpt[String]
    val enumOpt = (schema \ "enum").asOpt[List[String]]

    typeOpt match {
      case Some("object")  =>
        (schema \ "genericType").asOpt[String] map (_ => GenericObjectEl(schema)) getOrElse ObjectEl(schema)
      case Some("array")   => ArrayEl(schema)
      case Some("string")  =>
        enumOpt match {
          case Some(enum) => EnumEl(schema)
          case None       => StringEl(schema)
        }
      case Some("number")  => NumberEl(schema)
      case Some("integer") => IntegerEl(schema)
      case Some("boolean") => BooleanEl(schema)
      case Some("null")    => NullEl(schema)
      case Some(x)         => sys.error(s"Unkown json-schema type $x")
      case None            =>
        (schema \ "$ref").asOpt[String] match {
          case Some(_) => SchemaReference(schema)
          case None    =>
            (schema \ "enum").asOpt[List[String]] match {
              case Some(choices) => EnumEl(schema)
              case None          => Fragment(schema)
            }
        }
    }
  }


  def collectFragments(schemaObject: JsObject): Map[String, Schema] = {
    // Process the fragments and exclude the json-schema fields that we don't need to consider
    // (should be only objects as other fields are ignored as fragmens) ToDo: check this
    val keysToExclude =
      Seq("id", "type", "properties", "required", "oneOf", "anyOf", "allOf", "typeVariables", "genericTypes", "genericType")
    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](schemaObject.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }
    fragmentsToKeep collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Schema(fragment))
    }
  }

}
