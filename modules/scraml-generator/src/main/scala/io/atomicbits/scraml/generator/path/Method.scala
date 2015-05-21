package io.atomicbits.scraml.generator.path

/**
 * Created by peter on 21/05/15, Atomic BITS (http://atomicbits.io). 
 */
sealed trait Method

case object Get extends Method {
  override def toString = "GET"
}

case object Post extends Method {
  override def toString = "POST"
}

case object Put extends Method {
  override def toString = "PUT"
}

case object Delete extends Method {
  override def toString = "DELETE"
}

case object Head extends Method {
  override def toString = "HEAD"
}

case object Opt extends Method {
  override def toString = "OPTIONS"
}

case object Patch extends Method {
  override def toString = "PATCH"
}

case object Trace extends Method {
  override def toString = "TRACE"
}

case object Connect extends Method {
  override def toString = "CONNECT"
}

