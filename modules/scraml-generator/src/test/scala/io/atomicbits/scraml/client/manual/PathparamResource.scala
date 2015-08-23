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

package io.atomicbits.scraml.client.manual

import io.atomicbits.scraml.client.manual.User
import io.atomicbits.scraml.client.manual.Address
import io.atomicbits.scraml.dsl._
import play.api.libs.json.JsValue


/**
 * Created by peter on 17/08/15. 
 */
class PathparamResource(value: String, req: RequestBuilder) extends ParamSegment[String](value, req) {

  def withHeader(header: (String, String)) =
    new PathparamResource(value, requestBuilder.withAddedHeaders(header))

  def withHeaders(newHeaders: (String, String)*) =
    new PathparamResource(value, requestBuilder.withAddedHeaders(newHeaders: _*))

  def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new TypeGetSegment[User](
    queryParams = Map(
      "queryparX" -> Option(queryparX).map(HttpParam(_)),
      "queryparY" -> Option(queryparY).map(HttpParam(_)),
      "queryParZ" -> queryParZ.map(HttpParam(_))
    ),
    validAcceptHeaders = List("application/json"),
    req = requestBuilder
  )

  def put(body: String) =
    new TypePutSegment[String, Address](
      Some(body),
      validAcceptHeaders = List("application/json"),
      validContentTypeHeaders = List("application/json"),
      req = requestBuilder)

  def put(body: JsValue) =
    new TypePutSegment[JsValue, Address](
      Some(body),
      validAcceptHeaders = List("application/json"),
      validContentTypeHeaders = List("application/json"),
      req = requestBuilder)

  def put(body: User) =
    new TypePutSegment[User, Address](
      Some(body),
      validAcceptHeaders = List("application/json"),
      validContentTypeHeaders = List("application/json"),
      req = requestBuilder)

  def post(formparX: Int, formParY: Double, formParZ: Option[String]) =
    new TypePostSegment(
      theBody = None,
      formParams = Map(
        "formparX" -> Option(formparX).map(HttpParam(_)),
        "formParY" -> Option(formParY).map(HttpParam(_)),
        "formParZ" -> formParZ.map(HttpParam(_))
      ),
      multipartParams = List.empty,
      validAcceptHeaders = List("application/json"),
      validContentTypeHeaders = List("application/json"),
      req = requestBuilder
    )

}
