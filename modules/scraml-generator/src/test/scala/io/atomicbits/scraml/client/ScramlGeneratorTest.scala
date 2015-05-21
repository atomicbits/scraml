package io.atomicbits.scraml.client

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.generator.client.rxhttpclient.RxHttpClient
import io.atomicbits.scraml.generator.path._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import scala.language.reflectiveCalls

/**
 * The client in this test is manually written to understand what kind of code we need to generate to support the DSL.
 */
case class XoClient(host: String,
                    port: Int = 80,
                    protocol: String = "http",
                    requestTimeout: Int = 5000,
                    maxConnections: Int = 5) {

  val request = Request(new RxHttpClient(protocol, host, port, requestTimeout, maxConnections))

  def rest = new PlainPathElement("rest", request) {
    def some = new PlainPathElement("some", request) {
      def smart = new PlainPathElement("smart", request) {
        def webservice = new PlainPathElement("webservice", request) {
          def pathparam(value: String) = new StringPathElement(value, request) {
            def get(queryparX: Double, queryparY: Int, queryParZ: Option[Int] = None) = new GetPathElement(
              queryParams = Map(
                "queryparX" -> Option(queryparX).map(_.toString),
                "queryparY" -> Option(queryparY).map(_.toString),
                "queryParZ" -> queryParZ.map(_.toString)
              ),
              validAcceptHeaders = List("application/json"),
              req = request
            ) {

              def headers(headers: (String, String)*) = new HeaderPathElement(
                headers = headers.toMap,
                req = request
              ) {

                def formatJson = new FormatJsonPathElement(request) {
                  def execute() = new ExecutePathElement(request).execute()
                }

                def execute() = new ExecutePathElement(request).execute()
              }

            }

            def put(body: String) = new PutPathElement(
              body = body,
              validAcceptHeaders = List("application/json"),
              validContentTypeHeaders = List("application/json"),
              req = request) {

              def headers(headers: (String, String)*) = new HeaderPathElement(
                headers = headers.toMap,
                req = request
              ) {
                def execute() = new ExecutePathElement(request).execute()
              }

            }
          }
        }
      }
    }
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
        get(urlEqualTo(s"/rest/some/smart/webservice/pathparamvalue"))
          .withQueryParam("queryparX", equalTo("2.0"))
          .withQueryParam("queryparY", equalTo("50"))
          .withQueryParam("queryParZ", equalTo("123"))
          .withHeader("Accept", equalTo("application/json"))
          .willReturn(
            aResponse()
              .withBody( """{"test": "OK"}""")
              .withStatus(200)))


      When("we create some restful requests using the DSL")

      val futureResultGet =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .get(queryparX = 2.0, queryparY = 50, queryParZ = Option(123))
          .headers("Accept" -> "application/json")
          .execute()

      val futureResultPut =
        XoClient(protocol = "http", host = host, port = port)
          .rest.some.smart.webservice.pathparam("pathparamvalue")
          .put("body")
          .headers(
            "Content-Type" -> "application/json",
            "Accept" -> "application/json"
          )
          .execute()


      Then("we should see the generated Request model printed out by the execute method")

    }
  }

}
