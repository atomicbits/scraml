package io.atomicbits.scraml.client

import io.atomicbits.scramlgen._
import org.scalatest.{FeatureSpec, GivenWhenThen}
import scala.language.reflectiveCalls


case class XoClient(host: String, port: Int = 80, protocol: String = "http") {

  val request = Request(protocol, host, port)

  def rest = new PlainPathElement("rest", request) {
    def locatie = new PlainPathElement("locatie", request) {
      def weglocatie = new PlainPathElement("weglocatie", request) {
        def weg = new PlainPathElement("weg", request) {
          def ident8(value: String) = new StringPathElement(value, request) {
            def get(opschrift: Double, afstand: Int, crs: Option[Int] = None) = new GetPathElement(
              queryParams = Map(
                "opschrift" -> Option(opschrift).map(_.toString),
                "afstand" -> Option(afstand).map(_.toString),
                "crs" -> crs.map(_.toString)
              ),
              validAcceptHeaders = List("application/json"),
              req = request
            ) {
              //              def execute() =


              def headers(headers: Map[String, String]) = new HeaderPathElement(
                headers = headers,
                req = request
              ) {

                def format = new FormatPathElement(request) {
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

              def headers(headers: Map[String, String]) = new HeaderPathElement(
                headers = headers,
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

  /**
   * Proposed DSL:
   *
   * XoClient("http://host:8080").rest.locatie.weglocatie.weg.ident8("N0080001").get.query(opschrift=2.0, afstand=50, crs=Option(123)).accept(ApplicationJson).format.exec()
   * --> Future[Result(200, FormattedJson(...))]
   *
   * XoClient("http://host:8080").rest.locatie.weglocatie.weg.post(PostData(...)).content(ApplicationJson).accept(ApplicationJson)
   * --> Future[Result]
   *
   * XoClient("http://host:8080").rest.locatie.weglocatie.weg.put(PostData(...)).content(ApplicationJson).accept(ApplicationJson)
   * --> Future[Result]
   *
   * XoClient("http://host:8080").rest.locatie.weglocatie.weg.ident8("N0080001").delete()
   * --> Future[Result]
   *
   */


  feature("generate a foo case class") {

    scenario("test scala macros with quasiquotes") {

      Given("the FromMacroCode macro annotation")


      When("we create an instance of Foo")

      XoClient("host", 8080, "http").rest.locatie.weglocatie.weg.ident8("N0080001").get(opschrift = 2.0, afstand = 50, crs = Option(123))
        .headers(Map("Accept" -> "application/json")).format.execute()

      XoClient("host", 8080, "http").rest.locatie.weglocatie.weg.ident8("N0080001").put("body")
        .headers(Map("Content-Type" -> "application/json", "Accept" -> "application/json")).execute()

      Then("we should be able to print foo")

    }
  }

}
