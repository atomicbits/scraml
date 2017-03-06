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
object PojoGenerator extends SourceGenerator {

  implicit val platform: Platform = JavaJackson

  val defaultDiscriminator = "type"

  def generate(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition): GenerationAggr = {

    /**
      * TOs are represented as POJOs in Java, which can without trouble extend from each other as long as only single-inheritance
      * is used. As soon as we deal with multiple-inheritance, we need to replace all POJOs that function as one of the parents in
      * a multiple-inheritance relation by their interface. The actual implementing class of that interface case gets the 'Impl' suffix.
      */
    val originalToCanonicalName = toClassDefinition.reference.canonicalName

    val hasOwnInterface = generationAggr.isParentInMultipleInheritanceRelation(originalToCanonicalName)

    val actualToCanonicalClassReference: ClassReference =
      if (hasOwnInterface) toClassDefinition.implementingInterfaceReference
      else toClassDefinition.reference

    val initialTosWithInterface: Seq[TransferObjectClassDefinition] =
      if (hasOwnInterface) Seq(toClassDefinition)
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

    val (interfaceToImplement, fieldsToGenerate, classToExtend) =
      if (hasOwnInterface) {
        val interfaceToImpl = Some(TransferObjectInterfaceDefinition(toClassDefinition, discriminator))
        val fieldsToGen     = allFields
        val classToExt      = None
        (interfaceToImpl, fieldsToGen, classToExt)
      } else {
        val interfaceToImpl = None
        val fieldsToGen     = ownFields
        val classToExt =
          generationAggr
            .directParents(originalToCanonicalName)
            .headOption // There should be at most one direct parent.
            .map(parent => generationAggr.toMap.getOrElse(parent, sys.error(s"Expected to find $parent in the generation aggregate.")))
        (interfaceToImpl, fieldsToGen, classToExt)
      }

    val generationAggrWithAddedInterfaces = interfaceToImplement.map(generationAggr.addInterfaceSourceDefinition).getOrElse(generationAggr)

    val childrenToSerialize =
      if (hasOwnInterface) {
        // serialization annotations will be kept in the top-level interface
        List.empty[TransferObjectClassDefinition]
      } else {
        if (!generationAggr.hasParents(originalToCanonicalName)) { // We're the top-level parent
          generationAggr
            .allChildren(originalToCanonicalName)
            .map(child => generationAggr.toMap.getOrElse(child, sys.error(s"Expected to find $child in the generation aggregate.")))
        } else {
          // serialization annotations only needs to be kept in the top-level parent
          List.empty[TransferObjectClassDefinition]
        }
      }

    generatePojo(
      interfaceToImplement,
      classToExtend,
      childrenToSerialize,
      fieldsToGenerate,
      allFields,
      jsonTypeInfo.map(_.discriminator),
      actualToCanonicalClassReference,
      toClassDefinition,
      jsonTypeInfo,
      generationAggrWithAddedInterfaces
    )
  }

  private def generatePojo(interfaceToImplement: Option[TransferObjectInterfaceDefinition],
                           classToExtend: Option[TransferObjectClassDefinition],
                           childrenToSerialize: List[TransferObjectClassDefinition],
                           fieldsToGenerate: Seq[Field],
                           allFields: Seq[Field],
                           skipFieldName: Option[String],
                           toClassReference: ClassReference,
                           toClassDefinition: TransferObjectClassDefinition,
                           jsonTypeInfo: Option[JsonTypeInfo],
                           generationAggr: GenerationAggr): GenerationAggr = {

    val importPointers: Seq[ClassPointer] = {
      fieldsToGenerate.map(_.classPointer) ++ childrenToSerialize.map(_.reference.classPointer) ++
        Seq(interfaceToImplement.map(_.origin.reference.classPointer), classToExtend.map(_.reference.classPointer)).flatten
    }
    val imports: Set[String] = platform.importStatements(toClassReference, importPointers.toSet)

    val jsonTypeAnnotations =
      if (childrenToSerialize.nonEmpty) {

        val jsonSubTypes =
          (toClassDefinition :: childrenToSerialize).map { toSerialize =>
            val discriminatorValue = toSerialize.actualTypeDiscriminatorValue
            val name               = toSerialize.reference.name
            s"""
               @JsonSubTypes.Type(value = $name.class, name = "$discriminatorValue")
             """
          }

        val typeDiscriminator = jsonTypeInfo.map(_.discriminator).getOrElse("type")

        s"""
           @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "$typeDiscriminator")
           @JsonSubTypes({
                 ${jsonSubTypes.mkString(",\n")}
           })
         """
      } else {
        ""
      }

    val source =
      s"""
        package ${toClassReference.packageName};

        import com.fasterxml.jackson.annotation.*;

        ${imports.mkString("\n")}

        $jsonTypeAnnotations
        ${generatePojoSource(toClassReference, interfaceToImplement, classToExtend, fieldsToGenerate, allFields, skipFieldName)}
     """

    val sourceFile =
      SourceFile(
        filePath = toClassReference.toFilePath,
        content  = source
      )

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

  private def generatePojoSource(toClassReference: ClassReference,
                                 interfaceToImplement: Option[TransferObjectInterfaceDefinition],
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
      interfaceToImplement.map(classToImpl => s"implements ${classToImpl.origin.reference.classDefinition}").getOrElse("")

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
