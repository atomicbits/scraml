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

package io.atomicbits.scraml.ramlparser.model

/**
  * Created by peter on 26/08/16.
  */
trait MediaType {

  def value: String

}
case class ActualMediaType(value: String) extends MediaType

case object NoMediaType extends MediaType {

  val value: String = ""

}

object MediaType {

  def apply(mimeType: String): MediaType = unapply(mimeType).getOrElse(NoMediaType)

  def unapply(mimeType: String): Option[MediaType] = {
    val (typeAndSubT, params) =
      mimeType.split(';').toList match {
        case typeAndSubType :: Nil                      => (Some(typeAndSubType.trim), None)
        case typeAndSubType :: parameters :: unexpected => (Some(typeAndSubType.trim), Some(parameters.trim))
        case Nil                                        => (None, None)
      }

    typeAndSubT.flatMap { typeSub =>
      typeSub.split('/').toList match {
        case ttype :: subtype :: anything => Some(ActualMediaType(typeSub))
        case _                            => None
      }
    }
  }

}
