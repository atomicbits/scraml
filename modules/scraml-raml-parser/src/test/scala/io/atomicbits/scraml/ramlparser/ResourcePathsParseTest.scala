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

import io.atomicbits.scraml.ramlparser.model.{Raml, Resource}
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.Matchers._
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}

import scala.util.Try

/**
  * Created by peter on 1/11/16.
  */
class ResourcePathsParseTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("resource paths parsing") {

    scenario("test resource paths in a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
      val parser = RamlParser("/raml08/TestApi.raml", "UTF-8", List("io", "atomicbits", "schemas"))

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a series of resource paths")
      val raml = parsedModel.get

      // collect resource paths

      def collectResources(resources: List[Resource]): List[List[String]] = {

        resources.flatMap(collectResourcePaths(_, List.empty))
      }

      def collectResourcePaths(currentResource: Resource, currentPath: List[String]): List[List[String]] = {

        val currentSegment =
          currentResource.urlParameter.map(param => s"{${currentResource.urlSegment}}").getOrElse(currentResource.urlSegment)

        val nextPath = currentPath :+ currentSegment

        currentResource.resources match {
          case Nil => List(nextPath)
          case rs  =>
            if (currentResource.actions.isEmpty) rs.flatMap(collectResourcePaths(_, nextPath))
            else nextPath :: rs.flatMap(collectResourcePaths(_, nextPath))
        }

      }


      val collectedResources = collectResources(raml.resources)
      //      println(s"collected resources:\n$collectedResources")

      val expectedResources =
        Set(
          List("rest", "user"),
          List("rest", "user", "upload"),
          List("rest", "user", "activate"),
          List("rest", "user", "{void}", "location"),
          List("rest", "user", "{userid}"),
          List("rest", "user", "{userid}", "dogs"),
          List("rest", "animals"),
          List("rest", "animals", "datafile", "upload"),
          List("rest", "animals", "datafile", "download"),
          List("rest", "books")
        )

      collectedResources.size shouldEqual expectedResources.size
      expectedResources.foreach { expected =>
        collectedResources should contain(expected)
      }

      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //            println(s"Parsed raml: $prettyModel")
    }

  }

}
