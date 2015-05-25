package io.atomicbits.scraml

import io.atomicbits.scraml.examples.TestClient01
import org.scalatest.{GivenWhenThen, FeatureSpec}

import scala.language.reflectiveCalls

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
class FooRamlModelGeneratorTest extends FeatureSpec with GivenWhenThen {

  feature("generate a foo case class") {

    scenario("test scala macros with quasiquotes") {

      Given("the FromMacroCode macro annotation")


      When("we create an instance of Foo")

      val client = TestClient01(host = "localhost", port = 8080)

      val userFoobarResource = client.rest.user.userid("foobar")

      userFoobarResource
        .get(lat = Some(51.3), lng = Some(2.76), distance = Some(500))
        .headers("Accept" -> "application/json")
        .execute()

      userFoobarResource
        .post(text = "Hello Foobar", value = None)
        .headers("Content-Type" -> "application/x-www-form-urlencoded")
        .execute()

      userFoobarResource
        .put("blablabla")
        .headers(
          "Content-Type" -> "application/json",
          "Accept" -> "application/json"
        )
        .execute()

      userFoobarResource.delete().headers().execute()

      // Or, shorter if there are no required Accept or Content-Type headers:
      userFoobarResource.delete().execute()


      Then("we should be able to print foo")
      println(s"foo: $userFoobarResource")

    }
  }

}
