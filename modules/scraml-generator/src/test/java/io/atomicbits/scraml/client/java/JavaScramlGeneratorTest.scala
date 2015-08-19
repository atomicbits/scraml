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

package io.atomicbits.scraml.client.java

import java.util
import java.util.concurrent.Future

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.dsl.java.Response
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, FeatureSpec}

/**
 * Created by peter on 19/08/15. 
 */
class JavaScramlGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {


  val port = 8181
  val host = "localhost"

  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  override def beforeAll() = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterAll() = {
    wireMockServer.stop()
  }

  feature("build a restful request using a Scala DSL") {

    scenario("test manually written Scala DSL") {

      Given("some manually written DSL code and a mock service that listens for client calls")

      stubFor(
        get(urlEqualTo(s"/rest/some/smart/webservice/pathparamvalue?queryparX=2.0&queryparY=50&queryParZ=123"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody( """{"firstName":"John", "lastName": "Doe", "age": 21}""")
              .withStatus(200)))

      stubFor(
        put(urlEqualTo(s"/rest/some/smart/webservice/pathparamvalue"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Accept", equalTo("application/json"))
          .withRequestBody(equalTo( """{"firstName":"John","lastName":"Doe","age":21}"""))
          .willReturn(
            aResponse()
              .withBody( """{"street":"Mulholland Drive", "city": "LA", "zip": "90210", "number": 105}""")
              .withStatus(200)))


      When("we execute some restful requests using the DSL")

      val client: JXoClient = new JXoClient(host, port, "http", 5000, 1, new util.HashMap[String, String]())
      val result: Future[Response[String]] =
        client.rest.some.webservice.pathparam("foo").withHeader("Cookie", "mjam").get(30.0, 5, 6).call()
      client.close()

    }

  }

}
