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

import io.atomicbits.scraml.ramlparser.model.types.Type
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.JsValue

import scala.util.Try

import io.atomicbits.scraml.ramlparser.parser.TryUtils._


/**
  * Created by peter on 10/02/16.
  */
case class MimeType(header: Header,
                    bodyType: Option[Type] = None,
                    formParameters: Parameters = Parameters())


object MimeType {

  def unapply(mimeTypeAndJsValue: (String, JsValue))(implicit parseContext: ParseContext): Option[Try[MimeType]] = {

    val (mimeType, json) = mimeTypeAndJsValue

    val tryFormParameters = Parameters((json \ "formParameters").toOption)

    val bodyType =
      json match {
        case Type(bType) => Some(bType)
        case _           => None
      }

    val mime =
      for {
        bType <- accumulate(bodyType)
        formParameters <- tryFormParameters
      } yield MimeType(Header(mimeType), bType, formParameters)

    Some(mime)
  }

}
