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
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.ramlparser.model.Raml
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.{ CanonicalName, NonPrimitiveType }

/**
  * Created by peter on 18/01/17.
  *
  * The generation aggregate contains information that may be needed for source definition generation and information
  * about data and knownledge that was already collected so far in the generation process. The generation process is
  * a recursive operation on the GenerationAggr, which can be expanded during code generation. In other words, new source
  * definitions may be added during code generation, especially the interface definitions are expected to be added then.
  *
  * @param sourceDefinitionsToProcess The collected source definitions up to 'now'.
  * @param sourceFilesGenerated The generated source files so far.
  * @param canonicalToMap The canonical TO map.
  * @param toMap The TO map is needed to find all fields that we have to put in a class extending from one or more parents.
  * @param toInterfaceMap The map containing the transfer objects that require an interface definition so far, keyed on the
  *                       canonical name of the original transfer object. This map is expected to grow while source
  *                       definitions for transfer objects are generated.
  * @param toChildParentsMap The direct child parents relations are needed to navigate through the class hierarchy of the transfer
  *                          objects. The toChildParentsMap is build up when the TOs are added to the toMap.
  * @param toParentChildrenMap The direct parent children relations are needed to navigate through the class hierarchy of the transfer
  *                            objects. The toParentChildrenMap is build up when the TOs are added to the toMap.
  */
