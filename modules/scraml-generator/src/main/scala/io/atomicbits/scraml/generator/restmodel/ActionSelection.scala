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

package io.atomicbits.scraml.generator.restmodel

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.ramlparser.model.{ Action, MediaType, NoMediaType, StatusCode }

/**
  * Created by peter on 20/01/17.
  */
case class ActionSelection(action: Action,
                           contentTypeMap: Map[MediaType, ContentType],
                           responseTypeMap: Map[MediaType, Set[ResponseTypeWithStatus]],
                           selectedContentTypeHeader: MediaType  = NoMediaType,
                           selectedResponsetypeHeader: MediaType = NoMediaType) {

  val contentTypeHeaders: Set[MediaType] = contentTypeMap.keys.toSet

  val responseTypeHeaders: Set[MediaType] = responseTypeMap.keys.toSet

  def selectedContentType: ContentType = contentTypeMap.getOrElse(selectedContentTypeHeader, NoContentType)

  def selectedResponseType: ResponseType = {
    if (selectedResponseTypesWithStatus.isEmpty) NoResponseType
    else {
      val smallestStatusCode                 = selectedResponseTypesWithStatus.map(_.status).min
      val responseTypeWithSmallestStatusCode = selectedResponseTypesWithStatus.groupBy(_.status)(smallestStatusCode)
      responseTypeWithSmallestStatusCode.head.responseType
    }
  }

  def selectedResponseTypesWithStatus: Set[ResponseTypeWithStatus] = responseTypeMap.getOrElse(selectedResponsetypeHeader, Set.empty)

  def withContentTypeSelection(contentTypeHeader: MediaType): ActionSelection = copy(selectedContentTypeHeader = contentTypeHeader)

  def withResponseTypeSelection(responseTypeHeader: MediaType): ActionSelection = copy(selectedResponsetypeHeader = responseTypeHeader)

}

object ActionSelection {

  def apply(action: Action)(implicit platform: Platform): ActionSelection = {

    val contentTypeMap: Map[MediaType, ContentType] = {
      val contentTypes = ContentType(action.body)
      if (contentTypes.isEmpty) Map(NoMediaType -> NoContentType)
      else contentTypes.groupBy(_.contentTypeHeader).transform((_, v) => v.head) // There can be only one content type per content type header.
    }

    val responseTypeMap: Map[MediaType, Set[ResponseTypeWithStatus]] = {
      val statusCodes = action.responses.responseMap.keys
      if (statusCodes.isEmpty) {
        Map(NoMediaType -> Set())
      } else {
        val minStatusCode          = statusCodes.min
        val response               = action.responses.responseMap(minStatusCode)
        val actualResponseTypes    = ResponseType(response)
        val responseTypes          = if (actualResponseTypes.isEmpty) Set(NoResponseType) else actualResponseTypes
        val responseTypeWithStatus = responseTypes.map(ResponseTypeWithStatus(_, minStatusCode))

        responseTypeWithStatus.groupBy(_.responseType.acceptHeader) // There can be multiple accept types per accept type header (with different status codes).
      }
    }

    ActionSelection(action, contentTypeMap, responseTypeMap)
  }

}

case class ResponseTypeWithStatus(responseType: ResponseType, status: StatusCode)
