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
import io.atomicbits.scraml.ramlparser.model.{ NativeId, Raml }
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedObject
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 12/03/17.
  */
class InlineObjectTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect the canonical representations of a simple fragmented json-schema definition") {

    scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/inlineobject/test-api.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml = parsedModel.get

      val parsedAttributes   = raml.types.typeReferences(NativeId("attributes")).asInstanceOf[ParsedObject]
      val parsedAttributeMap = parsedAttributes.properties.values.head.propertyType.parsed.asInstanceOf[ParsedObject]

      val attributeRequireValues = parsedAttributeMap.properties.valueMap.values.map(_.required).toList

      attributeRequireValues shouldBe List(false, false, false, false)

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val attributesMapName = CanonicalName.create("AttributesMap", List("io", "atomicbits", "raml10"))
      val attributesMapObj  = canonicalLookup.apply(attributesMapName)

      val attributesMap = attributesMapObj.asInstanceOf[ObjectType]
      attributesMap.properties.values.map(_.required).toList shouldBe List(false, false, false, false)

    }
  }

}
