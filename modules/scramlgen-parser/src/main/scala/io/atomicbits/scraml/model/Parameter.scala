package io.atomicbits.scraml.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Parameter(parameterType: ParameterType)

object Parameter {

  def apply(uriParameter: org.raml.model.parameter.AbstractParam): Parameter = {
    uriParameter.getType match {
      case org.raml.model.ParamType.STRING => Parameter(StringType)
      case org.raml.model.ParamType.NUMBER => Parameter(NumberType)
      case org.raml.model.ParamType.INTEGER => Parameter(IntegerType)
      case org.raml.model.ParamType.BOOLEAN => Parameter(BooleanType)
      case org.raml.model.ParamType.DATE => Parameter(DateType)
      case org.raml.model.ParamType.FILE => Parameter(FileType)
    }
  }

}
