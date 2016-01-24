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

package io.atomicbits.scraml.dsl.client

import scala.language.postfixOps
import scala.util.{Failure, Try}


/**
 * Created by peter on 8/01/16.
 */
object FactoryLoader {

  val defaultClientFactoryClass = "io.atomicbits.scraml.dsl.client.ning.Ning19ClientFactory"

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
      case e                            => Failure(e)
    }
  }

}
