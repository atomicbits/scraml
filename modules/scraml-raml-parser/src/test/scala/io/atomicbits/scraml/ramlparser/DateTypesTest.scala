/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
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
