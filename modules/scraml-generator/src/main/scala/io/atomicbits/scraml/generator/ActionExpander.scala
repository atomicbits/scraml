package io.atomicbits.scraml.generator

import io.atomicbits.scraml.parser.model.Action

import scala.reflect.macros.whitebox

/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ActionExpander {

  def expandAction(action: Action, c: whitebox.Context): c.universe.Tree = {

    import c.universe._

    q""

  }

}
