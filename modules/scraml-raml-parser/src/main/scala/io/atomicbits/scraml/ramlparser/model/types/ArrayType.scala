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
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}
import io.atomicbits.scraml.ramlparser.parser.JsUtils._

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
                     fragments: Fragment = Fragment()) extends NonePrimitiveType with AllowedAsObjectField with Fragmented {

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

  def asRequired = copy(required = Some(true))

}


object ArrayType {

  val value = "array"


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


  def apply(schema: JsValue)(implicit parseContext: ParseContext): Try[ArrayType] = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the items type
    val items =
    (schema \ "items").toOption.collect {
      case Type(someType) => someType
    } getOrElse
      Failure(
        RamlParseException(
          s"An array definition in ${parseContext.head} has either no 'items' field or an 'items' field with an invalid type declaration."
        )
      )

    // Process the required field
    val required = schema.fieldBooleanValue("required")

    val fragments = schema match {
      case Fragment(fragment) => fragment
    }

    TryUtils.withSuccess(
      items,
      Success(id),
      Success(required),
      Success(None),
      Success(None),
      Success(false),
      fragments
    )(ArrayType(_, _, _, _, _, _, _))
  }


  def unapply(arrayTypeExpression: String)(implicit parseContext: ParseContext): Option[Try[ArrayType]] = {
    if (arrayTypeExpression.endsWith("[]")) Some(ArrayType(arrayTypeExpression))
    else None
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ArrayType]] = {

    (Type.typeDeclaration(json), json) match {
      case (Some(JsString(ArrayType.value)), _)                                     => Some(ArrayType(json))
      case (_, JsString(arrayTypeExpression)) if arrayTypeExpression.endsWith("[]") => Some(ArrayType(arrayTypeExpression))
      case _                                                                        => None
    }

  }

}
