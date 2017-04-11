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

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.JsValue

import scala.language.postfixOps
import scala.util.{ Success, Try }

/**
  * Created by peter on 26/08/16.
  */
case class Body(contentMap: Map[MediaType, BodyContent] = Map.empty) {

  def forHeader(mimeType: MediaType): Option[BodyContent] = contentMap.get(mimeType)

}

object Body {

  def apply(json: JsValue)(implicit parseContext: ParseContext): Try[Body] = {

    json \ "body" toOption match {
      case Some(BodyContentAsMediaTypeMap(triedBodyContents)) =>
        triedBodyContents.map { bodyContentList =>
          val contentMap =
            bodyContentList.map { bodyContent =>
              bodyContent.mediaType -> bodyContent
            }
          Body(contentMap.toMap)
        }
      case Some(BodyContentAsDefaultMediaType(triedBodyContent)) =>
        triedBodyContent.map { bodyContent =>
          Body(Map(bodyContent.mediaType -> bodyContent))
        }
      case _ => Success(Body())
    }
  }

}
