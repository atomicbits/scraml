package io.atomicbits.scraml.generator

import io.atomicbits.scraml.parser.model._

import scala.reflect.macros.whitebox

/**
 * Created by peter on 24/05/15, Atomic BITS (http://atomicbits.io). 
 */
object ActionExpander {

  def expandAction(action: Action, c: whitebox.Context): c.universe.Tree = {

    import c.universe._


    def expandGetAction(): c.universe.Tree = {

      def expandQueryParameterAsGetParameter(qParam: (String, Parameter)): c.universe.Tree = {
        val (queryParameterName, parameter) = qParam

        val nameTermName = TermName(queryParameterName)
        val typeTypeName = parameter.parameterType match {
          case StringType => TypeName("String")
          case IntegerType => TypeName("Int")
          case NumberType => TypeName("Double")
          case BooleanType => TypeName("Boolean")
        }

        if (parameter.required) {
          q"val $nameTermName: $typeTypeName"
        } else {
          q"val $nameTermName: Option[$typeTypeName]"
        }
      }

      def expandQueryParameterAsMapEntry(qParam: (String, Parameter)): c.universe.Tree = {
        val (queryParameterName, parameter) = qParam
        val nameTermName = TermName(queryParameterName)
        parameter match {
          case Parameter(_, true) => q"""$queryParameterName -> Option($nameTermName).map(_.toString)"""
          case Parameter(_, false) => q"""$queryParameterName -> $nameTermName.map(_.toString)"""
        }
      }


      val queryParameterGetParameters =
        action.queryParameters.toList.map(param => expandQueryParameterAsGetParameter(param))
      val queryParameterMapEntries =
        action.queryParameters.toList.map(param => expandQueryParameterAsMapEntry(param))

      q"""
          def get(..$queryParameterGetParameters) = new GetSegment(
            queryParams = Map(
              ..$queryParameterMapEntries
            ),
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            req = requestBuilder
          ) {

            ${expandHeaders()}

          }
       """
    }

    def expandPutAction(): c.universe.Tree = {
      q"""
          def put(body: String) = new PutSegment(
            body = body,
            validAcceptHeaders = List(..${validAcceptHeaders()}),
            validContentTypeHeaders = List(..${validContentTypeHeaders()}),
            req = requestBuilder) {

            ${expandHeaders()}

          }
       """
    }

    def expandPostAction(): c.universe.Tree = {

//      if(action.)

      q""
    }

    def expandDeleteAction(): c.universe.Tree = {


      q""
    }

    def expandHeaders(): c.universe.Tree = {
      q"""
         def headers(headers: (String, String)*) = new HeaderSegment(
           headers = headers.toMap,
           req = requestBuilder
         ) {
           ${expandExecution()}
         }
       """
    }

    def expandExecution(): c.universe.Tree = {
      q"""
         def execute() = new ExecuteSegment(requestBuilder).execute()
       """
    }

    def validAcceptHeaders(): List[c.universe.Tree] = {
      action.responses.values.toList.flatMap(response => response.headers.keys.map(header => q"$header"))
    }

    def validContentTypeHeaders(): List[c.universe.Tree] = {
      action.body.keys.toList.map(header => q"$header")
    }

    action.actionType match {
      case Get => expandGetAction()
      case Put => expandPutAction()
      case Post => expandPostAction()
      case Delete => expandDeleteAction()
      case unknownAction => sys.error(s"$unknownAction actions are not supported yet.")
    }

  }

}
