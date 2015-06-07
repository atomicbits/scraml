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

package io.atomicbits.scraml.dsl.support.client.rxhttpclient

import be.wegenenverkeer.rxhttp.scala.ImplicitConversions._
import io.atomicbits.scraml.dsl.Response
import io.atomicbits.scraml.dsl.support.{Client, RequestBuilder}
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io).
 *
 * The only supported client at this time is the RxHttpClient.
 *
 */
case class RxHttpClient(protocol: String,
                        host: String,
                        port: Int,
                        requestTimeout: Int,
                        maxConnections: Int) extends Client {

  private lazy val client =
    new be.wegenenverkeer.rxhttp.RxHttpClient.Builder()
      .setRequestTimeout(requestTimeout)
      .setMaxConnections(maxConnections)
      .setBaseUrl(s"$protocol://$host:$port")
      .build.asScala


  override def execute(requesBuilder: RequestBuilder): Future[Response[String]] = {

    val clientWithResourcePathAndMethod = {
      client
        .requestBuilder()
        .setUrlRelativetoBase(requesBuilder.relativePath)
        .setMethod(requesBuilder.method.toString)
    }

    requesBuilder.allHeaders.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addHeader(key, value)
    }

    requesBuilder.queryParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addQueryParam(key, value)
    }

    // ToDo: support for form parameters, different body types (Array[Byte]), streaming,

    requesBuilder.body.foreach { body =>
      clientWithResourcePathAndMethod.setBody(body)
    }

    requesBuilder.formParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addFormParam(key, value)
    }

    val clientRequest = clientWithResourcePathAndMethod.build()

    client.execute[Response[String]](
      clientRequest,
      serverResponse => Response(serverResponse.getStatusCode, serverResponse.getResponseBody)
    )
  }


  override def executeToJson(request: RequestBuilder): Future[Response[JsValue]] =
    execute(request).map(res => res.map(Json.parse))


  override def executeToJsonDto[T](request: RequestBuilder)
                                  (implicit reader: Reads[T]): Future[Response[T]] = {
    executeToJson(request) map (res => res.map(reader.reads)) flatMap {
      case Response(status, JsSuccess(t, path)) => Future.successful(Response(status, t))
      case Response(status, JsError(e)) =>
        val validationMessages: Seq[String] = {
          e map {
            errorsByPath =>
              val (path, errors) = errorsByPath
              errors map (_.message)
          } flatten
        }
        Future.failed(new IllegalArgumentException(validationMessages mkString ", "))
    }
  }

}
