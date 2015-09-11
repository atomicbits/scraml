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

package io.atomicbits.scraml.generator.codegen.scala

import io.atomicbits.scraml.generator.model._
import io.atomicbits.scraml.parser.model.ActionType

/**
 * Created by peter on 28/08/15. 
 */
object ActionFunctionGenerator extends ActionGeneratorSupport {

  def generate(action: RichAction): List[String] = {

    action.contentTypes.headOption map {
      case _: StringContentType        => generateAction(action)
      case _: JsonContentType          => generateAction(action)
      case _: TypedContentType         => generateAction(action)
      case x: FormPostContentType      => generateFormAction(action, x)
      case _: MultipartFormContentType => generateMultipartFormPostAction(action)
      case x                           => sys.error(s"We don't expect a $x content type on a post action.")
    } getOrElse generateAction(action)

  }


  def generateFormAction(action: RichAction, formPostContentType: FormPostContentType): List[String] = {

    val formParameterMethodParameters =
      formPostContentType.formParameters.toList.sortBy(_._2.head.required).map { paramPair =>
        val (name, paramList) = paramPair
        if (paramList.isEmpty) sys.error(s"Form parameter $name has no valid type definition.")
        expandParameterAsMethodParameter((name, paramList.head))
        // We still don't understand why the form parameters are represented as a Map[String, List[Parameter]]
        // instead of just a Map[String, Parameter] in the Java Raml model. Here, we just use the first element
        // of the parameter list.
      }

    val formParameterMapEntries =
      formPostContentType.formParameters.toList.map { paramPair =>
        val (name, paramList) = paramPair
        expandParameterAsMapEntry((name, paramList.head))
      }

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val segmentType = createSegmentType(action.actionType, action.responseTypes.headOption)(None)

    val formAction: String =
      generateAction(
        actionType = action.actionType,
        actionParameters = formParameterMethodParameters,
        formParameterMapEntries = formParameterMapEntries,
        segmentType = segmentType,
        validAcceptHeaders = validAcceptHeaders,
        validContentTypeHeaders = validContentTypeHeaders
      )

    List(formAction)
  }

  def generateMultipartFormPostAction(action: RichAction): List[String] = {

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val multipartResponseType = createSegmentType(action.actionType, action.responseTypes.headOption)(None)

    val multipartAction: String =
      generateAction(
        actionType = action.actionType,
        actionParameters = List("parts: List[BodyPart]"),
        multipartParams = "parts",
        segmentType = multipartResponseType,
        validAcceptHeaders = validAcceptHeaders,
        validContentTypeHeaders = validContentTypeHeaders
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
      } map expandParameterAsMethodParameter

    val queryParameterMapEntries = action.queryParameters.toList.map(expandParameterAsMapEntry)

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val segmentType = createSegmentType(action.actionType, action.responseTypes.headOption)(None)

    val queryAction: String =
      generateAction(
        actionType = action.actionType,
        actionParameters = queryParameterMethodParameters,
        queryParameterMapEntries = queryParameterMapEntries,
        segmentType = segmentType,
        validAcceptHeaders = validAcceptHeaders,
        validContentTypeHeaders = validContentTypeHeaders
      )

    List(queryAction)
  }


  def generateBodyAction(action: RichAction): List[String] = {

    val validAcceptHeaders = action.responseTypes.map(_.acceptHeaderValue)
    val validContentTypeHeaders = action.contentTypes.map(_.contentTypeHeaderValue)

    val segmentTypeFactory = createSegmentType(action.actionType, action.responseTypes.headOption) _

    bodyTypes(action).map { bodyType =>

      val (actionBodyParameters, bodyField) = bodyType.map(bdType => (List(s"body: $bdType"), "Some(body)")).getOrElse(List.empty, "None")

      generateAction(
        actionType = action.actionType,
        actionParameters = actionBodyParameters,
        segmentType = segmentTypeFactory(bodyType),
        bodyField = bodyField,
        validAcceptHeaders = validAcceptHeaders,
        validContentTypeHeaders = validContentTypeHeaders
      )
    }

  }


  private def generateAction(actionType: ActionType,
                             segmentType: String,
                             actionParameters: List[String] = List.empty,
                             bodyField: String = "None",
                             queryParameterMapEntries: List[String] = List.empty,
                             formParameterMapEntries: List[String] = List.empty,
                             multipartParams: String = "List.empty",
                             validAcceptHeaders: List[String] = List.empty,
                             validContentTypeHeaders: List[String] = List.empty): String = {

    val actionTypeMethod: String = actionType.toString.toLowerCase

    s"""
       def $actionTypeMethod(${actionParameters.mkString(", ")}) =
         new $segmentType(
           method = $actionType,
           theBody = $bodyField,
           queryParams = Map(
             ${queryParameterMapEntries.mkString(",")}
           ),
           formParams = Map(
             ${formParameterMapEntries.mkString(",")}
           ),
           multipartParams = $multipartParams,
           validAcceptHeaders = List(${validAcceptHeaders.map(quoteString).mkString(",")}),
           validContentTypeHeaders = List(${validContentTypeHeaders.map(quoteString).mkString(",")}),
           req = requestBuilder
         )
     """

  }

}
