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

package io.atomicbits.scraml.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.client.XoClient._

import io.atomicbits.scraml.dsl._
import org.scalatest.concurrent.ScalaFutures

import scala.language.{postfixOps, reflectiveCalls}
import scala.concurrent._
import scala.concurrent.duration._

import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

/**
 * The client in this test is manually written to understand what kind of code we need to generate to support the DSL.
 */
case class XoClient(host: String,
                    port: Int = 80,
                    protocol: String = "http",
                    requestTimeout: Int = 5000,
                    maxConnections: Int = 5,
                    defaultHeaders: Map[String, String] = Map()) {


  import io.atomicbits.scraml.dsl.support._
  import io.atomicbits.scraml.dsl.support.client.rxhttpclient.RxHttpClient
  import play.api.libs.json._

  import XoClient._


  val requestBuilder = RequestBuilder(new RxHttpClient(protocol, host, port, None, requestTimeout, maxConnections, Map.empty))

  def close() = requestBuilder.client.close()

  def rest = new PlainSegment("rest", requestBuilder) {
    def some = new PlainSegment("some", requestBuilder) {
      def smart = new PlainSegment("smart", requestBuilder) {
        def webservice = new PlainSegment("webservice", requestBuilder) {
          def pathparam(value: String) = new ParamSegment[String](value, requestBuilder) {
            def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new GetSegment(
              queryParams = Map(
                "queryparX" -> Option(queryparX).map(HttpParam(_)),
                "queryparY" -> Option(queryparY).map(HttpParam(_)),
                "queryParZ" -> queryParZ.map(HttpParam(_))
              ),
              validAcceptHeaders = List("application/json"),
              req = requestBuilder
            ) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                private val executeSegment = new ExecuteSegment[String, User](requestBuilder, None)

                def call() = executeSegment.callToTypeResponse()

              }

            }

            def put(body: String) = new PutSegment(
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = requestBuilder) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                private val executeSegment = new ExecuteSegment[String, Address](requestBuilder, Some(body))

                def call() = executeSegment.callToTypeResponse()

              }

            }

            def put(body: User) = new PutSegment(
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = requestBuilder) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                private val executeSegment = new ExecuteSegment[User, Address](requestBuilder, Some(body))

                def call() = executeSegment.callToTypeResponse()

              }

            }

            def post(formparX: Int, formParY: Double, formParZ: Option[String]) = new PostSegment(
              formParams = Map(
                "formparX" -> Option(formparX).map(HttpParam(_)),
                "formParY" -> Option(formParY).map(HttpParam(_)),
                "formParZ" -> formParZ.map(HttpParam(_))
              ),
              multipartParams = List.empty,
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = requestBuilder
            ) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                private val executeSegment = new ExecuteSegment[String, User](requestBuilder, None)

                def call() = executeSegment.callToTypeResponse()

              }

            }

          }
        }
      }
    }
  }

}

object XoClient {

  import play.api.libs.json._
  import scala.concurrent.Future
  import io.atomicbits.scraml.dsl.Response
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit class FutureResponseOps[T](val futureResponse: Future[Response[T]]) extends AnyVal {

    def asString: Future[String] = futureResponse.map(_.stringBody)

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

  //  implicit def OptionReads[T](implicit fmt: Reads[T]): Reads[Option[T]] = new Reads[Option[T]] {
  // See: https://www.playframework.com/documentation/2.4.x/Migration24
  //    def reads(json: JsValue) = JsSuccess(json.asOpt[T])
  //  }


  case class User(firstName: String, lastName: String, age: Int)

  object User {

    implicit val jsonFormatter: Format[User] = Json.format[User]

  }

  case class Address(street: String, city: String, zip: String, number: Int)

  object Address {

    implicit val jsonFormatter: Format[Address] = Json.format[Address]

  }

}


class ScramlGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

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

      val futureResultGet: Future[User] =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .get(queryparX = 2.0, queryparY = 50, queryParZ = Option(123))
          .headers("Accept" -> "application/json")
          .call().asType


      val futureResultPut: Future[Address] =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .put(User("John", "Doe", 21))
          .headers(
            "Content-Type" -> "application/json",
            "Accept" -> "application/json"
          )
          .call().asType

      Then("we should see the expected response values")

      val resultGet = Await.result(futureResultGet, 2 seconds)
      assertResult(User("John", "Doe", 21))(resultGet)

      val resultPut = Await.result(futureResultPut, 2 seconds)
      assertResult(Address("Mulholland Drive", "LA", "90210", 105))(resultPut)

    }
  }

}
