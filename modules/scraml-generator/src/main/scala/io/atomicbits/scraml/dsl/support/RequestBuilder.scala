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

import play.api.libs.json.Reads


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
                          headers: Map[String, String] = Map(),
                          defaultHeaders: Map[String, String] = Map.empty,
                          body: Option[String] = None) {

  def relativePath = reversePath.reverse.mkString("/", "/", "")

  def allHeaders = defaultHeaders ++ headers

  def isFormPost: Boolean = method == Post && formParameters.nonEmpty

  def execute() = client.execute(this)

  def executeToJson() = client.executeToJson(this)

  def executeToJsonDto[T]()(implicit reader: Reads[T]) = client.executeToJsonDto[T](this)

}
