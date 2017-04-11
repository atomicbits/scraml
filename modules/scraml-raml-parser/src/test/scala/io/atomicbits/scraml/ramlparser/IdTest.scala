/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser

import io.atomicbits.scraml.ramlparser.model.{ RelativeId, RootId }
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 14/02/17.
  */
class IdTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("RootId parsing") {

    scenario("test parsing a RootId") {

      Given("a RootId string ")
      val rootIdString = "http://atomicbits.io/model/home-address.json"

      When("we parse the RootId string")
      val rootId = RootId(rootIdString)

      Then("we get all four actions in the userid resource")
      rootId shouldBe RootId(hostPath = List("atomicbits", "io"), path = List("model"), name = "home-address")

    }

    scenario("test parsing a less obvious RootId") {

      Given("a RootId string ")
      val rootIdString = "https://atomicbits.io/home-address.myfile#"

      When("we parse the RootId string")
      val rootId = RootId(rootIdString)

      Then("we get all four actions in the userid resource")
      rootId shouldBe RootId(hostPath = List("atomicbits", "io"), path = List(), name = "home-address")

    }

  }

  feature("RelativeId parsing") {

    scenario("test parsing a relative id") {

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
