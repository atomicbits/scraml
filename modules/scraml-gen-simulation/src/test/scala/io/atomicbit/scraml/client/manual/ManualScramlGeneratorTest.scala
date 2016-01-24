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

package io.atomicbits.scraml.client.manual

import java.net.URL

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.dsl.client.ning.Ning19ClientFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import io.atomicbits.scraml.dsl.client.{ClientFactory, ClientConfig}
import io.atomicbits.scraml.dsl.RequestBuilder

import scala.concurrent._
import scala.concurrent.duration._
import scala.language.{postfixOps, reflectiveCalls}

/**
 * The client in this test is manually written to understand what kind of code we need to generate to support the DSL.
 */
class XoClient(private val _requestBuilder: RequestBuilder) {

  def rest = new RestResource(_requestBuilder.withAddedPathSegment("rest"))

  def _close() = _requestBuilder.client.close()

}


object XoClient {

  import io.atomicbits.scraml.dsl.Response
  import play.api.libs.json._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.Future

  def apply(url: URL,
            config: ClientConfig = ClientConfig(),
            defaultHeaders: Map[String, String] = Map(),
            clientFactory: Option[ClientFactory] = None): XoClient = {

    val requestBuilder =
      RequestBuilder(
        clientFactory.getOrElse(Ning19ClientFactory)
          .createClient(
            protocol = url.getProtocol,
            host = url.getHost,
            port = if (url.getPort == -1) url.getDefaultPort else url.getPort,
            prefix = if (url.getPath.isEmpty) None else Some(url.getPath),
            config = config,
            defaultHeaders = defaultHeaders
          ).get
      )

    new XoClient(requestBuilder)
  }

  def apply(host: String,
            port: Int,
            protocol: String,
            prefix: Option[String],
            config: ClientConfig,
            defaultHeaders: Map[String, String],
            clientFactory: Option[ClientFactory]) = {
    val requestBuilder =
      RequestBuilder(
        clientFactory.getOrElse(Ning19ClientFactory)
          .createClient(
            protocol = protocol,
            host = host,
            port = port,
            prefix = prefix,
            config = config,
            defaultHeaders = defaultHeaders
          ).get
      )

    new XoClient(requestBuilder)
  }

  implicit class FutureResponseOps[T](val futureResponse: Future[Response[T]]) extends AnyVal {

    def asString: Future[String] = futureResponse.map { resp =>
      resp.stringBody getOrElse {
        val message =
          if (resp.status != 200) s"The response has no string body because the request was not successful (status = ${resp.status})."
          else "The response has no string body despite status 200."
        throw new IllegalArgumentException(message)
      }
    }

    def asJson: Future[JsValue] =
      futureResponse.map { resp =>
        resp.jsonBody.getOrElse {
          val message =
            if (resp.status != 200) s"The response has no JSON body because the request was not successful (status = ${resp.status})."
            else "The response has no JSON body despite status 200."
          throw new IllegalArgumentException(message)
        }
      }

    def asType: Future[T] =
      futureResponse.map { resp =>
        resp.body.getOrElse {
          val message =
            if (resp.status != 200) s"The response has no typed body because the request was not successful (status = ${resp.status})."
            else "The response has no typed body despite status 200."
          throw new IllegalArgumentException(message)
        }
      }

  }

}


class ManualScramlGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  import XoClient._

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
        get(urlEqualTo(s"/rest/some/webservice/pathparamvalue?queryparX=2.0&queryparY=50&queryParZ=123"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody( """{"firstName":"John", "lastName": "Doe", "age": 21}""")
              .withStatus(200)))

      stubFor(
        put(urlEqualTo(s"/rest/some/webservice/pathparamvalue"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Accept", equalTo("application/json"))
          .withRequestBody(equalTo( """{"firstName":"John","lastName":"Doe","age":21}"""))
          .willReturn(
            aResponse()
              .withBody( """{"street":"Mulholland Drive", "city": "LA", "zip": "90210", "number": 105}""")
              .withStatus(200)))


      When("we execute some restful requests using the DSL")

      val futureResultGet: Future[User] =
        XoClient(protocol = "http", host = host, port = port, prefix = None, config = ClientConfig(), defaultHeaders = Map
          .empty, clientFactory = None)
          .rest.some.webservice.pathparam("pathparamvalue")
          .withHeaders("Accept" -> "application/json")
          .get(queryparX = 2.0, queryparY = 50, queryParZ = Option(123))
          .call().asType

      val futureResultPut: Future[Address] =
        XoClient(protocol = "http", host = host, port = port, prefix = None, config = ClientConfig(), defaultHeaders = Map
          .empty, clientFactory = None)
          .rest.some.webservice.pathparam("pathparamvalue")
          .withHeaders(
            "Content-Type" -> "application/json",
            "Accept" -> "application/json"
          )
          .put(User("John", "Doe", 21))
          .call().asType

      Then("we should see the expected response values")

      val resultGet = Await.result(futureResultGet, 2 seconds)
      assertResult(User("John", "Doe", 21))(resultGet)

      val resultPut = Await.result(futureResultPut, 2 seconds)
      assertResult(Address("Mulholland Drive", "LA", "90210", 105))(resultPut)

    }
  }

}
