package io.atomicbits.scraml.jsonschemaparser

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
      val schemaLookup = JsonSchemaParser.parse(List(source))


      Then("the schemalookup must contain absolute references to the nested schemas")
      And("the reference to the inner schema must be correctly expanded")

      println(s"schema lookup: $schemaLookup")

      schemaLookup.lookupTable("http://my.site/schema1") shouldEqual
        Json.obj(
          "id" -> "schema1",
          "type" -> "integer"
        )

      schemaLookup.lookupTable("http://my.site/myschema#/definitions/schema2") shouldEqual
        Json.obj(
          "id" -> "#/definitions/schema2",
          "type" -> "array",
          "items" ->
            Json.obj(
              "$ref" -> "http://my.site/schema1"
            )
        )

      (schemaLookup.lookupTable("http://my.site/myschema") \\ "$ref") shouldEqual
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

  }


}
