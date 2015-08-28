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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.parser.model._

/**
 * Created by peter on 28/08/15. 
 */
trait ActionParameterSupport {

  def expandParameterAsMethodParameter(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam

    val nameTermName = queryParameterName
    val typeTypeName = parameter.parameterType match {
      case StringType  => "String"
      case IntegerType => "Long"
      case NumberType  => "Double"
      case BooleanType => "Boolean"
      case FileType    => sys.error(s"RAML type 'FileType' is not yet supported.")
      case DateType    => sys.error(s"RAML type 'DateType' is not yet supported.")
    }

    if (parameter.repeated) {
      s"$nameTermName: List[$typeTypeName]"
    } else {
      if (parameter.required) {
        s"$nameTermName: $typeTypeName"
      } else {
        s"$nameTermName: Option[$typeTypeName]"
      }
    }
  }

  def expandParameterAsMapEntry(qParam: (String, Parameter)): String = {
    val (queryParameterName, parameter) = qParam
    parameter match {
      case Parameter(_, _, true)  => s""""$queryParameterName" -> Option($queryParameterName).map(HttpParam(_))"""
      case Parameter(_, true, false)  => s""""$queryParameterName" -> Option($queryParameterName).map(HttpParam(_))"""
      case Parameter(_, false, false) => s""""$queryParameterName" -> $queryParameterName.map(HttpParam(_))"""
    }
  }


}
