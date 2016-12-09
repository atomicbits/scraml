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
import io.atomicbits.scraml.ramlparser.model.ParsedParameter

/**
  * Created by peter on 28/08/15.
  */
case class ActionFunctionGenerator(actionCode: ActionCode) {


  def generate(action: RichAction)(implicit lang: Language): List[String] = {

    action.selectedContentType match {
      case x: FormPostContentType      => generateFormAction(action, x)
      case _: MultipartFormContentType => generateMultipartFormPostAction(action)
      case _: BinaryContentType        => generateBodyAction(action, binary = true)
      case _: AnyContentType           => generateBodyAction(action, binary = true)
      case x                           => generateBodyAction(action, binary = false)
    }

  }


  def generateFormAction(action: RichAction, formPostContentType: FormPostContentType): List[String] = {

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

    val segType: String = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val formAction: String =
      actionCode.generateAction(
        action = action,
        actionParameters = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        segmentType = segType,
        contentType = action.selectedContentType,
        responseType = action.selectedResponsetype
      )

    List(formAction)
  }


  def generateMultipartFormPostAction(action: RichAction)(implicit lang: Language): List[String] = {

    val responseType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val multipartAction: String =
      actionCode.generateAction(
        action = action,
        actionParameters = actionCode.expandMethodParameter(List("parts" -> ListClassReference("BodyPart"))),
        multipartParams = true,
        segmentType = responseType,
        contentType = action.selectedContentType,
        responseType = action.selectedResponsetype
      )

    List(multipartAction)
  }


  def generateBodyAction(action: RichAction, binary: Boolean): List[String] = {

    /**
      * In scala, we can get compiler issues with default values on overloaded action methods. That's why we don't add
      * a default value in such cases. We do this any time when there are overloaded action methods, i.e. when there
      * are multiple body types.
      */
    val bodyTypes = actionCode.bodyTypes(action)
    val noDefault = bodyTypes.size > 1
    val queryParameterMethodParameters =
      actionCode.sortQueryOrFormParameters(action.queryParameters.valueMap.toList)
        .map(actionCode.expandQueryOrFormParameterAsMethodParameter(_, noDefault))

    val queryParameterMapEntries = action.queryParameters.valueMap.toList.map(actionCode.expandQueryOrFormParameterAsMapEntry)

    val segmentTypeFactory = actionCode.createSegmentType(action.selectedResponsetype) _

    bodyTypes.map { bodyType =>

      val actionBodyParameter =
        bodyType.map(bdType => actionCode.expandMethodParameter(List("body" -> bdType))).getOrElse(List.empty)

      val allActionParameters = actionBodyParameter ++ queryParameterMethodParameters

      val segmentBodyType = if (binary) None else bodyType

      actionCode.generateAction(
        action = action,
        actionParameters = allActionParameters,
        queryParameterMapEntries = queryParameterMapEntries,
        segmentType = segmentTypeFactory(segmentBodyType),
        typedBodyParam = actionBodyParameter.nonEmpty && !binary,
        binaryParam = actionBodyParameter.nonEmpty && binary,
        contentType = action.selectedContentType,
        responseType = action.selectedResponsetype
      )
    }

  }

}


case class ActionFunctionResult(imports: Set[String] = Set.empty[String],
                                fields: List[String] = List.empty[String],
                                classes: List[ClassRep] = List.empty[ClassRep]) {

  def ++(other: ActionFunctionResult): ActionFunctionResult =
    ActionFunctionResult(imports ++ other.imports, fields ++ other.fields, classes ++ other.classes)

}

