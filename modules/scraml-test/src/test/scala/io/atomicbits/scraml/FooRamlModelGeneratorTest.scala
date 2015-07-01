/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import io.atomicbits.scraml.dsl.Response
import io.atomicbits.scraml.dsl.support.StringPart
import io.atomicbits.scraml.examples.TestClient01
import io.atomicbits.scraml.examples.TestClient01._
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, FeatureSpec}
import play.api.libs.json.Format
import play.libs.Json

import scala.concurrent.{Await, Future}
import scala.language.{postfixOps, reflectiveCalls}

import scala.concurrent.duration._

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
class FooRamlModelGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll {

  val port = 8281
  val host = "localhost"

  val wireMockServer = new WireMockServer(wireMockConfig().port(port))

  override def beforeAll() = {
    wireMockServer.start()
    WireMock.configureFor(host, port)
  }

  override def afterAll() = {
    wireMockServer.stop()
  }

  feature("Use the DSL based on a RAML specification") {

    val client = TestClient01(host = host, port = port,
      defaultHeaders = Map("Accept" -> "application/vnd-v1.0+json"))

    val userFoobarResource = client.rest.user.userid("foobar")

    scenario("test a GET request") {

      Given("a matching web service")

      stubFor(
        get(urlEqualTo(s"/rest/user/foobar?age=51.0&firstName=John"))
          .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
          .willReturn(
            aResponse()
              .withBody( """{"address": {"streetAddress": "Mulholland Drive", "city": "LA", "state": "California"}, "firstName":"John", "lastName": "Doe", "age": 21, "id": "1"}""")
              .withStatus(200)))


      When("execute a GET request")

      val eventualUserResponse: Future[User] =
        userFoobarResource
          .get(age = Some(51), firstName = Some("John"), lastName = None)
          .execToDto()


      Then("we should get the correct user object")

      val user = User(
        homePage = None,
        address = Some(Address("Mulholland Drive", "LA", "California")),
        age = 21,
        firstName = "John",
        lastName = "Doe",
        id = "1"
      )
      val userResponse = Await.result(eventualUserResponse, 2 seconds)
      assertResult(user)(userResponse)

    }


    scenario("test a form POST request") {

      Given("a matching web service")

      stubFor(
        post(urlEqualTo(s"/rest/user/foobar"))
          .withHeader("Content-Type", equalTo("application/x-www-form-urlencoded"))
          .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
          .withRequestBody(equalTo( """text=Hello%20Foobar"""))
          .willReturn(
            aResponse()
              .withBody("Post OK")
              .withStatus(200)
          )
      )



      When("execute a form POST request")

      val eventualPostResponse: Future[String] =
        userFoobarResource
          .post(text = "Hello Foobar", value = None).exec()



      Then("we should get the correct response")

      val postResponse = Await.result(eventualPostResponse, 2 seconds)
      assertResult("Post OK")(postResponse)

    }


    scenario("test a PUT request") {

      Given("a matching web service")

      val user = User(
        homePage = Some(Link("http://foo.bar", "GET", None)),
        address = Some(Address("Mulholland Drive", "LA", "California")),
        age = 21,
        firstName = "John",
        lastName = "Doe",
        id = "1"
      )

      val link = Link("http://foo.bar", "GET", None)

      import User._
      import Link._

      def userToJson()(implicit formatter: Format[User]) = {
        formatter.writes(user).toString()
      }

      def linkToJson()(implicit formatter: Format[Link]) = {
        formatter.writes(link).toString()
      }

      stubFor(
        put(urlEqualTo(s"/rest/user/foobar"))
          .withHeader("Content-Type", equalTo("application/vnd-v1.0+json"))
          .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
          .withRequestBody(equalTo(userToJson()))
          .willReturn(
            aResponse()
              .withBody(linkToJson())
              .withStatus(200)
          )
      )


      When("execute a PUT request")

      val eventualPutResponse: Future[Link] =
        userFoobarResource
          .put(user)
          .headers(
            "Content-Type" -> "application/vnd-v1.0+json",
            "Accept" -> "application/vnd-v1.0+json"
          )
          .execToDto()


      Then("we should get the correct response")

      val putResponse = Await.result(eventualPutResponse, 2 seconds)
      assertResult(link)(putResponse)

    }


    scenario("test a DELETE request") {

      Given("a matching web service")

      stubFor(
        delete(urlEqualTo(s"/rest/user/foobar"))
          .withHeader("Accept", equalTo("application/vnd-v1.0+json"))
          .willReturn(
            aResponse()
              .withBody("Delete OK")
              .withStatus(200)
          )
      )


      When("execute a DELETE request")

      val eventualPutResponse: Future[String] = userFoobarResource.delete().exec()


      Then("we should get the correct response")

      val putResponse = Await.result(eventualPutResponse, 2 seconds)
      assertResult("Delete OK")(putResponse)


    }

    scenario("test a multipart/form-data POST request") {

      Given("a form upload web service")
      stubFor(
        post(urlEqualTo(s"/rest/user/upload"))
          .withHeader("Content-Type", equalTo("multipart/form-data"))
          .willReturn(
            aResponse()
              .withBody("Post OK")
              .withStatus(200)
          )
      )

      When("a multipart/form-data POST request happens")
      val multipartFormPostResponse =
        client.rest.user.upload.post(List(StringPart(name = "test", value = "string part value"))).execToResponse()

      Then("we should get the correct response")


    }


  }

}
