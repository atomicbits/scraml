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

import io.atomicbits.scraml.ramlparser.model.{ NativeId, Raml }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedObject, ParsedString, ParsedTypeReference }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

import scala.util.Try

/**
  * Created by peter on 5/03/18.
  */
class TypesIncludeTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Scenario("test the inclusion of types at the top level types definition") {

    Given("a RAML 1.0 specification with types inclusion")
    val parser = RamlParser("/typesinclude/types-include-api.raml", "UTF-8")

    When("we parse the specification")
    val parsedModel: Try[Raml] = parser.parse

    Then("we get the included types")
    val raml = parsedModel.get

    val bookType   = raml.types(NativeId("Book")).asInstanceOf[ParsedObject]
    val authorType = raml.types(NativeId("Author")).asInstanceOf[ParsedObject]

    bookType.properties("title").propertyType.parsed match {
      case stringType: ParsedString => stringType.required shouldBe Some(true)
      case _                        => fail(s"The title property of a book should be a StringType.")
    }

    bookType.properties("author").propertyType.parsed match {
      case typeReference: ParsedTypeReference => typeReference.refersTo.asInstanceOf[NativeId] shouldBe NativeId("Author")
      case _                                  => fail(s"The author property of a book should be a ReferenceType.")
    }

  }

}
