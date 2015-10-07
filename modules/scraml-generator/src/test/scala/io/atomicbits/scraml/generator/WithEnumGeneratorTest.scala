package io.atomicbits.scraml.generator


import io.atomicbits.scraml.generator.model.{JsonTypeInfo, CustomClassRep, ClassRep, ClassReference}
import io.atomicbits.scraml.jsonschemaparser.RootId
import org.scalatest.concurrent.ScalaFutures
import org.scalatest._


class WithEnumGeneratorTest extends FeatureSpec with GivenWhenThen with BeforeAndAfterAll with ScalaFutures {

    feature("The scraml generator generates DSL classes suited for enums") {

      scenario("test generated Scala DSL") {

        Given("a RAML specification")
        val apiResourceUrl = this.getClass.getClassLoader.getResource("withenum/EnumApi.raml")

        When("we generate the RAMl specification into class representations")
        val classReps: Seq[ClassRep] =
          ScramlGenerator.generateClassReps(
            ramlApiPath = apiResourceUrl.toString,
            apiPackageName = "withenum",
            apiClassName = "EnumApi",
            ScramlGenerator.Scala
          )

        Then("we should get valid class representations")
        val classRepsByFullName: Map[String, ClassRep] = classReps.map(rep => rep.fullyQualifiedName -> rep).toMap

        val classes = List(
          "withenum.EnumApi",
          "withenum.rest.RestResource",
          "withenum.rest.withenum.WithEnumResource",
          "withenum.schema.WithEnum",
          "withenum.schema.WithEnumMethod"
        )

        classRepsByFullName.keys.foreach { key =>
          assert(classes.contains(key), s"Class $key is not generated.")
        }

       


        val linkResource = classRepsByFullName("withenum.schema.WithEnum")
        println(linkResource)
        
        val methodEnumClass = classRepsByFullName("withenum.schema.WithEnumMethod")
        println(methodEnumClass)
      }

    }

  }
