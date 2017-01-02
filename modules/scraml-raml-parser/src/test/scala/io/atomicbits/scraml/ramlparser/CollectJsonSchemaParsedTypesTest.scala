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

import io.atomicbits.scraml.ramlparser.lookup.{
  CanonicalLookupHelper,
  CanonicalNameGenerator,
  CanonicalTypeCollector,
  ParsedToCanonicalTypeTransformer
}
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.{ AbsoluteFragmentId, NativeId, Raml, RootId }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedObject, ParsedType }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try
import org.scalatest.Matchers._

/**
  * Created by peter on 2/01/17.
  */
class CollectJsonSchemaParsedTypesTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect all json-schema ParsedTypes after expanding all relative IDs to absolute IDs") {

    scenario("test collecting of all types in a json-schema type declaration") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                                   = parsedModel.get
      val parsedType                             = raml.types.get(NativeId("myfragments")).get
      val myFragmentsExpanded                    = canonicalTypeCollector.expandRelativeToAbsoluteIds(parsedType).asInstanceOf[ParsedObject]
      val collectedParsedTypes: List[ParsedType] = canonicalTypeCollector.collectJsonSchemaParsedTypes(myFragmentsExpanded)

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "bars")
        )
      )

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barlist")
        )
      )

      collectedParsedTypes.map(_.id) should contain(
        AbsoluteFragmentId(
          RootId("http://atomicbits.io/schema/fragments.json"),
          List("definitions", "barpointer")
        )
      )

    }

  }

  feature("Collect the canonical representations of the ParsedTypes") {

    scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schemas")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                                 = parsedModel.get
      val (ramlUpdated, canonicalLookupHelper) = canonicalTypeCollector.collectCanonicals(raml, CanonicalLookupHelper())
      canonicalLookupHelper.parsedTypeIndex
      println(s"$canonicalLookupHelper")
    }

  }

}
