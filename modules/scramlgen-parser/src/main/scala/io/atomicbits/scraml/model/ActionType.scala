package io.atomicbits.scraml.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
sealed trait ActionType

case object Get extends ActionType

case object Post extends ActionType

case object Put extends ActionType

case object Delete extends ActionType

case object Head extends ActionType

case object Patch extends ActionType

case object Options extends ActionType

case object Trace extends ActionType

