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
 * Created by peter on 28/08/15. 
 */
case class ActionFunctionGenerator(actionCode: ActionCode) {


  def generate(action: RichAction): List[String] = {

    action.selectedContentType match {
      case x: FormPostContentType      => generateFormAction(action, x)
      case _: MultipartFormContentType => generateMultipartFormPostAction(action)
      case x                           => generateAction(action)
    }

  }


  def generateFormAction(action: RichAction, formPostContentType: FormPostContentType): List[String] = {

    val actualFormParameters: Map[String, Parameter] = formPostContentType.formParameters.map { fps =>
      val (name, paramList) = fps
      if (paramList.isEmpty) sys.error(s"Form parameter $name has no valid type definition.")
      // We still don't understand why the form parameters are represented as a Map[String, List[Parameter]]
      // instead of just a Map[String, Parameter] in the Java Raml model. Here, we just use the first element
      // of the parameter list.
      (name, paramList.head)
    }

    val formParameterMethodParameters =
      actionCode.sortQueryOrFormParameters(actualFormParameters.toList).map { paramPair =>
        val (name, param) = paramPair
        actionCode.expandQueryOrFormParameterAsMethodParameter((name, param))
      }

    val formParameterMapEntries =
      formPostContentType.formParameters.toList.map { paramPair =>
        val (name, paramList) = paramPair
        actionCode.expandQueryOrFormParameterAsMapEntry((name, paramList.head))
      }

    val segmentType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val formAction: String =
      actionCode.generateAction(
        action = action,
        actionParameters = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        segmentType = segmentType,
        responseType = action.selectedResponsetype
      )

    List(formAction)
  }


  def generateMultipartFormPostAction(action: RichAction): List[String] = {

    val multipartResponseType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val multipartAction: String =
      actionCode.generateAction(
        action = action,
        actionParameters = actionCode.expandMethodParameter(List("parts" -> ListClassReference("BodyPart"))),
        multipartParams = Some("parts"),
        segmentType = multipartResponseType,
        responseType = action.selectedResponsetype
      )

    List(multipartAction)
  }


  def generateAction(action: RichAction): List[String] = {
    if (action.queryParameters.isEmpty) generateBodyAction(action)
    else generateQueryAction(action)
  }


  def generateQueryAction(action: RichAction): List[String] = {

    val queryParameterMethodParameters =
      actionCode.sortQueryOrFormParameters(action.queryParameters.toList).map(actionCode.expandQueryOrFormParameterAsMethodParameter)

    val queryParameterMapEntries = action.queryParameters.toList.map(actionCode.expandQueryOrFormParameterAsMapEntry)

    val segmentType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val queryAction: String =
      actionCode.generateAction(
        action = action,
        actionParameters = queryParameterMethodParameters,
        queryParameterMapEntries = queryParameterMapEntries,
        segmentType = segmentType,
        responseType = action.selectedResponsetype
      )

    List(queryAction)
  }


  def generateBodyAction(action: RichAction): List[String] = {

    val segmentTypeFactory = actionCode.createSegmentType(action.selectedResponsetype) _

    actionCode.bodyTypes(action).map { bodyType =>

      val actionBodyParameters =
        bodyType.map(bdType => actionCode.expandMethodParameter(List("body" -> bdType))).getOrElse(List.empty)

      actionCode.generateAction(
        action = action,
        actionParameters = actionBodyParameters,
        segmentType = segmentTypeFactory(bodyType),
        bodyField = actionBodyParameters.nonEmpty,
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

