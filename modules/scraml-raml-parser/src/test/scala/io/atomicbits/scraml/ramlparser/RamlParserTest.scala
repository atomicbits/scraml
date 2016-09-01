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

import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}


/**
  * Created by peter on 6/02/16.
  */
class RamlParserTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("RAML parser") {

    scenario("test parsing json-schema types in a RAML 1.0 model") {

      Given("a RAML 1.0 specification with json-schema types")
      val parser = RamlParser("/json-schema-types/TestApi.raml", "UTF-8", List("io", "atomicbits", "model"))

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we get a ...")
      val prettyModel = TestUtils.prettyPrint(parsedModel)
      println(s"Parsed raml: $prettyModel")
    }


    scenario("test parsing a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
       val parser = RamlParser("/raml08/TestApi.raml", "UTF-8", List("io", "atomicbits", "schemas"))

      When("we parse the specification")
       val parsedModel = parser.parse

      Then("we get a ...")
      val prettyModel = TestUtils.prettyPrint(parsedModel)
      println(s"Parsed raml: $prettyModel")

    }

  }


}
