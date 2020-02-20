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

package io.atomicbits.scraml.client.java

import java.util
import java.util.concurrent.{ Future, TimeUnit }

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.dsl.javajackson.client.ClientConfig
import io.atomicbits.scraml.dsl.javajackson.Response
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec

/**
  * Created by peter on 19/08/15.
  */
class JavaScramlGeneratorTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  val port = 8181
  val host = "localhost"

  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  override def beforeAll(): Unit = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterAll(): Unit = {
    wireMockServer.stop()
  }

  Feature("build a restful request using a Scala DSL") {

    Scenario("test manually written Scala DSL") {

      Given("some manually written DSL code and a mock service that listens for client calls")

      stubFor(
        get(urlEqualTo(s"/rest/some/webservice/foo?queryparX=30.0&queryparZ=6&queryparY=5"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("mjam"))
          .willReturn(aResponse()
            .withBody("""{"firstName":"John", "lastName": "Doe", "age": 21}""")
            .withStatus(200)))

      stubFor(
        get(urlEqualTo(s"/rest/some/webservice/bar?queryparX=21.5&queryparZ=66&queryparY=55"))
          .withHeader("Accept", equalTo("application/json"))
          .withHeader("Cookie", equalTo("bar"))
          .willReturn(aResponse()
            .withBody("""{"firstName":"Ziva", "lastName": "Zoef", "age": 2}""")
            .withStatus(200)))

      When("we execute some restful requests using the DSL")

      val client: JXoClient = new JXoClient(host, port, "http", null, new ClientConfig(), new util.HashMap[String, String](), null)
      val resource          = client.rest.some.webservice

      val request1 = resource.pathparam("foo").addHeader("Cookie", "mjam")
      val request2 = resource.pathparam("bar").addHeader("Cookie", "bar")

      val result1: Future[Response[Person]] = request1.get(30.0, 5, 6).call()
      val response1                         = result1.get(10, TimeUnit.SECONDS)

      val persoon1 = new Person()
      persoon1.setFirstName("John")
      persoon1.setLastName("Doe")
      persoon1.setAge(21L)
      assertResult(persoon1)(response1.getBody)

      val result2: Future[Response[Person]] = request2.get(21.5, 55, 66).call()
      val response2                         = result2.get(10, TimeUnit.SECONDS)

      val persoon2 = new Person()
      persoon2.setFirstName("Ziva")
      persoon2.setLastName("Zoef")
      persoon2.setAge(2L)
      assertResult(persoon2)(response2.getBody)

      client._close()
    }

  }

}
