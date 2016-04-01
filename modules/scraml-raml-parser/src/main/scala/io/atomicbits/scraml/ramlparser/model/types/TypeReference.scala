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

import io.atomicbits.scraml.ramlparser.model.{RefExtractor, IdExtractor, Id}
import io.atomicbits.scraml.ramlparser.parser.TryUtils
import play.api.libs.json.JsObject

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class TypeReference(id: Id,
                         refersTo: Id,
                         required: Boolean = false,
                         genericTypes: Map[String, Type] = Map.empty,
                         fragments: Map[String, Type] = Map.empty) extends PrimitiveType with AllowedAsObjectField {

  override def updated(updatedId: Id): Type = copy(id = updatedId)

}


object TypeReference {

  def apply(schema: JsObject)(implicit nameToIdOpt: String => Id): Try[TypeReference] = {

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
      Success(id),
      Success(ref),
      Success(required.getOrElse(false)),
      genericTypes,
      fragments
    )(new TypeReference(_, _, _, _, _))
  }

}
