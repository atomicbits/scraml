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

import io.atomicbits.scraml.dsl.scalaplay.Client
import io.atomicbits.scraml.dsl.scalaplay.client.{ ClientConfig, ClientFactory }

import scala.util.{ Failure, Try }

/**
  * Created by peter on 8/01/16.
  */
object Ning2ClientFactory extends ClientFactory {

  override def createClient(protocol: String,
                            host: String,
                            port: Int,
                            prefix: Option[String],
                            config: ClientConfig,
                            defaultHeaders: Map[String, String]): Try[Client] = {
    Try {
      new Ning2Client(protocol, host, port, prefix, config, defaultHeaders)
    } recoverWith {
      case cnfe: NoClassDefFoundError =>
        Failure(
          new NoClassDefFoundError(
            s"${cnfe.getMessage}} The Scraml ning client factory cannot find the necessary ning dependencies to instantiate its client. Did you add the necessary ning dependencies to your project?"
          )
        )
      case e => Failure(e)
    }
  }

}
