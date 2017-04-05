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

package io.atomicbits.scraml.generator.restmodel

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.ClassPointer
import io.atomicbits.scraml.ramlparser.model.{ BodyContent, Parameter }

/**
  * Created by peter on 3/04/17.
  */
object TypedRestOps {

  implicit class ParameterExtension(val parameter: Parameter) {

    def classPointer(): ClassPointer = {
      parameter.parameterType.canonical.collect {
        case typeReference => Platform.typeReferenceToClassPointer(typeReference)
      } getOrElse sys.error(s"Could not retrieve the canonical type reference from parameter $parameter.")
    }

  }

  implicit class BodyContentExtension(val bodyContent: BodyContent) {

    def classPointerOpt(): Option[ClassPointer] = {
      for {
        bdType <- bodyContent.bodyType
        canonicalBdType <- bdType.canonical
      } yield {
        Platform.typeReferenceToClassPointer(canonicalBdType)
      }
    }

  }

}
