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

import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedArray, ParsedObject, ParsedString }
import io.atomicbits.scraml.ramlparser.model.{ Get, NativeId, Raml }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 24/03/17.
  */
class QuestionMarkMarksOptionalPropertyOrParameterTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Question marks can be used to mark optional properties or parameters") {

    scenario("Test parsing an optional property ") {

      Given("a RAML specification with an optional property marked by a '?'")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/questionmarkforoptional/questionmark-api.raml", "UTF-8")

      When("we parse the RAML spec")
      val raml: Raml = parser.parse.get

      Then("we should see that the property is optional")
      val parsedAnimal = raml.types.typeReferences(NativeId("Animal")).asInstanceOf[ParsedObject]
      parsedAnimal.properties.valueMap("kind").required shouldBe true
      parsedAnimal.properties.valueMap("gender").required shouldBe true
      parsedAnimal.properties.valueMap("isNice").required shouldBe false
      parsedAnimal.properties.valueMap("isVeryNice?").required shouldBe false

      parsedAnimal.properties.valueMap.get("isNice?").isDefined shouldBe false
      parsedAnimal.properties.valueMap.get("isVeryNice??").isDefined shouldBe false

    }

    scenario("Test parsing an optional parameter") {

      Given("a RAML specification with an optional parameter marked by a '?'")
      val defaultBasePath = List("io", "atomicbits", "raml10")
      val parser          = RamlParser("/questionmarkforoptional/questionmark-api.raml", "UTF-8")

      When("we parse the RAML spec")
      val raml: Raml = parser.parse.get

      Then("we should see that the parameter is optional")
      val queryParameters = raml.resourceMap("animals").actionMap(Get).queryParameters.valueMap

      queryParameters("kind").required shouldBe true
      queryParameters("gender").required shouldBe false
      queryParameters("name").required shouldBe false
      queryParameters("caretaker").required shouldBe false
      queryParameters("food").required shouldBe true

      queryParameters.get("gender?").isDefined shouldBe false

      queryParameters("name").parameterType.parsed shouldBe a[ParsedArray]
      queryParameters("name").parameterType.parsed.asInstanceOf[ParsedArray].items shouldBe a[ParsedString]

      queryParameters("caretaker").parameterType.parsed shouldBe a[ParsedArray]
      queryParameters("caretaker").parameterType.parsed.asInstanceOf[ParsedArray].items shouldBe a[ParsedString]

      queryParameters("food").parameterType.parsed shouldBe a[ParsedArray]
      queryParameters("food").parameterType.parsed.asInstanceOf[ParsedArray].items shouldBe a[ParsedString]

    }

  }

}
