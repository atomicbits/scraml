/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.support

import io.atomicbits.scraml.dsl.Response
import play.api.libs.json.{JsValue, Format, Reads}

import scala.concurrent.Future


/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
case class RequestBuilder(client: Client,
                          reversePath: List[String] = Nil,
                          method: Method = Get,
                          queryParameters: Map[String, String] = Map.empty,
                          formParameters: Map[String, String] = Map.empty,
                          validAcceptHeaders: List[String] = Nil,
                          validContentTypeHeaders: List[String] = Nil,
                          headers: Map[String, String] = Map()) {

  def relativePath = reversePath.reverse.mkString("/", "/", "")

  def defaultHeaders = client.defaultHeaders

  def allHeaders = defaultHeaders ++ headers

  def isFormPost: Boolean = method == Post && formParameters.nonEmpty

  def exec[B](body: Option[B])(implicit bodyFormat: Format[B]) = client.exec(this, body)

  def execToResponse[B](body: Option[B])(implicit bodyFormat: Format[B]): Future[Response[String]] = client.execToResponse(this, body)

  def execToJson[B](body: Option[B])(implicit bodyFormat: Format[B]): Future[JsValue] = client.execToJson(this, body)

  def execToDto[B, R](body: Option[B])
                            (implicit bodyFormat: Format[B], responseFormat: Format[R]) =
    client.execToDto[B, R](this, body)

}
