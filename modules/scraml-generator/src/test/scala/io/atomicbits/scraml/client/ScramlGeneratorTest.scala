package io.atomicbits.scraml.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._

import io.atomicbits.scraml.dsl.Response
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

  import XoClient._  // ToDo generate import.

  val requestBuilder = RequestBuilder(new RxHttpClient(protocol, host, port, requestTimeout, maxConnections))


  def rest = new PlainSegment("rest", requestBuilder) {
    def some = new PlainSegment("some", requestBuilder) {
      def smart = new PlainSegment("smart", requestBuilder) {
        def webservice = new PlainSegment("webservice", requestBuilder) {
          def pathparam(value: String) = new ParamSegment[String](value, requestBuilder) {
            def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new GetSegment(
              queryParams = Map(
                "queryparX" -> Option(queryparX).map(_.toString),
                "queryparY" -> Option(queryparY).map(_.toString),
                "queryParZ" -> queryParZ.map(_.toString)
              ),
              validAcceptHeaders = List("application/json"),
              req = requestBuilder
            ) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                def formatJson = new FormatJsonSegment(requestBuilder) {
                  def execute() = new ExecuteSegment(requestBuilder).execute()
                }

                def execute() = new ExecuteSegment(requestBuilder).execute()
              }

            }

            def put(body: String) = new PutSegment(
              body = body,
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = requestBuilder) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {
                def execute() = new ExecuteSegment(requestBuilder).execute()
              }

            }

            def post(formparX: Int, formParY: Double, formParZ: Option[String]) = new PostSegment(
              formParams = Map(
                "formparX" -> Option(formparX).map(_.toString),
                "formParY" -> Option(formParY).map(_.toString),
                "formParZ" -> formParZ.map(_.toString)
              ),
              body = None,
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = requestBuilder
            ) {

              def headers(headers: (String, String)*) = new HeaderSegment(
                headers = headers.toMap,
                req = requestBuilder
              ) {

                def execute() = new ExecuteSegment(requestBuilder).execute()

              }

            }

          }
        }
      }
    }
  }

}

object XoClient {


}


class ScRamlGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

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
              .withBody( """{"test": "OK"}""")
              .withStatus(200)))

      stubFor(
        put(urlEqualTo(s"/rest/some/smart/webservice/pathparamvalue"))
          .withHeader("Content-Type", equalTo("application/json"))
          .withHeader("Accept", equalTo("application/json"))
          .withRequestBody(equalTo("body"))
          .willReturn(
            aResponse()
              .withBody( """{"test": "OK"}""")
              .withStatus(200)))


      When("we execute some restful requests using the DSL")

      val futureResultGet: Future[Response[String]] =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .get(queryparX = 2.0, queryparY = 50, queryParZ = Option(123))
          .headers("Accept" -> "application/json")
          .execute()

      val futureResultPut: Future[Response[String]] =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .put("body")
          .headers(
            "Content-Type" -> "application/json",
            "Accept" -> "application/json"
          )
          .execute()


      Then("we should see the expected response values")

      val resultGet = Await.result(futureResultGet, 2 seconds)
      assertResult(Response(200, """{"test": "OK"}"""))(resultGet)

      val resultPut = Await.result(futureResultPut, 2 seconds)
      assertResult(Response(200, """{"test": "OK"}"""))(resultPut)

    }
  }

}
