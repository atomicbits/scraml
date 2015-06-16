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

import io.atomicbits.scraml.jsonschemaparser._
import play.api.libs.json._

import scala.language.postfixOps

/**
 * Created by peter on 7/06/15. 
 */
case class ObjectEl(id: Id,
                    properties: Map[String, Schema],
                    required: Boolean,
                    requiredFields: List[String] = List.empty,
                    oneOfSelection: List[Selection] = List.empty,
                    fragments: Map[String, Schema] = Map.empty,
                    name: Option[String] = None,
                    canonicalName: Option[String] = None) extends FragmentedSchema with AllowedAsObjectField {

  override def updated(updatedId: Id): Schema = copy(id = updatedId)

}

// ToDo: handle the oneOf field
object ObjectEl {

  def apply(schema: JsObject): ObjectEl = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the properties
    val properties =
      schema \ "properties" toOption match {
        case Some(props: JsObject) =>
          Some(props.value.toSeq collect {
            case (fieldName, fragment: JsObject) => (fieldName, Schema(fragment))
          } toMap)
        case _ => None
      }

    // Process the fragments
    val keysToExclude = Seq("id", "properties", "required", "oneOf", "anyOf", "allOf")
    val fragmentsToKeep =
      keysToExclude.foldLeft[Map[String, JsValue]](schema.value.toMap) { (schemaMap, excludeKey) =>
        schemaMap - excludeKey
      }
    val fragments = fragmentsToKeep collect {
      case (fragmentFieldName, fragment: JsObject) => (fragmentFieldName, Schema(fragment))
    }

    // Process the required field
    val (required, requiredFields) =
      schema \ "required" toOption match {
        case Some(req: JsArray) =>
          (None, Some(req.value.toList collect {
            case JsString(value) => value
          }))
        case Some(JsBoolean(b)) => (Some(b), None)
        case _ => (None, None)
      }

    // Process the named field
    val name = (schema \ "name").asOpt[String]

    ObjectEl(
      id = id,
      required = required.getOrElse(false),
      requiredFields = requiredFields.getOrElse(List.empty[String]),
      properties = properties.getOrElse(Map.empty[String, Schema]),
      fragments = fragments.toMap,
      name = name
    )

  }

}
