package io.atomicbits.scraml.generator

import org.raml.parser.rule.ValidationResult

import io.atomicbits.scraml.parser._
import io.atomicbits.scraml.parser.model._

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._


// Selective packaging: http://www.scala-sbt.org/sbt-native-packager/formats/universal.html
// Macro projects: http://www.scala-sbt.org/0.13/docs/Macro-Projects.html (macro module in same project as core module)

// What we need is:
// http://stackoverflow.com/questions/21515325/add-a-compile-time-only-dependency-in-sbt

class ScRaml(ramlSpecPath: String) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro ScRamlGenerator.generate

}

object ScRamlGenerator {

  // Macro annotations must be whitebox. If you declare a macro annotation as blackbox, it will not work.
  // See: http://docs.scala-lang.org/overviews/macros/annotations.html
  def generate(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

    import c.universe._

    val ramlSpecPath = c.prefix.tree match {
      case Apply(_, List(Literal(Constant(x)))) => x.toString
      case _ => c.abort(c.enclosingPosition, "RAML specification path not specified")
    }

    val className = annottees.map(_.tree) match {
      case List(q"class $name") => name
      case _ => c.abort(c.enclosingPosition, "the annotation can only be used with classes")
    }

    // Validate RAML spec
    println(s"Running RAML validation on $ramlSpecPath: ")
    val validationResults: List[ValidationResult] = RamlParser.validateRaml(ramlSpecPath)
    if (validationResults.nonEmpty) {
      println("Invalid RAML specification:")
      c.abort(c.enclosingPosition, RamlParser.printValidations(validationResults))
    }
    println("RAML model is valid")

    // Generate the RAML model
    println("Running RAML model generation")
    val raml: Raml = RamlParser.buildRaml(ramlSpecPath).asScala
    println(s"RAML model generated")


    def expandResourcesFromRaml(): List[c.universe.Tree] = {

      /**
       * Expanding a resource consists of two high-level steps:
       * 1. expand the current path segment (possibly a path parameter) if it is non-empty and expand it into the DSL
       * 2. expand the resource's actions and sub-resources recursively
       */
      def expandResource(resource: Resource): c.universe.Tree = {

        val urlSegment = resource.urlSegment
        val segmentAsString = q""" $urlSegment """
        val segmentAsDefName = TermName(resource.urlSegment)

        val expandedSubResources = resource.resources.map(resource => expandResource(resource))
        val expandedActions = resource.actions.map(action => expandAction(action))

        def noSegment = {
          q"""
              ..$expandedActions
              ..$expandedSubResources
           """
        }

        def plainSegment = {
          q"""
            def $segmentAsDefName = new PlainSegment($segmentAsString, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
        }

        def stringSegment = {
          q"""
            def $segmentAsDefName(value: String) = new ParamSegment[String](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
        }

        def intSegment = {
          q"""
            def $segmentAsDefName(value: Int) = new ParamSegment[Int](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
        }

        def doubleSegment = {
          q"""
            def $segmentAsDefName(value: Double) = new ParamSegment[Double](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
        }

        def booleanSegment = {
          q"""
            def $segmentAsDefName(value: Boolean) = new ParamSegment[Boolean](value, requestBuilder) {
              ..$expandedActions
              ..$expandedSubResources
            }
           """
        }

        if (resource.urlSegment.isEmpty) {
          noSegment
        } else
          resource.urlParameter match {
            case None => plainSegment
            case Some(Parameter(StringType, _)) => stringSegment
            case Some(Parameter(IntegerType, _)) => intSegment
            case Some(Parameter(NumberType, _)) => doubleSegment
            case Some(Parameter(BooleanType, _)) => booleanSegment
          }

      }

      def expandAction(action: Action): c.universe.Tree = {
        q""
      }

      // ToDo: incorporate RAML schemas: raml.schemas

      raml.resources.map(resource => expandResource(resource))

    }

    val resources = expandResourcesFromRaml()

    // rewrite the class definition
    c.Expr(
      q"""
       case class $className(host: String,
                             port: Int = 80,
                             protocol: String = "http",
                             requestTimeout: Int = 5000,
                             maxConnections: Int = 2) {

         import io.atomicbits.scraml.dsl.support._
         import io.atomicbits.scraml.dsl.support.client.rxhttpclient.RxHttpClient

         protected val requestBuilder = RequestBuilder(new RxHttpClient(protocol, host, port, requestTimeout, maxConnections))

         ..$resources

       }
     """
    )

  }


}

