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

import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ParsedObject, ParsedString, ParsedTypeReference$}
import io.atomicbits.scraml.ramlparser.model.{NativeId, Raml}
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 25/11/16.
  */
class TypesParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  scenario("test parsing types in a RAML 1.0 model") {

    Given("a RAML 1.0 specification with some types defined")
    val parser = RamlParser("/types/TypesTestApi.raml", "UTF-8", List("io", "atomicbits", "types"))

    When("we parse the specification")
    val parsedModel: Try[Raml] = parser.parse

    Then("we get all four actions in the userid resource")
    val raml = parsedModel.get


    val bookType = raml.types(NativeId("Book")).asInstanceOf[ParsedObject]
    val authorType = raml.types(NativeId("Author")).asInstanceOf[ParsedObject]
    val comicBookType = raml.types(NativeId("ComicBook")).asInstanceOf[ParsedObject]

    bookType.properties("title").propertyType match {
      case stringType: ParsedString => stringType.required shouldBe Some(true)
      case _                        => fail(s"The title property of a book should be a StringType.")
    }

    bookType.properties("author").propertyType match {
      case typeReference: ParsedTypeReference => typeReference.refersTo.asInstanceOf[NativeId] shouldBe NativeId("Author")
      case _                                  => fail(s"The author property of a book should be a ReferenceType.")
    }

    comicBookType.parent shouldBe Some(NativeId("Book"))
    comicBookType.properties("hero").propertyType match {
      case stringType: ParsedString => stringType.required shouldBe None
      case _                        => fail(s"The hero property of a comicbook should be a StringType.")
    }

          val prettyModel = TestUtils.prettyPrint(parsedModel)
           println(s"Parsed raml: $prettyModel")

  }

}
