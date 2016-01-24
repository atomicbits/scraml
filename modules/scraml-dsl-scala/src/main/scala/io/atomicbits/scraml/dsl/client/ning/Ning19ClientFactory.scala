/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.client.ning

import io.atomicbits.scraml.dsl.Client
import io.atomicbits.scraml.dsl.client.{ClientConfig, ClientFactory}

import scala.util.{Failure, Try}

/**
 * Created by peter on 8/01/16.
 */
object Ning19ClientFactory extends ClientFactory {

  override def createClient(protocol: String,
                            host: String,
                            port: Int,
                            prefix: Option[String],
                            config: ClientConfig,
                            defaultHeaders: Map[String, String]): Try[Client] = {
    Try{
      new Ning19Client(protocol, host, port, prefix, config, defaultHeaders)
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
