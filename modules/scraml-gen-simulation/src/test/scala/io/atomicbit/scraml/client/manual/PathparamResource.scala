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

import io.atomicbits.scraml.dsl.scalaplay._
import play.api.libs.json.JsValue

/**
  * Created by peter on 17/08/15.
  */
class PathparamResource(_value: String, private val _req: RequestBuilder) extends ParamSegment[String](_value, _req) {

  def withHeaders(newHeaders: (String, String)*) =
    new PathparamResource(_value, _requestBuilder.withAddedHeaders(newHeaders: _*))

  def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new TypeMethodSegment[String, User](
    method = Get,
    queryParams = Map(
      "queryparX" -> Option(queryparX).map(SimpleHttpParam.create(_)),
      "queryparY" -> Option(queryparY).map(SimpleHttpParam.create(_)),
      "queryParZ" -> queryParZ.map(SimpleHttpParam.create(_))
    ),
    expectedAcceptHeader = Some("application/json"),
    req                  = _requestBuilder
  )

  def put(body: String) =
    new TypeMethodSegment[String, Address](
      method                    = Put,
      theBody                   = Some(body),
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

  def put(body: JsValue) =
    new TypeMethodSegment[JsValue, Address](
      method                    = Put,
      theBody                   = Some(body),
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

  def put(body: User) =
    new TypeMethodSegment[User, Address](method                    = Put,
                                         theBody                   = Some(body),
                                         expectedAcceptHeader      = Some("application/json"),
                                         expectedContentTypeHeader = Some("application/json"),
                                         req                       = _requestBuilder)

  def post(formparX: Int, formParY: Double, formParZ: Option[String]) =
    new TypeMethodSegment(
      method  = Post,
      theBody = None,
      formParams = Map(
        "formparX" -> Option(formparX).map(SimpleHttpParam.create(_)),
        "formParY" -> Option(formParY).map(SimpleHttpParam.create(_)),
        "formParZ" -> formParZ.map(SimpleHttpParam.create(_))
      ),
      multipartParams           = List.empty,
      expectedAcceptHeader      = Some("application/json"),
      expectedContentTypeHeader = Some("application/json"),
      req                       = _requestBuilder
    )

}
