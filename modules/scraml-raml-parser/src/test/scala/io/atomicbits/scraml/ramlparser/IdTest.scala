/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser

import io.atomicbits.scraml.ramlparser.model.{ RelativeId, RootId }
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

/**
  * Created by peter on 14/02/17.
  */
class IdTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("RootId parsing") {

    Scenario("test parsing a RootId") {

      Given("a RootId string ")
      val rootIdString = "http://atomicbits.io/model/home-address.json"

      When("we parse the RootId string")
      val rootId = RootId(rootIdString)

      Then("we get all four actions in the userid resource")
      rootId shouldBe RootId(hostPath = List("atomicbits", "io"), path = List("model"), name = "home-address")

    }

    Scenario("test parsing a less obvious RootId") {

      Given("a RootId string ")
      val rootIdString = "https://atomicbits.io/home-address.myfile#"

      When("we parse the RootId string")
      val rootId = RootId(rootIdString)

      Then("we get all four actions in the userid resource")
      rootId shouldBe RootId(hostPath = List("atomicbits", "io"), path = List(), name = "home-address")

    }

  }

  Feature("RelativeId parsing") {

    Scenario("test parsing a relative id") {

      Given("relative id strings")
      val relativeIdString01 = "some/path/file.json"
      val relativeIdString02 = "/some/path/file.json"
      val relativeIdString03 = "/some/path/file"
      val relativeIdString04 = "book"

      When("we parse the relative ids")
      val relativeId01 = RelativeId(relativeIdString01)
      val relativeId02 = RelativeId(relativeIdString02)
      val relativeId03 = RelativeId(relativeIdString03)
      val relativeId04 = RelativeId(relativeIdString04)

      Then("the relative ids must have the expected parts")
      relativeId01.path shouldBe List("some", "path")
      relativeId01.name shouldBe "file"
      relativeId02.path shouldBe List("some", "path")
      relativeId02.name shouldBe "file"
      relativeId03.path shouldBe List("some", "path")
      relativeId03.name shouldBe "file"
      relativeId04.path shouldBe List()
      relativeId04.name shouldBe "book"

    }

  }

}
