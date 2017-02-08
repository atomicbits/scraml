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
import io.atomicbits.scraml.ramlparser.model.Action

/**
  * Created by peter on 20/01/17.
  */
case class ActionSelection(action: Action,
                           selectedContentType: ContentType   = NoContentType,
                           selectedResponsetype: ResponseType = NoResponseType) {

  def contentTypes(generationAggr: GenerationAggr)(implicit platform: Platform): Set[ContentType] = {
    val contentTypes = ContentType(action.body, generationAggr)
    if (contentTypes.isEmpty) Set(NoContentType)
    else contentTypes
  }

  def responseTypes(generationAggr: GenerationAggr)(implicit platform: Platform): Set[ResponseType] = {
    val responseTypes = action.responses.responseMap.values.flatMap(ResponseType(_, generationAggr)).toSet
    if (responseTypes.isEmpty) Set(NoResponseType)
    else responseTypes
  }

  def withContentTypeSelection(contentType: ContentType): ActionSelection = copy(selectedContentType = contentType)

  def withResponseTypeSelection(responseType: ResponseType): ActionSelection = copy(selectedResponsetype = responseType)

}

object ActionSelection {

  implicit class ActionOps(val action: Action) {

    def withContentTypeSelection(contentType: ContentType): ActionSelection =
      ActionSelection(action, selectedContentType = contentType)

    def withResponseTypeSelection(responseType: ResponseType): ActionSelection =
      ActionSelection(action, selectedResponsetype = responseType)

  }

}
