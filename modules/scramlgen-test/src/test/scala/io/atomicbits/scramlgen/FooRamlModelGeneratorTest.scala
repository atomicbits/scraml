package io.atomicbits.scramlgen

import io.atomicbits.scramlgen.examples.Foo
import org.scalatest.{GivenWhenThen, FeatureSpec}
import scala.language.reflectiveCalls
import scala.concurrent.Future


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

sealed trait PathElement {

  protected def request: Request

  def headers(headers: List[(String, String)], req: Request): PathElement = {
    this
  }

}

case class PlainPathElement(pathElement: String, req: Request) extends PathElement {

  val request = req.copy(reversePath = pathElement :: req.reversePath)
}

case class StringPathElement(value: String, req: Request) extends PathElement {

  val request = req.copy(reversePath = value :: req.reversePath)
}

case class IntPathelement(value: Int, req: Request) extends PathElement {

  val request = req.copy(reversePath = value.toString :: req.reversePath)
}

case class DoublePathelement(value: Double, req: Request) extends PathElement {

  val request = req.copy(reversePath = value.toString :: req.reversePath)
}

case class BooleanPathelement(value: Boolean, req: Request) extends PathElement {

  val request = req.copy(reversePath = value.toString :: req.reversePath)
}

sealed trait MethodPathElement extends PathElement

case class GetPathElement(queryParams: List[(String, Option[String])], req: Request) extends MethodPathElement {

  val queryParameterMap = queryParams.toMap.collect { case (key, Some(value)) => (key, value) }

  val request = req.copy(queryParameters = queryParameterMap, method = Get)

}

case class PutPathElement(body: String, req: Request) extends MethodPathElement {

  val request = req.copy(method = Put, body = Option(body))

}


case class FormatPathElement(req: Request) {

  val request = req.copy(formatResultBody = true)

}

case class ExecutePathElement(req: Request) {

  //  def execute[T](): Future[T] = ???
  def execute(): Unit = println(s"request: $req")

}

sealed trait Method

case object Get extends Method

case object Post extends Method

case object Put extends Method

case object Delete extends Method

case object Head extends Method

case object Opt extends Method

case object Patch extends Method

trait MediaTypeHeader {

  def mediaType: String

}

trait AcceptHeader extends MediaTypeHeader

trait ContentTypeHeader extends MediaTypeHeader

case class Request(protocol: String,
                   host: String,
                   port: Int,
                   reversePath: List[String] = Nil,
                   method: Method = Get,
                   queryParameters: Map[String, String] = Map.empty,
                   validAcceptHeaders: List[String] = Nil,
                   validContentTypeHeaders: List[String] = Nil,
                   body: Option[String] = None,
                   formatResultBody: Boolean = false)

case class XoClient(host: String, port: Int = 80, protocol: String = "http") {

  val request = Request(protocol, host, port)

  def rest = new PlainPathElement("rest", request) {
    def locatie = new PlainPathElement("locatie", request) {
      def weglocatie = new PlainPathElement("weglocatie", request) {
        def weg = new PlainPathElement("weg", request) {
          def ident8(value: String) = new StringPathElement(value, request) {
            def get(opschrift: Double, afstand: Int, crs: Option[Int] = None) = new GetPathElement(
              List(
                "opschrift" -> Option(opschrift).map(_.toString),
                "afstand" -> Option(afstand).map(_.toString),
                "crs" -> crs.map(_.toString)
              ),
              request
            ) {
              //              def execute() =


              def format = new FormatPathElement(request) {
                def execute() = new ExecutePathElement(request).execute()
              }

              def execute() = new ExecutePathElement(request).execute()
            }


            def put(body: String) = new PutPathElement(body, request) {

              def execute() = new ExecutePathElement(request).execute()

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
class FooRamlModelGeneratorTest extends FeatureSpec with GivenWhenThen {

  feature("generate a foo case class") {

    scenario("test scala macros with quasiquotes") {

      Given("the FromMacroCode macro annotation")


      When("we create an instance of Foo")
      XoClient("host", 8080, "http").rest.locatie.weglocatie.weg.ident8("N0080001").get(opschrift = 2.0, afstand = 50, crs = Option(123))
        .format.execute()
      println("Creating foo: ")

      val foo = Foo("hello")

      XoClient("host", 8080, "http").rest.locatie.weglocatie.weg.ident8("N0080001").put("body").execute()

      Then("we should be able to print foo")
      println(s"foo: $foo")


    }
  }

}
