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

package io.atomicbits.scraml.dsl.scalaplay

/**
  * An exception that wraps the status code and error message of a response.
  */
case class RestException(message: String, status: Int) extends RuntimeException(message) {}

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
