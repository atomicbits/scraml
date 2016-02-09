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
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, FeatureSpec}


/**
  * Created by peter on 6/02/16.
  */
class RamlParserTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Basic RAML parser") {

    scenario("test a basic RAML 1.0 example") {

      Given("a RAML 1.0 specification")





      When("we parse the specification")

      val result = RamlParser("/test001.raml", "UTF-8").parse
      println(s"Parsed raml: $result")

      Then("we get a ...")


    }
  }


}
