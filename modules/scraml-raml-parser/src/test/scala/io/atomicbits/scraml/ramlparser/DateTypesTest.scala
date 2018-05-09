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
import io.atomicbits.scraml.ramlparser.model.{ NativeId, Raml }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedObject, ParsedString }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try

/**
  * Created by peter on 6/07/17.
  */
class DateTypesTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  scenario("test parsing date types in a RAML 1.0 model") {

    Given("a RAML 1.0 specification with date types")
    val defaultBasePath = List("io", "atomicbits", "types")
    val parser          = RamlParser("/date-types/DateTypesTest.raml", "UTF-8")

    When("we parse the specification")
    val parsedModel: Try[Raml] = parser.parse

    Then("we get the parsed date times")
    val raml = parsedModel.get

//    val bookType      = raml.types(NativeId("Book")).asInstanceOf[ParsedObject]
//    bookType.properties("title").propertyType.parsed match {
//      case stringType: ParsedString => stringType.required shouldBe Some(true)
//      case _                        => fail(s"The title property of a book should be a StringType.")
//    }

    val prettyModel = TestUtils.prettyPrint(parsedModel)

    val canonicalTypeCollector         = CanonicalTypeCollector(CanonicalNameGenerator(defaultBasePath))
    val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

  }

}
