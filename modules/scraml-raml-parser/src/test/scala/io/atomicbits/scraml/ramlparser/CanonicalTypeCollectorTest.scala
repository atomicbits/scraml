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

import io.atomicbits.scraml.ramlparser.lookup.{ CanonicalNameGenerator, CanonicalTypeCollector }
import io.atomicbits.scraml.ramlparser.model._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.{ ParsedArray, ParsedNumber, ParsedString, ParsedTypeReference }
import io.atomicbits.scraml.ramlparser.parser.RamlParser
import org.scalatest.Matchers._
import org.scalatest.{ BeforeAndAfterAll, FeatureSpec, GivenWhenThen }

import scala.language.postfixOps
import scala.util.Try

/**
  * Created by peter on 30/12/16.
  */
class CanonicalTypeCollectorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  feature("Collect the canonical representations of a simple fragmented json-schema definition") {

    scenario("test collecting of all canonical types") {

      Given("a RAML specification containing a json-schema definition with fragments")
      val defaultBasePath = List("io", "atomicbits", "schema")
      val parser          = RamlParser("/fragments/TestFragmentsApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml]          = parser.parse
      implicit val canonicalNameGenerator = CanonicalNameGenerator(defaultBasePath)
      val canonicalTypeCollector          = CanonicalTypeCollector(canonicalNameGenerator)

      Then("we all our relative fragment IDs and their references are expanded to absolute IDs")
      val raml                           = parsedModel.get
      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val fragments =
        canonicalLookup(CanonicalName.create(name = "Fragments", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      val foobarPointer = fragments.properties("foobarpointer").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobarPointer.genericType.isInstanceOf[NumberType.type] shouldBe true

      val foobarList = fragments.properties("foobarlist").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobarList.genericType.isInstanceOf[NumberType.type] shouldBe true

      val foobars = fragments.properties("foobars").asInstanceOf[Property[ArrayTypeReference]].ttype
      foobars.genericType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true
      foobars.genericType.asInstanceOf[NonPrimitiveTypeReference].refers shouldBe
        CanonicalName.create(name = "FragmentsDefinitionsBars", packagePath = List("io", "atomicbits", "schema"))

      val foo = fragments.properties("foo").asInstanceOf[Property[ArrayTypeReference]].ttype
      foo.genericType.isInstanceOf[NonPrimitiveTypeReference] shouldBe true
      foo.genericType.asInstanceOf[NonPrimitiveTypeReference].refers shouldBe
        CanonicalName.create(name = "FragmentsDefinitionsAddress", packagePath = List("io", "atomicbits", "schema"))

      val fragmentDefBars =
        canonicalLookup(CanonicalName.create(name = "FragmentsDefinitionsBars", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      fragmentDefBars.properties("baz").isInstanceOf[Property[StringType.type]] shouldBe true

      val fragmentDefAddress =
        canonicalLookup(CanonicalName.create(name = "FragmentsDefinitionsAddress", packagePath = List("io", "atomicbits", "schema")))
          .asInstanceOf[ObjectType]

      fragmentDefAddress.properties("city").isInstanceOf[Property[StringType.type]] shouldBe true
      fragmentDefAddress.properties("state").isInstanceOf[Property[StringType.type]] shouldBe true
      fragmentDefAddress.properties("zip").isInstanceOf[Property[IntegerType.type]] shouldBe true
      fragmentDefAddress.properties("streetAddress").isInstanceOf[Property[StringType.type]] shouldBe true
    }

  }

  feature("Collect the canonical representations of a complex and mixed json-schema/RAML1.0 definition") {

    scenario("test collecting json-schema types in a RAML model") {

      Given("a RAML specification containing json-schema definitions")
      val defaultBasePath = List("io", "atomicbits", "schema")
      val parser          = RamlParser("/raml08/TestApi.raml", "UTF-8", defaultBasePath)

      When("we parse the specification")
      val parsedModel: Try[Raml] = parser.parse
      val canonicalTypeCollector = CanonicalTypeCollector(CanonicalNameGenerator(defaultBasePath))

      Then("we get all expected canonical representations")
      val raml = parsedModel.get

      val (ramlUpdated, canonicalLookup) = canonicalTypeCollector.collect(raml)

      val pagedList              = CanonicalName.create(name = "PagedList", packagePath              = List("io", "atomicbits", "schema"))
      val method                 = CanonicalName.create(name = "Method", packagePath                 = List("io", "atomicbits", "schema"))
      val geometry               = CanonicalName.create(name = "Geometry", packagePath               = List("io", "atomicbits", "schema"))
      val point                  = CanonicalName.create(name = "Point", packagePath                  = List("io", "atomicbits", "schema"))
      val multiPoint             = CanonicalName.create(name = "MultiPoint", packagePath             = List("io", "atomicbits", "schema"))
      val lineString             = CanonicalName.create(name = "LineString", packagePath             = List("io", "atomicbits", "schema"))
      val multiLineString        = CanonicalName.create(name = "MultiLineString", packagePath        = List("io", "atomicbits", "schema"))
      val polygon                = CanonicalName.create(name = "Polygon", packagePath                = List("io", "atomicbits", "schema"))
      val multiPolygon           = CanonicalName.create(name = "MultiPolygon", packagePath           = List("io", "atomicbits", "schema"))
      val geometryCollection     = CanonicalName.create(name = "GeometryCollection", packagePath     = List("io", "atomicbits", "schema"))
      val crs                    = CanonicalName.create(name = "Crs", packagePath                    = List("io", "atomicbits", "schema"))
      val namedCrsProperty       = CanonicalName.create(name = "NamedCrsProperty", packagePath       = List("io", "atomicbits", "schema"))
      val animal                 = CanonicalName.create(name = "Animal", packagePath                 = List("io", "atomicbits", "schema"))
      val dog                    = CanonicalName.create(name = "Dog", packagePath                    = List("io", "atomicbits", "schema"))
      val cat                    = CanonicalName.create(name = "Cat", packagePath                    = List("io", "atomicbits", "schema"))
      val fish                   = CanonicalName.create(name = "Fish", packagePath                   = List("io", "atomicbits", "schema"))
      val userOther              = CanonicalName.create(name = "UserOther", packagePath              = List("io", "atomicbits", "schema"))
      val link                   = CanonicalName.create(name = "Link", packagePath                   = List("io", "atomicbits", "schema"))
      val book                   = CanonicalName.create(name = "Book", packagePath                   = List("io", "atomicbits", "schema"))
      val userDefinitionsAddress = CanonicalName.create(name = "UserDefinitionsAddress", packagePath = List("io", "atomicbits", "schema"))
      val user                   = CanonicalName.create(name = "User", packagePath                   = List("io", "atomicbits", "schema"))
      val author                 = CanonicalName.create(name = "Author", packagePath                 = List("io", "atomicbits", "schema"))
      val error                  = CanonicalName.create(name = "Error", packagePath                  = List("io", "atomicbits", "schema"))

      val expectedCanonicalNames = Set(
        pagedList,
        method,
        geometry,
        point,
        multiPoint,
        lineString,
        multiLineString,
        polygon,
        multiPolygon,
        geometryCollection,
        namedCrsProperty,
        crs,
        animal,
        dog,
        cat,
        fish,
        userOther,
        link,
        book,
        userDefinitionsAddress,
        user,
        author,
        error
      )

      val collectedCanonicalNames = canonicalLookup.map.map {
        case (canonicalName, theType) => canonicalName
      } toSet

      expectedCanonicalNames -- collectedCanonicalNames shouldBe Set.empty
      collectedCanonicalNames -- expectedCanonicalNames shouldBe Set.empty

      val dogType = canonicalLookup(dog).asInstanceOf[ObjectType]

      dogType.typeDiscriminator shouldBe Some("_type")
      dogType.typeDiscriminatorValue shouldBe Some("Dog")
      dogType.parents shouldBe List(NonPrimitiveTypeReference(refers = animal))
      dogType.properties("name") shouldBe Property(name    = "name", ttype    = StringType, required         = false, typeConstraints = None)
      dogType.properties("canBark") shouldBe Property(name = "canBark", ttype = BooleanType, typeConstraints = None)
      dogType.properties("gender") shouldBe Property(name  = "gender", ttype  = StringType, typeConstraints  = None)
      dogType.canonicalName shouldBe dog

      // Check the updated RAML model
      val restResource = ramlUpdated.resourceMap("rest")
      val userResource = restResource.resourceMap("user")

      val userGetAction     = userResource.actionMap(Get)
      val queryParameterMap = userGetAction.queryParameters.valueMap
      queryParameterMap("age").required shouldBe false
      queryParameterMap("age").parameterType.parsed.isInstanceOf[ParsedNumber] shouldBe true
      queryParameterMap("age").parameterType.canonical shouldBe Some(NumberType)
      queryParameterMap("organization").required shouldBe false
      queryParameterMap("organization").parameterType.parsed.isInstanceOf[ParsedArray] shouldBe true
      queryParameterMap("organization").parameterType.canonical shouldBe Some(ArrayTypeReference(genericType = StringType))
      val okResponse            = userGetAction.responses.responseMap(StatusCode("200"))
      val okResponseBodyContent = okResponse.body.contentMap(MediaType("application/vnd-v1.0+json"))
      okResponseBodyContent.bodyType.get.parsed.isInstanceOf[ParsedArray] shouldBe true
      okResponseBodyContent.bodyType.get.canonical shouldBe
        Some(
          ArrayTypeReference(genericType = NonPrimitiveTypeReference(refers = user))
        )

      val userIdResource      = userResource.resourceMap("userid")
      val userIdDogsResource  = userIdResource.resourceMap("dogs")
      val userIdDogsGetAction = userIdDogsResource.actionMap(Get)

      // Check the paged list type representation
      val pagedListTypeRepresentation =
        userIdDogsGetAction.responses.responseMap(StatusCode("200")).body.contentMap(MediaType("application/vnd-v1.0+json")).bodyType.get

      val parsedPagedListType: ParsedTypeReference = pagedListTypeRepresentation.parsed.asInstanceOf[ParsedTypeReference]
      parsedPagedListType.refersTo shouldBe RootId("http://atomicbits.io/schema/paged-list.json")
      val dogTypeReference = parsedPagedListType.genericTypes("T").asInstanceOf[ParsedTypeReference]
      dogTypeReference.refersTo shouldBe RootId("http://atomicbits.io/schema/dog.json")
      parsedPagedListType.genericTypes("U").isInstanceOf[ParsedString] shouldBe true

      val canonicalPagedListType: NonPrimitiveTypeReference =
        pagedListTypeRepresentation.canonical.get.asInstanceOf[NonPrimitiveTypeReference]
      canonicalPagedListType.genericTypes(TypeParameter("T")).isInstanceOf[NonPrimitiveTypeReference]
      canonicalPagedListType.genericTypes(TypeParameter("U")).isInstanceOf[NonPrimitiveTypeReference]

      // Check the paged list type model
      val pagedListType = canonicalLookup(pagedList).asInstanceOf[ObjectType]
      pagedListType.properties("elements") shouldBe
        Property(name = "elements", ttype = ArrayTypeReference(genericType = TypeParameter("T")), required = true, typeConstraints = None)

      val userType = canonicalLookup(user).asInstanceOf[ObjectType]
      userType.properties("address").required shouldBe false

    }

  }

}
