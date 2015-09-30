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

    val formParameterMethodParameters =
      formPostContentType.formParameters.toList.sortBy(_._2.head.required).map { paramPair =>
        val (name, paramList) = paramPair
        if (paramList.isEmpty) sys.error(s"Form parameter $name has no valid type definition.")
        actionCode.expandQueryOrFormParameterAsMethodParameter((name, paramList.head))
        // We still don't understand why the form parameters are represented as a Map[String, List[Parameter]]
        // instead of just a Map[String, Parameter] in the Java Raml model. Here, we just use the first element
        // of the parameter list.
      }

    val formParameterMapEntries =
      formPostContentType.formParameters.toList.map { paramPair =>
        val (name, paramList) = paramPair
        actionCode.expandQueryOrFormParameterAsMapEntry((name, paramList.head))
      }

    val segmentType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val formAction: String =
      generateAction(
        action = action,
        actionParameters = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        segmentType = segmentType
      )

    List(formAction)
  }


  def generateMultipartFormPostAction(action: RichAction): List[String] = {

    val multipartResponseType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val multipartAction: String =
      generateAction(
        action = action,
        actionParameters = actionCode.expandMethodParameter(List("parts" -> ListClassReference("BodyPart"))),
        multipartParams = Some("parts"),
        segmentType = multipartResponseType
      )

    List(multipartAction)
  }


  def generateAction(action: RichAction): List[String] = {
    if (action.queryParameters.isEmpty) generateBodyAction(action)
    else generateQueryAction(action)
  }


  def generateQueryAction(action: RichAction): List[String] = {

    val queryParameterMethodParameters =
      action.queryParameters.toList.sortBy { t =>
        val (field, param) = t
        (!param.required, !param.repeated)
      } map actionCode.expandQueryOrFormParameterAsMethodParameter

    val queryParameterMapEntries = action.queryParameters.toList.map(actionCode.expandQueryOrFormParameterAsMapEntry)

    val segmentType = actionCode.createSegmentType(action.selectedResponsetype)(None)

    val queryAction: String =
      generateAction(
        action = action,
        actionParameters = queryParameterMethodParameters,
        queryParameterMapEntries = queryParameterMapEntries,
        segmentType = segmentType
      )

    List(queryAction)
  }


  def generateBodyAction(action: RichAction): List[String] = {

    val segmentTypeFactory = actionCode.createSegmentType(action.selectedResponsetype) _

    actionCode.bodyTypes(action).map { bodyType =>

      val actionBodyParameters =
        bodyType.map(bdType => actionCode.expandMethodParameter(List("body" -> bdType))).getOrElse(List.empty)

      generateAction(
        action = action,
        actionParameters = actionBodyParameters,
        segmentType = segmentTypeFactory(bodyType),
        bodyField = actionBodyParameters.nonEmpty
      )
    }

  }


  private def generateAction(action: RichAction,
                             segmentType: String,
                             actionParameters: List[String] = List.empty,
                             bodyField: Boolean = false,
                             queryParameterMapEntries: List[String] = List.empty,
                             formParameterMapEntries: List[String] = List.empty,
                             multipartParams: Option[String] = None): String = {

    val actionType = action.actionType
    val actionTypeMethod: String = actionType.toString.toLowerCase

    val expectedAcceptHeader = action.selectedResponsetype.acceptHeaderOpt
    val expectedContentTypeHeader = action.selectedContentType.contentTypeHeaderOpt

    val acceptHeader = expectedAcceptHeader.map(acceptH => s"""Some("$acceptH")""").getOrElse("None")
    val contentHeader = expectedContentTypeHeader.map(contentHeader => s"""Some("$contentHeader")""").getOrElse("None")

    val bodyFieldValue = if(bodyField) "Some(body)" else "None"

    val multipartParamsValue = multipartParams.getOrElse("List.empty")

    s"""
       def $actionTypeMethod(${actionParameters.mkString(", ")}) =
         new $segmentType(
           method = $actionType,
           theBody = $bodyFieldValue,
           queryParams = Map(
             ${queryParameterMapEntries.mkString(",")}
           ),
           formParams = Map(
             ${formParameterMapEntries.mkString(",")}
           ),
           multipartParams = $multipartParamsValue,
           expectedAcceptHeader = $acceptHeader,
           expectedContentTypeHeader = $contentHeader,
           req = requestBuilder
         ).call()
     """

  }

}


case class ActionFunctionResult(imports: Set[String] = Set.empty[String],
                                fields: List[String] = List.empty[String],
                                classes: List[ClassRep] = List.empty[ClassRep]) {

  def ++(other: ActionFunctionResult): ActionFunctionResult =
    ActionFunctionResult(imports ++ other.imports, fields ++ other.fields, classes ++ other.classes)

}

