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

import io.atomicbits.scraml.ramlparser.model.parsedtypes._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.Matchers._
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.util.Try

/**
  * Created by peter on 1/11/16.
  */
class QueryParameterParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("query parameter parsing") {

    scenario("test parsing query parameters in a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
      val parser = RamlParser("/raml08/TestApi.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get the expected query parameters")
      val raml                   = parsedModel.get
      val restResource: Resource = raml.resources.filter(_.urlSegment == "rest").head
      val userResource: Resource = restResource.resources.filter(_.urlSegment == "user").head
      val getAction: Action      = userResource.actions.filter(_.actionType == Get).head

      val organizationQueryParameter: Parameter = getAction.queryParameters.byName("organization").get

      organizationQueryParameter.parameterType.parsed shouldBe a[ParsedArray]

      organizationQueryParameter.parameterType.parsed.asInstanceOf[ParsedArray].items shouldBe a[ParsedString]
      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")

    }

  }

}
