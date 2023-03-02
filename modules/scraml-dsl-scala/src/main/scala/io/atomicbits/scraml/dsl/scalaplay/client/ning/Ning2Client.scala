/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.scalaplay.client.ning

import java.nio.charset.Charset
import java.util.{List => JList, Map => JMap}
import java.util.concurrent.CompletionStage
import java.util.function.{BiConsumer, Function => JFunction}

import org.asynchttpclient.AsyncCompletionHandlerBase
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import org.asynchttpclient.Request
import org.asynchttpclient.request.body.generator.InputStreamBodyGenerator
import org.asynchttpclient.Dsl._

import scala.concurrent.ExecutionContext.Implicits.global
import io.atomicbits.scraml.dsl.scalaplay.client.ClientConfig
import io.atomicbits.scraml.dsl.scalaplay._
import io.netty.handler.codec.http.HttpHeaders
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json._

import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}
import scala.collection.JavaConverters._

/**
  * Created by peter on 28/10/15.
  */
case class Ning2Client(protocol: String,
                       host: String,
                       port: Int,
                       prefix: Option[String],
                       config: ClientConfig,
                       defaultHeaders: Map[String, String])
    extends Client {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[Ning2Client])

  private val cleanPrefix = prefix.map { pref =>
    val strippedPref = pref.stripPrefix("/").stripSuffix("/")
    s"/$strippedPref"
  } getOrElse ""

  private lazy val client = {
    val configBuilder: DefaultAsyncHttpClientConfig.Builder = new DefaultAsyncHttpClientConfig.Builder
    asyncHttpClient(applyConfiguration(configBuilder).build)
  }

  def callToJsonResponse(requestBuilder: RequestBuilder, body: Option[String]): Future[Response[JsValue]] = {
    callToStringResponse(requestBuilder, body).map { response =>
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

  def callToTypeResponse[R](requestBuilder: RequestBuilder, body: Option[String])(
      implicit responseFormat: Format[R]): Future[Response[R]] = {
    // In case of a non-200 or non-204 response, we set the typed body to None and keep the future successful and return the
    // Response object. When the JSON body on a 200-response cannot be parsed into the expected type, we DO fail the future because
    // in that case we violate the RAML specs.
    callToJsonResponse(requestBuilder, body) map { response =>
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
      case response @ Response(_, _, _, Some(JsSuccess(t, path)), _) => Future.successful(response.copy(body = Some(t)))
      case response @ Response(_, _, _, Some(JsError(Nil)), _)       => Future.successful(response.copy(body = None))
      case response @ Response(_, _, _, None, _)                     => Future.successful(response.copy(body = None))
      case Response(_, _, _, Some(JsError(e)), _) =>
        val validationMessages = {
          e flatMap { errorsByPath =>
            val (path, errors) = errorsByPath
            errors map (error => s"$path -> ${error.message}")
          }
        }
        Future.failed(
          new IllegalArgumentException(
            s"JSON validation error in the response from ${requestBuilder.summary}: ${validationMessages mkString ", "}"))
      case _ => sys.error("Unexpected type") // shouldn't occur, make scalac happy
    }
  }

  def callToStringResponse(requestBuilder: RequestBuilder, body: Option[String]): Future[Response[String]] = {

    val transformer: org.asynchttpclient.Response => Response[String] = { response =>
      val headers: Map[String, List[String]] = headersToMap(response.getHeaders)

      val responseCharset: String = getResponseCharsetFromHeaders(headers).getOrElse(config.responseCharset.displayName)

      val stringResponseBody: Option[String] = Option(response.getResponseBody(Charset.forName(responseCharset)))

      Response[String](response.getStatusCode, stringResponseBody, None, stringResponseBody, headers)
    }

    callToResponse(requestBuilder, body, transformer)
  }

  def callToBinaryResponse(requestBuilder: RequestBuilder, body: Option[String]): Future[Response[BinaryData]] = {

    val transformer: org.asynchttpclient.Response => Response[BinaryData] = { response =>
      val binaryData: BinaryData = new Ning2BinaryData(response)

      val headers: Map[String, List[String]] = headersToMap(response.getHeaders)

      Response[BinaryData](response.getStatusCode, None, None, Some(binaryData), headers)
    }

    callToResponse(requestBuilder, body, transformer)
  }

  private def callToResponse[T](requestBuilder: RequestBuilder,
                                body: Option[String],
                                transformer: org.asynchttpclient.Response => Response[T]): Future[Response[T]] = {
    val ningBuilder = {
      // Create builder
      val ningRb: org.asynchttpclient.RequestBuilder = new org.asynchttpclient.RequestBuilder
      val baseUrl: String                             = protocol + "://" + host + ":" + port + cleanPrefix
      ningRb.setUrl(baseUrl + "/" + requestBuilder.relativePath.stripPrefix("/"))
      ningRb.setMethod(requestBuilder.method.toString)
      ningRb
    }

    requestBuilder.allHeaders.foreach {
      case (key, values) =>
        values.foreach { value =>
          ningBuilder.addHeader(key, value)
        }
    }

    requestBuilder.queryParameters.foreach {
      case (key, value) =>
        value match {
          case SimpleHttpParam(parameter)  => ningBuilder.addQueryParam(key, parameter)
          case ComplexHttpParam(parameter) => ningBuilder.addQueryParam(key, parameter)
          case RepeatedHttpParam(parameters) =>
            parameters.foreach(parameter => ningBuilder.addQueryParam(key, parameter))
        }
    }

    body.foreach { body =>
      ningBuilder.setBody(body)
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
          case SimpleHttpParam(parameter)  => ningBuilder.addFormParam(key, parameter)
          case ComplexHttpParam(parameter) => ningBuilder.addFormParam(key, parameter)
          case RepeatedHttpParam(parameters) =>
            parameters.foreach(parameter => ningBuilder.addFormParam(key, parameter))
        }
    }

    requestBuilder.multipartParams.foreach {
      case part: ByteArrayPart =>
        ningBuilder.addBodyPart(
          new org.asynchttpclient.request.body.multipart.ByteArrayPart(
            part.name,
            part.bytes,
            part.contentType.orNull,
            part.charset.orNull,
            part.fileName.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
      case part: FilePart =>
        ningBuilder.addBodyPart(
          new org.asynchttpclient.request.body.multipart.FilePart(
            part.name,
            part.file,
            part.contentType.orNull,
            part.charset.orNull,
            part.fileName.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
      case part: StringPart =>
        ningBuilder.addBodyPart(
          new org.asynchttpclient.request.body.multipart.StringPart(
            part.name,
            part.value,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
      case part: InputStreamPart =>
        ningBuilder.addBodyPart(
          new org.asynchttpclient.request.body.multipart.InputStreamPart(
            part.name,
            part.inputStream,
            part.fileName,
            part.contentLength,
            part.contentType.orNull,
            part.charset.orNull,
            part.contentId.orNull,
            part.transferEncoding.orNull
          )
        )
    }

    val ningRequest: Request = ningBuilder.build()
    LOGGER.debug(s"Executing request: $ningRequest")
    LOGGER.trace(s"Request body: $body")

    val promise = Promise[Response[T]]()

    client.executeRequest(
      ningRequest,
      new AsyncCompletionHandlerBase() {
        @throws(classOf[Exception])
        override def onCompleted(response: org.asynchttpclient.Response): org.asynchttpclient.Response = {
          val resp: Try[Response[T]] = Try(transformer(response))
          promise.complete(resp)
          null
        }

        override def onThrowable(t: Throwable) = {
          super.onThrowable(t)
          promise.failure(t)
          // explicitely return Unit to avoid compilation errors on systems with strict compilation rules switched on,
          // such as "-Ywarn-value-discard"
          ()
        }

      }
    )

    promise.future
  }

  def close(): Unit = client.close()

  private def applyConfiguration(builder: DefaultAsyncHttpClientConfig.Builder): DefaultAsyncHttpClientConfig.Builder = {
    builder.setReadTimeout(config.readTimeout)
    builder.setMaxConnections(config.maxConnections)
    builder.setRequestTimeout(config.requestTimeout)
    builder.setMaxRequestRetry(config.maxRequestRetry)
    builder.setConnectTimeout(config.connectTimeout)
    builder.setConnectionTtl(config.connectionTTL)
    builder.setMaxConnectionsPerHost(config.maxConnectionsPerHost)
    builder.setPooledConnectionIdleTimeout(config.pooledConnectionIdleTimeout)
    builder.setUseInsecureTrustManager(config.useInsecureTrustManager)
    builder.setFollowRedirect(config.followRedirect)
    builder.setMaxRedirects(config.maxRedirects)
    builder.setStrict302Handling(config.strict302Handling)
  }

  private[ning] def getResponseCharsetFromHeaders(headers: Map[String, List[String]]): Option[String] = {

    val contentTypeValuesOpt =
      headers.map { keyValues =>
        val (key, values) = keyValues
        key.toLowerCase() -> values
      } get "content-type"

    for {
      contentTypeValues <- contentTypeValuesOpt
      contentTypeValueWithCharset <- contentTypeValues.find(_.toLowerCase().contains("charset"))
      charsetPart <- contentTypeValueWithCharset.toLowerCase().split(";").toList.find(_.contains("charset"))
      splitOnCharset = charsetPart.split("charset").toList
      charsetString <- {
        splitOnCharset match {
          case _ :: value :: other =>
            val cleanValue = value.trim.stripPrefix("=").trim
            Try(Charset.forName(cleanValue)).toOption.map(_.name())
          case _ => None
        }
      }
    } yield charsetString
  }

  private def headersToMap(httpHeaders: HttpHeaders) = {
    httpHeaders.names.asScala.foldLeft(Map.empty[String,List[String]]) { (map, name) =>
      map + (name -> httpHeaders.getAll(name).asScala.toList)
    }
  }

  private def toJavaFunction[A, B](f: A => B) = new JFunction[A, B] {
    override def apply(a: A): B = f(a)
  }

  private def fromJavaFuture[B](jfuture: CompletionStage[B]): Future[B] = {
    val p = Promise[B]()

    val consumer = new BiConsumer[B, Throwable] {
      override def accept(v: B, t: Throwable): Unit =
        if (t == null) {
          p.complete(Success(v))
          // explicitely return Unit to avoid compilation errors on systems with strict compilation rules switched on,
          // such as "-Ywarn-value-discard"
          ()
        } else {
          p.complete(Failure(t))
          // explicitely return Unit to avoid compilation errors on systems with strict compilation rules switched on,
          // such as "-Ywarn-value-discard"
          ()
        }
    }

    jfuture.whenComplete(consumer)
    p.future
  }

}
