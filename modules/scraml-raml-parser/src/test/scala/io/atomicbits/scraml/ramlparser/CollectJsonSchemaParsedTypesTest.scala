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

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalLookupHelper, CanonicalNameGenerator, CanonicalTypeCollector }
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedObject, ParsedTypeReference }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }

import scala.util.Try
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._
import io.atomicbits.util.TestUtils._

/**
  * Created by peter on 2/01/17.
  */
class CollectJsonSchemaParsedTypesTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("Collect all json-schema ParsedTypes after expanding all relative IDs to absolute IDs") {

    Scenario("test collecting of all types in a json-schema type declaration") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator: CanonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                = parsedModel.get
      val parsedType          = raml.types.get(NativeId("myfragments")).get
      val myFragmentsExpanded = canonicalTypeCollector.indexer.expandRelativeToAbsoluteIds(parsedType).asInstanceOf[ParsedObject]

      val barReference = myFragmentsExpanded.fragments.fragmentMap("bar").asInstanceOf[ParsedTypeReference]
      barReference.refersTo shouldBe NoId

      val collectedParsedTypes: CanonicalLookupHelper =
        canonicalTypeCollector.indexer.collectJsonSchemaParsedTypes(myFragmentsExpanded, CanonicalLookupHelper())

      collectedParsedTypes.parsedTypeIndex.keys should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "bars")
        )
      )

      collectedParsedTypes.parsedTypeIndex.keys should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barlist")
        )
      )

      collectedParsedTypes.parsedTypeIndex.keys should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barpointer")
        )
      )

      // println(s"$collectedParsedTypes")
    }

    Scenario("test collecting of all types in a complex json-schema type declaration with RAML 1.0 type declarations") {

      Given("a RAML specification containing complex json-schema definitions and RAML 1.0 type definitions")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator: CanonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we get all four actions in the userid resource")
      val raml                              = parsedModel.get
      val canonicalLHWithIndexedParsedTypes = canonicalTypeCollector.indexer.indexParsedTypes(raml, CanonicalLookupHelper())

      val geometryTypeOpt = canonicalLHWithIndexedParsedTypes.getParsedTypeWithProperId(NativeId("geometry"))
      geometryTypeOpt.isDefined shouldBe true
      geometryTypeOpt.get.isInstanceOf[ParsedObject] shouldBe true

      // ToDo: check for the presence of the Cat and the Fish
      val animalTypeOpt = canonicalLHWithIndexedParsedTypes.getParsedTypeWithProperId(NativeId("Animal"))

      canonicalLHWithIndexedParsedTypes
        .getParsedTypeWithProperId(RootId("http://atomicbits.io/schema/animal.json"))
        .isDefined shouldBe true

      canonicalLHWithIndexedParsedTypes.getParsedTypeWithProperId(RootId("http://atomicbits.io/schema/dog.json")).isDefined shouldBe true
      // println(s"${prettyPrint(canonicalLHWithIndexedParsedTypes)}")
    }

  }

}
