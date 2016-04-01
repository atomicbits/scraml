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

import io.atomicbits.scraml.ramlparser.model.{IdExtractor, ImplicitId, RelativeId, Id}
import io.atomicbits.scraml.ramlparser.parser.TryUtils
import play.api.libs.json.{JsBoolean, JsString, JsArray, JsObject}

import scala.util.{Success, Try}

/**
  * Created by peter on 25/03/16.
  */
case class ObjectType(id: Id,
                      baseType: List[Id],
                      properties: Map[String, Type],
                      // facets: Option[String],
                      required: Boolean,
                      requiredFields: List[String] = List.empty,
                      selection: Option[Selection] = None,
                      fragments: Map[String, Type] = Map.empty,
                      typeVariables: List[String] = List.empty,
                      typeDiscriminator: Option[String] = None) extends FragmentedType with AllowedAsObjectField with Type {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}



object ObjectType {


  def apply(schema: JsObject, nameOpt: Option[String])(implicit nameToIdOpt: String => Id): Try[ObjectType] = {

    // Process the id
    val id: Id = {
      val provisionaryId =
        schema match {
          case IdExtractor(schemaId) => schemaId
        }
      // If there is no explicit id field set, then use the given name (if there is one) to create the id.
      (provisionaryId, nameOpt.map(nameToIdOpt)) match {
        case (ImplicitId, Some(nameBasedId)) => nameBasedId
        case (otherId, _)                    => otherId
      }
    }

    // Process the properties
    val properties: Try[Map[String, Type]] =
      (schema \ "properties").toOption.collect {
        case props: JsObject =>
          val propertyTryMap =
            props.value collect {
              case (fieldName, fragment: JsObject) => (fieldName, Type(fragment))
            }
          TryUtils.accumulate(propertyTryMap.toMap)
      } getOrElse Success(Map.empty[String, Type])

    val fragments = Type.collectFragments(schema)

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
        case Some(typeVars: JsArray) => typeVars.value.toList.collect { case JsString(value) => value }
        case _                       => List.empty[String]
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
          } map (tryToInterpretOneOfSelectionAsObjectType(_, id, typeDiscriminator.getOrElse("type")))
          TryUtils.accumulate(selectionSchemas.toList).map(selections => OneOf(selections.toList))
      }

    val anyOf =
      (schema \ "anyOf").toOption collect {
        case selections: JsArray =>
          val selectionSchemas = selections.value collect {
            case jsObj: JsObject => jsObj
          } map (Type(_))
          TryUtils.accumulate(selectionSchemas.toList).map(selections => AnyOf(selections.toList))
      }

    val allOf =
      (schema \ "allOf").toOption collect {
        case selections: JsArray =>
          val selectionSchemas = selections.value collect {
            case jsObj: JsObject => jsObj
          } map (Type(_))
          TryUtils.accumulate(selectionSchemas.toList).map(selections => AllOf(selections.toList))
      }

    val selection = List(oneOf, anyOf, allOf).flatten.headOption


    TryUtils.withSuccess(
      Success(id),
      Success(List.empty[Id]),
      properties,
      Success(required.getOrElse(false)),
      Success(requiredFields.getOrElse(List.empty[String])),
      TryUtils.accumulate(selection),
      TryUtils.accumulate(fragments),
      Success(typeVariables),
      Success(typeDiscriminator)
    )(new ObjectType(_, _, _, _, _, _, _, _, _))
  }


  def schemaToDiscriminatorValue(schema: Type): Option[String] = {
    Some(schema) collect {
      case enumType: EnumType if enumType.choices.length == 1 => enumType.choices.head
    }
  }


  private def tryToInterpretOneOfSelectionAsObjectType(schema: JsObject, parentId: Id, typeDiscriminator: String)
                                                      (implicit nameToId: String => Id): Try[Type] = {

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
        case _          => None
      }
    }

    Type(schema).flatMap {
      case objectType: ObjectType => Success(objectType)
      case frag: Fragment         =>
        typeDiscriminatorFromProperties(frag).flatMap(fixId(frag.id, parentId, _)) map { relativeId =>
          Type(schema + ("type" -> JsString("object")) + ("id" -> JsString(relativeId.id)))
        } getOrElse Success(frag)
      case x                      => Success(x)
    }
  }

}
