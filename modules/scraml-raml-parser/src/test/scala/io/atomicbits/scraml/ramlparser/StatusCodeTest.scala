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

import io.atomicbits.scraml.ramlparser.model.StatusCode
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 16/03/17.
  */
class StatusCodeTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Status code test") {

    scenario("test the ordering of the status codes") {

      Given("a set containing status codes")
      val statusCodes = Set(StatusCode("401"), StatusCode("201"), StatusCode("404"))

      When("the min value of the set is retrieved")
      val minStatusCode = statusCodes.min

      Then("we get the status code with the smallest code")
      minStatusCode shouldBe StatusCode("201")

    }
  }

}
