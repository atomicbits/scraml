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
                        prefix: Option[String],
                        requestTimeout: Int,
                        maxConnections: Int,
                        defaultHeaders: Map[String, String]) extends Client {

  val cleanPrefix = prefix.map { pref =>
    val strippedPref = pref.stripPrefix("/").stripSuffix("/")
    s"/$strippedPref"
  } getOrElse ""

  private lazy val client =
    new be.wegenenverkeer.rxhttp.RxHttpClient.Builder()
      .setRequestTimeout(requestTimeout)
      .setMaxConnections(maxConnections)
      .setBaseUrl(s"$protocol://$host:$port$cleanPrefix")
      .build.asScala


  override def exec[B](requesBuilder: RequestBuilder, body: Option[B])
                      (implicit bodyFormat: Format[B]): Future[String] = {
    execToResponse(requesBuilder, body).map(_.body)
  }

  override def execToResponse[B](requesBuilder: RequestBuilder, body: Option[B])
                                (implicit bodyFormat: Format[B]): Future[Response[String]] = {

    val clientWithResourcePathAndMethod = {
      client
        .requestBuilder()
        .setUrlRelativetoBase(requesBuilder.relativePath)
        .setMethod(requesBuilder.method.toString)
    }

    (defaultHeaders ++ requesBuilder.headers).foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addHeader(key, value)
    }

    requesBuilder.queryParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addQueryParam(key, value)
    }

    // ToDo: support for form parameters, different body types (Array[Byte]), streaming,

    body.foreach { body =>
      clientWithResourcePathAndMethod.setBody(bodyFormat.writes(body).toString())
    }

    requesBuilder.formParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addFormParam(key, value)
    }

    val clientRequest = clientWithResourcePathAndMethod.build()

    println(s"Executing request: $clientRequest")

    client.execute[Response[String]](
      clientRequest,
      serverResponse => Response(serverResponse.getStatusCode, serverResponse.getResponseBody)
    )
  }


  override def execToJson[B](request: RequestBuilder, body: Option[B])
                            (implicit bodyFormat: Format[B]): Future[JsValue] =
    execToResponse(request, body).map(res => Json.parse(res.body))


  def execToJsonResponse[B](request: RequestBuilder, body: Option[B])
                           (implicit bodyFormat: Format[B]): Future[Response[JsValue]] =
    execToResponse(request, body).map(res => res.map(Json.parse))


  override def execToDto[B, R](request: RequestBuilder, body: Option[B])
                              (implicit bodyFormat: Format[B],
                               responseFormat: Format[R]): Future[R] = {
    execToJsonResponse(request, body) map (res => res.map(responseFormat.reads)) flatMap {
      case Response(status, JsSuccess(t, path)) => Future.successful(t)
      case Response(status, JsError(e))         =>
        val validationMessages: Seq[String] = {
          e flatMap {
            errorsByPath =>
              val (path, errors) = errorsByPath
              errors map (error => s"$path -> ${error.message}")
          }
        }
        Future.failed(new IllegalArgumentException(s"JSON validation error in the response from ${request.summary}: ${validationMessages mkString ", "}"))
    }
  }

}
