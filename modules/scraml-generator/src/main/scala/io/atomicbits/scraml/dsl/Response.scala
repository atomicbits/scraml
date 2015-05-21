package io.atomicbits.scraml.dsl

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
case class Response[T](status: Int, body: T) {

  def map[S](f: T => S) = {
    Response(status, f(body))
  }

}
