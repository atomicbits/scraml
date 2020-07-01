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

package io.atomicbits.scraml.generator.platform.scalaplay

import io.atomicbits.scraml.generator.codegen.{ DslSourceRewriter, GenerationAggr }
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 14/01/17.
  */
case class CaseClassGenerator(scalaPlay: ScalaPlay) extends SourceGenerator {

  val defaultDiscriminator = "type"

  implicit val platform: ScalaPlay = scalaPlay

  def generate(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr = {

    /**
      * TOs are represented as case classes in Scala and because case classes cannot inherit from each other, we need to work around
      * polymorphism using traits. In particular, we need to create a specific trait for each case class that takes part in a subclass
      * relation, except for the leaf classes (which don't have any children). Multiple-inheritance is solved using traits as well.
      */
    // We need to mark each parent to need a trait of its own that contains the fields of that parent and we will implement the traits
    // of all parents in this case class.

    val originalToCanonicalName = toClassDefinition.reference.canonicalName

    val hasOwnTrait = generationAggr.isParent(originalToCanonicalName)

    val actualToCanonicalClassReference: ClassReference =
      if (hasOwnTrait) toClassDefinition.implementingInterfaceReference
      else toClassDefinition.reference

    val initialTosWithTrait: Seq[TransferObjectClassDefinition] =
      if (hasOwnTrait) Seq(toClassDefinition)
      else Seq.empty

    val initialFields: Seq[Field] = toClassDefinition.fields

    // Add all parents recursively as traits to implement and collect all fields.
    val parentNames: List[CanonicalName] = generationAggr.allParents(originalToCanonicalName)
    val traitsAndFieldsAggr              = (initialTosWithTrait, initialFields)

    val (recursiveExtendedTraits, collectedFields) =
      parentNames.foldLeft(traitsAndFieldsAggr) { (aggr, parentName) =>
        val (traits, fields) = aggr
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        val withParentFields = fields ++ parentDefinition.fields
        val withParentTrait  = traits :+ parentDefinition
        (withParentTrait, withParentFields)
      }

    val discriminator: String =
      (toClassDefinition.typeDiscriminator +: recursiveExtendedTraits.map(_.typeDiscriminator)).flatten.headOption
        .getOrElse(defaultDiscriminator)

    val jsonTypeInfo: Option[JsonTypeInfo] =
      if (generationAggr.isInHierarchy(originalToCanonicalName)) {
        Some(JsonTypeInfo(discriminator = discriminator, discriminatorValue = toClassDefinition.actualTypeDiscriminatorValue))
      } else {
        None
      }

    val traitsToImplement =
      generationAggr
        .directParents(originalToCanonicalName)
        .filter { parent => // ToDo: simplify this!!! If hasOwnTrait, then no parent traits are implemented, only its own trait is!!!
          if (hasOwnTrait) !generationAggr.isParentOf(parent, originalToCanonicalName)
          else true
        }
        .foldLeft(initialTosWithTrait) { (traitsToImpl, parentName) =>
          val parentDefinition =
            generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
          traitsToImpl :+ parentDefinition
        }
        .map(TransferObjectInterfaceDefinition(_, discriminator))

    val traitsToGenerate = recursiveExtendedTraits.map(TransferObjectInterfaceDefinition(_, discriminator))

    // add the collected traits to the generationAggr.toInterfaceMap if they aren't there yet
    val generationAggrWithAddedInterfaces =
      traitsToGenerate.foldLeft(generationAggr) { (aggr, collectedTrait) =>
        aggr.addInterfaceSourceDefinition(collectedTrait)
      }

    // We know that Play Json 2.4 has trouble with empty case classes, so we inject a random non-required field in that case
    val atLeastOneField =
      if (collectedFields.nonEmpty) collectedFields
      else Seq(Field(fieldName = s"__injected_field", classPointer = StringClassPointer, required = false))

    val recursiveFields: Set[Field] = findRecursiveFields(atLeastOneField, toClassDefinition.reference :: toClassDefinition.parents)

    generateCaseClass(
      traitsToImplement,
      atLeastOneField,
      jsonTypeInfo.map(_.discriminator),
      recursiveFields,
      actualToCanonicalClassReference,
      jsonTypeInfo,
      generationAggrWithAddedInterfaces
    )
  }

  /**
    * Find all recursive fields.
    *
    * All fields that have a type or a type with a type parameter (1) that refers to the containing type or one of its parents is a
    * recursive definition.
    * (1) We mean all type parameters recursively, as in {@code List[List[Geometry]] } should be lazy as well.
    *
    * @param fields         The fields to test for recursion.
    * @param selfAndParents The current TO class reference and all its parents.
    * @return The set containing all fields that have a recursive type.
    */
  private def findRecursiveFields(fields: Seq[Field], selfAndParents: List[ClassReference]): Set[Field] = {

    def isRecursive(field: Field): Boolean = isRecursiveClassReference(field.classPointer.native)

    def isRecursiveClassReference(classReference: ClassReference): Boolean = {
      val hasRecursiveType         = selfAndParents.contains(classReference)
      val typeParamClassReferences = classReference.typeParamValues.map(_.native)
      typeParamClassReferences.foldLeft(hasRecursiveType) {
        case (aggr, typeParamClassReference) => aggr || isRecursiveClassReference(typeParamClassReference.native)
      }
    }

    fields.foldLeft(Set.empty[Field]) {
      case (recursiveFields, field) if isRecursive(field) => recursiveFields + field
      case (recursiveFields, field)                       => recursiveFields
    }

  }

  private def generateCaseClass(traits: Seq[TransferObjectInterfaceDefinition],
                                fields: Seq[Field],
                                skipFieldName: Option[String],
                                recursiveFields: Set[Field],
                                toClassReference: ClassReference,
                                jsonTypeInfo: Option[JsonTypeInfo],
                                generationAggr: GenerationAggr): GenerationAggr = {

    val importPointers = {
      val ownFields      = fields.map(_.classPointer) // includes all the fields of its parents for case classes
      val traitsToExtend = traits.map(_.classReference)

      (ownFields ++ traitsToExtend).toSet
    }

    val imports: Set[String] = platform.importStatements(toClassReference, importPointers)

    val typeHintImport =
      if (jsonTypeInfo.isDefined) {
        val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")
        s"import $dslBasePackage.json.TypedJson._"
      } else {
        ""
      }

    val sortedFields = selectAndSortFields(fields, skipFieldName)

    val source =
      s"""
        package ${toClassReference.packageName}

        import play.api.libs.json._
        $typeHintImport

        ${imports.mkString("\n")}

        ${generateCaseClassDefinition(traits, sortedFields, toClassReference)}
        
        ${generateCompanionObject(sortedFields, recursiveFields, toClassReference, jsonTypeInfo)}
     """

    val sourceFile =
      SourceFile(
        filePath = toClassReference.toFilePath,
        content  = source
      )

    generationAggr.addSourceFile(sourceFile)
  }

  private def selectAndSortFields(fields: Seq[Field], skipFieldName: Option[String] = None): Seq[Field] = {
    val selectedFields =
      skipFieldName.map { skipField: String =>
        fields.filterNot(_.fieldName == skipField)
      }.getOrElse(fields)

    val sortedFields = selectedFields.sortBy(field => (!field.required, field.fieldName))
    sortedFields
  }

  private def generateCaseClassDefinition(traits: Seq[TransferObjectInterfaceDefinition],
                                          sortedFields: Seq[Field],
                                          toClassReference: ClassReference): String = {

    val fieldExpressions = sortedFields.map(_.fieldDeclarationWithDefaultValue)

    val extendedTraitDefs = traits.map(_.classReference.classDefinition)

    val extendsExpression =
      if (extendedTraitDefs.nonEmpty) extendedTraitDefs.mkString("extends ", " with ", "")
      else ""

    // format: off
    s"""
       case class ${toClassReference.classDefinition}(${fieldExpressions.mkString(",")}) $extendsExpression 
     """
    // format: on
  }

  private def generateCompanionObject(sortedFields: Seq[Field],
                                      recursiveFields: Set[Field],
                                      toClassReference: ClassReference,
                                      jsonTypeInfo: Option[JsonTypeInfo]): String = {

    val formatUnLiftFields = sortedFields.map(field => platform.fieldFormatUnlift(field, recursiveFields))

    def complexFormatterDefinition: (String, String) =
      ("import play.api.libs.functional.syntax._", s"def jsonFormatter: Format[${toClassReference.classDefinition}] = ")

    def complexTypedFormatterDefinition: (String, String) = {
      /*
       * This is the only way we know that formats typed variables, but it has problems with recursive types,
       * (see https://www.playframework.com/documentation/2.4.x/ScalaJsonCombinators#Recursive-Types).
       */
      val typeParametersFormat = toClassReference.typeParameters.map(typeParameter => s"${typeParameter.name}: Format")
      (s"import play.api.libs.functional.syntax._",
       s"def jsonFormatter[${typeParametersFormat.mkString(",")}]: Format[${toClassReference.classDefinition}] = ")
    }

    def singleFieldFormatterBody =
      s"${formatUnLiftFields.head}.inmap(${toClassReference.name}.apply, unlift(${toClassReference.name}.unapply))"

    def multiFieldFormatterBody =
      s"""
         ( ${formatUnLiftFields.mkString("~\n")}
         )(${toClassReference.name}.apply, unlift(${toClassReference.name}.unapply))
       """

    def over22FieldFormatterBody = {
      val groupedFields: List[List[Field]] = sortedFields.toList.grouped(22).toList

      val (fieldGroupDefinitions, fieldGroupNames, groupedFieldDeclarations): (List[String], List[String], List[String]) =
        groupedFields.zipWithIndex.map {
          case (group, index) =>
            val formatFields   = group.map(field => platform.fieldFormatUnlift(field, recursiveFields))
            val fieldGroupName = s"fieldGroup$index"
            val fieldGroupDefinition =
              if (formatFields.size > 1) {
                s"""
                val fieldGroup$index =
                  (${formatFields.mkString("~\n")}).tupled
               """
              } else {
                s"""
                val fieldGroup$index =
                  (${formatFields.mkString("~\n")})
               """
              }

            val fieldDeclarations = group.map { field =>
              if (field.required)
                platform.classDefinition(field.classPointer)
              else
                s"Option[${platform.classDefinition(field.classPointer)}]"
            }
            val groupedFieldDeclaration =
              if (fieldDeclarations.size > 1) {
                s"""(${fieldDeclarations.mkString(", ")})"""
              } else {
                fieldDeclarations.head
              }

            (fieldGroupDefinition, fieldGroupName, groupedFieldDeclaration)
        }.foldRight((List.empty[String], List.empty[String], List.empty[String])){ // unzip with 3 elements
          case ((definition, group, ttype), (defList, groupList, typeList)) =>
            ( definition :: defList, group :: groupList, ttype :: typeList)
        }

      s""" {
           ${fieldGroupDefinitions.mkString("\n")}

           def pack: (${groupedFieldDeclarations.mkString(", ")}) => ${toClassReference.name} = {
              case (${groupedFields.map { group =>
                        s"(${group.map(_.safeDeconstructionName).mkString(", ")})"
                      }.mkString(", ")}) =>
                      ${toClassReference.name}.apply(${sortedFields.map(_.safeDeconstructionName).mkString(", ")})
           }

           def unpack: ${toClassReference.name} => (${groupedFieldDeclarations.mkString(", ")}) = {
              cclass =>
              (${groupedFields.map { group =>
                   s"(${group.map(_.safeFieldName).map(cf => s"cclass.$cf").mkString(", ")})"
                 }.mkString(", ")})
           }

           (${fieldGroupNames.mkString(" and ")}).apply(pack, unpack)

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
    def simpleFormatter: (String, String) =
      ("", s"val jsonFormatter: Format[${toClassReference.classDefinition}] = Json.format[${toClassReference.classDefinition}]")

    val hasTypeVariables   = toClassReference.typeParameters.nonEmpty
    val anyFieldRenamed    = sortedFields.exists(field => field.fieldName != field.safeFieldName)
    val hasSingleField     = formatUnLiftFields.size == 1
    val hasOver22Fields    = formatUnLiftFields.size > 22
    val hasJsonTypeInfo    = jsonTypeInfo.isDefined
    val hasRecursiveFields = recursiveFields.nonEmpty

    // ToDo: Inject the json type discriminator and its value on the write side if there is one defined.
    // ToDo: make jsonFormatter not implicit and use it in the TypeHint in Animal and make a new implicit typedJsonFormatter that extends
    // ToDo: the jsonFormatter with the type discriminator and its value. Peek in the TypeHint implementation for how to do the latter
    val ((imports, formatter), body) =
      (hasTypeVariables, anyFieldRenamed, hasSingleField, hasOver22Fields, hasJsonTypeInfo, hasRecursiveFields) match {
        case (true, _, true, _, _, _)       => (complexTypedFormatterDefinition, singleFieldFormatterBody)
        case (true, _, _, true, _, _)       => (complexTypedFormatterDefinition, over22FieldFormatterBody)
        case (true, _, _, _, _, _)          => (complexTypedFormatterDefinition, multiFieldFormatterBody)
        case (false, _, true, _, _, _)      => (complexFormatterDefinition, singleFieldFormatterBody)
        case (false, _, _, true, _, _)      => (complexFormatterDefinition, over22FieldFormatterBody)
        case (false, true, false, _, _, _)  => (complexFormatterDefinition, multiFieldFormatterBody)
        case (false, false, _, _, _, true)  => (complexFormatterDefinition, multiFieldFormatterBody)
        case (false, false, _, _, _, false) => (simpleFormatter, "")
      }

    val objectName = toClassReference.name

    // The default formatter is implicit only when there is no need to inject a type descriminator.
    val implicitFormatterOrNot = if (hasJsonTypeInfo) "" else "implicit"

    val formatterWithTypeField =
      jsonTypeInfo.map { jsTypeInfo =>
        s"""
           implicit val jsonFormat: Format[$objectName] =
             TypeHintFormat(
               "${jsTypeInfo.discriminator}",
               $objectName.jsonFormatter.withTypeHint("${jsTypeInfo.discriminatorValue}")
             )
         """
      }.getOrElse("")

    s"""
       object $objectName {
       
         $imports
       
         $implicitFormatterOrNot $formatter $body
         
         $formatterWithTypeField

       }
     """
  }

}
