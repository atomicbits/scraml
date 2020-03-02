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
import play.api.libs.json.{ JsArray, JsBoolean, JsString, JsValue }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import io.atomicbits.scraml.util.TryUtils
import io.atomicbits.scraml.util.TryUtils._

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 1/11/16.
  */
case class ParsedMultipleInheritance(parents: Set[ParsedTypeReference],
                                     properties: ParsedProperties,
                                     requiredProperties: List[String]       = List.empty,
                                     typeParameters: List[String]           = List.empty, // unused for now
                                     typeDiscriminator: Option[String]      = None, // unused for now
                                     typeDiscriminatorValue: Option[String] = None, // unused for now
                                     required: Option[Boolean]              = None,
                                     model: TypeModel                       = RamlModel,
                                     id: Id                                 = ImplicitId)
    extends NonPrimitiveType
    with AllowedAsObjectField {

  override def updated(updatedId: Id): ParsedMultipleInheritance = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = {
    val updatedProperties =
      properties.map { property =>
        property.copy(propertyType = TypeRepresentation(property.propertyType.parsed.asTypeModel(typeModel)))
      }
    copy(model = typeModel, parents = parents.map(_.asTypeModel(typeModel)), properties = updatedProperties)
  }

  def asRequired = copy(required = Some(true))

}

object ParsedMultipleInheritance {

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedMultipleInheritance]] = {

    def processParentReferences(parents: Seq[JsValue]): Option[Try[Set[ParsedTypeReference]]] = {

      val parentRefs =
        parents.collect {
          case JsString(parentRef) => ParsedType(parentRef)
        }

      val typeReferences =
        parentRefs.collect {
          case Success(typeReference: ParsedTypeReference) => Success(typeReference)
          case Success(unionType: ParsedUnionType) =>
            Failure(
              RamlParseException(s"We do not yet support multiple inheritance where one of the parents is a union type expression.")
            )
        }

      val triedTypeReferences: Try[Seq[ParsedTypeReference]] = accumulate(typeReferences)

      if (typeReferences.size > 1) Some(triedTypeReferences.map(_.toSet))
      else None
    }

    // Process the id
    val id: Id = JsonSchemaIdExtractor(json)

    val model: TypeModel = TypeModel(json)

    // Process the properties
    val properties: Try[ParsedProperties] = ParsedProperties((json \ "properties").toOption, model)

    // Process the required field
    val (required, requiredFields) =
      (json \ "required").toOption match {
        case Some(req: JsArray) =>
          (None, Some(req.value.toList collect {
            case JsString(value) => value
          }))
        case Some(JsBoolean(b)) => (Some(b), None)
        case _                  => (None, None)
      }

    val triedParentsOpt =
      (ParsedType.typeDeclaration(json), json) match {
        case (Some(JsArray(parentReferences)), _) => processParentReferences(parentReferences.toSeq)
        case (_, JsArray(parentReferences))       => processParentReferences(parentReferences.toSeq)
        case _                                    => None
      }

    triedParentsOpt.map { triedParents =>
      TryUtils.withSuccess(
        triedParents,
        properties,
        Success(requiredFields.getOrElse(List.empty)),
        Success(List()),
        Success(None),
        Success(None),
        Success(required),
        Success(model),
        Success(id)
      )(new ParsedMultipleInheritance(_, _, _, _, _, _, _, _, _))

    }

  }

}
