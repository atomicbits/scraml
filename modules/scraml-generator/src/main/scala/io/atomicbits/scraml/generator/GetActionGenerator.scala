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

import io.atomicbits.scraml.generator.model.{StringResponseType, TypedResponseType, JsonResponseType, RichAction}

/**
 * Created by peter on 28/08/15. 
 */
object GetActionGenerator extends ActionParameterSupport {

  def generate(action: RichAction): List[String] = {

    val queryParameterMethodParameters =
      action.queryParameters.toList.map(param => expandParameterAsMethodParameter(param))

    val queryParameterMapEntries =
      action.queryParameters.toList.map(param => expandParameterAsMapEntry(param))

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)

    val getSegmentType =
      action.responseTypes.headOption map {
        case StringResponseType(acceptHeader)          => "StringGetSegment"
        case JsonResponseType(acceptHeader)            => "JsonGetSegment"
        case TypedResponseType(acceptHeader, classRep) => s"TypeGetSegment[${classRep.classDefinition}}]"
      } getOrElse "StringGetSegment"

    List(
      s"""
         def get(${queryParameterMethodParameters.mkString(",")}) = new $getSegmentType(
           queryParams = Map(
             ${queryParameterMapEntries.mkString(",")}
           ),
           validAcceptHeaders = List(${validAcceptHeaders.mkString(",")}),
           req = requestBuilder
         )
       """
    )

  }

}
