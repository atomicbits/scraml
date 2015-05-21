package io.atomicbits.scraml.parser

import org.raml.model._
import org.raml.parser.rule.ValidationResult
import org.scalatest.Matchers._
import org.scalatest._

import scala.collection.JavaConverters._

/**
 * Created by peter on 12/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
class RamlParserTest extends FeatureSpec with GivenWhenThen {

  feature("the raml-java-parser project provides a working RAML parser") {

    scenario("test the RAML validator") {

      Given("the source of a valid RAML model")
      val ramlSource = "io/atomicbits/rules/instagram.yaml"

      When("we validate the RAML model")
      val validationResults: List[ValidationResult] = RamlParser.validateRaml(ramlSource)

      Then("the validation result should have no errors")
      validationResults shouldBe empty

    }

    scenario("test the RAML parser") {

      Given("the source of a valid RAML model")
      val ramlSource = "io/atomicbits/rules/instagram.yaml"

      When("we build the RAML model")
      val raml: Raml = RamlParser.buildRaml(ramlSource)

      Then("we get a valid RAML model")
      val locationsResource = raml.getResources.asScala.get("/locations")
      locationsResource shouldBe defined
      println(prettyResource(locationsResource.get))

    }

    scenario("test Raml.asScala version of the RAML model") {

      Given("the source of a valid RAML model")
      val ramlSource = "io/atomicbits/rules/instagram.yaml"

      When("we build the RAML.asScala model")
      val raml = RamlParser.buildRaml(ramlSource).asScala

      Then("we get a valid RAML Scala model")
      println(s"Scala RAML model: $raml")

    }

  }

  def prettyResource(resource: Resource): String = {

    val stringBuilder = new StringBuilder()

    def prettyHelper(resource: Resource, nbSpaces: Int = 1): String = {
      val indent = " " * nbSpaces
      s"""$indent displayName:      ${resource.getDisplayName}
          |$indent parentUri:       ${resource.getParentUri}
          |$indent relativeUri:     ${resource.getRelativeUri}
          |$indent type:            ${resource.getType}
          |$indent uriParameters:   ${resource.getUriParameters.asScala.mkString(", ")}
          |$indent is:              ${resource.getIs.asScala.mkString(", ")}
          |$indent actions:         ${resource.getActions.keySet().asScala.mkString(", ")}
          |${resource.getActions.values().asScala.map(prettyAction(_, nbSpaces + 3)).mkString("\n")}
          |$indent resources:
                    |${resource.getResources.values().asScala.map(prettyHelper(_, nbSpaces + 3)).mkString("\n")}
         """.stripMargin
    }

    def prettyAction(action: Action, nbSpaces: Int): String = {
      val indent = " " * nbSpaces
      val headers = action.getHeaders.asScala
      val body = Option(action.getBody).map(_.asScala)
      val responses = Option(action.getResponses).map(_.asScala)
      s"""$indent protocols:         ${action.getProtocols.asScala.mkString(", ")}
          |$indent query parameters:  ${action.getQueryParameters.keySet().asScala.mkString(", ")}
          |$indent headers:           ${headers.keys.map(key => s"$key -> ${headers(key).getDisplayName}").mkString(", ")}
          |$indent body:              ${prettyBody(body)}
          |$indent responses:         ${prettyResponse(responses)}
       """.stripMargin
    }

    def prettyResponse(response: Option[scala.collection.mutable.Map[String, Response]]): String = {
      response match {
        case Some(resp) =>
          resp.keys.map { key =>
            s"$key -> ${prettyBody(Option(resp(key)).flatMap(bdy => Option(bdy.getBody)).map(_.asScala))}"
          }.mkString(", ")
        case _ => "no response"
      }
    }

    def prettyBody(body: Option[scala.collection.mutable.Map[String, MimeType]]): String = {
      body match {
        case Some(bdy) =>
          bdy.keys.map { key =>
            s"$key --> ${Option(bdy(key)).map(_.getType).getOrElse("")}"
          }.mkString(", ")
        case _ => "no body"
      }
    }

    prettyHelper(resource)

  }

}
