/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model.parsedtypes

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json.{ JsObject, JsString, JsValue }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 1/04/16.
  */
case class ParsedTypeReference(refersTo: Id,
                               id: Id                                = ImplicitId,
                               required: Option[Boolean]             = None,
                               genericTypes: Map[String, ParsedType] = Map.empty,
                               fragments: Fragments                  = Fragments(),
                               model: TypeModel                      = RamlModel)
    extends NonPrimitiveType
    with AllowedAsObjectField
    with Fragmented {

  override def updated(updatedId: Id): ParsedTypeReference = copy(id = updatedId)

  override def asTypeModel(typeModel: TypeModel): ParsedTypeReference = copy(model = typeModel)

}

object ParsedTypeReference {

  val value = "$ref"

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[ParsedTypeReference] = {

    val model: TypeModel = TypeModel(json)

    val id = JsonSchemaIdExtractor(json)

    val ref = json match {
      case RefExtractor(refId) => refId
    }

    val required = (json \ "required").asOpt[Boolean]

    val genericTypes = getGenericTypes(json)

    val fragments = json match {
      case Fragments(fragment) => fragment
    }

    TryUtils.withSuccess(
      Success(ref),
      Success(id),
      Success(required),
      genericTypes,
      fragments,
      Success(model)
    )(new ParsedTypeReference(_, _, _, _, _, _))
  }

  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[ParsedTypeReference]] = {

    def simpleStringTypeReference(theOtherType: String): Option[Try[ParsedTypeReference]] = {

      val triedOption: Try[Option[ParsedTypeReference]] =
        ParsedType(theOtherType).map {
          case typeRef: ParsedTypeReference => Some(typeRef)
          case _                            => None
        }

      triedOption match {
        case Success(Some(typeRef)) => Some(Success(typeRef))
        case Success(None)          => None
        case Failure(x)             => Some(Failure[ParsedTypeReference](x))
      }
    }

    (ParsedType.typeDeclaration(json), (json \ ParsedTypeReference.value).toOption, json) match {
      case (None, Some(_), _) => Some(ParsedTypeReference(json))
      case (Some(JsString(otherType)), None, _) =>
        simpleStringTypeReference(otherType).map { triedParsedTypeRef =>
          for {
            parsedTypeRef <- triedParsedTypeRef
            genericTypes <- getGenericTypes(json)
            req = (json \ "required").asOpt[Boolean]
          } yield parsedTypeRef.copy(genericTypes = genericTypes, required = req)
        }
      case (_, _, JsString(otherType)) => simpleStringTypeReference(otherType)
      case _                           => None
    }

  }

  private def getGenericTypes(json: JsValue)(implicit parseContext: ParseContext): Try[Map[String, ParsedType]] = {
    (json \ "genericTypes").toOption.collect {
      case genericTs: JsObject =>
        val genericTsMap =
          genericTs.value collect {
            case (field, ParsedType(t)) => (field, t)
          }
        TryUtils.accumulate[String, ParsedType](genericTsMap.toMap)
    } getOrElse Try(Map.empty[String, ParsedType])
  }

}
