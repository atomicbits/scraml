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
import play.api.libs.json._

import scala.concurrent.Future

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
trait Client {

  def exec[B](request: RequestBuilder, body: Option[B])
                (implicit bodyFormat: Format[B]): Future[String]

  def execToResponse[B](request: RequestBuilder, body: Option[B])
                          (implicit bodyFormat: Format[B]): Future[Response[String]]

  def execToJson[B](request: RequestBuilder, body: Option[B])
                      (implicit bodyFormat: Format[B]): Future[JsValue]

  def execToDto[B, R](request: RequestBuilder, body: Option[B])
                            (implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[R]

}
