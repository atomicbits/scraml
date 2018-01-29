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

import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }
import org.scalatest.Matchers._

import scala.util.Try

/**
  * Created by peter on 1/11/16.
  */
class TraitsParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  scenario("test the application of traits in a complex RAML 1.0 model") {

    Given("a RAML 1.0 specification with a traits definition")
    val parser = RamlParser("/raml08/TestApi.raml", "UTF-8")

    When("we parse the specification")
    val parsedModel: Try[Raml] = parser.parse

    Then("we get all four actions in the userid resource")
    val raml                   = parsedModel.get
    val restResource: Resource = raml.resourceMap("rest")
    val userResource: Resource = restResource.resourceMap("user")

    val uploadResource: Resource = userResource.resourceMap("upload")
    val uploadPostAction: Action = uploadResource.actionMap(Post)
    val response401Opt           = uploadPostAction.responses.responseMap.get(StatusCode("401"))
    response401Opt should not be None
    val bodyContentOpt = response401Opt.get.body.contentMap.get(MediaType("application/json"))
    bodyContentOpt should not be None

    val resourcetraitResource: Resource = userResource.resourceMap("resourcetrait")

    val getAction: Action = resourcetraitResource.actionMap(Get)
    val getResponse401Opt = getAction.responses.responseMap.get(StatusCode("401"))
    getResponse401Opt should not be None
    val getBodyContentOpt = getResponse401Opt.get.body.contentMap.get(MediaType("application/json"))
    getBodyContentOpt should not be None

    val putAction: Action = resourcetraitResource.actionMap(Put)
    val putResponse401Opt = putAction.responses.responseMap.get(StatusCode("401"))
    putResponse401Opt should not be None
    val putBodyContentOpt = putResponse401Opt.get.body.contentMap.get(MediaType("application/json"))
    putBodyContentOpt should not be None

    val postAction: Action = resourcetraitResource.actionMap(Post)
    val postResponse401Opt = postAction.responses.responseMap.get(StatusCode("401"))
    postResponse401Opt should not be None
    val postBodyContentOpt = postResponse401Opt.get.body.contentMap.get(MediaType("application/existing+json"))
    postBodyContentOpt should not be None

    val deleteAction: Action = resourcetraitResource.actionMap(Delete)
    val deleteResponse401Opt = deleteAction.responses.responseMap.get(StatusCode("401"))
    deleteResponse401Opt should not be None
    val deleteBodyContentOpt = deleteResponse401Opt.get.body.contentMap.get(MediaType("application/alternative+json"))
    deleteBodyContentOpt should not be None

  }

}
