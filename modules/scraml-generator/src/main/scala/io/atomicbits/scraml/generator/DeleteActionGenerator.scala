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
object DeleteActionGenerator {

  def generate(action: RichAction): List[String] = {

    val deleteBodyTypes: List[String] =
      action.contentTypes.headOption map {
        case StringContentType(contentTypeHeader)          => List("String")
        case JsonContentType(contentTypeHeader)            => List("String", "JsValue")
        case TypedContentType(contentTypeHeader, classRep) => List("String", "JsValue", classRep.classDefinition)
        case x                                             => sys.error(s"We don't expect a $x content type on a delete action.")
      } getOrElse List("String")

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val deleteSegmentTypeFactory = createDeleteSegmentType(action.responseTypes.headOption) _

    deleteBodyTypes.map { deleteBodyType =>
      generateDeleteAction(deleteBodyType, deleteSegmentTypeFactory(deleteBodyType), validAcceptHeaders, validContentTypeHeaders)
    }

  }

  private def generateDeleteAction(deleteBodyType: String,
                                   deleteSegmentType: String,
                                   validAcceptHeaders: List[String],
                                   validContentTypeHeaders: List[String]): String = {

    s"""
       def delete(body: $deleteBodyType) =
         new $deleteSegmentType(
           Some(body),
           validAcceptHeaders = List(${validAcceptHeaders.mkString(",")}),
           validContentTypeHeaders = List(${validContentTypeHeaders.mkString(",")}),
           req = requestBuilder
         )
     """

  }

  private def createDeleteSegmentType(responseType: Option[ResponseType])(deleteBodyType: String): String = {
    responseType map {
      case StringResponseType(acceptHeader)          => s"StringDeleteSegment[$deleteBodyType]"
      case JsonResponseType(acceptHeader)            => s"JsonDeleteSegment[$deleteBodyType]"
      case TypedResponseType(acceptHeader, classRep) => s"TypeDeleteSegment[$deleteBodyType, ${classRep.classDefinition}}]"
      case x                                         => sys.error(s"We don't expect a $x content type on a delete action.")
    } getOrElse s"StringDeleteSegment[$deleteBodyType]"
  }

}
