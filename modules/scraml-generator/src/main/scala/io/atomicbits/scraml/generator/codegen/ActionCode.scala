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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.restmodel.{ ActionSelection, ContentType, ResponseType }
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.{ Parameter, QueryString }

/**
  * Created by peter on 20/01/17.
  */
trait ActionCode {

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassReference): String

  def queryStringType(actionSelection: ActionSelection): Option[ClassPointer]

  /**
    * The list of body types that need to be available on a specific action function.
    */
  def bodyTypes(action: ActionSelection): List[Option[ClassPointer]]

  def responseTypes(action: ActionSelection): List[Option[ClassPointer]]

  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String]

  def hasPrimitiveBody(optBodyType: Option[ClassPointer]): Boolean = {
    optBodyType.collect {
      case StringClassPointer | ByteClassPointer | BooleanClassPointer(_) | LongClassPointer(_) | DoubleClassPointer(_) => true
    } getOrElse false
  }

  def responseClassDefinition(responseType: ResponseType): String

  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)]

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): SourceCodeFragment

  def expandQueryStringAsMethodParameter(queryString: QueryString): SourceCodeFragment

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String

  def generateAction(actionSelection: ActionSelection,
                     bodyType: Option[ClassPointer],
                     queryStringType: Option[ClassPointer],
                     isBinary: Boolean,
                     actionParameters: List[String]        = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     isTypedBodyParam: Boolean             = false,
                     isMultipartParams: Boolean            = false,
                     isBinaryParam: Boolean                = false,
                     contentType: ContentType,
                     responseType: ResponseType): String

}
