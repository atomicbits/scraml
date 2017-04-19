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
      val apiLocation = "objecthierarchy/TestObjectHierarchyApi.raml"

      When("we generate the RAMl specification into class representations")
      implicit val platform = ScalaPlay

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .buildGenerationAggr(
            ramlApiPath     = apiLocation,
            packageBasePath = List("io", "atomicbits", "scraml"),
            apiClassName    = "TestObjectHierarchyApi",
            ScalaPlay
          )
          .generate

      Then("we should get valid a class hierarchy")

      val animalToClassName = CanonicalName("Animal", List("io", "atomicbits", "schema"))
      val catToClassName    = CanonicalName("Cat", List("io", "atomicbits", "schema"))
      val dogToClassName    = CanonicalName("Dog", List("io", "atomicbits", "schema"))
      val fishToClassName   = CanonicalName("Fish", List("io", "atomicbits", "schema"))

      val animalToDef: TransferObjectClassDefinition = generationAggr.toMap(animalToClassName)

      val animalChildren: Set[CanonicalName] = generationAggr.directChildren(animalToClassName)

      animalChildren should contain(catToClassName)
      animalChildren should contain(dogToClassName)
      animalChildren should contain(fishToClassName)

      generationAggr.directParents(animalToClassName) shouldBe Set.empty
      generationAggr.directParents(catToClassName) shouldBe Set(animalToClassName)
      generationAggr.directParents(dogToClassName) shouldBe Set(animalToClassName)
      generationAggr.directParents(fishToClassName) shouldBe Set(animalToClassName)
    }

    scenario("test generated Scala DSL") {

      Given("a RAML specification")
      val apiLocation = "io/atomicbits/scraml/TestApi.raml"

      When("we generate the RAMl specification into class representations")
      implicit val platform = ScalaPlay

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .buildGenerationAggr(
            ramlApiPath     = apiLocation,
            packageBasePath = List("io", "atomicbits", "scraml"),
            apiClassName    = "TestApi",
            platform
          )
          .generate

      Then("we should get valid class representations")

      val generatedFilePaths =
        generationAggr.sourceFilesGenerated
          .map(_.filePath.toString)
          .toSet

      val expectedFilePaths = Set(
        "io/atomicbits/scraml/TestApi.scala",
        "io/atomicbits/scraml/rest/RestResource.scala",
        "io/atomicbits/scraml/rest/user/UserResource.scala",
        "io/atomicbits/scraml/rest/user/userid/dogs/DogsResource.scala",
        "io/atomicbits/scraml/rest/user/userid/UseridResource.scala",
        "io/atomicbits/scraml/rest/user/userid/AcceptApplicationVndV01JsonHeaderSegment.scala",
        "io/atomicbits/scraml/rest/user/userid/AcceptApplicationVndV10JsonHeaderSegment.scala",
        "io/atomicbits/scraml/rest/user/userid/ContentApplicationVndV01JsonHeaderSegment.scala",
        "io/atomicbits/scraml/rest/user/userid/ContentApplicationVndV10JsonHeaderSegment.scala",
        "io/atomicbits/scraml/rest/user/upload/UploadResource.scala",
        "io/atomicbits/scraml/rest/user/activate/ActivateResource.scala",
        "io/atomicbits/scraml/rest/animals/AnimalsResource.scala",
        "io/atomicbits/schema/User.scala",
        "io/atomicbits/schema/UserDefinitionsAddress.scala",
        "io/atomicbits/schema/Link.scala",
        "io/atomicbits/schema/PagedList.scala",
        "io/atomicbits/schema/Animal.scala",
        "io/atomicbits/schema/AnimalImpl.scala",
        "io/atomicbits/schema/Dog.scala",
        "io/atomicbits/schema/Cat.scala",
        "io/atomicbits/schema/Fish.scala",
        "io/atomicbits/schema/Method.scala",
        "io/atomicbits/schema/Geometry.scala",
        "io/atomicbits/schema/GeometryImpl.scala",
        "io/atomicbits/schema/Point.scala",
        "io/atomicbits/schema/LineString.scala",
        "io/atomicbits/schema/MultiPoint.scala",
        "io/atomicbits/schema/MultiLineString.scala",
        "io/atomicbits/schema/Polygon.scala",
        "io/atomicbits/schema/MultiPolygon.scala",
        "io/atomicbits/schema/GeometryCollection.scala",
        "io/atomicbits/schema/Crs.scala",
        "io/atomicbits/schema/NamedCrsProperty.scala",
        "io/atomicbits/scraml/rest/user/void/VoidResource.scala",
        "io/atomicbits/scraml/rest/user/void/location/LocationResource.scala",
        "io/atomicbits/scraml/Book.scala",
        "io/atomicbits/scraml/Author.scala",
        "io/atomicbits/scraml/books/BooksResource.scala"
      )

      generatedFilePaths -- expectedFilePaths shouldBe Set.empty
      expectedFilePaths -- generatedFilePaths shouldBe Set.empty

      val geometryToClassName = CanonicalName("Geometry", List("io", "atomicbits", "schema"))

      val bboxFieldClassPointer = generationAggr.toMap(geometryToClassName).fields.filter(_.fieldName == "bbox").head.classPointer

      bboxFieldClassPointer shouldBe ListClassPointer(DoubleClassPointer(primitive = false))
    }

    scenario("test generated Java DSL") {

      Given("a RAML specification")
      val apiLocation = "io/atomicbits/scraml/TestApi.raml"

      When("we generate the RAMl specification into class representations")
      implicit val platform = JavaJackson

      val generationAggr: GenerationAggr =
        ScramlGenerator
          .buildGenerationAggr(
            ramlApiPath     = apiLocation,
            packageBasePath = List("io", "atomicbits", "scraml"),
            apiClassName    = "TestApi",
            platform
          )
          .generate

      Then("we should get valid class representations")

      val generatedFilePaths =
        generationAggr.sourceFilesGenerated
          .map(_.filePath.toString)
          .toSet

      val expectedFilePaths = Set(
        "io/atomicbits/scraml/TestApi.java",
        "io/atomicbits/scraml/rest/RestResource.java",
        "io/atomicbits/scraml/rest/user/UserResource.java",
        "io/atomicbits/scraml/rest/user/userid/dogs/DogsResource.java",
        "io/atomicbits/scraml/rest/user/userid/UseridResource.java",
        "io/atomicbits/scraml/rest/user/userid/AcceptApplicationVndV01JsonHeaderSegment.java",
        "io/atomicbits/scraml/rest/user/userid/AcceptApplicationVndV10JsonHeaderSegment.java",
        "io/atomicbits/scraml/rest/user/userid/ContentApplicationVndV01JsonHeaderSegment.java",
        "io/atomicbits/scraml/rest/user/userid/ContentApplicationVndV10JsonHeaderSegment.java",
        "io/atomicbits/scraml/rest/user/upload/UploadResource.java",
        "io/atomicbits/scraml/rest/user/activate/ActivateResource.java",
        "io/atomicbits/scraml/rest/animals/AnimalsResource.java",
        "io/atomicbits/schema/User.java",
        "io/atomicbits/schema/UserDefinitionsAddress.java",
        "io/atomicbits/schema/Link.java",
        "io/atomicbits/schema/PagedList.java",
        // "io/atomicbits/schema/AnimalImpl.java", // Java Pojo can extend one another, the interfaces are introduced only when multiple-inheritance is involved!
        "io/atomicbits/schema/Animal.java",
        "io/atomicbits/schema/Dog.java",
        "io/atomicbits/schema/Cat.java",
        "io/atomicbits/schema/Fish.java",
        "io/atomicbits/schema/Method.java",
        // "io/atomicbits/schema/GeometryImpl.java",
        "io/atomicbits/schema/Geometry.java",
        "io/atomicbits/schema/Point.java",
        "io/atomicbits/schema/LineString.java",
        "io/atomicbits/schema/MultiPoint.java",
        "io/atomicbits/schema/MultiLineString.java",
        "io/atomicbits/schema/Polygon.java",
        "io/atomicbits/schema/MultiPolygon.java",
        "io/atomicbits/schema/GeometryCollection.java",
        "io/atomicbits/schema/Crs.java",
        "io/atomicbits/schema/NamedCrsProperty.java",
        "io/atomicbits/scraml/rest/user/voidesc/VoidResource.java",
        "io/atomicbits/scraml/rest/user/voidesc/location/LocationResource.java",
        "io/atomicbits/scraml/Book.java",
        "io/atomicbits/scraml/Author.java",
        "io/atomicbits/scraml/books/BooksResource.java"
      )

      generatedFilePaths -- expectedFilePaths shouldBe Set.empty
      expectedFilePaths -- generatedFilePaths shouldBe Set.empty
    }

  }
}
