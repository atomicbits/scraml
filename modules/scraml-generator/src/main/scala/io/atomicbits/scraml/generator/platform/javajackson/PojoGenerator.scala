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

package io.atomicbits.scraml.generator.platform.javajackson

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName

/**
  * Created by peter on 1/03/17.
  */
object PojoGenerator extends SourceGenerator with PojoGeneratorSupport {

  implicit val platform: Platform = JavaJackson

  val defaultDiscriminator = "type"

  def generate(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr = {

    /**
      * TOs are represented as POJOs in Java, which can without trouble extend from each other as long as only single-inheritance
      * is used. As soon as we deal with multiple-inheritance, we need to replace all POJOs that function as one of the parents in
      * a multiple-inheritance relation by their interface. The actual implementing class of that interface case gets the 'Impl' suffix.
      */
    val originalToCanonicalName = toClassDefinition.reference.canonicalName

    val toHasOwnInterface = hasOwnInterface(originalToCanonicalName, generationAggr)

    val actualToCanonicalClassReference: ClassReference =
      if (toHasOwnInterface) toClassDefinition.implementingInterfaceReference
      else toClassDefinition.reference

    val initialTosWithInterface: Seq[TransferObjectClassDefinition] =
      if (toHasOwnInterface) Seq(toClassDefinition)
      else Seq.empty

    val ownFields: Seq[Field]            = toClassDefinition.fields
    val parentNames: List[CanonicalName] = generationAggr.allParents(originalToCanonicalName)
    val interfacesAndFieldsAggr          = (initialTosWithInterface, ownFields)

    val (recursiveExtendedParents, allFields) =
      parentNames.foldLeft(interfacesAndFieldsAggr) { (aggr, parentName) =>
        val (interfaces, fields) = aggr
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        val withParentFields    = fields ++ parentDefinition.fields
        val withParentInterface = interfaces :+ parentDefinition
        (withParentInterface, withParentFields)
      }

    val discriminator: String =
      (toClassDefinition.typeDiscriminator +: recursiveExtendedParents.map(_.typeDiscriminator)).flatten.headOption
        .getOrElse(defaultDiscriminator)

    val jsonTypeInfo: Option[JsonTypeInfo] =
      if (generationAggr.isInHierarchy(originalToCanonicalName)) {
        Some(JsonTypeInfo(discriminator = discriminator, discriminatorValue = toClassDefinition.actualTypeDiscriminatorValue))
      } else {
        None
      }

    val hasMultipleDirectParents = generationAggr.directParents(originalToCanonicalName).size > 1

    val (interfacesToImplement, fieldsToGenerate, classToExtend) =
      (toHasOwnInterface, hasMultipleDirectParents) match {
        case (true, _) =>
          val interfaceToImpl = List(TransferObjectInterfaceDefinition(toClassDefinition, discriminator))
          val fieldsToGen     = allFields
          val classToExt      = None
          (interfaceToImpl, fieldsToGen, classToExt)
        case (false, false) =>
          val interfaceToImpl = List.empty[TransferObjectInterfaceDefinition]
          val fieldsToGen     = ownFields
          val classToExt =
            generationAggr
              .directParents(originalToCanonicalName)
              .headOption // There should be at most one direct parent.
              .map(parent => generationAggr.toMap.getOrElse(parent, sys.error(s"Expected to find $parent in the generation aggregate.")))
          (interfaceToImpl, fieldsToGen, classToExt)
        case (false, true) =>
          val interfaceToImpl =
            generationAggr
              .directParents(originalToCanonicalName)
              .map(parent => generationAggr.toMap.getOrElse(parent, sys.error(s"Expected to find $parent in the generation aggregate.")))
              .map(TransferObjectInterfaceDefinition(_, discriminator))
          val fieldsToGen = allFields
          val classToExt  = None
          (interfaceToImpl.toList, fieldsToGen, classToExt)
      }

    val skipFieldName = jsonTypeInfo.map(_.discriminator)

    val childrenToSerialize =
      if (!toHasOwnInterface) compileChildrenToSerialize(originalToCanonicalName, toClassDefinition, generationAggr)
      else Set.empty[ChildToSerialize]

    val importPointers: Seq[ClassPointer] = {
      fieldsToGenerate.map(_.classPointer) ++ childrenToSerialize.map(_.classReference) ++
        interfacesToImplement.map(_.origin.reference.classPointer) ++ Seq(classToExtend.map(_.reference.classPointer)).flatten
    }
    val imports: Set[String] = platform.importStatements(actualToCanonicalClassReference, importPointers.toSet)

    val jsonTypeAnnotations = generateJsonTypeAnnotations(childrenToSerialize, jsonTypeInfo)

    val source =
      s"""
        package ${actualToCanonicalClassReference.packageName};

        import com.fasterxml.jackson.annotation.*;

        ${imports.mkString("\n")}

        $jsonTypeAnnotations
        ${generatePojoSource(actualToCanonicalClassReference,
                             interfacesToImplement,
                             classToExtend,
                             fieldsToGenerate,
                             allFields,
                             skipFieldName)}
     """

    val generationAggrWithAddedInterfaces = interfacesToImplement.foldLeft(generationAggr) { (aggr, interf) =>
      generationAggr.addInterfaceSourceDefinition(interf)
    }

    val sourceFile =
      SourceFile(
        filePath = actualToCanonicalClassReference.toFilePath,
        content  = source
      )

    generationAggrWithAddedInterfaces.copy(sourceFilesGenerated = sourceFile +: generationAggrWithAddedInterfaces.sourceFilesGenerated)
  }

  private def generatePojoSource(toClassReference: ClassReference,
                                 interfacesToImplement: List[TransferObjectInterfaceDefinition],
                                 classToExtend: Option[TransferObjectClassDefinition],
                                 fieldsToGenerate: Seq[Field],
                                 allFields: Seq[Field],
                                 skipFieldName: Option[String] = None): String = {

    def fieldsWithoutSkipField(fields: Seq[Field]): Seq[Field] = {
      skipFieldName map { skipField =>
        fields.filterNot(_.fieldName == skipField)
      } getOrElse fields
    }

    val selectedFields = fieldsWithoutSkipField(fieldsToGenerate)
    val sortedFields   = selectedFields.sortBy(_.safeFieldName) // In Java Pojo's, we sort by field name!

    val selectedFieldsWithParentFields = fieldsWithoutSkipField(allFields)
    val sortedFieldsWithParentFields   = selectedFieldsWithParentFields.sortBy(_.safeFieldName)

    val privateFieldExpressions = sortedFields.map { field =>
      s"""
           @JsonProperty(value = "${field.fieldName}")
           private ${field.fieldDeclaration};
         """
    }

    val getterAndSetters = sortedFields map {
      case fieldRep @ Field(fieldName, classPointer, required) =>
        val fieldNameCap = fieldRep.safeFieldName.capitalize
        s"""
           public ${classPointer.classDefinition} get$fieldNameCap() {
             return ${fieldRep.safeFieldName};
           }

           public void set$fieldNameCap(${classPointer.classDefinition} ${fieldRep.safeFieldName}) {
             this.${fieldRep.safeFieldName} = ${fieldRep.safeFieldName};
           }

         """
    }

    val extendsClass = classToExtend.map(classToExt => s"extends ${classToExt.reference.classDefinition}").getOrElse("")
    val implementsClass =
      if (interfacesToImplement.nonEmpty)
        interfacesToImplement.map(classToImpl => classToImpl.origin.reference.classDefinition).mkString("implements ", ", ", "")
      else ""

    val constructorInitialization = sortedFieldsWithParentFields map { sf =>
      val fieldNameCap = sf.safeFieldName.capitalize
      s"this.set$fieldNameCap(${sf.safeFieldName});"
    }

    val constructorFieldDeclarations = sortedFieldsWithParentFields.map(_.fieldDeclaration)

    val fieldConstructor =
      if (constructorFieldDeclarations.nonEmpty)
        s"""
          public ${toClassReference.name}(${constructorFieldDeclarations.mkString(", ")}) {
            ${constructorInitialization.mkString("\n")}
          }
         """
      else ""

    s"""
      public class ${toClassReference.classDefinition} $extendsClass $implementsClass {

        ${privateFieldExpressions.mkString("\n")}

        public ${toClassReference.name}() {
        }

        $fieldConstructor

        ${getterAndSetters.mkString("\n")}

      }
     """
  }

}
