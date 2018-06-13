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

  // val values = contentMap.values.toList

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
