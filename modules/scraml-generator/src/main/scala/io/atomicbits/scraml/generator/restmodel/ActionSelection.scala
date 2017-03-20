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

package io.atomicbits.scraml.generator.restmodel

import io.atomicbits.scraml.generator.codegen.GenerationAggr
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

  def apply(action: Action, generationAggr: GenerationAggr)(implicit platform: Platform): ActionSelection = {

    val contentTypeMap: Map[MediaType, ContentType] = {
      val contentTypes = ContentType(action.body, generationAggr)
      if (contentTypes.isEmpty) Map(NoMediaType -> NoContentType)
      else contentTypes.groupBy(_.contentTypeHeader).mapValues(_.head) // There can be only one content type per content type header.
    }

    val responseTypeMap: Map[MediaType, Set[ResponseTypeWithStatus]] = {
      val statusCodes = action.responses.responseMap.keys
      if (statusCodes.isEmpty) {
        Map(NoMediaType -> Set())
      } else {
        val minStatusCode          = statusCodes.min
        val response               = action.responses.responseMap(minStatusCode)
        val actualResponseTypes    = ResponseType(response, generationAggr)
        val responseTypes          = if (actualResponseTypes.isEmpty) Set(NoResponseType) else actualResponseTypes
        val responseTypeWithStatus = responseTypes.map(ResponseTypeWithStatus(_, minStatusCode))

        responseTypeWithStatus.groupBy(_.responseType.acceptHeader) // There can be multiple accept types per accept type header (with different status codes).
      }
    }

    ActionSelection(action, contentTypeMap, responseTypeMap)
  }

}

case class ResponseTypeWithStatus(responseType: ResponseType, status: StatusCode)