case class GenerationAggr(sourceDefinitionsToProcess: Seq[SourceDefinition],
                          canonicalToMap: Map[CanonicalName, NonPrimitiveType],
                          sourceDefinitionsProcessed: Seq[SourceDefinition]                     = Seq.empty,
                          sourceFilesGenerated: Seq[SourceFile]                                 = Seq.empty,
                          toMap: Map[CanonicalName, TransferObjectClassDefinition]              = Map.empty,
                          toInterfaceMap: Map[CanonicalName, TransferObjectInterfaceDefinition] = Map.empty,
                          toChildParentsMap: Map[CanonicalName, Set[CanonicalName]]             = Map.empty,
                          toParentChildrenMap: Map[CanonicalName, Set[CanonicalName]]           = Map.empty) {

  def addSourceDefinition(sourceDefinition: SourceDefinition): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinition +: sourceDefinitionsToProcess)

  def addSourceDefinitions(sourceDefinitionsToAdd: Seq[SourceDefinition]): GenerationAggr =
    copy(sourceDefinitionsToProcess = sourceDefinitionsToProcess ++ sourceDefinitionsToAdd)

  def addSourceFile(sourceFile: SourceFile): GenerationAggr =
    copy(sourceFilesGenerated = sourceFile +: sourceFilesGenerated)

  def addSourceFiles(sourceFiles: Seq[SourceFile]): GenerationAggr =
    copy(sourceFilesGenerated = sourceFiles ++ sourceFilesGenerated)

  def addInterfaceSourceDefinition(interfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr = {
    val canonicalName = interfaceDefinition.origin.reference.canonicalName
    toInterfaceMap.get(canonicalName) match {
      case Some(toInterfaceDefinition) => this
      case None =>
        this
          .copy(toInterfaceMap = toInterfaceMap + (canonicalName -> interfaceDefinition))
          .addSourceDefinition(interfaceDefinition)
    }
  }

  def hasParents(canonicalName: CanonicalName): Boolean = toChildParentsMap.get(canonicalName).exists(_.nonEmpty)

  def hasChildren(canonicalName: CanonicalName): Boolean = toParentChildrenMap.get(canonicalName).exists(_.nonEmpty)

  def hasInterface(canonicalName: CanonicalName): Boolean = toInterfaceMap.get(canonicalName).isDefined

  def getInterfaceDefinition(canonicalName: CanonicalName): Option[TransferObjectInterfaceDefinition] = toInterfaceMap.get(canonicalName)

  def directParents(canonicalName: CanonicalName): Set[CanonicalName] = toChildParentsMap.getOrElse(canonicalName, Set.empty)

  def isParentOf(potentialParent: CanonicalName, potentialChild: CanonicalName): Boolean =
    allParents(potentialChild).contains(potentialParent)

  def directChildren(canonicalName: CanonicalName): Set[CanonicalName] = toParentChildrenMap.getOrElse(canonicalName, Set.empty)

  /**
    * Find all leaf children of the given canonical name (itself not included if it is a leaf child).
    */
  def leafChildren(canonicalName: CanonicalName): Set[CanonicalName] = {

    def findLeafChildren(childrenToCheck: List[CanonicalName], leafChildrenFound: Set[CanonicalName] = Set.empty): Set[CanonicalName] = {
      childrenToCheck match {
        case Nil                                              => leafChildrenFound
        case child :: remainingChildren if isLeafChild(child) => findLeafChildren(remainingChildren, leafChildrenFound + child)
        case child :: remainingChildren =>
          findLeafChildren(directChildren(child).toList ::: remainingChildren, leafChildrenFound)
      }
    }

    findLeafChildren(directChildren(canonicalName).toList)
  }

  /**
    * Find all non-leaf children of the given canonical name (itself not included if it is a non-leaf child).
    */
  def nonLeafChildren(canonicalName: CanonicalName): Set[CanonicalName] = {

    def findNonLeafChildren(childrenToCheck: List[CanonicalName],
                            nonLeafChildrenFound: Set[CanonicalName] = Set.empty): Set[CanonicalName] = {
      childrenToCheck match {
        case Nil => nonLeafChildrenFound
        case child :: remainingChildren if hasChildren(child) =>
          findNonLeafChildren(directChildren(child).toList ::: remainingChildren, nonLeafChildrenFound + child)
        case child :: remainingChildren => findNonLeafChildren(remainingChildren, nonLeafChildrenFound)
      }
    }

    findNonLeafChildren(directChildren(canonicalName).toList)
  }

  def isParent(canonicalName: CanonicalName): Boolean = hasChildren(canonicalName)

  def isChild(canonicalName: CanonicalName): Boolean = hasParents(canonicalName)

  def isInHierarchy(canonicalName: CanonicalName): Boolean = isChild(canonicalName) || isParent(canonicalName)

  def isLeafChild(canonicalName: CanonicalName): Boolean = isChild(canonicalName) && !isParent(canonicalName)

  def isNonLeafChild(canonicalName: CanonicalName): Boolean = isChild(canonicalName) && isParent(canonicalName)

  /**
    * A class is a parent in a multiple inheritance relation if it has a child (direct or indirect) that has more than one parent.
    */
  def isParentInMultipleInheritanceRelation(canonicalName: CanonicalName): Boolean =
    allChildren(canonicalName).exists(directParents(_).size > 1)

  /**
    * @return A breadth-first list of all parent canonical names.
    */
  def allParents(canonicalName: CanonicalName): List[CanonicalName] = {

    def findParents(parentsToExpand: List[CanonicalName], parentsFound: List[CanonicalName] = List.empty): List[CanonicalName] = {
      parentsToExpand match {
        case Nil => parentsFound
        case moreParents =>
          val nextLevelOfParents =
            parentsToExpand.flatMap { parent =>
              directParents(parent).toList
            }
          findParents(nextLevelOfParents, parentsFound ++ parentsToExpand)
      }
    }

    findParents(directParents(canonicalName).toList)
  }

  def allChildren(canonicalName: CanonicalName): List[CanonicalName] = {

    def findChildren(childrenToExpand: List[CanonicalName], childrenFound: List[CanonicalName] = List.empty): List[CanonicalName] = {
      childrenToExpand match {
        case Nil => childrenFound
        case moreChildren =>
          val nextLevelOfChildren =
            childrenToExpand.flatMap { child =>
              directChildren(child).toList
            }
          findChildren(nextLevelOfChildren, childrenFound ++ childrenToExpand)
      }
    }

    findChildren(directChildren(canonicalName).toList)
  }

  /**
    * Adds a TO definition and update the child-parents map and the parent-children map.
    *
    * @param canonicalName The canonical name of the TO.
    * @param toDefinition The definition of the TO.
    * @return The generation aggregate.
    */
  def addToDefinition(canonicalName: CanonicalName, toDefinition: TransferObjectClassDefinition): GenerationAggr = {

    val parentsMap: (CanonicalName, Set[CanonicalName]) = canonicalName -> toDefinition.parents.map(_.canonicalName).toSet

    val updatedAggr = updateChildParentsRelations(parentsMap)

    updatedAggr.copy(toMap = updatedAggr.toMap + (canonicalName -> toDefinition))
  }

  def generate(implicit platform: Platform): GenerationAggr = {

    import Platform._

    sourceDefinitionsToProcess match {
      case srcDef :: srcDefs => srcDef.toSourceFile(this.markSourceDefinitionsHeadAsProcessed).generate
      case Nil               => this
    }

  }

  private def markSourceDefinitionsHeadAsProcessed: GenerationAggr =
    copy(
      sourceDefinitionsToProcess = sourceDefinitionsToProcess.tail,
      sourceDefinitionsProcessed = sourceDefinitionsToProcess.head +: sourceDefinitionsProcessed
    )

  private def updateChildParentsRelations(childWithParents: (CanonicalName, Set[CanonicalName])): GenerationAggr = {

    val (child, parents) = childWithParents

    def updatedAggregate = {
      val aggrWithUpdatedParentChildrenRelation =
        parents.foldLeft(this) { (aggr, parent) =>
          val childrenOfParent = aggr.toParentChildrenMap.getOrElse(parent, Set.empty[CanonicalName])
          aggr.copy(toParentChildrenMap = aggr.toParentChildrenMap + (parent -> (childrenOfParent + child)))
        }

      aggrWithUpdatedParentChildrenRelation
        .copy(toChildParentsMap = aggrWithUpdatedParentChildrenRelation.toChildParentsMap + childWithParents)
    }

    if (parents.nonEmpty) updatedAggregate
    else this
  }

}

