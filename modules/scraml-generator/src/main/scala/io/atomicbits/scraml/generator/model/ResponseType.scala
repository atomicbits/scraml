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

package io.atomicbits.scraml.generator.model

import io.atomicbits.scraml.generator.ClassRep

/**
 * Created by peter on 26/08/15. 
 */
sealed trait ResponseType {

  def acceptHeaderValue: String

}

case class StringResponseType(acceptHeaderValue: String) extends ResponseType

case class JsonResponseType(acceptHeaderValue: String) extends ResponseType

case class TypedResponseType(acceptHeaderValue: String, classRep: ClassRep) extends ResponseType

case object NoResponseType extends ResponseType {
  val acceptHeaderValue = ""
}


object ResponseType {

  def apply(acceptHeader: String, classRep: Option[ClassRep]): ResponseType = {

    if (classRep.isDefined) {
      TypedResponseType(acceptHeader, classRep.get)
    } else if (acceptHeader.toLowerCase.contains("json")) {
      JsonResponseType(acceptHeader)
    } else {
      StringResponseType(acceptHeader)
    }

  }

}
