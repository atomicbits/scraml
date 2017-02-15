/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser

import io.atomicbits.scraml.ramlparser.model.RootId
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 14/02/17.
  */
class RootIdTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

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

}
