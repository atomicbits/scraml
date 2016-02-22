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
import java.util.concurrent.{Future, TimeUnit}

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.jdsl.Response
import io.atomicbits.scraml.jdsl.client.ClientConfig
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

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
        get(urlEqualTo(s"/rest/some/webservice/foo?queryparX=30.0&queryparZ=6&queryparY=5"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("mjam"))
          .willReturn(
            aResponse()
              .withBody( """{"firstName":"John", "lastName": "Doe", "age": 21}""")
              .withStatus(200)))

      stubFor(
        get(urlEqualTo(s"/rest/some/webservice/bar?queryparX=21.5&queryparZ=66&queryparY=55"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("bar"))
          .willReturn(
            aResponse()
              .withBody( """{"firstName":"Ziva", "lastName": "Zoef", "age": 2}""")
              .withStatus(200)))

      When("we execute some restful requests using the DSL")

      val client: JXoClient = new JXoClient(host, port, "http", null, new ClientConfig(), new util.HashMap[String, String](), null)
      val resource = client.rest.some.webservice

      val request1 = resource.pathparam("foo").addHeader("Cookie", "mjam")
      val request2 = resource.pathparam("bar").addHeader("Cookie", "bar")

      val result1: Future[Response[Person]] = request1.get(30.0, 5, 6).call()
      val response1 = result1.get(10, TimeUnit.SECONDS)

      val persoon1 = new Person()
      persoon1.setFirstName("John")
      persoon1.setLastName("Doe")
      persoon1.setAge(21L)
      assertResult(persoon1)(response1.getBody)

      val result2: Future[Response[Person]] = request2.get(21.5, 55, 66).call()
      val response2 = result2.get(10, TimeUnit.SECONDS)

      val persoon2 = new Person()
      persoon2.setFirstName("Ziva")
      persoon2.setLastName("Zoef")
      persoon2.setAge(2L)
      assertResult(persoon2)(response2.getBody)

      client._close()
    }

  }

}
