package io.atomicbits.scraml.generator.client

import io.atomicbits.scraml.generator.path.Request

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json._

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
trait Client {

  def execute(request: Request): Future[Response[String]]

  def executeToJson(request: Request): Future[Response[JsValue]]

  def executeToJsonDto[T](request: Request)(implicit reader: Reads[T]): Future[Response[T]]

}

case class Response[T](status: Int, body: T) {

  def map[S](f: T => S) = {
    Response(status, f(body))
  }

}
