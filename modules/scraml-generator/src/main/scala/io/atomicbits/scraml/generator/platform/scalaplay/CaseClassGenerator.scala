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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 14/01/17.
  */
object CaseClassGenerator extends SourceGenerator {

  implicit val platform: Platform = ScalaPlay

  def generate(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr = {

    /**
      * TOs are represented as case classes in Scala and because case classes cannot inherit from each other, we need to work around
      * polymorphism using traits. In particular, we need to create a specific trait for each case class that takes part in a subclass
      * relation, except for the leaf classes (which don't have any children). Multiple-inheritance is solved using traits as well.
      */
    // We need to mark each parent to need a trait of its own that contains the fields of that parent and we will implement the traits
    // of all parents in this case class.

    val toCanonicalName = toClassDefinition.reference.canonicalName

    val initTosWithTrait: Seq[TransferObjectClassDefinition] =
      if (generationAggr.hasChildren(toCanonicalName)) Seq(toClassDefinition)
      else Seq.empty
    val initAllFields: Seq[Field] = toClassDefinition.fields

    // Add all parents recursively as traits to implement and collect all fields.
    val parentNames: List[CanonicalName] = generationAggr.allParents(toCanonicalName)
    val traitsAndFieldsAggr              = (initTosWithTrait, initAllFields)

    val (collectedTosWithTrait, collectedFields) =
      parentNames.foldLeft(traitsAndFieldsAggr) { (aggr, parentName) =>
        val (traits, fields) = aggr
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        val withParentFields = fields ++ parentDefinition.fields
        val withParentTrait  = traits :+ parentDefinition
        (withParentTrait, withParentFields)
      }

    val discriminator: Option[String] = collectedTosWithTrait.flatMap(_.jsonTypeInfo).headOption.map(_.discriminator)

    val collectedTraits = collectedTosWithTrait.map(TransferObjectInterfaceDefinition(_, discriminator.getOrElse("type")))

    // add the collected traits to the generationAggr.toInterfaceMap if they aren't there yet
    val generationAggrWithAddedInterfaces =
      collectedTraits.foldLeft(generationAggr) { (aggr, collectedTrait) =>
        aggr.addInterface(collectedTrait.origin.reference.canonicalName, collectedTrait)
      }

    generateCaseClass(collectedTraits, collectedFields, discriminator, toClassDefinition, generationAggrWithAddedInterfaces)
  }

  private def generateCaseClass(traits: Seq[TransferObjectInterfaceDefinition],
                                fields: Seq[Field],
                                skipFieldName: Option[String],
                                toClassDefinition: TransferObjectClassDefinition,
                                generationAggr: GenerationAggr): GenerationAggr = {

    val imports: Set[String] =
      platform.importStatements(toClassDefinition.reference, (fields.map(_.classPointer) ++ traits.map(_.classReference)).toSet)

    val sortedFields = selectAndSortFields(fields, skipFieldName)

    val source =
      s"""
        package ${toClassDefinition.reference.packageName}

        import play.api.libs.json._

        ${imports.mkString("\n")}

        ${generateCaseClassDefinition(traits, sortedFields, toClassDefinition)}
        
        ${generateCompanionObject(traits, sortedFields, toClassDefinition)}
     """

    val sourceFile =
      SourceFile(
        filePath = platform.classReferenceToFilePath(toClassDefinition.reference),
        content  = source
      )

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

  private def selectAndSortFields(fields: Seq[Field], skipFieldName: Option[String] = None): Seq[Field] = {
    val selectedFields =
      skipFieldName map { skipField =>
        fields.filterNot(_.fieldName == skipField)
      } getOrElse fields

    val sortedFields = selectedFields.sortBy(field => (!field.required, field.fieldName))
    sortedFields
  }

  private def generateCaseClassDefinition(traits: Seq[TransferObjectInterfaceDefinition],
                                          sortedFields: Seq[Field],
                                          toClassDefinition: TransferObjectClassDefinition): String = {

    val fieldExpressions = sortedFields.map(_.fieldExpression)

    val extendedTraitDefs = traits.map(_.origin.reference.classDefinition)

    val extendsExpression =
      if (extendedTraitDefs.nonEmpty) extendedTraitDefs.mkString("extends ", " with ", "")
      else ""

    // format: off
    s"""
       case class ${toClassDefinition.reference.classDefinition}(${fieldExpressions.mkString(",")}) $extendsExpression 
     """
    // format: on
  }

  private def generateCompanionObject(traits: Seq[TransferObjectInterfaceDefinition],
                                      sortedFields: Seq[Field],
                                      toClassDefinition: TransferObjectClassDefinition): String = {

    val formatUnLiftFields = sortedFields.map(field => ScalaPlay.fieldFormatUnlift(field))
    val classReference     = toClassDefinition.reference

    def complexFormatterDefinition =
      s"""
          import play.api.libs.functional.syntax._

          implicit def jsonFormatter: Format[${classReference.classDefinition}] = """

    def complexTypedFormatterDefinition = {
      /*
       * This is the only way we know that formats typed variables, but it has problems with recursive types,
       * (see https://www.playframework.com/documentation/2.4.x/ScalaJsonCombinators#Recursive-Types).
       */
      val typeParametersFormat = classReference.typeParameters.map(typeParameter => s"${typeParameter.name}: Format")
      s"""
          import play.api.libs.functional.syntax._

          implicit def jsonFormatter[${typeParametersFormat.mkString(",")}]: Format[${classReference.classDefinition}] = """
    }

    def singleFieldFormatterBody =
      s"${formatUnLiftFields.head}.inmap(${classReference.name}.apply, unlift(${classReference.name}.unapply))"

    def multiFieldFormatterBody =
      s"""
         ( ${formatUnLiftFields.mkString("~\n")}
         )(${classReference.name}.apply, unlift(${classReference.name}.unapply))
       """

    def over22FieldFormatterBody = {
      val groupedFields: List[List[Field]] = sortedFields.toList.grouped(22).toList

      val (fieldGroupDefinitions, fieldGroupNames): (List[String], List[String]) =
        groupedFields.zipWithIndex.map {
          case (group, index) =>
            val formatFields   = group.map(field => ScalaPlay.fieldFormatUnlift(field))
            val fieldGroupName = s"fieldGroup$index"
            val fieldGroupDefinition =
              s"""
                val fieldGroup$index =
                  (${formatFields.mkString("~\n")}).tupled
               """
            (fieldGroupDefinition, fieldGroupName)
        } unzip

      s""" {
           ${fieldGroupDefinitions.mkString("\n")}

           (${fieldGroupNames.mkString(" and ")}).apply({
             case (${groupedFields
        .map { group =>
          s"(${group.map(_.safeFieldName).mkString(", ")})"
        }
        .mkString(", ")})
               => ${classReference.name}.apply(${sortedFields.map(_.safeFieldName).mkString(", ")})
           }, cclass =>
              (${groupedFields
        .map { group =>
          s"(${group.map(_.safeFieldName).map(cf => s"cclass.$cf").mkString(", ")})"
        }
        .mkString(", ")})
           )
        }
       """
    }

    /**
      * The reason why we like to use the easy macro version below is that it resolves issues like the recursive
      * type problem that the elaborate "Complex version" has
      * (see https://www.playframework.com/documentation/2.4.x/ScalaJsonCombinators#Recursive-Types)
      * Types like the one below cannot be formatted with the "Complex version":
      *
      * > case class Tree(value: String, children: List[Tree])
      *
      * To format it with the "Complex version" has to be done as follows:
      *
      * > case class Tree(value: String, children: List[Tree])
      * > object Tree {
      * >   import play.api.libs.functional.syntax._
      * >   implicit def jsonFormatter: Format[Tree] = // Json.format[Tree]
      * >     ((__ \ "value").format[String] ~
      * >       (__ \ "children").lazyFormat[List[Tree]](Reads.list[Tree](jsonFormatter), Writes.list[Tree](jsonFormatter)))(Tree.apply, unlift(Tree.unapply))
      * > }
      *
      * To format it with the "Easy version" is simply:
      *
      * > implicit val jsonFormatter: Format[Tree] = Json.format[Tree]
      *
      */
    def simpleFormatter =
      s"implicit val jsonFormatter: Format[${classReference.classDefinition}] = Json.format[${classReference.classDefinition}]"

    val hasTypeVariables = classReference.typeParameters.nonEmpty
    val anyFieldRenamed  = sortedFields.exists(field => field.fieldName != field.safeFieldName)
    val hasSingleField   = formatUnLiftFields.size == 1
    val hasOver22Fields  = formatUnLiftFields.size > 22

    val formatter =
      (hasTypeVariables, anyFieldRenamed, hasSingleField, hasOver22Fields) match {
        case (true, _, true, _)      => s"$complexTypedFormatterDefinition $singleFieldFormatterBody"
        case (true, _, _, true)      => s"$complexTypedFormatterDefinition $over22FieldFormatterBody"
        case (true, _, _, _)         => s"$complexTypedFormatterDefinition $multiFieldFormatterBody"
        case (false, _, true, _)     => s"$complexFormatterDefinition $singleFieldFormatterBody"
        case (false, _, _, true)     => s"$complexFormatterDefinition $over22FieldFormatterBody"
        case (false, true, false, _) => s"$complexFormatterDefinition $multiFieldFormatterBody"
        case (false, false, _, _)    => simpleFormatter
      }

    val objectName = toClassDefinition.reference.name

    s"""
       object $objectName {
       
         $formatter
       
       }
     """
  }

}
