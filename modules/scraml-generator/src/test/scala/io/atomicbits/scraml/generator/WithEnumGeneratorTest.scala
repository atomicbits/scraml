package io.atomicbits.scraml.generator

import io.atomicbits.scraml.generator.oldmodel._
import io.atomicbits.scraml.generator.platform.scalaplay.ScalaPlay
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
          ramlApiPath    = apiResourceUrl.toString,
          apiPackageName = "io.atomicbits",
          apiClassName   = "EnumApi",
          Scala,
          ScalaPlay
        )

      Then("we should get valid class representations")
      val classRepsByFullName: Map[String, ClassRep] = classReps.map(rep => rep.fullyQualifiedName -> rep).toMap

      val classes = List(
        "io.atomicbits.EnumApi",
        "io.atomicbits.rest.RestResource",
        "io.atomicbits.rest.withenum.WithEnumResource",
        "io.atomicbits.schema.WithEnum",
        "io.atomicbits.schema.WithEnumMethod"
      )

      classRepsByFullName.keys.foreach { key =>
        assert(classes.contains(key), s"Class $key is not generated.")
      }

      val linkResource = classRepsByFullName("io.atomicbits.schema.WithEnum")
//        println(linkResource)

      val methodEnumClass = classRepsByFullName("io.atomicbits.schema.WithEnumMethod")
//        println(methodEnumClass)
    }

  }

}
