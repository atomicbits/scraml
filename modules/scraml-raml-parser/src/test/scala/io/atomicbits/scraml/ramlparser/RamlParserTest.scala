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

import io.atomicbits.scraml.ramlparser.model.types._
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import io.atomicbits.util.TestUtils
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, GivenWhenThen}
import org.scalatest.Matchers._

import scala.language.postfixOps
import scala.util.Try


/**
  * Created by peter on 6/02/16.
  */
class RamlParserTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("RAML parser") {

    scenario("test parsing fragments in json-schema") {

      Given("a json-schema containing fragments")
      val parser = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", List("io", "atomicbits", "model"))

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a properly parsed fragments object")
      val raml = parsedModel.get

      val objectWithFragments: ObjectType = raml.types.typeReferences(NativeId("myfragments")).asInstanceOf[ObjectType]

      val barType: Type = objectWithFragments.fragments.fragmentMap("bar")

      barType shouldBe a[TypeReference]
      barType.asInstanceOf[TypeReference].refersTo shouldBe NativeId("baz")


      val definitionFragment: Fragments = objectWithFragments.fragments.fragmentMap("definitions").asInstanceOf[Fragments]

      val addressType = definitionFragment.fragmentMap("address")
      addressType shouldBe a[ObjectType]
      val address = addressType.asInstanceOf[ObjectType]

      address.id shouldBe FragmentId(List("definitions", "address"))
      address.properties("city") shouldBe a[StringType]
      address.properties("state") shouldBe a[StringType]
      address.properties("zip") shouldBe a[IntegerType]
      address.properties("streetAddress") shouldBe a[StringType]

      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")
    }


    scenario("test parsing json-schema types in a RAML 1.0 model") {

      Given("a RAML 1.0 specification with json-schema types")
      val parser = RamlParser("/json-schema-types/TestApi.raml", "UTF-8", List("io", "atomicbits", "model"))

      When("we parse the specification")
      val parsedModel = parser.parse

      Then("we get a ...")
      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")
    }


    scenario("test parsing query parameters in a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
      val parser = RamlParser("/raml08/TestApi.raml", "UTF-8", List("io", "atomicbits", "schemas"))

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a ...")
      val raml = parsedModel.get
      val restResource: Resource = raml.resources.filter(_.urlSegment == "rest").head
      val userResource: Resource = restResource.resources.filter(_.urlSegment == "user").head
      val getAction: Action = userResource.actions.filter(_.actionType == Get).head

      val organizationQueryParameter: Parameter = getAction.queryParameters.byName("organization").get

      organizationQueryParameter.parameterType shouldBe a[ArrayType]

      organizationQueryParameter.parameterType.asInstanceOf[ArrayType].items shouldBe a[StringType]


      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //       println(s"Parsed raml: $prettyModel")

    }


    scenario("test resource paths in a complex RAML 1.0 model") {

      Given("a RAML 1.0 specification")
      val parser = RamlParser("/raml08/TestApi.raml", "UTF-8", List("io", "atomicbits", "schemas"))

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse

      Then("we get a ...")
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
          List("rest", "user"), // X
          List("rest", "user", "upload"),
          List("rest", "user", "activate"),
          List("rest", "user", "{void}", "location"),
          List("rest", "user", "{userid}"), // X
          List("rest", "user", "{userid}", "dogs"),
          List("rest", "animals"), // X
          List("rest", "animals", "datafile", "upload"),
          List("rest", "animals", "datafile", "download")
        )

      collectedResources.size shouldEqual expectedResources.size
      expectedResources.foreach { expected =>
        collectedResources should contain(expected)
      }


      //      val prettyModel = TestUtils.prettyPrint(parsedModel)
      //      println(s"Parsed raml: $prettyModel")

    }


  }


}
