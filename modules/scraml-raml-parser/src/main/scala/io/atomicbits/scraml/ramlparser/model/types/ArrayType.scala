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
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import play.api.libs.json.{JsObject, JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import io.atomicbits.scraml.util.TryUtils

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 25/03/16.
  */
case class ArrayType(items: Type,
                     id: Id = ImplicitId,
                     required: Option[Boolean] = None,
                     minItems: Option[Int] = None,
                     maxItems: Option[Int] = None,
                     uniqueItems: Boolean = false,
                     fragments: Fragments = Fragments(),
                     model: TypeModel = RamlModel) extends NonePrimitiveType with AllowedAsObjectField with Fragmented {

  override def updated(updatedId: Id): ArrayType = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): Type = copy(model = typeModel, items = items.asTypeModel(typeModel))

  def asRequired = copy(required = Some(true))

}


object ArrayType {

  val value = "array"


  def apply(triedPrimitiveType: Try[PrimitiveType])(implicit parseContext: ParseContext): Try[ArrayType] = {

    val id = triedPrimitiveType.map(_.id)

    val primitiveWithErasedId =
      triedPrimitiveType.map { prim =>
        prim.updated(ImplicitId)
      }

    val required = triedPrimitiveType.map(_.required)

    TryUtils.withSuccess(
      primitiveWithErasedId,
      id,
      required,
      Success(None),
      Success(None),
      Success(false),
      Success(new Fragments())
    )(ArrayType(_, _, _, _, _, _, _))
  }


  def apply(arrayExpression: String)(implicit parseContext: ParseContext): Try[ArrayType] = {

    if (arrayExpression.endsWith("[]")) {
      val typeName = arrayExpression.stripSuffix("[]")
      Type(typeName).map(ArrayType(_))
    } else {
      Failure(
        RamlParseException(
          s"Expression $arrayExpression in ${parseContext.head} is not an array expression, it should end with '[]'."
        )
      )
    }

  }


  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[ArrayType] = {

    val model: TypeModel = TypeModel(json)

    // Process the id
    val id = json match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the items type
    val items =
    (json \ "items").toOption.collect {
      case Type(someType) => someType
    } getOrElse
      Failure(
        RamlParseException(
          s"An array definition in ${parseContext.head} has either no 'items' field or an 'items' field with an invalid type declaration."
        )
      )

    // Process the required field
    val required = json.fieldBooleanValue("required")

    val fragments = json match {
      case Fragments(fragment) => fragment
    }

    TryUtils.withSuccess(
      items,
      Success(id),
      Success(required),
      Success(None),
      Success(None),
      Success(false),
      fragments,
      Success(model)
    )(ArrayType(_, _, _, _, _, _, _, _))
  }


  def unapply(arrayTypeExpression: String)(implicit parseContext: ParseContext): Option[Try[ArrayType]] = {
    if (arrayTypeExpression.endsWith("[]")) Some(ArrayType(arrayTypeExpression))
    else None
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ArrayType]] = {

    // The repeated field is no longer present in RAML 1.0, but for backward compatibility reasons, we still parse it and
    // interpret these values as array types.
    val repeatedValue = (json \ "repeat").asOpt[Boolean]

    (Type.typeDeclaration(json), json, repeatedValue) match {
      case (Some(JsString(ArrayType.value)), _, _)                                     => Some(ArrayType(json))
      case (_, JsString(arrayTypeExpression), _) if arrayTypeExpression.endsWith("[]") => Some(ArrayType(arrayTypeExpression))
      case (_, PrimitiveType(tryType), Some(true))                                     => Some(ArrayType(tryType))
      case _                                                                           => None
    }

  }

}
