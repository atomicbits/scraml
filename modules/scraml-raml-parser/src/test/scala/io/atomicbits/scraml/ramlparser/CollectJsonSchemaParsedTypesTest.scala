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

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator, CanonicalTypeCollector }
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedObject, ParsedType, ParsedTypeReference }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try
import org.scalatest.Matchers._
import io.atomicbits.util.TestUtils._

/**
  * Created by peter on 2/01/17.
  */
class CollectJsonSchemaParsedTypesTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect all json-schema ParsedTypes after expanding all relative IDs to absolute IDs") {

    scenario("test collecting of all types in a json-schema type declaration") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                = parsedModel.get
      val parsedType          = raml.types.get(NativeId("myfragments")).get
      val myFragmentsExpanded = canonicalTypeCollector.expandRelativeToAbsoluteIds(parsedType).asInstanceOf[ParsedObject]

      val barReference = myFragmentsExpanded.fragments.fragmentMap("bar").asInstanceOf[ParsedTypeReference]
      barReference.refersTo shouldBe NoId

      val collectedParsedTypes: List[ParsedType] = canonicalTypeCollector.collectJsonSchemaParsedTypes(myFragmentsExpanded)

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "bars")
        )
      )

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barlist")
        )
      )

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barpointer")
        )
      )

      // println(s"$collectedParsedTypes")
    }

    scenario("test collecting of all types in a complex json-schema type declaration with RAML 1.0 type declarations") {

      Given("a RAML specification containing complex json-schema definitions and RAML 1.0 type definitions")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we get all four actions in the userid resource")
      val raml                              = parsedModel.get
      val canonicalLHWithIndexedParsedTypes = canonicalTypeCollector.indexParsedTypes(raml, CanonicalLookupHelper())

      val geometryTypeOpt = canonicalLHWithIndexedParsedTypes.getParsedType(NativeId("geometry"))
      geometryTypeOpt.isDefined shouldBe true
      geometryTypeOpt.get.isInstanceOf[ParsedObject] shouldBe true

      // ToDo: check for the presence of the Cat and the Fish
      val animalTypeOpt = canonicalLHWithIndexedParsedTypes.getParsedType(NativeId("Animal"))

      // println(s"${prettyPrint(canonicalLHWithIndexedParsedTypes)}")
    }

  }

}
