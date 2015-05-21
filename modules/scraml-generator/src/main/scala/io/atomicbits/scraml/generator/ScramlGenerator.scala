package io.atomicbits.scraml.generator

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._


// Selective packaging: http://www.scala-sbt.org/sbt-native-packager/formats/universal.html
// Macro projects: http://www.scala-sbt.org/0.13/docs/Macro-Projects.html (macro module in same project as core module)

// What we need is:
// http://stackoverflow.com/questions/21515325/add-a-compile-time-only-dependency-in-sbt

object ScRamlGenerator {

  // Macro annotations must be whitebox. If you declare a macro annotation as blackbox, it will not work.
  // See: http://docs.scala-lang.org/overviews/macros/annotations.html
  def generate(c: whitebox.Context)(annottees: c.Expr[Any]*): c.Expr[Any] = {

    import c.universe._

    val bar = c.Expr[Any](q""" case class Foo(val text: String) """)

    println(s"RAML model generation called: $c, bar: $bar")

    bar
    
//    val q"class $name" = q"class Foo"
//    val params = List(q"val text: String")

//    annottees.map(_.tree) match {
//      case (classDecl: ClassDef) :: Nil => c.Expr(q"""case class Foo(val text: String) { } """)
//      case _ => c.Expr(q"""case class Foo(val text: String) { } """)
//    }

//    c.Expr(q"""case class $name(..$params) { } """)
//    c.Expr(q"""case class Foo(text: String) { } """)


  }

}

class ScRaml(schemaFile: String) extends StaticAnnotation {

  def macroTransform(annottees: Any*): Any = macro ScRamlGenerator.generate

}
