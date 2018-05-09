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
      val parser          = RamlParser("/relativeid/car-api.raml", "UTF-8")

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
          name  = "drive",
          ttype = NonPrimitiveTypeReference(refers = CanonicalName.create(name = "Engine", packagePath = List("io", "atomicbits", "model")))
        )
    }
  }

}
