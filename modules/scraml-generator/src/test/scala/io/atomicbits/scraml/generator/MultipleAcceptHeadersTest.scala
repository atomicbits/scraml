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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.concurrent.ScalaFutures
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.parser.RamlParser

import scala.util.Try

/**
  * Created by peter on 12/03/17.
  */
class MultipleAcceptHeadersTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  feature("A RAML model may have multiple equal or different content type or accept headers") {

    scenario("test multiple equal accept headers in a RAML model") {

      Given("a json-schema containing an object hierarcy")
      val packageBasePath = List("io", "atomicbits")

      When("we generate the RAMl specification into a resource DSL")
      implicit val platform = ScalaPlay(packageBasePath)

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
