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

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import play.api.libs.json.{JsArray, JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import io.atomicbits.scraml.util.TryUtils._

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 1/11/16.
  */
case class ParsedMultipleInheritance(parents: Set[ParsedTypeReference],
                                     required: Option[Boolean] = None,
                                     model: TypeModel = RamlModel,
                                     id: Id = ImplicitId) extends NonPrimitiveType with AllowedAsObjectField {


  override def updated(updatedId: Id): ParsedMultipleInheritance = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel, parents = parents.map(_.asTypeModel(typeModel)))

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
          case Success(unionType: ParsedUnionType)         =>
            Failure(
              RamlParseException(s"We do not yet support multiple inheritance where one of the parents is a union type expression.")
            )
        }


      val triedTypeReferences: Try[Seq[ParsedTypeReference]] = accumulate(typeReferences)

      if (typeReferences.size > 1) Some(triedTypeReferences.map(_.toSet))
      else None
    }


    (ParsedType.typeDeclaration(json), json) match {
      case (Some(JsArray(parentReferences)), _) =>
        processParentReferences(parentReferences).map { triedParents =>
          val required = json.fieldBooleanValue("required")
          triedParents.map(ParsedMultipleInheritance(_, required))
        }
      case (_, JsArray(parentReferences))       =>
        processParentReferences(parentReferences).map(triedParents => triedParents.map(ParsedMultipleInheritance(_)))
      case _                                    => None
    }

  }

}
