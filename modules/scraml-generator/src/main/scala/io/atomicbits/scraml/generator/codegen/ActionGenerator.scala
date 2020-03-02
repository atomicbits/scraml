/*
 *
 * (C) Copyright 2018 Atomic BITS (http://atomicbits.io).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
  def generateActionFunctions(resourceClassDefinition: ResourceClassDefinition)(implicit platform: Platform): SourceCodeFragment = {

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
      groupedByActionType.values.toList.map(_.toList).flatMap {
        case actionOfKindList @ (aok :: Nil) => List(ActionPath(NoContentHeaderSegment, NoAcceptHeaderSegment, actionOfKindList.head))
        case actionOfKindList @ (aok :: aoks) =>
          actionOfKindList.map { actionOfKind =>
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
        case Nil => Set.empty
      }.toSet

    val uniqueActionPaths: Map[ContentHeaderSegment, Map[AcceptHeaderSegment, Set[ActionSelection]]] =
      actionPathToAction
        .groupBy(_.contentHeader)
        .mapValues(_.groupBy(_.acceptHeader))
        .mapValues(_.mapValues(_.map(_.action)).toMap).toMap

    val actionPathExpansion =
      uniqueActionPaths map {
        case (NoContentHeaderSegment, acceptHeaderMap) =>
          expandAcceptHeaderMap(resourcePackageParts, acceptHeaderMap)

        case (ActualContentHeaderSegment(contentType), acceptHeaderMap) =>
          expandContentTypePath(resourcePackageParts, contentType, acceptHeaderMap)
      }

    if (actionPathExpansion.nonEmpty) actionPathExpansion.reduce(_ ++ _)
    else SourceCodeFragment()
  }

  private def expandContentTypePath(
      resourcePackageParts: List[String],
      contentType: ContentType,
      acceptHeaderMap: Map[AcceptHeaderSegment, Set[ActionSelection]])(implicit platform: Platform): SourceCodeFragment = {

    // create the content type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a content type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the acceptHeader map to source of the above class
    // add the List[ClassRep] results of the expansion of the acceptHeader map to the List[ClassRep] result

    val SourceCodeFragment(acceptSegmentMethodImports, acceptSegmentMethods, acceptHeaderClasses) =
      expandAcceptHeaderMap(resourcePackageParts, acceptHeaderMap)

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Content${CleanNameTools.cleanClassName(contentType.contentTypeHeader.value)}HeaderSegment"
    val headerSegment: HeaderSegmentClassDefinition =
      createHeaderSegment(resourcePackageParts, headerSegmentClassName, acceptSegmentMethodImports, acceptSegmentMethods)

    val contentHeaderMethodName      = s"content${CleanNameTools.cleanClassName(contentType.contentTypeHeader.value)}"
    val contentHeaderSegment: String = actionCode.contentHeaderSegmentField(contentHeaderMethodName, headerSegment.reference)

    SourceCodeFragment(imports                    = Set.empty,
                       sourceDefinition           = List(contentHeaderSegment),
                       headerPathClassDefinitions = headerSegment :: acceptHeaderClasses)
  }

  private def expandAcceptHeaderMap(resourcePackageParts: List[String], acceptHeaderMap: Map[AcceptHeaderSegment, Set[ActionSelection]])(
      implicit platform: Platform): SourceCodeFragment = {

    val actionPathExpansion: List[SourceCodeFragment] =
      acceptHeaderMap.toList match {
        case (_, actionSelections) :: Nil =>
          actionSelections.map(ActionFunctionGenerator(actionCode).generate).toList
        case ahMap @ (ah :: ahs) =>
          ahMap flatMap {
            case (NoAcceptHeaderSegment, actionSelections) =>
              actionSelections.map(ActionFunctionGenerator(actionCode).generate).toList
            case (ActualAcceptHeaderSegment(responseType), actionSelections) =>
              List(expandResponseTypePath(resourcePackageParts, responseType, actionSelections))
          }
        case Nil => List.empty
      }

    actionPathExpansion.foldLeft(SourceCodeFragment())(_ ++ _)
  }

  private def expandResponseTypePath(resourcePackageParts: List[String], responseType: ResponseType, actions: Set[ActionSelection])(
      implicit platform: Platform): SourceCodeFragment = {

    // create the result type path class extending a HeaderSegment and add the class to the List[ClassRep] result
    // add a result type path field that instantiates the above class (into the List[String] result)
    // add the List[String] results of the expansion of the actions to the above class and also add the imports needed by the actions
    // into the above class

    val actionFunctionResults = actions.map(ActionFunctionGenerator(actionCode).generate)
    val actionImports         = actionFunctionResults.flatMap(_.imports)
    val actionMethods         = actionFunctionResults.flatMap(_.sourceDefinition).toList

    // Header segment classes have the same class name in Java as in Scala.
    val headerSegmentClassName = s"Accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}HeaderSegment"
    val headerSegment: HeaderSegmentClassDefinition =
      createHeaderSegment(resourcePackageParts, headerSegmentClassName, actionImports, actionMethods)

    val acceptHeaderMethodName      = s"accept${CleanNameTools.cleanClassName(responseType.acceptHeader.value)}"
    val acceptHeaderSegment: String = actionCode.contentHeaderSegmentField(acceptHeaderMethodName, headerSegment.reference)

    SourceCodeFragment(imports = Set.empty, sourceDefinition = List(acceptHeaderSegment), headerPathClassDefinitions = List(headerSegment))
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
