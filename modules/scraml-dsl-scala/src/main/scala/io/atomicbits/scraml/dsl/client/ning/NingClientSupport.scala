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

package io.atomicbits.scraml.dsl.client.ning

import java.util.concurrent.CompletionStage
import java.util.function.{BiConsumer, Function ⇒ JFunction}

import scala.concurrent.ExecutionContext.Implicits.global

import com.ning.http.client.{AsyncCompletionHandler, Request, AsyncHttpClient, AsyncHttpClientConfig}
import io.atomicbits.scraml.dsl.client.ClientConfig
import io.atomicbits.scraml.dsl._
import org.slf4j.{LoggerFactory, Logger}
import play.api.libs.json._

import scala.concurrent.{Promise, Future}
import scala.util.{Try, Failure, Success}

import scala.collection.JavaConversions._
import scala.collection.JavaConverters._

/**
 * Created by peter on 28/10/15.
 */
case class NingClientSupport(protocol: String,
                             host: String,
                             port: Int,
                             prefix: Option[String],
                             config: ClientConfig,
                             defaultHeaders: Map[String, String]) extends Client {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[NingClientSupport])

  private val cleanPrefix = prefix.map { pref =>
    val strippedPref = pref.stripPrefix("/").stripSuffix("/")
    s"/$strippedPref"
  } getOrElse ""

  private lazy val client = {
    val configBuilder: AsyncHttpClientConfig.Builder = new AsyncHttpClientConfig.Builder
    new AsyncHttpClient(applyConfiguration(configBuilder).build)
  }


  def callToJsonResponse[B](requestBuilder: RequestBuilder, body: Option[B])(implicit bodyFormat: Format[B]): Future[Response[JsValue]] = {
    callToStringResponse[B](requestBuilder, body).map { response =>
      if (response.status == 200) {
        val respJson = response.map(Json.parse)
        respJson.copy(jsonBody = respJson.body)
      } else {
        response.map(_ => JsNull).copy(body = None)
      }
    }
  }


  def callToTypeResponse[B, R](requestBuilder: RequestBuilder, body: Option[B])
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
      case response@Response(_, _, _, None, _)                     => Future.successful(response.copy(body = None))
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

  def callToStringResponse[B](requestBuilder: RequestBuilder, body: Option[B])
                             (implicit bodyFormat: Format[B]): Future[Response[String]] = {
    val ningBuilder = {
      // Create builder
      val ningRb: com.ning.http.client.RequestBuilder = new com.ning.http.client.RequestBuilder
      val baseUrl: String = protocol + "://" + host + ":" + port + cleanPrefix
      ningRb.setUrl(baseUrl + "/" + requestBuilder.relativePath.stripPrefix("/"))
      ningRb.setMethod(requestBuilder.method.toString)
      ningRb
    }

    (defaultHeaders ++ requestBuilder.headers).foreach {
      element =>
        val (key, value) = element
        ningBuilder.addHeader(key, value)
    }

    requestBuilder.queryParameters.foreach {
      element =>
        val (key, value) = element
        value match {
          case SingleHttpParam(parameter)    => ningBuilder.addQueryParam(key, parameter)
          case RepeatedHttpParam(parameters) =>
            parameters.foreach(parameter => ningBuilder.addQueryParam(key, parameter))
        }
    }

    // ToDo: support for different body types (Array[Byte]), streaming,

    body.foreach {
      body =>
        ningBuilder.setBody(bodyFormat.writes(body).toString())
    }

    requestBuilder.formParameters.foreach {
      element =>
        val (key, value) = element
        value match {
          case SingleHttpParam(parameter)    => ningBuilder.addFormParam(key, parameter)
          case RepeatedHttpParam(parameters) =>
            parameters.foreach(parameter => ningBuilder.addFormParam(key, parameter))
        }
    }

    requestBuilder.multipartParams.foreach {
      case part: ByteArrayPart =>
        ningBuilder.addBodyPart(
          new com.ning.http.client.multipart.ByteArrayPart(
            part.name,
            part.bytes,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
      case part: FilePart      =>
        ningBuilder.addBodyPart(
          new com.ning.http.client.multipart.FilePart(
            part.name,
            part.file,
            part.contentType.orNull,
            part.charset.orNull,
            part.fileName.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
      case part: StringPart    =>
        ningBuilder.addBodyPart(
          new com.ning.http.client.multipart.StringPart(
            part.name,
            part.value,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
    }

    val ningRequest: Request = ningBuilder.build()

    // ToDo: logging s"client request: $ningRequest"

    LOGGER.debug(s"Executing request: $ningRequest")

    val promise = Promise[Response[String]] ()

    client.executeRequest(ningRequest, new AsyncCompletionHandler[String]() {
      @throws(classOf[Exception])
      def onCompleted(response: com.ning.http.client.Response): String = {
        val resp: Try[Response[String]] =
          Try {
            val stringResponseBody: String = response.getResponseBody(config.responseCharset.displayName)
            val responseBody: Option[String] =
              if (stringResponseBody == null || stringResponseBody.isEmpty) {
                None
              } else {
                Some(stringResponseBody)
              }
            val headers: Map[String, List[String]] =
              mapAsScalaMap(response.getHeaders).foldLeft(Map.empty[String, List[String]]) { (map, headerPair) =>
                val (key, value) = headerPair
                map + (key -> value.asScala.toList)
              }
            Response[String] (response.getStatusCode, stringResponseBody, None, responseBody, headers)
          }
        promise.complete(resp)
        null
      }

      override def onThrowable(t: Throwable) {
        super.onThrowable(t)
        promise.failure(t)
      }

    })

    promise.future
  }


  def close(): Unit = client.close()


  private def applyConfiguration(builder: AsyncHttpClientConfig.Builder): AsyncHttpClientConfig.Builder = {
    builder.setReadTimeout(config.requestTimeout)
    builder.setMaxConnections(config.maxConnections)
    builder.setRequestTimeout(config.requestTimeout)
    builder.setMaxRequestRetry(config.maxRequestRetry)
    builder.setConnectTimeout(config.connectTimeout)
    builder.setConnectionTTL(config.connectionTTL)
    builder.setWebSocketTimeout(config.webSocketTimeout)
    builder.setMaxConnectionsPerHost(config.maxConnectionsPerHost)
    builder.setAllowPoolingConnections(config.allowPoolingConnections)
    builder.setAllowPoolingSslConnections(config.allowPoolingSslConnections)
    builder.setPooledConnectionIdleTimeout(config.pooledConnectionIdleTimeout)
    builder.setAcceptAnyCertificate(config.acceptAnyCertificate)
    builder.setFollowRedirect(config.followRedirect)
    builder.setMaxRedirects(config.maxRedirects)
    builder.setRemoveQueryParamsOnRedirect(config.removeQueryParamOnRedirect)
    builder.setStrict302Handling(config.strict302Handling)
  }


  private def toJavaFunction[A, B](f: A => B): JFunction[A, B] = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  private def fromJavaFuture[B](jfuture: CompletionStage[B]): Future[B] = {
    val p = Promise[B] ()

    val consumer = new BiConsumer[B, Throwable] {
      override def accept(v: B, t: Throwable): Unit =
        if (t == null) p.complete(Success(v))
        else p.complete(Failure(t))
    }

    jfuture.whenComplete(consumer)
    p.future
  }

}