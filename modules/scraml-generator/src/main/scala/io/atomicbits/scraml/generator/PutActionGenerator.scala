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
object PutActionGenerator extends ActionGeneratorSupport {

  def generate(action: RichAction): List[String] = {

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val putSegmentTypeFactory = createSegmentType(action.actionType, action.responseTypes.headOption) _

    bodyTypes(action).map { putBodyType =>
      generatePutAction(putBodyType, putSegmentTypeFactory(putBodyType), validAcceptHeaders, validContentTypeHeaders)
    }

  }

  private def generatePutAction(bodyType: Option[String],
                                segmentType: String,
                                validAcceptHeaders: List[String],
                                validContentTypeHeaders: List[String]): String = {

    val (actionBodyParameter, bodyField) = bodyType.map(bdType => (s"body: $bdType", "Some(body)")).getOrElse("", "None")

    s"""
       def put($actionBodyParameter) =
         new $segmentType(
           $bodyField,
           validAcceptHeaders = List(${validAcceptHeaders.map(quoteString).mkString(",")}),
           validContentTypeHeaders = List(${validContentTypeHeaders.map(quoteString).mkString(",")}),
           req = requestBuilder
         )
     """

  }


}
