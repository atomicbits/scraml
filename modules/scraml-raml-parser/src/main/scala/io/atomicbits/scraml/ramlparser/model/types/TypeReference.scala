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

import io.atomicbits.scraml.ramlparser.model.{Id, IdExtractor, ImplicitId, RefExtractor}
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class TypeReference(refersTo: Id,
                         id: Id = ImplicitId,
                         required: Option[Boolean] = None,
                         genericTypes: Map[String, Type] = Map.empty,
                         fragments: Map[String, Type] = Map.empty) extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

}


object TypeReference {

  val value = "$ref"


  def apply(schema: JsValue)(implicit parseContext: ParseContext): Try[TypeReference] = {

    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    val ref = schema match {
      case RefExtractor(refId) => refId
    }

    val required = (schema \ "required").asOpt[Boolean]

    val genericTypes: Try[Map[String, Type]] =
      (schema \ "genericTypes").toOption.collect {
        case genericTs: JsObject =>
          val genericTsMap =
            genericTs.value collect {
              case (field, jsObj: JsObject) => (field, Type(jsObj))
            }
          TryUtils.accumulate[String, Type](genericTsMap.toMap)
      } getOrElse Try(Map.empty[String, Type])

    val fragments = TryUtils.accumulate(Type.collectFragments(schema))

    TryUtils.withSuccess(
      Success(ref),
      Success(id),
      Success(required),
      genericTypes,
      fragments
    )(new TypeReference(_, _, _, _, _))
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[TypeReference]] = {

    def checkOtherType(theOtherType: String): Option[Try[TypeReference]] = {
      Type(theOtherType) match {
        case typeRef: Try[TypeReference] => Some(typeRef)
        case _                           => None
      }
    }

    (Type.typeDeclaration(json), (json \ TypeReference.value).toOption, json) match {
      case (None, Some(_), _)                   => Some(TypeReference(json))
      case (Some(JsString(otherType)), None, _) => checkOtherType(otherType)
      case (_, _, JsString(otherType))          => checkOtherType(otherType)
      case _                                    => None
    }

  }

}
