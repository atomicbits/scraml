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

package io.atomicbits.scraml.dsl.scalaplay.client.ning

import io.atomicbits.scraml.dsl.scalaplay.client.ClientConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

/**
  * Created by peter on 22/04/16.
  */
class Ning19ClientTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  feature("Extracting the charset from the response headers") {

    scenario("test a valid charset in a response header") {

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
