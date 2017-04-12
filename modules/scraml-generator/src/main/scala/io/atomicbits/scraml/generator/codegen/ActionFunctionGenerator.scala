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

import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.Parameter

/**
  * Created by peter on 20/01/17.
  */
case class ActionFunctionGenerator(actionCode: ActionCode) {

  def generate(actionSelection: ActionSelection): SourceCodeFragment = {

    actionSelection.selectedContentType match {
      case x: FormPostContentType      => generateFormAction(actionSelection, x)
      case _: MultipartFormContentType => generateMultipartFormPostAction(actionSelection)
      case _: BinaryContentType        => generateBodyAction(actionSelection, binary = true)
      case _: AnyContentType           => generateBodyAction(actionSelection, binary = true)
      case x                           => generateBodyAction(actionSelection, binary = false)
    }

  }

  def generateFormAction(actionSelection: ActionSelection, formPostContentType: FormPostContentType): SourceCodeFragment = {

    val actualFormParameters: Map[String, Parameter] = formPostContentType.formParameters.valueMap

    val formParameterMethodParameterCode: List[SourceCodeFragment] =
      actionCode.sortQueryOrFormParameters(actualFormParameters.toList).map { paramPair =>
        val (name, param) = paramPair
        actionCode.expandQueryOrFormParameterAsMethodParameter((name, param))
      }

    val formParameterMethodParameterImports = formParameterMethodParameterCode.flatMap(_.imports)
    val formParameterMethodParameters       = formParameterMethodParameterCode.flatMap(_.sourceDefinition)

    val formParameterMapEntries: List[String] =
      actualFormParameters.map {
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

    val actionFunctionDefinitions = List(formAction)
    val actionImports             = generateActionImports(actionSelection)

    SourceCodeFragment(imports = actionImports ++ formParameterMethodParameterImports, sourceDefinition = actionFunctionDefinitions)
  }

  def generateMultipartFormPostAction(actionSelection: ActionSelection): SourceCodeFragment = {

    val multipartAction: String =
      actionCode.generateAction(
        actionSelection   = actionSelection,
        bodyType          = None,
        isBinary          = false,
        actionParameters  = actionCode.expandMethodParameter(List("parts" -> ListClassPointer(BodyPartClassPointer))),
        isMultipartParams = true,
        contentType       = actionSelection.selectedContentType,
        responseType      = actionSelection.selectedResponseType
      )

    val actionFunctionDefinitions = List(multipartAction)
    val actionImports             = generateActionImports(actionSelection)

    SourceCodeFragment(imports = actionImports, sourceDefinition = actionFunctionDefinitions)
  }

  def generateBodyAction(actionSelection: ActionSelection, binary: Boolean): SourceCodeFragment = {

    /**
      * In scala, we can get compiler issues with default values on overloaded action methods. That's why we don't add
      * a default value in such cases. We do this any time when there are overloaded action methods, i.e. when there
      * are multiple body types.
      */
    val bodyTypes = actionCode.bodyTypes(actionSelection)
    // Scala action methods will have trouble with default query parameter values when there are more than one body types.
    // Repetition of the action method will cause compiler conflicts in that case, so default values must be omitted then.
    val noDefault = bodyTypes.size > 1
    val queryParameterMethodParameterCode: List[SourceCodeFragment] =
      actionCode
        .sortQueryOrFormParameters(actionSelection.action.queryParameters.valueMap.toList)
        .map(actionCode.expandQueryOrFormParameterAsMethodParameter(_, noDefault))

    val queryParameterMethodParameterImports = queryParameterMethodParameterCode.flatMap(_.imports)
    val queryParameterMethodParameters       = queryParameterMethodParameterCode.flatMap(_.sourceDefinition)

    val actionFunctionDefinitions =
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

    val actionImports = generateActionImports(actionSelection)

    SourceCodeFragment(imports = actionImports ++ queryParameterMethodParameterImports, sourceDefinition = actionFunctionDefinitions)
  }

  private def generateActionImports(actionSelection: ActionSelection): Set[ClassPointer] = {

    // Todo: add the imports from the Parameters (headers / query parameters)
    actionSelection.action.queryParameters
    actionSelection.action.headers
    val bodyTypes     = actionCode.bodyTypes(actionSelection).flatten.toSet
    val responseTypes = actionCode.responseTypes(actionSelection).flatten.toSet

    bodyTypes ++ responseTypes
  }

}

case class SourceCodeFragment(imports: Set[ClassPointer]                         = Set.empty,
                              sourceDefinition: List[String]                     = List.empty,
                              headerPathClassDefinitions: List[SourceDefinition] = List.empty) {

  def ++(other: SourceCodeFragment): SourceCodeFragment =
    SourceCodeFragment(imports ++ other.imports,
                       sourceDefinition ++ other.sourceDefinition,
                       headerPathClassDefinitions ++ other.headerPathClassDefinitions)

}
