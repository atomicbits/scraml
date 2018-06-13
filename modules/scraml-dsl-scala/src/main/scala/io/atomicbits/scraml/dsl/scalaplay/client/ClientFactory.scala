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

import io.atomicbits.scraml.dsl.scalaplay.Client

import scala.util.Try

/**
  * Created by peter on 8/01/16.
  */
trait ClientFactory {

  def createClient(protocol: String,
                   host: String,
                   port: Int,
                   prefix: Option[String],
                   config: ClientConfig,
                   defaultHeaders: Map[String, String]): Try[Client]

}
