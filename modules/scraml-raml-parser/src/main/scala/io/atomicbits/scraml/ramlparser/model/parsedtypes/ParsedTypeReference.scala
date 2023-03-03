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

import com.fasterxml.jackson.core.JsonParseException
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.{ ParseContext, RamlParseException }
import io.atomicbits.scraml.util.TryUtils
import play.api.libs.json.{ JsArray, JsObject, JsString, JsValue }

import scala.util.{ Failure, Success, Try }

/**
  * Created by peter on 1/04/16.
  */
case class ParsedTypeReference(refersTo: Id,
                               id: Id                         = ImplicitId,
                               required: Option[Boolean]      = None,
                               genericTypes: List[ParsedType] = List.empty,
                               fragments: Fragments           = Fragments(),
                               model: TypeModel               = RamlModel)
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

    val ref = (json: @unchecked) match {
      case RefExtractor(refId) => refId
    }

    val required = (json \ "required").asOpt[Boolean]

    val genericTypes = getGenericTypes(json)

    val fragments = (json: @unchecked) match {
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

  private def getGenericTypes(json: JsValue)(implicit parseContext: ParseContext): Try[List[ParsedType]] = {
    (json \ "genericTypes").toOption.collect {
      case genericTs: JsArray =>
        val genericTsList =
          genericTs.value collect {
            case ParsedType(t) => t
          }
        TryUtils.accumulate(genericTsList.toList).map(_.toList)
      case genericTs: JsObject =>
        val message =
          s"""
             |-
             |
             |The following generic type reference refers to its type parameter values
             |using an object map. This is the old way and it is not supported any longer.
             |Replace it with a JSON array. The order of the elements in the array must 
             |correspond to the order of the type parameters in the referred type. 
             |
             |The conflicting genericTypes construct is:
             |
             |$genericTs
             |
             |in
             |
             |${parseContext.sourceTrail.mkString(" <- ")}
             |
             |-
           """.stripMargin
        Failure(RamlParseException(message))
    } getOrElse Try(List.empty[ParsedType])
  }

}
