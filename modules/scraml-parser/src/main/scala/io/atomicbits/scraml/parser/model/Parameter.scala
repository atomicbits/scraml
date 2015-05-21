package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Parameter(parameterType: ParameterType, required: Boolean)

object Parameter {

  def apply(uriParameter: org.raml.model.parameter.AbstractParam): Parameter = {
    uriParameter.getType match {
      case org.raml.model.ParamType.STRING => Parameter(StringType, uriParameter.isRequired)
      case org.raml.model.ParamType.NUMBER => Parameter(NumberType, uriParameter.isRequired)
      case org.raml.model.ParamType.INTEGER => Parameter(IntegerType, uriParameter.isRequired)
      case org.raml.model.ParamType.BOOLEAN => Parameter(BooleanType, uriParameter.isRequired)
      case org.raml.model.ParamType.DATE => Parameter(DateType, uriParameter.isRequired)
      case org.raml.model.ParamType.FILE => Parameter(FileType, uriParameter.isRequired)
    }
  }

}
