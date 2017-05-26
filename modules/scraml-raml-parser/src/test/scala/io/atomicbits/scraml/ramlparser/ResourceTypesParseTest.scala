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

package io.atomicbits.scraml.ramlparser

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 26/05/17.
  */
class ResourceTypesParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  scenario("test the application of resourceTypes in a RAML 1.0 model") {

    Given("a RAML 1.0 specification with a resourceTypes definition")
    val parser = RamlParser("/resourcetypes/zoo-api.raml", "UTF-8", List("io", "atomicbits", "zoo"))

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

}
