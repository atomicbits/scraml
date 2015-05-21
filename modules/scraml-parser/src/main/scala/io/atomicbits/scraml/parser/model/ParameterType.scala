package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
sealed trait ParameterType

case object StringType extends ParameterType

case object NumberType extends ParameterType

case object IntegerType extends ParameterType

case object BooleanType extends ParameterType

case object DateType extends ParameterType

case object FileType extends ParameterType

