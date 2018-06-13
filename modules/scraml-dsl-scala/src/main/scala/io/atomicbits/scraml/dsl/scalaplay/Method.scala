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
