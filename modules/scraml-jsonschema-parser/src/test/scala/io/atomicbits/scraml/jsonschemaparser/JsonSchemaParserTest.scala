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

package io.atomicbits.scraml.jsonschemaparser

import io.atomicbits.scraml.jsonschemaparser.AbsoluteId
import org.scalatest._
import org.scalatest.Matchers._
import play.api.libs.json.{JsString, Json}

/**
 * Created by peter on 27/05/15, Atomic BITS (http://atomicbits.io). 
 */
class JsonSchemaParserTest extends FeatureSpec with GivenWhenThen {

  feature("Subschema dereferencing") {

    scenario("A nested JSON schema definition should be referenced correctly") {

      Given("a nested JSON schema definition")
      val source =
        """
          |{
          | "id": "http://my.site/myschema#",
          | "definitions": {
          |   "schema1": {
          |     "id": "schema1",
          |     "type": "integer"
          |   },
          |   "schema2": {
          |     "id": "#/definitions/schema2",
          |     "type": "array",
          |     "items": {
          |       "$ref": "schema1"
          |     }
          |   }
          | }
          |}
        """.stripMargin


      When("the definition is parsed")
      val schemaLookup = JsonSchemaParser.parse(Map("link1" -> source))


      Then("the schemalookup must contain absolute references to the nested schemas")
      And("the reference to the inner schema must be correctly expanded")

      println(s"schema lookup: $schemaLookup")

      schemaLookup.lookupTable(AbsoluteId("http://my.site/schema1")) shouldEqual
        Json.obj(
          "id" -> "schema1",
          "type" -> "integer"
        )

      (schemaLookup.lookupTable(AbsoluteId("http://my.site/myschema")) \\ "$ref") shouldEqual
        Seq(JsString("http://my.site/schema1"))

      // The schemaLookup looks as follows:
      """
        | SchemaLookup(
        |   Map(
        |     http://my.site/myschema -> {
        |       "id": "http://my.site/myschema#",
        |       "definitions": {
        |         "schema1":
        |           {
        |             "id": "schema1",
        |             "type": "integer"
        |           },
        |         "schema2":
        |           {
        |             "id": "#/definitions/schema2",
        |             "type": "array",
        |             "items": {
        |               "$ref": "http://my.site/schema1"
        |             }
        |           }
        |         }
        |       },
        |     http://my.site/schema1 -> {
        |       "id": "schema1",
        |       "type": "integer"
        |     },
        |     http://my.site/myschema#/definitions/schema2 -> {
        |       "id": "#/definitions/schema2",
        |       "type": "array",
        |       "items": {
        |         "$ref": "http://my.site/schema1"
        |       }
        |     }
        |   )
        | )
      """

    }

    scenario("Object references in a nested JSON schema definition should be given original canonical names") {

      Given("a nested JSON schema definition containing object references")
      val source =
        """
          |{
          | "id": "http://my.site/user.json#",
          | "definitions": {
          |   "address": {
          |     "id": "home-address.json",
          |     "type": "object",
          |     "properties": {
          |       "streetAddress": { "type": "string" },
          |       "city":          { "type": "string" },
          |       "state":         { "type": "string" }
          |     },
          |     "required": ["streetAddress", "city", "state"]
          |    },
          |   "certificate": {
          |     "id": "#/definitions/certificate",
          |     "type": "object",
          |     "properties": {
          |       "name": {
          |         "type": "string",
          |         "required": true
          |       },
          |       "grade": {
          |         "type": "string",
          |         "required": true
          |       }
          |     }
          |   },
          |   "credentials": {
          |     "id": "#/definitions/credentials",
          |     "type": "object",
          |     "properties": {
          |       "schoolName": {
          |         "type": "string",
          |         "required": true
          |       },
          |       "certificates": {
          |         "type": "array",
          |         "items": {
          |           "$ref": "#/definitions/certificate"
          |         }
          |       }
          |     }
          |   },
          |   "non-object-schema": {
          |     "id": "will-not-have-canonical-name",
          |     "type": "integer"
          |   }
          | },
          | "type": "object",
          | "properties": {
          |   "id": {
          |      "required": true,
          |      "type": "string"
          |    },
          |    "firstName": {
          |      "required": true,
          |      "type": "string"
          |    },
          |    "lastName": {
          |      "required": true,
          |      "type": "string"
          |    },
          |    "age": {
          |      "required": true,
          |      "type": "integer"
          |    },
          |    "homePage": {
          |      "required": false,
          |      "type": "integer"
          |    },
          |    "address": {
          |      "$ref": "home-address.json"
          |    },
          |    "credentials": {
          |      "$ref": "#/definitions/credentials"
          |    }
          |  }
          |}
        """.stripMargin


      When("the definition is parsed")
      val schemaLookup = JsonSchemaParser.parse(Map("link2" -> source))


      Then("the object ids should be mapped onto their canonical names")
//      schemaLookup.canonicalNames shouldEqual
//        Map(
//          "http://my.site/user.json" -> "User",
//          "http://my.site/home-address.json" -> "HomeAddress"
//        )

    }

  }

}
