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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.model._

/**
 * Created by peter on 28/08/15. 
 */
object PutActionGenerator {

  def generate(action: RichAction): List[String] = {

    val putBodyTypes: List[String] =
      action.contentTypes.headOption map {
        case StringContentType(contentTypeHeader)          => List("String")
        case JsonContentType(contentTypeHeader)            => List("String", "JsValue")
        case TypedContentType(contentTypeHeader, classRep) => List("String", "JsValue", classRep.classDefinition)
      } getOrElse List("String")

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val putSegmentTypeFactory = createPutSegmentType(action.responseTypes.headOption) _

    putBodyTypes.map { putBodyType =>
      generatePutAction(putBodyType, putSegmentTypeFactory(putBodyType), validAcceptHeaders, validContentTypeHeaders)
    }

  }

  private def generatePutAction(putBodyType: String,
                                putSegmentType: String,
                                validAcceptHeaders: List[String],
                                validContentTypeHeaders: List[String]): String = {

    s"""
       def put(body: $putBodyType) =
         new $putSegmentType(
           Some(body),
           validAcceptHeaders = List(${validAcceptHeaders.mkString(",")}),
           validContentTypeHeaders = List(${validContentTypeHeaders.mkString(",")}),
           req = requestBuilder
         )
     """

  }

  private def createPutSegmentType(responseType: Option[ResponseType])(putBodyType: String): String = {
    responseType map {
      case StringResponseType(acceptHeader)          => s"StringPutSegment[$putBodyType]"
      case JsonResponseType(acceptHeader)            => s"JsonPutSegment[$putBodyType]"
      case TypedResponseType(acceptHeader, classRep) => s"TypePutSegment[$putBodyType, ${classRep.classDefinition}}]"
    } getOrElse s"StringPutSegment[$putBodyType]"
  }

}
