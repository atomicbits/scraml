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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.javajackson.JavaJackson
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._
import org.scalatest.Matchers._

/**
  * Created by peter on 10/09/15.
  */
class GeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

  import io.atomicbits.scraml.generator.platform.Platform._

  feature("The scraml generator generates DSL classes") {

    scenario("test the generation of an object hierarchy") {

      Given("a json-schema containing an object hierarcy")
      val apiResourceUrl = this.getClass.getClassLoader.getResource("objecthierarchy/TestObjectHierarchyApi.raml")

      When("we generate the RAMl specification into class representations")
      implicit val platform = ScalaPlay

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .generateSourceFiles(
            ramlApiPath    = apiResourceUrl.toString,
            apiPackageName = "io.atomicbits.scraml",
            apiClassName   = "TestObjectHierarchyApi",
            ScalaPlay
          )
          .generate

      Then("we should get valid a class hierarchy")

      val animalToClassName = CanonicalName("Animal", List("io", "atomicbits", "schema"))
      val catToClassName    = CanonicalName("Cat", List("io", "atomicbits", "schema"))
      val dogToClassName    = CanonicalName("Dog", List("io", "atomicbits", "schema"))
      val fishToClassName   = CanonicalName("Fish", List("io", "atomicbits", "schema"))

      val animalToDef: TransferObjectClassDefinition = generationAggr.toMap(animalToClassName)

      val animalChildren: Set[CanonicalName] = generationAggr.children(animalToClassName)

      animalChildren should contain(catToClassName)
      animalChildren should contain(dogToClassName)
      animalChildren should contain(fishToClassName)

      generationAggr.parents(animalToClassName) shouldBe Set.empty
      generationAggr.parents(catToClassName) shouldBe Set(animalToClassName)
      generationAggr.parents(dogToClassName) shouldBe Set(animalToClassName)
      generationAggr.parents(fishToClassName) shouldBe Set(animalToClassName)
    }

    scenario("test generated Scala DSL") {

      Given("a RAML specification")
      val apiResourceUrl = this.getClass.getClassLoader.getResource("io/atomicbits/scraml/TestApi.raml")

      When("we generate the RAMl specification into class representations")
      implicit val platform = ScalaPlay

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .generateSourceFiles(
            ramlApiPath    = apiResourceUrl.toString,
            apiPackageName = "io.atomicbits.scraml",
            apiClassName   = "TestApi",
            platform
          )
          .generate

      Then("we should get valid class representations")

      val generatedClasses = generationAggr.sourceDefinitionsProcessed.map(_.classReference.fullyQualifiedName).toSet

      val expectedClasses = Set(
        "io.atomicbits.scraml.TestApi",
        "io.atomicbits.scraml.rest.RestResource",
        "io.atomicbits.scraml.rest.user.UserResource",
        "io.atomicbits.scraml.rest.user.userid.dogs.DogsResource",
        "io.atomicbits.scraml.rest.user.userid.UseridResource",
        "io.atomicbits.scraml.rest.user.userid.AcceptApplicationVndV01JsonHeaderSegment",
        "io.atomicbits.scraml.rest.user.userid.AcceptApplicationVndV10JsonHeaderSegment",
        "io.atomicbits.scraml.rest.user.userid.ContentApplicationVndV01JsonHeaderSegment",
        "io.atomicbits.scraml.rest.user.userid.ContentApplicationVndV10JsonHeaderSegment",
        "io.atomicbits.scraml.rest.user.upload.UploadResource",
        "io.atomicbits.scraml.rest.user.activate.ActivateResource",
        "io.atomicbits.scraml.rest.animals.AnimalsResource",
        "io.atomicbits.schema.User",
        "io.atomicbits.schema.UserOther",
        "io.atomicbits.schema.UserDefinitionsAddress",
        "io.atomicbits.schema.Link",
        "io.atomicbits.schema.PagedList",
        "io.atomicbits.schema.Animal",
        "io.atomicbits.schema.AnimalTrait",
        "io.atomicbits.schema.Dog",
        "io.atomicbits.schema.Cat",
        "io.atomicbits.schema.Fish",
        "io.atomicbits.schema.Method",
        "io.atomicbits.schema.Geometry",
        "io.atomicbits.schema.GeometryTrait",
        "io.atomicbits.schema.Point",
        "io.atomicbits.schema.LineString",
        "io.atomicbits.schema.MultiPoint",
        "io.atomicbits.schema.MultiLineString",
        "io.atomicbits.schema.Polygon",
        "io.atomicbits.schema.MultiPolygon",
        "io.atomicbits.schema.GeometryCollection",
        "io.atomicbits.schema.Crs",
        "io.atomicbits.schema.NamedCrsProperty",
        "io.atomicbits.scraml.rest.user.void.VoidResource",
        "io.atomicbits.scraml.rest.user.void.location.LocationResource",
        "io.atomicbits.scraml.Book",
        "io.atomicbits.scraml.Author",
        "io.atomicbits.scraml.books.BooksResource"
      )

      generatedClasses -- expectedClasses shouldBe Set.empty
      expectedClasses -- generatedClasses shouldBe Set.empty

      val geometryToClassName = CanonicalName("Geometry", List("io", "atomicbits", "schema"))

      val bboxFieldClassPointer = generationAggr.toMap(geometryToClassName).fields.filter(_.fieldName == "bbox").head.classPointer

      bboxFieldClassPointer shouldBe ListClassReference(DoubleClassReference(primitive = false))
    }

    ignore("We ignore this test as long as the Java implementation of the code generation is missing.") {
      scenario("test generated Java DSL") {

        Given("a RAML specification")
        val apiResourceUrl = this.getClass.getClassLoader.getResource("io/atomicbits/scraml/TestApi.raml")

        When("we generate the RAMl specification into class representations")
        implicit val platform = JavaJackson

        val generationAggr: GenerationAggr =
          ScramlGenerator
            .generateSourceFiles(
              ramlApiPath    = apiResourceUrl.toString,
              apiPackageName = "io.atomicbits.scraml",
              apiClassName   = "TestApi",
              platform
            )
            .generate

        Then("we should get valid class representations")

        val generatedClasses = generationAggr.sourceDefinitionsProcessed.map(_.classReference.fullyQualifiedName).toSet

        val expectedClasses = Set(
          "io.atomicbits.scraml.TestApi",
          "io.atomicbits.scraml.rest.RestResource",
          "io.atomicbits.scraml.rest.user.UserResource",
          "io.atomicbits.scraml.rest.user.userid.dogs.DogsResource",
          "io.atomicbits.scraml.rest.user.userid.UseridResource",
          "io.atomicbits.scraml.rest.user.userid.AcceptApplicationVndV01JsonHeaderSegment",
          "io.atomicbits.scraml.rest.user.userid.AcceptApplicationVndV10JsonHeaderSegment",
          "io.atomicbits.scraml.rest.user.userid.ContentApplicationVndV01JsonHeaderSegment",
          "io.atomicbits.scraml.rest.user.userid.ContentApplicationVndV10JsonHeaderSegment",
          "io.atomicbits.scraml.rest.user.upload.UploadResource",
          "io.atomicbits.scraml.rest.user.activate.ActivateResource",
          "io.atomicbits.scraml.rest.animals.AnimalsResource",
          "io.atomicbits.schema.User",
          "io.atomicbits.schema.UserOther",
          "io.atomicbits.schema.UserDefinitionsAddress",
          "io.atomicbits.schema.Link",
          "io.atomicbits.schema.PagedList",
          "io.atomicbits.schema.AnimalImpl",
          "io.atomicbits.schema.Animal",
          "io.atomicbits.schema.Dog",
          "io.atomicbits.schema.Cat",
          "io.atomicbits.schema.Fish",
          "io.atomicbits.schema.Method",
          "io.atomicbits.schema.GeometryImpl",
          "io.atomicbits.schema.Point",
          "io.atomicbits.schema.LineString",
          "io.atomicbits.schema.MultiPoint",
          "io.atomicbits.schema.MultiLineString",
          "io.atomicbits.schema.Polygon",
          "io.atomicbits.schema.MultiPolygon",
          "io.atomicbits.schema.GeometryCollection",
          "io.atomicbits.schema.Crs",
          "io.atomicbits.schema.NamedCrsProperty",
          "io.atomicbits.scraml.rest.user.voidesc.VoidResource",
          "io.atomicbits.scraml.rest.user.voidesc.location.LocationResource",
          "io.atomicbits.scraml.Book",
          "io.atomicbits.scraml.Author",
          "io.atomicbits.scraml.books.BooksResource"
        )

        generatedClasses -- expectedClasses shouldBe Set.empty
        expectedClasses -- generatedClasses shouldBe Set.empty
      }
    }

  }
}
