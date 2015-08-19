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

package io.atomicbits.scraml.client

import XoClient._
import io.atomicbits.scraml.dsl._

/**
 * Created by peter on 17/08/15. 
 */
class PathparamResource(value: String, req: RequestBuilder) extends ParamSegment[String](value, req) {

  def withHeader(headerKey: String, headerValue: String) =
    new PathparamResource(value, requestBuilder.copy(headers = requestBuilder.headers + (headerKey -> headerValue)))

  def withHeaders(newHeaders: (String, String)*) =
    new PathparamResource(value, requestBuilder.copy(headers = requestBuilder.headers ++ newHeaders))

  def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new GetSegment(
    queryParams = Map(
      "queryparX" -> Option(queryparX).map(HttpParam(_)),
      "queryparY" -> Option(queryparY).map(HttpParam(_)),
      "queryParZ" -> queryParZ.map(HttpParam(_))
    ),
    validAcceptHeaders = List("application/json"),
    req = requestBuilder
  ) {

    private val executeSegment = new ExecuteSegment[String, User](requestBuilder, None)

    def call() = executeSegment.callToTypeResponse()

  }

  def put(body: String) = new PutSegment(
    validAcceptHeaders = List("application/json"),
    validContentTypeHeaders = List("application/json"),
    req = requestBuilder) {

    private val executeSegment = new ExecuteSegment[String, Address](requestBuilder, Some(body))

    def call() = executeSegment.callToTypeResponse()

  }

  def put(body: User) = new PutSegment(
    validAcceptHeaders = List("application/json"),
    validContentTypeHeaders = List("application/json"),
    req = requestBuilder) {

    private val executeSegment = new ExecuteSegment[User, Address](requestBuilder, Some(body))

    def call() = executeSegment.callToTypeResponse()

  }

  def post(formparX: Int, formParY: Double, formParZ: Option[String]) = new PostSegment(
    formParams = Map(
      "formparX" -> Option(formparX).map(HttpParam(_)),
      "formParY" -> Option(formParY).map(HttpParam(_)),
      "formParZ" -> formParZ.map(HttpParam(_))
    ),
    multipartParams = List.empty,
    validAcceptHeaders = List("application/json"),
    validContentTypeHeaders = List("application/json"),
    req = requestBuilder
  ) {

    private val executeSegment = new ExecuteSegment[String, User](requestBuilder, None)

    def call() = executeSegment.callToTypeResponse()

  }

}
