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
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

/**
  * Created by peter on 11/02/17.
  */
class NativeClassHierarchyTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("json schema native id lookup test") {

    scenario("test json-schema native id lookup") {

      Given("a RAML 1.0 specification with json-schema types")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/nativeclasshierarchy/NativeClassHierarchyTest.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we are able to to a lookup of json-schema types using a native id in the resource definition")
      val raml = parsedModel.get

      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      canonicalLookup
    }

  }

}
