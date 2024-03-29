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
import play.api.libs.json.{ JsObject, JsString, JsValue }
import io.atomicbits.scraml.ramlparser.parser.JsUtils._
import io.atomicbits.scraml.util.TryUtils

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 25/03/16.
  */
case class ParsedArray(items: ParsedType,
                       id: Id                    = ImplicitId,
                       required: Option[Boolean] = None,
                       minItems: Option[Int]     = None,
                       maxItems: Option[Int]     = None,
                       uniqueItems: Boolean      = false,
                       fragments: Fragments      = Fragments(),
                       model: TypeModel          = RamlModel)
    extends NonPrimitiveType
    with AllowedAsObjectField
    with Fragmented {

  override def updated(updatedId: Id): ParsedArray = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedType = copy(model = typeModel, items = items.asTypeModel(typeModel))

  def asRequired = copy(required = Some(true))

}

object ParsedArray {

  val value = "array"

  def apply(triedPrimitiveType: Try[PrimitiveType])(implicit parseContext: ParseContext): Try[ParsedArray] = {

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
    )(ParsedArray(_, _, _, _, _, _, _))
  }

  def apply(arrayExpression: String)(implicit parseContext: ParseContext): Try[ParsedArray] = {

    if (arrayExpression.endsWith("[]")) {
      val typeName = arrayExpression.stripSuffix("[]")
      ParsedType(typeName).map(ParsedArray(_))
    } else {
      Failure(
        RamlParseException(
          s"Expression $arrayExpression in ${parseContext.head} is not an array expression, it should end with '[]'."
        )
      )
    }

  }

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[ParsedArray] = {

    val model: TypeModel = TypeModel(json)

    // Process the id
    val id = JsonSchemaIdExtractor(json)

    // Process the items type
    val items =
      (json \ "items").toOption.collect {
        case ParsedType(someType) => someType
      } getOrElse
        Failure(
          RamlParseException(
            s"An array definition in ${parseContext.head} has either no 'items' field or an 'items' field with an invalid type declaration."
          )
        )

    // Process the required field
    val required = json.fieldBooleanValue("required")

    val fragments = (json: @unchecked) match {
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
    )(ParsedArray(_, _, _, _, _, _, _, _))
  }

  def unapply(arrayTypeExpression: String)(implicit parseContext: ParseContext): Option[Try[ParsedArray]] = {
    if (arrayTypeExpression.endsWith("[]")) Some(ParsedArray(arrayTypeExpression))
    else None
  }

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedArray]] = {

    // The repeated field is no longer present in RAML 1.0, but for backward compatibility reasons, we still parse it and
    // interpret these values as array types.
    val repeatedValue = (json \ "repeat").asOpt[Boolean]

    (ParsedType.typeDeclaration(json), json, repeatedValue) match {
      case (Some(JsString(ParsedArray.value)), _, _)                                   => Some(ParsedArray(json))
      case (_, JsString(arrayTypeExpression), _) if arrayTypeExpression.endsWith("[]") => Some(ParsedArray(arrayTypeExpression))
      case (_, PrimitiveType(tryType), Some(true))                                     => Some(ParsedArray(tryType))
      case _                                                                           => None
    }

  }

}
