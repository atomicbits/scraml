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

import io.atomicbits.scraml.ramlparser.model.{IdExtractor, Id}
import io.atomicbits.scraml.ramlparser.parser.{RamlParseException, TryUtils}
import play.api.libs.json.JsObject

import scala.util.{Failure, Success, Try}

/**
  * Created by peter on 25/03/16.
  */
case class ArrayType(id: Id,
                     items: Type,
                     required: Boolean = false,
                     minItems: Option[Int] = None,
                     maxItems: Option[Int] = None,
                     uniqueItems: Boolean = false,
                     fragments: Map[String, Type] = Map.empty) extends Type with AllowedAsObjectField {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}


object ArrayType {

  def apply(schema: JsObject)(implicit nameToId: String => Id): Try[ArrayType] = {

    // Process the id
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }

    // Process the items type
    val items =
      schema \ "items" toOption match {
        case Some(obj: JsObject) => Type(obj)
        case _                   =>
          Failure[Type](RamlParseException(s"An array type must have an 'items' field that refers to a JsObject in $schema"))
      }

    // Process the required field
    val required = (schema \ "required").asOpt[Boolean]

    val fragments = TryUtils.accumulate(Type.collectFragments(schema))

    TryUtils.withSuccess(
      Success(id),
      items,
      Success(required.getOrElse(false)),
      Success(None),
      Success(None),
      Success(false),
      fragments
    )(ArrayType(_, _, _, _, _, _, _))
  }

}
