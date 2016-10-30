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
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Failure, Success, Try}
import io.atomicbits.scraml.util.TryUtils._


/**
  * Created by peter on 10/02/16.
  */
case class BodyContent(mediaType: MediaType,
                       bodyType: Option[Type] = None,
                       formParameters: Parameters = Parameters())


object BodyContentAsMediaTypeMap {

  /**
    * Tries to represent a body from a mediatype map
    *
    * @param json Everything under the 'body' field of a resource spec.
    */
  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[List[BodyContent]]] = {


    def fromJsObjectValues(mediaTypeAndJsValue: (String, JsValue))(implicit parseContext: ParseContext): Option[Try[BodyContent]] = {

      val (medType, json) = mediaTypeAndJsValue

      medType match {
        case mediaType@MediaType(mt) =>
          val tryFormParameters = Parameters((json \ "formParameters").toOption)

          val bodyType =
            json match {
              case Type(bType) => Some(bType)
              case _           => None
            }

          val triedBodyContent =
            for {
              bType <- accumulate(bodyType)
              formParameters <- tryFormParameters
            } yield BodyContent(MediaType(medType), bType, formParameters)

          Some(triedBodyContent)
        case _                       => None
      }

    }


    json match {
      case jsObj: JsObject =>
        val bodyContentList = jsObj.value.toList.map(fromJsObjectValues).flatten
        accumulate(bodyContentList) match {
          case Success(Nil)         => None
          case Success(someContent) => Some(Success(someContent))
          case failure@Failure(exc) => Some(failure)
        }
      case _               => None
    }

  }

}


object BodyContentAsDefaultMediaType {

  /**
    * Tries to represent a body from a mediatype map
    *
    * @param json Everything under the 'body' field of a resource spec.
    */
  def unapply(json: JsValue)(implicit parseContext: ParseContext): Option[Try[BodyContent]] = {

    parseContext.defaultMediaType.map { defaultMediaType =>

      val bodyType =
        json match {
          case Type(bType) => Some(bType)
          case _           => None
        }

      accumulate(bodyType).map(bType => BodyContent(defaultMediaType, bType))

    }

  }

}
