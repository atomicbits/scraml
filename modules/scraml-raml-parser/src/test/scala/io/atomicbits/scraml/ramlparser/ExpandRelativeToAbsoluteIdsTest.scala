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
import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 1/01/17.
  */
class ExpandRelativeToAbsoluteIdsTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Expand all relative IDs to absolute IDs in any json-schema ParsedType") {

    scenario("test fragment reference ID expansion in a json-schema type") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

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
