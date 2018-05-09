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
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 5/02/17.
  */
class NativeIdResourceBodyLookupTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("json schema native id lookup test") {

    scenario("test json-schema native id lookup") {

      Given("a RAML 1.0 specification with json-schema types")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/json-schema-types/TestApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we are able to to a lookup of json-schema types using a native id in the resource definition")
      val raml = parsedModel.get

      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val userResource                 = ramlUpdated.resources.filter(_.urlSegment == "user").head
      val getBody                      = userResource.actionMap(Get).responses.responseMap(StatusCode("200")).body
      val canonicalType: TypeReference = getBody.contentMap(MediaType("application/json")).bodyType.get.canonical.get

      canonicalType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true

      val user = canonicalType.asInstanceOf[NonPrimitiveTypeReference]
      user.refers shouldBe CanonicalName.create("User", List("io", "atomicbits", "model"))
    }

  }

  feature("json schema with required fields defined outside the properties list of the object") {

    scenario("a test json-schema with required fields list defined outside the properties") {

      Given("A RAML 0.8 specification with a json-schema that has its required fields defined outside the object properties")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/json-schema-types/TestApi.raml", "UTF-8")

      When("we parse the spec")
      val parsedModel = parser.parse

      Then("the required fields are marked as required")
      val raml = parsedModel.get

      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val userExtReq = canonicalLookup.map(CanonicalName.create("UserExtReq", List("io", "atomicbits", "model"))).asInstanceOf[ObjectType]

      val idProperty = userExtReq.properties("id").asInstanceOf[Property[StringType.type]]
      idProperty.required shouldBe true

      val firstNameProperty = userExtReq.properties("firstName").asInstanceOf[Property[StringType.type]]
      firstNameProperty.required shouldBe true

      val lastNameProperty = userExtReq.properties("lastName").asInstanceOf[Property[StringType.type]]
      lastNameProperty.required shouldBe true

      val ageProperty = userExtReq.properties("age").asInstanceOf[Property[NumberType.type]]
      ageProperty.required shouldBe false

    }

  }

}
