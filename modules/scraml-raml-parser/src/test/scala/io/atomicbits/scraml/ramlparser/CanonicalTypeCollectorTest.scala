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

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalNameGenerator, CanonicalTypeCollector }
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.Matchers._
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try

/**
  * Created by peter on 30/12/16.
  */
class CanonicalTypeCollectorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Canonical type collecting") {

    scenario("test collecting json-schema types in a RAML model") {

      Given("a RAML specification containing json-schema definitions")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse
      val canonicalTypeCollector = CanonicalTypeCollector(CanonicalNameGenerator(defaultBasePath))

      Then("we get all four actions in the userid resource")
      val raml = parsedModel.get
      // ToDo: finish test implementation
//      val (ramlCanonical, canonicalLookup) = canonicalTypeCollector.collect(raml)
//      println(s"${canonicalLookup.map}")
      // actionTypes should contain(Delete)
    }

  }

}
