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
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.{ Raml }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 20/03/17.
  */
class TypeParametersTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Test cases using type parameters") {

    scenario("a json-schema definition with a reference to a paged-list in an object field") {

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

    scenario("a RAML 1.0 definition with a reference to a paged-list in an object field") {

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
