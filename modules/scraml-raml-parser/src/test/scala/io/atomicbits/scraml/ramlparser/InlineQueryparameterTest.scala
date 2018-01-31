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
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 6/04/17.
  */
class InlineQueryparameterTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Parsing of complex inline query parameters") {

    scenario("An inline enum query parameter must be parsed, indexed and properly typed") {

      Given("a RAML specification with an inline enum query parameter")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/inlinequeryparameter/inlinequeryparameter-api.raml", "UTF-8")

      When("we parse the RAML spec")
      val raml: Raml                      = parser.parse.get
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)
      val (ramlUpdated, canonicalLookup)  = canonicalTypeCollector.collect(raml)

      Then("we should see that the query parameter type is generated")
      println(canonicalLookup)

      canonicalLookup.get(CanonicalName.create(name = "Food", packagePath = defaultBasePath)).isDefined shouldBe true

    }

  }

}
