/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License for more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model

import io.atomicbits.scraml.ramlparser.parser.{ParseContext, RamlParseException}
import play.api.libs.json.{JsObject, JsValue}

import scala.util.{Failure, Try}

/**
  * Created by peter on 10/02/16.
  */
case class Parameter(parameterType: ParameterType, repeated: Boolean = false)


case object Parameter {

  def apply(jsValue: JsValue)(implicit parseContext: ParseContext): Try[Parameter] = {


    def jsObjectToParameter(jsObject: JsObject): Try[Parameter] = {

      jsObject match {
        case StringType(stringType)                   => Try(Parameter(stringType))
        case NumberType(numberType)                   => Try(Parameter(numberType))
        case IntegerType(integerType)                 => Try(Parameter(integerType))
        case BooleanType(booleanType)                 => Try(Parameter(booleanType))
        case DateOnlyType(dateOnlyType)               => Try(Parameter(dateOnlyType))
        case TimeOnlyType(timeOnlyType)               => Try(Parameter(timeOnlyType))
        case DateTimeOnlyType(dateTimeOnlyType)       => Try(Parameter(dateTimeOnlyType))
        case DateTimeDefaultType(dateTimeDefaultType) => Try(Parameter(dateTimeDefaultType))
        case DateTimeRFC2616Type(dateTimeRfc2616Type) => Try(Parameter(dateTimeRfc2616Type))
        case ArrayType(arrayType)                     => Try(Parameter(parameterType = arrayType.items, repeated = true))
        case _                                        =>
          Failure(RamlParseException(s"Unable to parse parameter ${jsObject.toString()} in ${parseContext.head}."))
      }

    }

    jsValue match {
      case jsObj: JsObject => jsObjectToParameter(jsObj)
      case _               => Try(Parameter(parameterType = StringType()))
    }

  }


  def asUriParameter(jsValue: JsValue): Try[Parameter] = {
    // URI parameters are always required!
    Parameter(jsValue).map(param => param.copy(parameterType = param.parameterType.asRequired))
  }

}
