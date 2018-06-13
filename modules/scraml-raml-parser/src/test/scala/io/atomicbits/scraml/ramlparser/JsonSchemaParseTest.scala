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

import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.language.postfixOps
import scala.util.Try

/**
  * Created by peter on 6/02/16.
  */
class JsonSchemaParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("json schema parsing") {

    scenario("test parsing fragments in json-schema") {

      Given("a json-schema containing fragments")
      val parser = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a properly parsed fragments object")
      val raml = parsedModel.get

      val objectWithFragments: ParsedObject = raml.types.typeReferences(NativeId("myfragments")).asInstanceOf[ParsedObject]

      val barType: ParsedType = objectWithFragments.fragments.fragmentMap("bar")

      barType shouldBe a[ParsedTypeReference]
      barType.asInstanceOf[ParsedTypeReference].refersTo shouldBe NativeId("baz")

      val definitionFragment: Fragments = objectWithFragments.fragments.fragmentMap("definitions").asInstanceOf[Fragments]

      val addressType = definitionFragment.fragmentMap("address")
      addressType shouldBe a[ParsedObject]
      val address = addressType.asInstanceOf[ParsedObject]

      address.id shouldBe FragmentId(List("definitions", "address"))
      address.properties("city").propertyType.parsed shouldBe a[ParsedString]
      address.properties("state").propertyType.parsed shouldBe a[ParsedString]
      address.properties("zip").propertyType.parsed shouldBe a[ParsedInteger]
      address.properties("streetAddress").propertyType.parsed shouldBe a[ParsedString]

      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")
    }

    scenario("test collecting of all types in a complex json-schema type declaration with RAML 1.0 type declarations") {

      Given("a RAML specification containing complex json-schema definitions and RAML 1.0 type definitions")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get all four actions in the userid resource")
      val raml = parsedModel.get

      val userObject                            = raml.types.typeReferences(NativeId("user")).asInstanceOf[ParsedObject]
      val addressParsedProperty: ParsedProperty = userObject.properties.valueMap("address")

      addressParsedProperty.required shouldBe false

    }

  }

}
