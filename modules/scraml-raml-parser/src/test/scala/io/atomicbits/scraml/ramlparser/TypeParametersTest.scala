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
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

/**
  * Created by peter on 20/03/17.
  */
class TypeParametersTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("Test cases using type parameters") {

    Scenario("a json-schema definition with a reference to a paged-list in an object field") {

      Given("a RAML specification containing a json-schema definition with type parameters")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/typeparameters08/zoo-api.raml", "UTF-8")

      When("we parse the specification")
      val raml: Raml                     = parser.parse.get
      val canonicalNameGenerator         = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector         = CanonicalTypeCollector(canonicalNameGenerator)
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      Then("we find the type parameters in the parsed and the canonical types")
      val pagedListNonPrimitive: NonPrimitiveType =
        canonicalLookup.map(CanonicalName.create(name = "PagedList", packagePath = List("io", "atomicbits", "raml10")))
      val pagedListObject = pagedListNonPrimitive.asInstanceOf[ObjectType]

      pagedListObject.typeParameters shouldBe List(TypeParameter("T"), TypeParameter("U"))

      val zooNonPrimitive: NonPrimitiveType =
        canonicalLookup.map(CanonicalName.create(name = "Zoo", packagePath = List("io", "atomicbits", "raml10")))
      val zooObject = zooNonPrimitive.asInstanceOf[ObjectType]

      val pagedListTypeReference: Property[NonPrimitiveTypeReference] =
        zooObject.properties("animals").asInstanceOf[Property[NonPrimitiveTypeReference]]

      val animalTypeRef: NonPrimitiveTypeReference =
        pagedListTypeReference.ttype.genericTypes.head.asInstanceOf[NonPrimitiveTypeReference]

      animalTypeRef.refers shouldBe CanonicalName.create(name = "Animal", packagePath = List("io", "atomicbits", "raml10"))

      val intTypeRef: GenericReferrable = pagedListTypeReference.ttype.genericTypes.tail.head

      intTypeRef shouldBe IntegerType

    }

    Scenario("a RAML 1.0 definition with a reference to a paged-list in an object field") {

      Given("a RAML specification containing a RAML 1.0 definition with type parameters")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/typeparameters10/zoo-api.raml", "UTF-8")

      When("we parse the specification")
      val raml: Raml                     = parser.parse.get
      val canonicalNameGenerator         = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector         = CanonicalTypeCollector(canonicalNameGenerator)
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      Then("we find the type parameters in the parsed and the canonical types")
      val pagedListNonPrimitive: NonPrimitiveType =
        canonicalLookup.map(CanonicalName.create(name = "PagedList", packagePath = List("io", "atomicbits", "raml10")))
      val pagedListObject = pagedListNonPrimitive.asInstanceOf[ObjectType]

      pagedListObject.typeParameters shouldBe List(TypeParameter("T"), TypeParameter("U"))

      val zooNonPrimitive: NonPrimitiveType =
        canonicalLookup.map(CanonicalName.create(name = "Zoo", packagePath = List("io", "atomicbits", "raml10")))
      val zooObject = zooNonPrimitive.asInstanceOf[ObjectType]

      val pagedListTypeReference: Property[NonPrimitiveTypeReference] =
        zooObject.properties("animals").asInstanceOf[Property[NonPrimitiveTypeReference]]

      val animalTypeRef: NonPrimitiveTypeReference =
        pagedListTypeReference.ttype.genericTypes.head.asInstanceOf[NonPrimitiveTypeReference]

      animalTypeRef.refers shouldBe CanonicalName.create(name = "Animal", packagePath = List("io", "atomicbits", "raml10"))

      val intTypeRef: GenericReferrable = pagedListTypeReference.ttype.genericTypes.tail.head

      intTypeRef shouldBe IntegerType
    }

  }

}
