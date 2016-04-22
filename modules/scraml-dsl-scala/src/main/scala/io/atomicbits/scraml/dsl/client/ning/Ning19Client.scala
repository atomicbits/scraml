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

import com.ning.http.client.generators.InputStreamBodyGenerator

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
case class Ning19Client(protocol: String,
                        host: String,
                        port: Int,
                        prefix: Option[String],
                        config: ClientConfig,
                        defaultHeaders: Map[String, String]) extends Client {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Ning19Client])

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
      if (response.status >= 200 && response.status < 300) {
        // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
        // there are many responses in the 200 range with different typed responses.
        // if (response.status == 200) {
        val respJson = response.flatMap { responseString =>
          if (responseString != null && responseString.nonEmpty) Some(Json.parse(responseString))
          else None
        }
        respJson.copy(jsonBody = respJson.body)
      } else {
        response.copy(jsonBody = None, body = None)
      }
    }
  }


  def callToTypeResponse[B, R](requestBuilder: RequestBuilder, body: Option[B])
                              (implicit bodyFormat: Format[B], responseFormat: Format[R]): Future[Response[R]] = {
    // In case of a non-200 or non-204 response, we set the typed body to None and keep the future successful and return the
    // Response object. When the JSON body on a 200-response cannot be parsed into the expected type, we DO fail the future because
    // in that case we violate the RAML specs.
    callToJsonResponse[B](requestBuilder, body) map { response =>
      if (response.status >= 200 && response.status < 300) {
        // Where we assume that any response in the 200 range will map to the unique typed response. This doesn't hold true if
        // there are many responses in the 200 range with different typed responses.
        response.map(responseFormat.reads)
      } else {
        // We hijack the 'JsError(Nil)' type here to mark the non-200 case that has to result in a successful future with empty body.
        // Mind that the empty body only means that the requested type is None, the stringBody and jsonBody fields are present as well.
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

    val transformer: com.ning.http.client.Response => Response[String] = { response =>

      val stringResponseBody: Option[String] = Option(response.getResponseBody(config.responseCharset.displayName))

      val headers: Map[String, List[String]] =
        mapAsScalaMap(response.getHeaders).foldLeft(Map.empty[String, List[String]]) { (map, headerPair) =>
          val (key, value) = headerPair
          map + (key -> value.asScala.toList)
        }

      Response[String](response.getStatusCode, stringResponseBody, None, stringResponseBody, headers)
    }

    callToResponse(requestBuilder, body, transformer)
  }


  def callToBinaryResponse[B](requestBuilder: RequestBuilder, body: Option[B])
                             (implicit bodyFormat: Format[B]): Future[Response[BinaryData]] = {

    val transformer: com.ning.http.client.Response => Response[BinaryData] = { response =>

      val binaryData: BinaryData = new Ning19BinaryData(response)

      val headers: Map[String, List[String]] =
        mapAsScalaMap(response.getHeaders).foldLeft(Map.empty[String, List[String]]) { (map, headerPair) =>
          val (key, value) = headerPair
          map + (key -> value.asScala.toList)
        }

      Response[BinaryData](response.getStatusCode, None, None, Some(binaryData), headers)
    }

    callToResponse(requestBuilder, body, transformer)
  }


  private def callToResponse[B, T](requestBuilder: RequestBuilder,
                                   body: Option[B],
                                   transformer: com.ning.http.client.Response => Response[T])
                                  (implicit bodyFormat: Format[B]): Future[Response[T]] = {
    val ningBuilder = {
      // Create builder
      val ningRb: com.ning.http.client.RequestBuilder = new com.ning.http.client.RequestBuilder
      val baseUrl: String = protocol + "://" + host + ":" + port + cleanPrefix
      ningRb.setUrl(baseUrl + "/" + requestBuilder.relativePath.stripPrefix("/"))
      ningRb.setMethod(requestBuilder.method.toString)
      ningRb
    }

    (HeaderMap() ++ (defaultHeaders.toSeq: _*) ++ requestBuilder.headers).foreach {
      case (key, values) =>
        values.foreach { value =>
          ningBuilder.addHeader(key, value)
        }
    }

    requestBuilder.queryParameters.foreach {
      case (key, value) =>
        value match {
          case SingleHttpParam(parameter)    => ningBuilder.addQueryParam(key, parameter)
          case RepeatedHttpParam(parameters) =>
            parameters.foreach(parameter => ningBuilder.addQueryParam(key, parameter))
        }
    }

    val bodyToSend = body.map(bodyFormat.writes(_).toString())
    bodyToSend.foreach {
      body => ningBuilder.setBody(body)
    }

    requestBuilder.binaryBody.foreach {
      case FileBinaryRequest(file)               => ningBuilder.setBody(file)
      case InputStreamBinaryRequest(inputStream) => ningBuilder.setBody(new InputStreamBodyGenerator(inputStream))
      case ByteArrayBinaryRequest(byteArray)     => ningBuilder.setBody(byteArray)
      case StringBinaryRequest(text)             => ningBuilder.setBody(text)
    }

    requestBuilder.formParameters.foreach {
      case (key, value) =>
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
    LOGGER.debug(s"Executing request: $ningRequest")
    LOGGER.trace(s"Request body encoding: ${ningRequest.getBodyEncoding}")
    LOGGER.trace(s"Request body: $bodyToSend")

    val promise = Promise[Response[T]]()

    client.executeRequest(ningRequest, new AsyncCompletionHandler[String]() {
      @throws(classOf[Exception])
      def onCompleted(response: com.ning.http.client.Response): String = {
        val resp: Try[Response[T]] = Try(transformer(response))
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
    builder.setReadTimeout(config.readTimeout)
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
    builder.setStrict302Handling(config.strict302Handling)
  }


  private def toJavaFunction[A, B](f: A => B): JFunction[A, B] = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  private def fromJavaFuture[B](jfuture: CompletionStage[B]): Future[B] = {
    val p = Promise[B]()

    val consumer = new BiConsumer[B, Throwable] {
      override def accept(v: B, t: Throwable): Unit =
        if (t == null) p.complete(Success(v))
        else p.complete(Failure(t))
    }

    jfuture.whenComplete(consumer)
    p.future
  }

}
