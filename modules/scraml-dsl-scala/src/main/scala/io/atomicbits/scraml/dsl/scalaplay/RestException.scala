package io.atomicbits.scraml.dsl.scalaplay

/**
  * An exception that wraps the status code and error message of a response.
  */
case class RestException(message:String, status:Int) extends RuntimeException(message) {}

object RestException {

  /**
    * Find the last thrown RestException in the cause stack of Exceptions.
    */
  def findRestException(exception: Throwable): Option[RestException] = {
    exception match {
      case e: RestException => Some(e)
      case e =>
        val cause = getCause(e)

        cause match {
          case Some(x: RestException) => Some(x)
          case Some(x: Throwable)     => findRestException(x)
          case None                   => None
        }
    }
  }

  private def getCause(exception: Throwable): Option[Throwable] = {
    if (exception.getCause != null && exception.getCause != exception) {
      Some(exception.getCause)
    } else {
      None
    }
  }

}
