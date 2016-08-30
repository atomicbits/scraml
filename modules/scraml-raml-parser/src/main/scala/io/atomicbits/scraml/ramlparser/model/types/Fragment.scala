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

import io.atomicbits.scraml.ramlparser.model.{Id, IdExtractor}
import io.atomicbits.scraml.ramlparser.parser.{ParseContext, TryUtils}
import play.api.libs.json.{JsObject, JsString, JsValue}

import scala.util.{Success, Try}

/**
  * Created by peter on 1/04/16.
  */
case class Fragment(id: Id, fragments: Map[String, Type]) extends Identifiable with  Fragmented {

  override def updated(updatedId: Id): Identifiable = copy(id = updatedId)

  def isEmpty: Boolean = fragments.isEmpty

}


object Fragment {

  def apply(schema: JsObject)(implicit parseContext: ParseContext): Try[Fragment] = {
    val id = schema match {
      case IdExtractor(schemaId) => schemaId
    }
    val fragments = schema.value.toSeq collect {
      case (fragmentFieldName, Type(fragment)) => (fragmentFieldName, fragment)
    }

    TryUtils.withSuccess(
      Success(id),
      TryUtils.accumulate(fragments.toMap)
    )(Fragment(_, _))
  }


  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[Fragment]] = {

    json match {
      case jsObject: JsObject => Some(Fragment(jsObject))
      case _                  => None
    }

  }

}
