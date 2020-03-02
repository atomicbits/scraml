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
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

import scala.util.Try

/**
  * Created by peter on 1/01/17.
  */
class ExpandRelativeToAbsoluteIdsTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("Expand all relative IDs to absolute IDs in any json-schema ParsedType") {

    Scenario("test fragment reference ID expansion in a json-schema type") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse
      val canonicalTypeCollector = CanonicalTypeCollector(CanonicalNameGenerator(defaultBasePath))

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                = parsedModel.get
      val parsedType          = raml.types.get(NativeId("myfragments")).get
      val myFragmentsExpanded = canonicalTypeCollector.indexer.expandRelativeToAbsoluteIds(parsedType).asInstanceOf[ParsedObject]

      val definitionsFragment = myFragmentsExpanded.fragments.fragmentMap("definitions").asInstanceOf[Fragments]

      val addressFragment = definitionsFragment.fragments.fragmentMap("address").asInstanceOf[ParsedObject]
      println(s"$myFragmentsExpanded")
      addressFragment.id shouldBe AbsoluteFragmentId(RootId("http://atomicbits.io/schema/fragments.json"), List("definitions", "address"))

      val barsFragment = definitionsFragment.fragments.fragmentMap("bars").asInstanceOf[ParsedObject]
      println(s"$barsFragment")
      barsFragment.id shouldBe AbsoluteFragmentId(RootId("http://atomicbits.io/schema/fragments.json"), List("definitions", "bars"))

      val fooReference   = myFragmentsExpanded.properties.valueMap("foo").propertyType
      val fooArray       = fooReference.parsed.asInstanceOf[ParsedArray]
      val fooArrayItemId = fooArray.items.asInstanceOf[ParsedTypeReference].refersTo
      fooArrayItemId shouldBe AbsoluteFragmentId(RootId("http://atomicbits.io/schema/fragments.json"), List("definitions", "address"))

      val foobarsReference   = myFragmentsExpanded.properties.valueMap("foobars").propertyType
      val foobarsArray       = foobarsReference.parsed.asInstanceOf[ParsedArray]
      val foobarsArrayItemId = foobarsArray.items.asInstanceOf[ParsedTypeReference].refersTo
      foobarsArrayItemId shouldBe AbsoluteFragmentId(RootId("http://atomicbits.io/schema/fragments.json"), List("definitions", "bars"))

    }

  }

}
