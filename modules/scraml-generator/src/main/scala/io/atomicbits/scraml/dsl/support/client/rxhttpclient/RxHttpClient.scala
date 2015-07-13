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

package io.atomicbits.scraml.dsl.support.client.rxhttpclient

import be.wegenenverkeer.rxhttp.{HttpClientError, HttpServerError}
import be.wegenenverkeer.rxhttp.scala.ImplicitConversions._
import io.atomicbits.scraml.dsl.Response
import io.atomicbits.scraml.dsl.support._
import play.api.libs.json._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.language.postfixOps
import scala.collection.JavaConverters._

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


  def callTo200Response[B](requestBuilder: RequestBuilder, body: Option[B])
                          (implicit bodyFormat: Format[B]): Future[Response[String]] = {
    val clientWithResourcePathAndMethod = {
      client
        .requestBuilder()
        .setUrlRelativetoBase(requestBuilder.relativePath)
        .setMethod(requestBuilder.method.toString)
    }

    (defaultHeaders ++ requestBuilder.headers).foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addHeader(key, value)
    }

    requestBuilder.queryParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addQueryParam(key, value)
    }

    // ToDo: support for form parameters, different body types (Array[Byte]), streaming,

    body.foreach { body =>
      clientWithResourcePathAndMethod.setBody(bodyFormat.writes(body).toString())
    }

    requestBuilder.formParameters.foreach { element =>
      val (key, value) = element
      clientWithResourcePathAndMethod.addFormParam(key, value)
    }

    requestBuilder.multipartParams.foreach {
      case part: ByteArrayPart =>
        clientWithResourcePathAndMethod
          .addByteArrayBodyPart(
            part.name,
            part.bytes,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
      case part: FilePart      =>
        clientWithResourcePathAndMethod
          .addFileBodyPart(
            part.name,
            part.file,
            part.contentType.orNull,
            part.charset.orNull,
            part.fileName.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
      case part: StringPart    =>
        clientWithResourcePathAndMethod
          .addStringBodyPart(
            part.name,
            part.value,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
    }

    val clientRequest = clientWithResourcePathAndMethod.build()

    client.execute[Response[String]](
      clientRequest,
      serverResponse =>
        Response(
          status = serverResponse.getStatusCode,
          stringBody = serverResponse.getResponseBody,
          jsonBody = None,
          body = Some(serverResponse.getResponseBody),
          headers = serverResponse.getHeaders.asScala.foldLeft(Map.empty[String, List[String]]) { (map, el) =>
            val (key, value) = el
            map + (key -> value.asScala.toList)
          }
        )
    )
  }


  override def callToStringResponse[B](requestBuilder: RequestBuilder, body: Option[B])
                                      (implicit bodyFormat: Format[B]): Future[Response[String]] = {
    // The RxHttpClient returns a failed future on client or server errors (status >= 400).
    // We recover these failres into a successful future so that we are able to report on the status more directly.
    callTo200Response[B](requestBuilder, body).recover {
      case httpClientError: HttpClientError if httpClientError.getResponse.isPresent =>
        Response(httpClientError.getResponse.get().getStatusCode, httpClientError.getResponse.get().getResponseBody)
      case httpServerError: HttpServerError if httpServerError.getResponse.isPresent =>
        Response(httpServerError.getResponse.get().getStatusCode, httpServerError.getResponse.get().getResponseBody)
    }
  }

  override def callToJsonResponse[B](requestBuilder: RequestBuilder, body: Option[B])
                                    (implicit bodyFormat: Format[B]): Future[Response[JsValue]] = {
    // In case of a non-200 or non-204 response, we set the JSON body to None and keep the future successful and return the
    // Response object.
    callToStringResponse[B](requestBuilder, body).map { response =>
      if (response.status == 200) {
        val respJson = response.map(Json.parse)
        respJson.copy(jsonBody = respJson.body)
      } else {
        response.map(_ => JsNull).copy(body = None)
      }
    }
  }

  override def callToTypeResponse[B, R](requestBuilder: RequestBuilder, body: Option[B])
                                       (implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] = {
    // In case of a non-200 or non-204 response, we set the typed body to None and keep the future successful and return the
    // Response object. When the JSON body on a 200-response cannot be parsed into the expected type, we DO fail the future because
    // in that case we violate the RAML specs.
    callToJsonResponse[B](requestBuilder, body) map { response =>
      if (response.status == 200) {
        response.map(responseFormat.reads)
      } else {
        // We hijack the 'JsError(Nil)' type here to mark the non-200 case that has to result in a successful future with empty body.
        response.map(_ => JsError(Nil))
      }
    } flatMap {
      case response@Response(_, _, _, Some(JsSuccess(t, path)), _) => Future.successful(response.copy(body = Some(t)))
      case response@Response(_, _, _, Some(JsError(Nil)), _)       => Future.successful(response.copy(body = None))
      case Response(_, _, _, Some(JsError(e)), _)                  =>
        val validationMessages: Seq[String] = {
          e flatMap {
            errorsByPath =>
              val (path, errors) = errorsByPath
              errors map (error => s"$path -> ${error.message}")
          }
        }
        Future.failed(
          new IllegalArgumentException(
            s"JSON validation error in the response from ${requestBuilder.summary}: ${validationMessages mkString ", "}"))
    }
  }

}
