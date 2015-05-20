package io.atomicbits.scraml

import scala.annotation.StaticAnnotation
import scala.language.experimental.macros
import scala.reflect.macros._

// Add constructor arguments here.
class expand extends StaticAnnotation {
  def macroTransform(annottees: Any*): Any = macro Expander.expand_impl
}

object Expander {
  def expand_impl(c: whitebox.Context)(annottees: c.Expr[Any]*) = {
    import c.universe._

    println("Expander called")

    annottees.map(_.tree) match {
      case List(q"trait $name") => c.Expr[Any](
        // Add your own logic here, possibly using arguments on the annotation.
        q"""
          trait $name
          case class Foo(i: Int) extends $name
          case class Bar(s: String) extends $name
          case object Baz extends $name
        """
      )
      // Add validation and error handling here.
    }
  }
}