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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.concurrent.ScalaFutures
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.featurespec.AnyFeatureSpec

/**
  * Created by peter on 12/03/17.
  */
class MultipleAcceptHeadersTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  Feature("A RAML model may have multiple equal or different content type or accept headers") {

    Scenario("test multiple equal accept headers in a RAML model") {

      Given("a json-schema containing an object hierarcy")
      val packageBasePath = List("io", "atomicbits")

      When("we generate the RAMl specification into a resource DSL")
      implicit val platform: ScalaPlay = ScalaPlay(packageBasePath)

      val raml: Raml = RamlParser("multipleacceptheaders/TestMultipleAcceptHeaders.raml", "UTF-8").parse.get

      val (ramlExp, canonicalLookup) = raml.collectCanonicals(packageBasePath)

      val generationAggregator: GenerationAggr =
        GenerationAggr(apiName        = "TestMultipleAcceptHeaders",
                       apiBasePackage = packageBasePath,
                       raml           = ramlExp,
                       canonicalToMap = canonicalLookup.map)
      generationAggregator.generate

      Then("we should get valid DSL code in the presense of multple accept headers")

    }
  }

}
