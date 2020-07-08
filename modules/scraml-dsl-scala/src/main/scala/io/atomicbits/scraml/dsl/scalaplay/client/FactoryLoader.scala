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

package io.atomicbits.scraml.dsl.scalaplay.client

import scala.language.postfixOps
import scala.util.{ Failure, Try }

/**
  * Created by peter on 8/01/16.
  */
object FactoryLoader {

  val defaultClientFactoryClass = "io.atomicbits.scraml.dsl.scalaplay.client.ning.Ning2ClientFactory"

  def load(clientFactoryClass: Option[String] = None): Try[ClientFactory] = {
    val factoryClass = clientFactoryClass.getOrElse(defaultClientFactoryClass)
    Try {
      Class.forName(factoryClass).newInstance().asInstanceOf[ClientFactory]
    } recoverWith {
      case cnfe: NoClassDefFoundError =>
        Failure(
          new NoClassDefFoundError(
            s"${cnfe.getMessage} The scraml FactoryLoader cannot load factory class $factoryClass. Did you add the dependency to this factory?"
          )
        )
      case e => Failure(e)
    }
  }

}
