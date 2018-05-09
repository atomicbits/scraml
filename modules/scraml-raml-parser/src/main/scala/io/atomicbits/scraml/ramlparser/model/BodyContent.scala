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

import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedType
import io.atomicbits.scraml.ramlparser.parser.ParseContext
import play.api.libs.json.{ JsObject, JsValue }

import scala.util.{ Failure, Success, Try }
import io.atomicbits.scraml.util.TryUtils._

/**
  * Created by peter on 10/02/16.
  */
case class BodyContent(mediaType: MediaType, bodyType: Option[TypeRepresentation] = None, formParameters: Parameters = new Parameters())

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
        case mediaType @ MediaType(mt) =>
          val tryFormParameters = Parameters((json \ "formParameters").toOption)

          val bodyType =
            json match {
              case ParsedType(bType) => Some(bType)
              case _                 => None
            }

          val triedBodyContent =
            for {
              bType <- accumulate(bodyType)
              formParameters <- tryFormParameters
            } yield BodyContent(MediaType(medType), bType.map(TypeRepresentation(_)), formParameters)

          Some(triedBodyContent)
        case _ => None
      }

    }

    json match {
      case jsObj: JsObject =>
        val bodyContentList = jsObj.value.toList.map(fromJsObjectValues).flatten
        accumulate(bodyContentList) match {
          case Success(Nil)           => None
          case Success(someContent)   => Some(Success(someContent))
          case failure @ Failure(exc) => Some(failure)
        }
      case _ => None
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
          case ParsedType(bType) => Some(bType)
          case _                 => None
        }

      accumulate(bodyType).map(bType => BodyContent(defaultMediaType, bType.map(TypeRepresentation(_))))

    }

  }

}
