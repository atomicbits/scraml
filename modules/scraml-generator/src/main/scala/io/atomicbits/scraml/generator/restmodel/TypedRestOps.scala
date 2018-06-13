/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.restmodel

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel.ClassPointer
import io.atomicbits.scraml.ramlparser.model.{ BodyContent, Parameter, QueryString }

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

  implicit class QueryStringExtension(val queryString: QueryString) {

    def classPointer(): ClassPointer = {
      queryString.queryStringType.canonical.collect {
        case typeReference => Platform.typeReferenceToClassPointer(typeReference)
      } getOrElse sys.error(s"Could not retrieve the canonical type reference from queryString $queryString.")
    }

  }

}
