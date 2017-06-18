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
import io.atomicbits.scraml.ramlparser.model.{ Get, MediaType, Raml, StatusCode }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 15/02/17.
  */
class JsonSchemaWithRelativeIdsTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect the canonical representations of a simple fragmented json-schema definition") {

    scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/relativeid/car-api.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                           = parsedModel.get
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val carsResource                 = ramlUpdated.resources.filter(_.urlSegment == "cars").head
      val getBody                      = carsResource.actionMap(Get).responses.responseMap(StatusCode("200")).body
      val canonicalType: TypeReference = getBody.contentMap(MediaType("application/json")).bodyType.get.canonical.get

      canonicalType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true
      val car     = canonicalType.asInstanceOf[NonPrimitiveTypeReference]
      val carName = CanonicalName.create("Car", List("io", "atomicbits", "model"))
      car.refers shouldBe carName

      val carType = canonicalLookup.map(carName).asInstanceOf[ObjectType]
      carType.properties("drive") shouldBe
        Property(
          name = "drive",
          ttype =
            NonPrimitiveTypeReference(refers = CanonicalName.create(name = "Engine", packagePath = List("io", "atomicbits", "model")))
        )
    }
  }

}
