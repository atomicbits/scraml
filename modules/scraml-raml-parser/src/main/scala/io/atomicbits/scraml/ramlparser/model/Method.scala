/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model

/**
  * Created by peter on 10/02/16.
  */
sealed trait Method {

  def action: String
}

object Method {

  def unapply(action: String): Option[Method] = {
    action match {
      case Get.action     => Some(Get)
      case Post.action    => Some(Post)
      case Put.action     => Some(Put)
      case Delete.action  => Some(Delete)
      case Head.action    => Some(Head)
      case Patch.action   => Some(Patch)
      case Options.action => Some(Options)
      case Trace.action   => Some(Trace)
      case _              => None
    }
  }

}

case object Get extends Method {

  val action = "get"
}

case object Post extends Method {

  val action = "post"
}

case object Put extends Method {

  val action = "put"
}

case object Delete extends Method {

  val action = "delete"
}

case object Head extends Method {

  val action = "head"
}

case object Patch extends Method {

  val action = "patch"
}

case object Options extends Method {

  val action = "options"
}

case object Trace extends Method {

  val action = "trace"
}
