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
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

class WithEnumGeneratorTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  import io.atomicbits.scraml.generator.platform.Platform._

  implicit val platform: ScalaPlay.type = ScalaPlay

  Feature("The scraml generator generates DSL classes suited for enums") {

    Scenario("test generated Scala DSL") {

      Given("a RAML specification")
      val apiResourceUrl = this.getClass.getClassLoader.getResource("withenum/EnumApi.raml")

      When("we generate the RAMl specification into class representations")
      implicit val platform: Platform = ScalaPlay(List("io", "atomicbits"))

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .buildGenerationAggr(
            ramlApiPath  = apiResourceUrl.toString,
            apiClassName = "EnumApi",
            platform
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
