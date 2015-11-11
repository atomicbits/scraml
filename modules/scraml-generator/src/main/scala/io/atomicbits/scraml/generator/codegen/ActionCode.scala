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

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.parser.model.Parameter

/**
 * Created by peter on 30/09/15.
 */
trait ActionCode {


  def contentHeaderSegmentField(contentHeaderMethodName: String, headerSegment: ClassRep): String

  def headerSegmentClass(headerSegmentClassRef: ClassReference, imports: Set[String], methods: List[String]): String

  def bodyTypes(action: RichAction): List[Option[ClassPointer]]

  def expandMethodParameter(parameters: List[(String, ClassPointer)]): List[String]

  def createSegmentType(responseType: ResponseType)(optBodyType: Option[ClassPointer]): String

  def responseClassDefinition(responseType: ResponseType): String

  def sortQueryOrFormParameters(fieldParams: List[(String, Parameter)]): List[(String, Parameter)]

  def expandQueryOrFormParameterAsMethodParameter(qParam: (String, Parameter), noDefault: Boolean = false): String

  def expandQueryOrFormParameterAsMapEntry(qParam: (String, Parameter)): String

  def quoteString(text: String): String = s""""$text""""

  def generateAction(action: RichAction,
                     segmentType: String,
                     actionParameters: List[String] = List.empty,
                     bodyField: Boolean = false,
                     queryParameterMapEntries: List[String] = List.empty,
                     formParameterMapEntries: List[String] = List.empty,
                     multipartParams: Option[String] = None,
                     contentType: ContentType,
                     responseType: ResponseType): String

}
