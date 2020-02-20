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

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalNameGenerator, CanonicalTypeCollector }
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

/**
  * Created by peter on 6/04/17.
  */
class InlineQueryparameterTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("Parsing of complex inline query parameters") {

    Scenario("An inline enum query parameter must be parsed, indexed and properly typed") {

      Given("a RAML specification with an inline enum query parameter")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/inlinequeryparameter/inlinequeryparameter-api.raml", "UTF-8")

      When("we parse the RAML spec")
      val raml: Raml                      = parser.parse.get
      implicit val canonicalNameGenerator: CanonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)
      val (ramlUpdated, canonicalLookup)  = canonicalTypeCollector.collect(raml)

      Then("we should see that the query parameter type is generated")
      println(canonicalLookup)

      canonicalLookup.get(CanonicalName.create(name = "Food", packagePath = defaultBasePath)).isDefined shouldBe true

    }

  }

}