object GenerationAggr {

  def apply(apiName: String,
            apiBasePackage: List[String],
            raml: Raml,
            canonicalToMap: Map[CanonicalName, NonPrimitiveType]): GenerationAggr = {

    def collectResourceDefinitions(
        resourceDefinitionsToProcess: List[ResourceClassDefinition],
        collectedResourceDefinitions: List[ResourceClassDefinition] = List.empty): List[ResourceClassDefinition] = {

      resourceDefinitionsToProcess match {
        case Nil => collectedResourceDefinitions
        case _ =>
          val childResourceDefinitions = resourceDefinitionsToProcess.flatMap(_.childResourceDefinitions)
          collectResourceDefinitions(childResourceDefinitions, collectedResourceDefinitions ++ resourceDefinitionsToProcess)
      }
    }

    val topLevelResourceDefinitions = raml.resources.map(ResourceClassDefinition(apiBasePackage, List.empty, _))

    val clientClassDefinition =
      ClientClassDefinition(
        apiName                     = apiName,
        baseUri                     = raml.baseUri,
        basePackage                 = apiBasePackage,
        topLevelResourceDefinitions = topLevelResourceDefinitions
      )

    val collectedResourceDefinitions = collectResourceDefinitions(topLevelResourceDefinitions)

    val sourceDefinitions: Seq[SourceDefinition] = clientClassDefinition +: collectedResourceDefinitions

    val generationAggrBeforeCanonicalDefinitions =
      GenerationAggr(sourceDefinitionsToProcess = sourceDefinitions, canonicalToMap = canonicalToMap)

    val finalGenerationAggregate: GenerationAggr =
      CanonicalToSourceDefinitionGenerator.transferObjectsToClassDefinitions(generationAggrBeforeCanonicalDefinitions)

    finalGenerationAggregate
  }

}
