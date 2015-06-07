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

import io.atomicbits.scraml.jsonschemaparser.model._
import org.scalatest._
import org.scalatest.Matchers._

/**
 * Created by peter on 27/05/15, Atomic BITS (http://atomicbits.io). 
 */
class JsonSchemaParserTest extends FeatureSpec with GivenWhenThen {

  feature("Parsing a raw JSON schema") {

    scenario("A nested JSON schema definition should be parsed into the schema model without simplifications") {

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


      When("the definition is parsed to the raw schema model")
      val rawSchemas = JsonSchemaParser.parseRawSchemas(Map("link1" -> source))

      Then("the raw schema adheres to the expected model")
      rawSchemas shouldEqual
        Map(
          "link1" ->
            Fragment(
              AbsoluteId("http://my.site/myschema"),
              Map(
                "definitions" ->
                  Fragment(
                    ImplicitId,
                    Map(
                      "schema1" -> IntegerEl(RelativeId("schema1"), required = false),
                      "schema2" ->
                        ArrayEl(
                          FragmentId("#/definitions/schema2"),
                          SchemaReference(ImplicitId, RelativeId("schema1")), required = false
                        )
                    )
                  )
              )
            )
        )

    }

    scenario("A complex JSON schema definition should be parsed into the schema model without simplifications") {

      Given("a nested JSON schema definition 2")
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


      When("the definition is parsed to the raw schema model 2")
      val rawSchema2 = JsonSchemaParser.parseRawSchemas(Map("link2" -> source))


      Then("the raw schema adheres to the expected model 2")
      rawSchema2 shouldEqual
        Map(
          "link2" ->
            ObjectEl(
              AbsoluteId("http://my.site/user.json"),
              Map(
                "homePage" -> IntegerEl(ImplicitId, required = false),
                "age" -> IntegerEl(ImplicitId, required = true),
                "lastName" -> StringEl(ImplicitId, None, required = true),
                "firstName" -> StringEl(ImplicitId, None, required = true),
                "id" -> StringEl(ImplicitId, None, required = true),
                "address" -> SchemaReference(ImplicitId, RelativeId("home-address.json")),
                "credentials" -> SchemaReference(ImplicitId, FragmentId("#/definitions/credentials"))
              ),
              required = false,
              List(),
              List(),
              Map(
                "definitions" ->
                  Fragment(
                    ImplicitId,
                    Map(
                      "address" ->
                        ObjectEl(
                          RelativeId("home-address.json"),
                          Map(
                            "streetAddress" -> StringEl(ImplicitId, None, required = false),
                            "city" -> StringEl(ImplicitId, None, required = false),
                            "state" -> StringEl(ImplicitId, None, required = false)
                          ),
                          required = false,
                          List("streetAddress", "city", "state"),
                          List(),
                          Map(),
                          None,
                          None
                        ),
                      "certificate" ->
                        ObjectEl(
                          FragmentId("#/definitions/certificate"),
                          Map(
                            "name" -> StringEl(ImplicitId, None, required = true),
                            "grade" -> StringEl(ImplicitId, None, required = true)
                          ),
                          required = false,
                          List(),
                          List(),
                          Map(),
                          None,
                          None
                        ),
                      "credentials" ->
                        ObjectEl(
                          FragmentId("#/definitions/credentials"),
                          Map(
                            "schoolName" -> StringEl(ImplicitId, None, required = true),
                            "certificates" ->
                              ArrayEl(
                                ImplicitId,
                                SchemaReference(ImplicitId, FragmentId("#/definitions/certificate")),
                                required = false
                              )
                          ),
                          required = false,
                          List(),
                          List(),
                          Map(),
                          None,
                          None
                        ),
                      "non-object-schema" ->
                        IntegerEl(
                          RelativeId("will-not-have-canonical-name"),
                          required = false
                        )
                    )
                  )
              ),
              None,
              None
            )
        )
    }

  }

}
