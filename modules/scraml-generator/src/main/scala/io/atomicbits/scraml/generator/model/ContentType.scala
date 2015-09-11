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

import io.atomicbits.scraml.parser.model.Parameter

/**
 * Created by peter on 26/08/15. 
 */
sealed trait ContentType {

  def contentTypeHeaderValue: String

}

case class StringContentType(contentTypeHeaderValue: String) extends ContentType

case class JsonContentType(contentTypeHeaderValue: String) extends ContentType

case class TypedContentType(contentTypeHeaderValue: String, classRep: ClassRep) extends ContentType

case class FormPostContentType(contentTypeHeaderValue: String, formParameters: Map[String, List[Parameter]]) extends ContentType

case class MultipartFormContentType(contentTypeHeaderValue: String) extends ContentType

case object NoContentType extends ContentType {
  val contentTypeHeaderValue = ""
}

object ContentType {

  def apply(contentTypeHeader: String,
            classRep: Option[ClassRep],
            formParameters: Map[String, List[Parameter]]): ContentType = {

    if (contentTypeHeader.toLowerCase == "multipart/form-data") {
      MultipartFormContentType(contentTypeHeader)
    } else if (formParameters.nonEmpty) {
      FormPostContentType(contentTypeHeader, formParameters)
    } else if (classRep.isDefined) {
      TypedContentType(contentTypeHeader, classRep.get)
    } else if (contentTypeHeader.toLowerCase.contains("json")) {
      JsonContentType(contentTypeHeader)
    } else {
      StringContentType(contentTypeHeader)
    }
  }

}