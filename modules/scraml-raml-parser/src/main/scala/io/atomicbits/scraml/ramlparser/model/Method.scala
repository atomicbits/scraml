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
