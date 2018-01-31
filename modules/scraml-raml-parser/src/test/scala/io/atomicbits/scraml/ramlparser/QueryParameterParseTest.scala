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
