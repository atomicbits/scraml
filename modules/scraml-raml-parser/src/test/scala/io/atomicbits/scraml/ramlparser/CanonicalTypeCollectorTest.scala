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
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.Matchers._
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try

/**
  * Created by peter on 30/12/16.
  */
class CanonicalTypeCollectorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect the canonical representations of a simple fragmented json-schema definition") {

    scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schema")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                           = parsedModel.get
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val fragments =
        canonicalLookup(CanonicalName.create(name = "Fragments", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      val foobarPointer = fragments.properties("foobarpointer").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobarPointer.genericType.isInstanceOf[NumberType.type] shouldBe true

      val foobarList = fragments.properties("foobarlist").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobarList.genericType.isInstanceOf[NumberType.type] shouldBe true

      val foobars = fragments.properties("foobars").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobars.genericType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true
      foobars.genericType.asInstanceOf[NonPrimitiveTypeReference].refers shouldBe
        CanonicalName.create(name = "FragmentsDefinitionsBars", packagePath = List("io", "atomicbits", "schema"))

      val foo = fragments.properties("foo").asInstanceOf[Property[ArrayTypeReference]].ttype
      foo.genericType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true
      foo.genericType.asInstanceOf[NonPrimitiveTypeReference].refers shouldBe
        CanonicalName.create(name = "FragmentsDefinitionsAddress", packagePath = List("io", "atomicbits", "schema"))

      val fragmentDefBars =
        canonicalLookup(CanonicalName.create(name = "FragmentsDefinitionsBars", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      fragmentDefBars.properties("baz").isInstanceOf[Property[StringType.type]] shouldBe true

      val fragmentDefAddress =
        canonicalLookup(CanonicalName.create(name = "FragmentsDefinitionsAddress", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      fragmentDefAddress.properties("city").isInstanceOf[Property[StringType.type]] shouldBe true
      fragmentDefAddress.properties("state").isInstanceOf[Property[StringType.type]] shouldBe true
      fragmentDefAddress.properties("zip").isInstanceOf[Property[IntegerType.type]] shouldBe true
      fragmentDefAddress.properties("streetAddress").isInstanceOf[Property[StringType.type]] shouldBe true
    }

  }

  feature("Collect the canonical representations of a complex and mixed json-schema/RAML1.0 definition") {

    scenario("test collecting json-schema types in a RAML model") {

      Given("a RAML specification containing json-schema definitions")
      val defaultBasePath = List("io", "atomicbits", "schema")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse
      val canonicalTypeCollector = CanonicalTypeCollector(CanonicalNameGenerator(defaultBasePath))

      Then("we get all four actions in the userid resource")
      val raml = parsedModel.get

      // val () = canonicalTypeCollector.collectCanonicals(raml, CanonicalLookupHelper())

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      // ToDo: finish test implementation
      println(s"${canonicalLookup.map}")
    }

  }

}
