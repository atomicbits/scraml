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

package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Response(headers: Map[String, Parameter], body: Map[String, MimeType])

object Response {

  def apply(response: org.raml.model.Response): Response = {

    val headers: Map[String, Parameter] =
      Transformer.transformMap[org.raml.model.parameter.Header, Parameter](Parameter(_))(response.getHeaders)

    val body: Map[String, MimeType] =
      Transformer.transformMap[org.raml.model.MimeType, MimeType](MimeType(_))(response.getBody)

    Response(headers, body)
  }

}
