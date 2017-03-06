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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.restmodel.{ ActionSelection, ContentType, ResponseType }
import io.atomicbits.scraml.generator.typemodel.{ ClassPointer, ClassReference }
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedParameter

/**
  * Created by peter on 20/01/17.
  */
trait ActionCode {

  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassReference): String

  // def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[ClassPointer], methods: List[String]): String

  /**
    * The list of body types that need to be available on a specific action function.
    */
  def bodyTypes(action: ActionSelection): List[Option[ClassPointer]]

  def responseTypes(action: ActionSelection): List[Option[ClassPointer]]

  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String]

//  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String

  def responseClassDefinition(responseType: ResponseType): String

  def sortQueryOrFormParameters(fieldParams: List[(String, ParsedParameter)]): List[(String, ParsedParameter)]

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, ParsedParameter), noDefault: Boolean = false): String

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, ParsedParameter)): String

  def quoteString(text: String): String = s""""$text""""

  def generateAction(actionSelection: ActionSelection,
                     bodyType: Option[ClassPointer],
                     isBinary: Boolean,
                     actionParameters: List[String]        = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     isTypedBodyParam: Boolean             = false,
                     isMultipartParams: Boolean            = false,
                     isBinaryParam: Boolean                = false,
                     contentType: ContentType,
                     responseType: ResponseType,
                     generationAggr: GenerationAggr): String

}
