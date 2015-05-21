package io.atomicbits.scraml.generator.client
package rxhttpclient

import io.atomicbits.scraml.generator.client.Client
import io.atomicbits.scraml.generator.path.Request
import be.wegenenverkeer.rxhttp.scala.ImplicitConversions._
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
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

  def execute(request: Request): Future[Response[String]] = {

    val clientWithPathAndMethod = {
      client
        .requestBuilder()
        .setUrlRelativetoBase(request.relativePath)
        .setMethod(request.method.toString)
    }

    request.headers.foreach { element =>
      val (key, value) = element
      clientWithPathAndMethod.addHeader(key, value)
    }

    request.queryParameters.foreach { element =>
      val (key, value) = element
      clientWithPathAndMethod.addQueryParam(key, value)
    }

    // ToDo: support for form parameters, different body types (Array[Byte]), streaming,

    request.body.foreach { body =>
      clientWithPathAndMethod.setBody(body)
    }

    val clientRequest = clientWithPathAndMethod.build()

    client.execute[Response[String]](
      clientRequest,
      serverResponse => Response(serverResponse.getStatusCode, serverResponse.getResponseBody)
    )
  }

  override def executeToJson(request: Request): Future[Response[JsValue]] =
    execute(request).map(res => res.map(Json.parse))

  override def executeToJsonDto[T](request: Request)
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
