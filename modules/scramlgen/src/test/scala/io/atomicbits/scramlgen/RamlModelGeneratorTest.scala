package io.atomicbits.scramlgen

import org.scalatest.{GivenWhenThen, FeatureSpec}

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */

//@FromMacroCode("bla") class Foo

//@expand() class MyAdt

//case class Foo(text: String)

class RamlModelGeneratorTest extends FeatureSpec with GivenWhenThen {

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
