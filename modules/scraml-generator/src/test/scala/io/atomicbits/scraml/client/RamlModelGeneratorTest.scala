package io.atomicbits.scraml.client

import io.atomicbits.scraml._
import org.scalatest.{FeatureSpec, GivenWhenThen}
import scala.language.reflectiveCalls

/**
 * This code is manually written to understand what kind of code we need to generate to support the DSL.
 */
case class XoClient(host: String, port: Int = 80, protocol: String = "http") {

  val request = Request(protocol, host, port)

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

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
class RamlModelGeneratorTest extends FeatureSpec with GivenWhenThen {

  feature("generate a foo case class") {

    scenario("test scala macros with quasiquotes") {

      Given("the FromMacroCode macro annotation")


      When("we create an instance of Foo")

      XoClient(protocol = "http", host = "host", port = 8080)
        .rest.some.smart.webservice.pathparam("path-param-value")
        .get(queryparX = 2.0, queryparY = 50, queryParZ = Option(123))
        .headers("Accept" -> "application/json")
        .formatJson
        .execute()

      XoClient(protocol = "http", host = "host", port = 8080)
        .rest.some.smart.webservice.pathparam("path-param-value")
        .put("body")
        .headers(
          "Content-Type" -> "application/json",
          "Accept" -> "application/json"
        )
        .execute()

      Then("we should see the generated Request model printed out")

    }
  }

}
