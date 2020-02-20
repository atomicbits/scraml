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

import io.atomicbits.scraml.ramlparser.lookup.{CanonicalNameGenerator, CanonicalTypeCollector}
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{StringType, TypeReference}
import io.atomicbits.scraml.ramlparser.model.{Parameter, Raml, Resource}
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.should.Matchers._
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen}

import scala.util.Try

/**
  * Created by peter on 1/11/16.
  */
class ResourcePathsParseTest extends AnyFeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  Feature("resource paths parsing") {

    Scenario("test resource paths in a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
      val defaultBasePath = List("io", "atomicbits", "schema")
      val parser = RamlParser("/raml08/TestApi.raml", "UTF-8")

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
          List("rest", "user", "resourcetrait"),
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

      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector = CanonicalTypeCollector(canonicalNameGenerator)
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val userIdUrlParameter: Option[Parameter] =
        ramlUpdated.resourceMap("rest").resourceMap("user").resourceMap("userid").urlParameter

      userIdUrlParameter.isDefined shouldBe true
      val canonicalUrlParamTypeRef: Option[TypeReference] = userIdUrlParameter.flatMap(_.parameterType.canonical)
      canonicalUrlParamTypeRef shouldBe Some(StringType)

      // val prettyModel = TestUtils.prettyPrint(parsedModel)
      //            println(s"Parsed raml: $prettyModel")
    }

  }

}
