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

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }
import io.atomicbits.scraml.generator.restmodel._
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.{ Method, Resource }

/**
  * Created by peter on 20/01/17.
  */
case class ActionGenerator(actionCode: ActionCode) {

  /**
    * The reason why we treat all actions of a resource together is that certain paths towards the actual action
    * execution of the resource's actions may overlap when it concerns actions that have overlapping mandatory
    * content-type and/or accept header paths. Although such situations may be rare, we want to support them well,
    * so we pass all actions of a single resource together.
    *
    * @param resourceClassDefinition The resource class definition of the resource whose actions are going to be processed
    *                                (NOT recursively!)
    * @return A list of action function definitions or action paths that lead to the action function. Action paths will only be
    *         required if multiple contenttype and/or accept headers will lead to a different typed body and/or response (we
    *         don't support those yet, but we will do so in the future).
    */
  def generateActionFunctions(resourceClassDefinition: ResourceClassDefinition)(implicit platform: Platform): ActionFunctionResult = {

    val resourcePackageParts: List[String] = resourceClassDefinition.resourcePackage
    val resource: Resource                 = resourceClassDefinition.resource

    val actionSelections: Set[ActionSelection] = resource.actions.map(ActionSelection(_)).toSet

    val actionsWithTypeSelection: Set[ActionSelection] =
      actionSelections.flatMap { actionSelection =>
        for {
          contentType <- actionSelection.contentTypeHeaders
          responseType <- actionSelection.responseTypeHeaders
          actionWithTypeSelection = actionSelection.withContentTypeSelection(contentType).withResponseTypeSelection(responseType)
        } yield actionWithTypeSelection
      }

    val groupedByActionType: Map[Method, Set[ActionSelection]] = actionsWithTypeSelection.groupBy(_.action.actionType)

    // now, we have to map the actions onto a segment path if necessary
    val actionPathToAction: Set[ActionPath] =
      groupedByActionType.values.toList.map(_.toList) flatMap {
        case actionOfKindList @ (aok :: Nil) => List(ActionPath(NoContentHeaderSegment, NoAcceptHeaderSegment, actionOfKindList.head))
        case actionOfKindList @ (aok :: aoks) =>
          actionOfKindList map { actionOfKind =>
            val contentHeader =
              actionOfKind.selectedContentType match {
                case NoContentType   => NoContentHeaderSegment
                case ct: ContentType => ActualContentHeaderSegment(ct)
              }
            val acceptHeader =
              actionOfKind.selectedResponseType match { // The selectedResponseTypesWithStatus have the same media type.
                case NoResponseType   => NoAcceptHeaderSegment
                case rt: ResponseType => ActualAcceptHeaderSegment(rt)
              }
            ActionPath(contentHeader, acceptHeader, actionOfKind)
          }
      } toSet

    val uniqueActionPaths: Map[ContentHeaderSegment, Map[AcceptHeaderSegment, Set[ActionSelection]]] =
      actionPathToAction
        .groupBy(_.contentHeader)
        .mapValues(_.groupBy(_.acceptHeader))
        .mapValues(_.mapValues(_.map(_.action)))

    val actionPathExpansion =
      uniqueActionPaths map {
        case (NoContentHeaderSegment, acceptHeaderMap) =>
          expandAcceptHeaderMap(resourcePackageParts, acceptHeaderMap)

        case (ActualContentHeaderSegment(contentType), acceptHeaderMap) =>
          expandContentTypePath(resourcePackageParts, contentType, acceptHeaderMap)
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else ActionFunctionResult()
  }

  private def expandContentTypePath(
      resourcePackageParts: List[String],
      contentType: ContentType,
      acceptHeaderMap: Map[AcceptHeaderSegment, Set[ActionSelection]])(implicit platform: Platform): ActionFunctionResult = {

    // create the content type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a content type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the acceptHeader map to source of the above class
    // add the List[ClassRep] results of the expansion of the acceptHeader map to the List[ClassRep] result

    val ActionFunctionResult(acceptSegmentMethodImports, acceptSegmentMethods, acceptHeaderClasses) =
      expandAcceptHeaderMap(resourcePackageParts, acceptHeaderMap)

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Content${CleanNameTools.cleanClassName(contentType.contentTypeHeader.value)}HeaderSegment"
    val headerSegment: HeaderSegmentClassDefinition =
      createHeaderSegment(resourcePackageParts, headerSegmentClassName, acceptSegmentMethodImports, acceptSegmentMethods)

    val contentHeaderMethodName      = s"content${CleanNameTools.cleanClassName(contentType.contentTypeHeader.value)}"
    val contentHeaderSegment: String = actionCode.contentHeaderSegmentField(contentHeaderMethodName, headerSegment.reference)

    ActionFunctionResult(imports                    = Set.empty,
                         actionFunctionDefinitions  = List(contentHeaderSegment),
                         headerPathClassDefinitions = headerSegment :: acceptHeaderClasses)
  }

  private def expandAcceptHeaderMap(resourcePackageParts: List[String], acceptHeaderMap: Map[AcceptHeaderSegment, Set[ActionSelection]])(
      implicit platform: Platform): ActionFunctionResult = {

    val actionPathExpansion: List[ActionFunctionResult] =
      acceptHeaderMap.toList match {
        case (_, actionSelections) :: Nil =>
          List(
            ActionFunctionResult(
              actionSelections.flatMap(generateActionImports),
              actionSelections.flatMap(ActionFunctionGenerator(actionCode).generate).toList,
              List.empty
            )
          )
        case ahMap @ (ah :: ahs) =>
          ahMap map {
            case (NoAcceptHeaderSegment, actionSelections) =>
              ActionFunctionResult(
                actionSelections.flatMap(generateActionImports),
                actionSelections.flatMap(ActionFunctionGenerator(actionCode).generate).toList,
                List.empty
              )
            case (ActualAcceptHeaderSegment(responseType), actionSelections) =>
              expandResponseTypePath(resourcePackageParts, responseType, actionSelections)
          }
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else ActionFunctionResult()
  }

  private def expandResponseTypePath(resourcePackageParts: List[String], responseType: ResponseType, actions: Set[ActionSelection])(
      implicit platform: Platform): ActionFunctionResult = {

    // create the result type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a result type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the actions to the above class and also add the imports needed by the actions
    // into the above class

    val actionImports = actions.flatMap(generateActionImports)
    val actionMethods = actions.flatMap(ActionFunctionGenerator(actionCode).generate).toList

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}HeaderSegment"
    val headerSegment: HeaderSegmentClassDefinition =
      createHeaderSegment(resourcePackageParts, headerSegmentClassName, actionImports, actionMethods)

    val acceptHeaderMethodName      = s"accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}"
    val acceptHeaderSegment: String = actionCode.contentHeaderSegmentField(acceptHeaderMethodName, headerSegment.reference)

    ActionFunctionResult(imports                    = Set.empty,
                         actionFunctionDefinitions  = List(acceptHeaderSegment),
                         headerPathClassDefinitions = List(headerSegment))
  }

  private def generateActionImports(action: ActionSelection)(implicit platform: Platform): Set[ClassPointer] = {

    val bodyTypes     = actionCode.bodyTypes(action).flatten.toSet
    val responseTypes = actionCode.responseTypes(action).flatten.toSet

    bodyTypes ++ responseTypes
  }

  private def createHeaderSegment(packageParts: List[String],
                                  className: String,
                                  imports: Set[ClassPointer],
                                  methods: List[String]): HeaderSegmentClassDefinition = {

    val classReference = ClassReference(name = className, packageParts = packageParts)

    HeaderSegmentClassDefinition(reference = classReference, imports = imports, methods = methods)
  }

  // Helper class to represent the path from a resource to an action over a content header segment and a accept header segment.
  case class ActionPath(contentHeader: ContentHeaderSegment, acceptHeader: AcceptHeaderSegment, action: ActionSelection)

  sealed trait HeaderSegment

  sealed trait ContentHeaderSegment extends HeaderSegment

  sealed trait AcceptHeaderSegment extends HeaderSegment

  case object NoContentHeaderSegment extends ContentHeaderSegment

  case class ActualContentHeaderSegment(header: ContentType) extends ContentHeaderSegment

  case object NoAcceptHeaderSegment extends AcceptHeaderSegment

  case class ActualAcceptHeaderSegment(header: ResponseType) extends AcceptHeaderSegment

}
