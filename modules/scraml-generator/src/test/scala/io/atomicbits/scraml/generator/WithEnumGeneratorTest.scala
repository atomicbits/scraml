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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.Matchers._

class WithEnumGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  import io.atomicbits.scraml.generator.platform.Platform._

  implicit val platform = ScalaPlay

  feature("The scraml generator generates DSL classes suited for enums") {

    scenario("test generated Scala DSL") {

      Given("a RAML specification")
      val apiResourceUrl = this.getClass.getClassLoader.getResource("withenum/EnumApi.raml")

      When("we generate the RAMl specification into class representations")
      val generationAggr: GenerationAggr =
        ScramlGenerator
          .buildGenerationAggr(
            ramlApiPath    = apiResourceUrl.toString,
            apiPackageName = "io.atomicbits",
            apiClassName   = "EnumApi",
            ScalaPlay
          )
          .generate

      Then("we should get valid class representations")
      val generatedClasses = generationAggr.sourceDefinitionsProcessed.map(_.classReference.fullyQualifiedName).toSet

      val expectedClasses = Set(
        "io.atomicbits.EnumApi",
        "io.atomicbits.rest.RestResource",
        "io.atomicbits.rest.withenum.WithEnumResource",
        "io.atomicbits.schema.WithEnum",
        "io.atomicbits.schema.WithEnumMethod"
      )

      generatedClasses -- expectedClasses shouldBe Set.empty
      expectedClasses -- generatedClasses shouldBe Set.empty
    }

  }

}
