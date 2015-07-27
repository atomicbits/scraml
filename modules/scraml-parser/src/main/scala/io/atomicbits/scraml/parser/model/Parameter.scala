/*
 * (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * Contributors:
 *     Peter Rigole
 *
 */

package io.atomicbits.scraml.parser.model

/**
 * Created by peter on 17/05/15, Atomic BITS bvba (http://atomicbits.io). 
 */
case class Parameter(parameterType: ParameterType, required: Boolean, repeated: Boolean = false)

object Parameter {

  def apply(uriParameter: org.raml.model.parameter.AbstractParam): Parameter = {
    uriParameter.getType match {
      case org.raml.model.ParamType.STRING => Parameter(StringType, uriParameter.isRequired, uriParameter.isRepeat)
      case org.raml.model.ParamType.NUMBER => Parameter(NumberType, uriParameter.isRequired, uriParameter.isRepeat)
      case org.raml.model.ParamType.INTEGER => Parameter(IntegerType, uriParameter.isRequired, uriParameter.isRepeat)
      case org.raml.model.ParamType.BOOLEAN => Parameter(BooleanType, uriParameter.isRequired, uriParameter.isRepeat)
      case org.raml.model.ParamType.DATE => Parameter(DateType, uriParameter.isRequired, uriParameter.isRepeat)
      case org.raml.model.ParamType.FILE => Parameter(FileType, uriParameter.isRequired, uriParameter.isRepeat)
    }
  }

}
