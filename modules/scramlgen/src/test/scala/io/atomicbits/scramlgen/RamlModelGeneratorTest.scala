package io.atomicbits.scramlgen

import org.scalatest.{GivenWhenThen, FeatureSpec}

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


  feature("generate a case class") {

    println("Starting test...")

    scenario("test scala macros with quasiquotes") {

      Given("the FromMacroCode macro annotation")


      When("we create an instance of Foo")
//      println("Creating foo: ")
//      val foo = Foo("hello")

      Then("we should be able to print foo")
//      println(s"foo: $foo")


    }

  }

}
