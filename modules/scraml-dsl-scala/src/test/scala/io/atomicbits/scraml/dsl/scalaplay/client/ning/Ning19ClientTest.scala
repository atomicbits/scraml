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

import io.atomicbits.scraml.dsl.scalaplay.client.ClientConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec

/**
  * Created by peter on 22/04/16.
  */
class Ning19ClientTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  Feature("Extracting the charset from the response headers") {

    Scenario("test a valid charset in a response header") {

      Given("A ning client")
      val client = Ning19Client(
        protocol       = "http",
        host           = "localhost",
        port           = 8080,
        prefix         = None,
        config         = ClientConfig(),
        defaultHeaders = Map.empty
      )

      val headers = Map(
        "Accept" -> List("application/json", "application/bson"),
        "Content-Type" -> List("application/json;charset=ascii")
      )

      When("the charset value is requested")
      val charsetValue: Option[String] = client.getResponseCharsetFromHeaders(headers)

      Then("the charset from the content-type header is returned")
      assert(charsetValue == Some("US-ASCII"))

    }
  }

}
