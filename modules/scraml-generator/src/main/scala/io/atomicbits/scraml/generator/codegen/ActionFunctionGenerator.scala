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

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.parsedtypes.ParsedParameter

/**
  * Created by peter on 20/01/17.
  */
case class ActionFunctionGenerator(actionCode: ActionCode) {

  def generate(actionSelection: ActionSelection)(implicit platform: Platform): List[String] = {

    actionSelection.selectedContentType match {
      case x: FormPostContentType      => generateFormAction(actionSelection, x)
      case _: MultipartFormContentType => generateMultipartFormPostAction(actionSelection)
      case _: BinaryContentType        => generateBodyAction(actionSelection, binary = true)
      case _: AnyContentType           => generateBodyAction(actionSelection, binary = true)
      case x                           => generateBodyAction(actionSelection, binary = false)
    }

  }

  def generateFormAction(actionSelection: ActionSelection, formPostContentType: FormPostContentType): List[String] = {

    val actualFormParameters: Map[String, ParsedParameter] = formPostContentType.formParameters.valueMap

    val formParameterMethodParameters: List[String] =
      actionCode.sortQueryOrFormParameters(actualFormParameters.toList).map { paramPair =>
        val (name, param) = paramPair
        actionCode.expandQueryOrFormParameterAsMethodParameter((name, param))
      }

    val formParameterMapEntries: List[String] =
      formPostContentType.formParameters.valueMap.map {
        case (name, parameter) => actionCode.expandQueryOrFormParameterAsMapEntry((name, parameter))
      } toList

    val segType: String = actionCode.createSegmentType(actionSelection.selectedResponsetype)(None)

    val formAction: String =
      actionCode.generateAction(
        action                  = actionSelection,
        actionParameters        = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        segmentType             = segType,
        contentType             = actionSelection.selectedContentType,
        responseType            = actionSelection.selectedResponsetype
      )

    List(formAction)
  }

  def generateMultipartFormPostAction(actionSelection: ActionSelection)(implicit platform: Platform): List[String] = {

    val responseType = actionCode.createSegmentType(actionSelection.selectedResponsetype)(None)

    val multipartAction: String =
      actionCode.generateAction(
        action           = actionSelection,
        actionParameters = actionCode.expandMethodParameter(List("parts" -> ListClassReference(BodyPartClassReference))),
        multipartParams  = true,
        segmentType      = responseType,
        contentType      = actionSelection.selectedContentType,
        responseType     = actionSelection.selectedResponsetype
      )

    List(multipartAction)
  }

  def generateBodyAction(actionSelection: ActionSelection, binary: Boolean): List[String] = {

    /**
      * In scala, we can get compiler issues with default values on overloaded action methods. That's why we don't add
      * a default value in such cases. We do this any time when there are overloaded action methods, i.e. when there
      * are multiple body types.
      */
    val bodyTypes = actionCode.bodyTypes(actionSelection)
    val noDefault = bodyTypes.size > 1
    val queryParameterMethodParameters =
      actionCode
        .sortQueryOrFormParameters(actionSelection.action.queryParameters.valueMap.toList)
        .map(actionCode.expandQueryOrFormParameterAsMethodParameter(_, noDefault))

    val queryParameterMapEntries =
      actionSelection.action.queryParameters.valueMap.toList.map(actionCode.expandQueryOrFormParameterAsMapEntry)

    val segmentTypeFactory = actionCode.createSegmentType(actionSelection.selectedResponsetype) _

    bodyTypes.map { bodyType =>
      val actionBodyParameter =
        bodyType.map(bdType => actionCode.expandMethodParameter(List("body" -> bdType))).getOrElse(List.empty)

      val allActionParameters = actionBodyParameter ++ queryParameterMethodParameters

      val segmentBodyType = if (binary) None else bodyType

      actionCode.generateAction(
        action                   = actionSelection,
        actionParameters         = allActionParameters,
        queryParameterMapEntries = queryParameterMapEntries,
        segmentType              = segmentTypeFactory(segmentBodyType),
        typedBodyParam           = actionBodyParameter.nonEmpty && !binary,
        binaryParam              = actionBodyParameter.nonEmpty && binary,
        contentType              = actionSelection.selectedContentType,
        responseType             = actionSelection.selectedResponsetype
      )
    }

  }

}

case class ActionFunctionResult(imports: Set[ClassPointer]      = Set.empty,
                                fields: List[String]            = List.empty,
                                classes: List[SourceDefinition] = List.empty) {

  def ++(other: ActionFunctionResult): ActionFunctionResult =
    ActionFunctionResult(imports ++ other.imports, fields ++ other.fields, classes ++ other.classes)

}
