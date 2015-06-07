/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

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

      val client = TestClient01(host = "localhost", port = 8080,
        defaultHeaders = Map("Accept" -> "application/json"))

      val userFoobarResource = client.rest.user.userid("foobar")

      userFoobarResource
        .get(age = Some(51), firstName = Some("John"), lastName = None)
        .execute()

      userFoobarResource
        .post(text = "Hello Foobar", value = None)
        .execute()

      userFoobarResource
        .put("blablabla")
        .headers(
          "Content-Type" -> "application/json",
          "Accept" -> "application/json"
        )
        .execute()

      userFoobarResource.delete().execute()


      Then("we should be able to print foo")
      println(s"foo: $userFoobarResource")

    }
  }

}
