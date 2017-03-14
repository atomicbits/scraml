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

    val formAction: String =
      actionCode.generateAction(
        actionSelection         = actionSelection,
        bodyType                = None,
        isBinary                = false,
        actionParameters        = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        contentType             = actionSelection.selectedContentType,
        responseType            = actionSelection.selectedResponseType
      )

    List(formAction)
  }

  def generateMultipartFormPostAction(actionSelection: ActionSelection)(implicit platform: Platform): List[String] = {

    val multipartAction: String =
      actionCode.generateAction(
        actionSelection   = actionSelection,
        bodyType          = None,
        isBinary          = false,
        actionParameters  = actionCode.expandMethodParameter(List("parts" -> ListClassReference(BodyPartClassReference))),
        isMultipartParams = true,
        contentType       = actionSelection.selectedContentType,
        responseType      = actionSelection.selectedResponseType
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

    bodyTypes.map { bodyType =>
      val actionBodyParameter =
        bodyType.map(bdType => actionCode.expandMethodParameter(List("body" -> bdType))).getOrElse(List.empty)

      val allActionParameters = actionBodyParameter ++ queryParameterMethodParameters

      val segmentBodyType = if (binary) None else bodyType

      actionCode.generateAction(
        actionSelection  = actionSelection,
        bodyType         = bodyType,
        isBinary         = binary,
        actionParameters = allActionParameters,
        isTypedBodyParam = actionBodyParameter.nonEmpty && !binary,
        isBinaryParam    = actionBodyParameter.nonEmpty && binary,
        contentType      = actionSelection.selectedContentType,
        responseType     = actionSelection.selectedResponseType
      )
    }

  }

}

case class ActionFunctionResult(imports: Set[ClassPointer]                         = Set.empty,
                                actionFunctionDefinitions: List[String]            = List.empty,
                                headerPathClassDefinitions: List[SourceDefinition] = List.empty) {

  def ++(other: ActionFunctionResult): ActionFunctionResult =
    ActionFunctionResult(imports ++ other.imports,
                         actionFunctionDefinitions ++ other.actionFunctionDefinitions,
                         headerPathClassDefinitions ++ other.headerPathClassDefinitions)

}
