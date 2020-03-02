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
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

/**
  * Created by peter on 11/02/17.
  */
class NativeClassHierarchyTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("json schema native id lookup test") {

    Scenario("test json-schema native id lookup") {

      Given("a RAML 1.0 specification with json-schema types")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/nativeclasshierarchy/NativeClassHierarchyTest.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we are able to to a lookup of json-schema types using a native id in the resource definition")
      val raml = parsedModel.get

      implicit val canonicalNameGenerator: CanonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      canonicalLookup
    }

  }

}
