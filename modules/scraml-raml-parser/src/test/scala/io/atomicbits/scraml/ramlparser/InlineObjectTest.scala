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
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedObject
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

import scala.util.Try

/**
  * Created by peter on 12/03/17.
  */
class InlineObjectTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("Collect the canonical representations of a simple fragmented json-schema definition") {

    Scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "model")
      val parser          = RamlParser("/inlineobject/test-api.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator: CanonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
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
