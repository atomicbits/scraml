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
    * execution of the resource's actions may be overlapping when it concerns actions that have overlapping mandatory
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

    val actionSelections: List[ActionSelection] = resource.actions.map(ActionSelection(_))

    val actionsWithTypeSelection: List[ActionSelection] =
      actionSelections.flatMap { actionSelection =>
        for {
          contentType <- actionSelection.contentTypes
          responseType <- actionSelection.responseTypes
          actionWithTypeSelection = actionSelection.withContentTypeSelection(contentType).withResponseTypeSelection(responseType)
        } yield actionWithTypeSelection
      }

    val groupedByActionType: Map[Method, List[ActionSelection]] = actionsWithTypeSelection.groupBy(_.action.actionType)

    // now, we have to map the actions onto a segment path if necessary
    val actionPathToAction: List[ActionPath] =
      groupedByActionType.values flatMap {
        case actionOfKindList @ (aok :: Nil) => List(ActionPath(NoContentHeaderSegment, NoAcceptHeaderSegment, actionOfKindList.head))
        case actionOfKindList @ (aok :: aoks) =>
          actionOfKindList map { actionOfKind =>
            val contentHeader =
              actionOfKind.selectedContentType match {
                case NoContentType   => NoContentHeaderSegment
                case ct: ContentType => ActualContentHeaderSegment(ct)
              }
            val acceptHeader =
              actionOfKind.selectedResponsetype match {
                case NoResponseType   => NoAcceptHeaderSegment
                case rt: ResponseType => ActualAcceptHeaderSegment(rt)
              }
            ActionPath(contentHeader, acceptHeader, actionOfKind)
          }
      } toList

    val uniqueActionPaths: Map[ContentHeaderSegment, Map[AcceptHeaderSegment, List[ActionSelection]]] =
      actionPathToAction
        .groupBy(_.contentHeader)
        .mapValues(_.groupBy(_.acceptHeader))
        .mapValues(_.mapValues(_.map(_.action)))

    val actionPathExpansion: List[ActionFunctionResult] =
      uniqueActionPaths.toList map {
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
      acceptHeaderMap: Map[AcceptHeaderSegment, List[ActionSelection]])(implicit platform: Platform): ActionFunctionResult = {

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
    val contentHeaderSegment: String = actionCode.contentHeaderSegmentField(contentHeaderMethodName, headerSegment.classReference)

    ActionFunctionResult(imports = Set.empty, fields = List(contentHeaderSegment), classes = headerSegment :: acceptHeaderClasses)
  }

  private def expandAcceptHeaderMap(resourcePackageParts: List[String], acceptHeaderMap: Map[AcceptHeaderSegment, List[ActionSelection]])(
      implicit platform: Platform): ActionFunctionResult = {

    val actionPathExpansion: List[ActionFunctionResult] =
      acceptHeaderMap.toList match {
        case (_, actions) :: Nil =>
          List(
            ActionFunctionResult(
              actions.toSet.flatMap(generateActionImports),
              actions.flatMap(ActionFunctionGenerator(actionCode).generate),
              List.empty
            )
          )
        case ahMap @ (ah :: ahs) =>
          ahMap map {
            case (NoAcceptHeaderSegment, actions) =>
              ActionFunctionResult(
                actions.toSet.flatMap(generateActionImports),
                actions.flatMap(ActionFunctionGenerator(actionCode).generate),
                List.empty
              )
            case (ActualAcceptHeaderSegment(responseType), actions) => expandResponseTypePath(resourcePackageParts, responseType, actions)
          }
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else ActionFunctionResult()
  }

  private def expandResponseTypePath(resourcePackageParts: List[String], responseType: ResponseType, actions: List[ActionSelection])(
      implicit platform: Platform): ActionFunctionResult = {

    // create the result type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a result type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the actions to the above class and also add the imports needed by the actions
    // into the above class

    val actionImports = actions.toSet.flatMap(generateActionImports)
    val actionMethods = actions.flatMap(ActionFunctionGenerator(actionCode).generate)

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}HeaderSegment"
    val headerSegment: HeaderSegmentClassDefinition =
      createHeaderSegment(resourcePackageParts, headerSegmentClassName, actionImports, actionMethods)

    val acceptHeaderMethodName      = s"accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}"
    val acceptHeaderSegment: String = actionCode.contentHeaderSegmentField(acceptHeaderMethodName, headerSegment.classReference)

    ActionFunctionResult(imports = Set.empty, fields = List(acceptHeaderSegment), classes = List(headerSegment))
  }

  private def generateActionImports(action: ActionSelection)(implicit platform: Platform): Set[ClassPointer] = {

    val contentTypeImports =
      action.selectedContentType match {
        case BinaryContentType(contentTypeHeader)          => Set[ClassPointer](BinaryDataClassReference)
        case TypedContentType(contentTypeHeader, classRep) => Set(classRep)
        case _                                             => Set.empty[ClassPointer]
      }

    val responseTypeImports =
      action.selectedResponsetype match {
        case BinaryResponseType(acceptHeader)          => Set[ClassPointer](BinaryDataClassReference)
        case TypedResponseType(acceptHeader, classRep) => Set(classRep)
        case _                                         => Set.empty[ClassPointer]
      }

    contentTypeImports ++ responseTypeImports
  }

  private def createHeaderSegment(packageParts: List[String],
                                  className: String,
                                  imports: Set[ClassPointer],
                                  methods: List[String]): HeaderSegmentClassDefinition = {

    val classReference = ClassReference(name = className, packageParts = packageParts)

    HeaderSegmentClassDefinition(classReference = classReference, imports = imports, methods = methods)
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
