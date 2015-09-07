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
import play.api.libs.json
import play.api.libs.json._

import scala.language.postfixOps

/**
 * Created by peter on 7/06/15. 
 */
case class ObjectEl(id: Id,
                    properties: Map[String, Schema],
                    required: Boolean,
                    requiredFields: List[String] = List.empty,
                    selection: Option[Selection] = None,
                    fragments: Map[String, Schema] = Map.empty,
                    typeVariables: List[String] = List.empty,
                    typeDiscriminator: Option[String] = None) extends FragmentedSchema with AllowedAsObjectField {

  override def updated(updatedId: Id): Schema = copy(id = updatedId)

}

// ToDo: handle the oneOf, anyOf and allOf fields
object ObjectEl {


  def apply(schema: JsObject): ObjectEl = {

    // Process the id
    val id: Id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the properties
    val properties =
      schema \ "properties" toOption match {
        case Some(props: JsObject) =>
          Some(props.value.toSeq collect {
            case (fieldName, fragment: JsObject) => (fieldName, Schema(fragment))
          } toMap)
        case _                     => None
      }

    // Process the fragments and exclude the json-schema fields that we don't need to consider
    // (should be only objects as other fields are ignored as fragmens) ToDo: check this
    val keysToExclude = Seq("id", "type", "properties", "required", "oneOf", "anyOf", "allOf")
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
        case _                  => (None, None)
      }


    // Process the typeVariables field
    val typeVariables: List[String] =
      schema \ "typeVariables" toOption match {
        case Some(req: JsArray) => req.value.toList.collect { case JsString(value) => value }
        case _                  => List.empty[String]
      }

    // Process the typeDiscriminator field
    val typeDiscriminator: Option[String] =
      schema \ "typeDiscriminator" toOption match {
        case Some(JsString(value)) => Some(value)
        case _                     => None
      }

    val oneOf =
      (schema \ "oneOf").toOption collect {
        case selections: JsArray =>
          val selectionSchemas = selections.value collect {
            case jsObj: JsObject => jsObj
          } map (tryToInterpretOneOfSelectionAsObjectEl(_, id, typeDiscriminator.getOrElse("type")))
          OneOf(selectionSchemas.toList)
      }

    val anyOf =
      (schema \ "anyOf").toOption collect {
        case selections: JsArray =>
          val selectionSchemas = selections.value collect {
            case jsObj: JsObject => jsObj
          } map (Schema(_))
          AnyOf(selectionSchemas.toList)
      }

    val allOf =
      (schema \ "allOf").toOption collect {
        case selections: JsArray =>
          val selectionSchemas = selections.value collect {
            case jsObj: JsObject => jsObj
          } map (Schema(_))
          AllOf(selectionSchemas.toList)
      }

    val selection = List(oneOf, anyOf, allOf).flatten.headOption


    ObjectEl(
      id = id,
      required = required.getOrElse(false),
      requiredFields = requiredFields.getOrElse(List.empty[String]),
      selection = selection,
      properties = properties.getOrElse(Map.empty[String, Schema]),
      fragments = fragments.toMap,
      typeVariables = typeVariables,
      typeDiscriminator = typeDiscriminator
    )

  }


  def schemaToDiscriminatorValue(schema: Schema): Option[String] = {
    Some(schema) collect {
      case enumEl: EnumEl if enumEl.choices.length == 1 => enumEl.choices.head
    }
  }


  private def tryToInterpretOneOfSelectionAsObjectEl(schema: JsObject, parentId: Id, typeDiscriminator: String): Schema = {

    def typeDiscriminatorFromProperties(oneOfFragment: Fragment): Option[String] = {
      oneOfFragment.fragments.get("properties") collect {
        case propFrag: Fragment => propFrag.fragments.get(typeDiscriminator) flatMap schemaToDiscriminatorValue
      } getOrElse None
    }

    def fixId(id: Id, parentId: Id, discriminatorValue: String): Option[RelativeId] = {
      id match {
        case ImplicitId => // fix id based on the parentId if there isn't one
          if (discriminatorValue.exists(_.isLower)) Some(RelativeId(id = discriminatorValue))
          else Some(RelativeId(id = discriminatorValue.toLowerCase))
        case _                                 => None
      }
    }

    Schema(schema) match {
      case objEl: ObjectEl => objEl
      case frag: Fragment  =>
        typeDiscriminatorFromProperties(frag).flatMap(fixId(frag.id, parentId, _)) map { relativeId =>
          Schema(schema + ("type" -> JsString("object")) + ("id" -> JsString(relativeId.id)))
        } getOrElse frag
      case x               => x
    }
  }


}
