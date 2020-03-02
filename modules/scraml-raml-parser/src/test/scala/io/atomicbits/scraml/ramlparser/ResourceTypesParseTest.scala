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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, GivenWhenThen }
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._

import scala.util.Try

/**
  * Created by peter on 26/05/17.
  */
class ResourceTypesParseTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("test the application of resourceTypes in a RAML 1.0 model") {

    Scenario("test the application of unparameterized resourceTypes in a RAML 1.0 model") {

      Given("a RAML 1.0 specification with a resourceTypes definition")
      val parser = RamlParser("/resourcetypes/zoo-api.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get the expanded resources")
      val raml = parsedModel.get

      val zooResource: Resource                  = raml.resourceMap("zoo")
      val zooAnimalsResource: Resource           = zooResource.resourceMap("animals")
      val zooAnimalGetAction: Action             = zooAnimalsResource.actionMap(Get)
      val zooAnimalGetResponse: Option[Response] = zooAnimalGetAction.responses.responseMap.get(StatusCode("200"))
      val zooAnimalGetBodyContentOpt             = zooAnimalGetResponse.get.body.contentMap.get(MediaType("application/json"))
      zooAnimalGetBodyContentOpt should not be None
      val zooAnimalPostActionOpt: Option[Action] = zooAnimalsResource.actionMap.get(Post)
      zooAnimalPostActionOpt shouldBe None

      val animalsResource: Resource           = raml.resourceMap("animals")
      val animalGetAction: Action             = animalsResource.actionMap(Get)
      val animalGetResponse: Option[Response] = animalGetAction.responses.responseMap.get(StatusCode("200"))
      val animalGetBodyContentOpt             = animalGetResponse.get.body.contentMap.get(MediaType("application/json"))
      animalGetBodyContentOpt should not be None
      val animalPostActionOpt: Option[Action] = animalsResource.actionMap.get(Post)
      animalPostActionOpt should not be None

    }

    Scenario("test the application of parameterized resourceTypes in a RAML 1.0 model") {

      Given("a RAML 1.0 specification with a parameterized resourceTypes definition")
      val parser = RamlParser("/resourcetypes/zoo-api.raml", "UTF-8")

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get the expanded resources with all parameters filled in")
      val raml = parsedModel.get

      val zooKeepersResource: Resource = raml.resourceMap("zookeepers")
      val zooKeepersGetAction: Action  = zooKeepersResource.actionMap(Get)
      val allQueryParameters           = zooKeepersGetAction.queryParameters.values.map(_.name).toSet
      allQueryParameters shouldBe Set("title", "digest_all_fields", "access_token", "numPages", "zookeepers")

    }

  }

}
