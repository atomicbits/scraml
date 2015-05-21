package io.atomicbits.scraml.dsl.support

import io.atomicbits.scraml.dsl.Response
import play.api.libs.json._

import scala.concurrent.Future

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
trait Client {

  def execute(request: RequestBuilder): Future[Response[String]]

  def executeToJson(request: RequestBuilder): Future[Response[JsValue]]

  def executeToJsonDto[T](request: RequestBuilder)(implicit reader: Reads[T]): Future[Response[T]]

}
