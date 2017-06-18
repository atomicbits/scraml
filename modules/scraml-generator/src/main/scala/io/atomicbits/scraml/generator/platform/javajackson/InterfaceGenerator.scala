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
import io.atomicbits.scraml.generator.platform.SourceGenerator
import io.atomicbits.scraml.generator.typemodel._
import io.atomicbits.scraml.generator.platform.Platform._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 1/03/17.
  */
case class InterfaceGenerator(javaJackson: JavaJackson) extends SourceGenerator with PojoGeneratorSupport {

  implicit val platform: JavaJackson = javaJackson

  def generate(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr = {

    val toClassDefinition       = toInterfaceDefinition.origin
    val originalToCanonicalName = toClassDefinition.reference.canonicalName

    val parentNames: List[CanonicalName] = generationAggr.allParents(originalToCanonicalName)

    val initialTosWithInterface: Seq[TransferObjectClassDefinition] = Seq(toInterfaceDefinition.origin)
    val ownFields: Seq[Field]                                       = toInterfaceDefinition.origin.fields
    val interfacesAndFieldsAggr                                     = (initialTosWithInterface, ownFields)

    val fields: Seq[Field] =
      parentNames.foldLeft(toInterfaceDefinition.origin.fields) { (collectedFields, parentName) =>
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        collectedFields ++ parentDefinition.fields
      }

    val interfacesToImplement =
      generationAggr
        .directParents(originalToCanonicalName)
        .foldLeft(Seq.empty[TransferObjectClassDefinition]) { (intsToImpl, parentName) =>
          val parentDefinition =
            generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
          intsToImpl :+ parentDefinition
        }
        .map(TransferObjectInterfaceDefinition(_, toInterfaceDefinition.discriminator))

    val isTopLevelInterface = !generationAggr.hasParents(originalToCanonicalName)

    val childrenToSerialize =
      if (isTopLevelInterface) {
        generationAggr
          .allChildren(originalToCanonicalName)
          .map(child => generationAggr.toMap.getOrElse(child, sys.error(s"Expected to find $child in the generation aggregate.")))
      } else {
        List.empty[TransferObjectClassDefinition]
      }

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
        .getOrElse(PojoGenerator(platform).defaultDiscriminator)

    val jsonTypeInfo: Option[JsonTypeInfo] =
      if (generationAggr.isInHierarchy(originalToCanonicalName)) {
        Some(JsonTypeInfo(discriminator = discriminator, discriminatorValue = toClassDefinition.actualTypeDiscriminatorValue))
      } else {
        None
      }

    generateInterface(
      originalToCanonicalName,
      interfacesToImplement.toList,
      childrenToSerialize,
      allFields,
      jsonTypeInfo.map(_.discriminator),
      toInterfaceDefinition.classReference,
      toClassDefinition,
      jsonTypeInfo,
      generationAggr
    )
  }

  private def generateInterface(originalToCanonicalName: CanonicalName,
                                interfacesToImplement: List[TransferObjectInterfaceDefinition],
                                childrenToSerialize: List[TransferObjectClassDefinition],
                                fieldsToGenerate: Seq[Field],
                                skipFieldName: Option[String],
                                interfaceClassReference: ClassReference,
                                interfaceClassDefinition: TransferObjectClassDefinition,
                                jsonTypeInfo: Option[JsonTypeInfo],
                                generationAggr: GenerationAggr): GenerationAggr = {

    val childrenToSerialize = compileChildrenToSerialize(originalToCanonicalName, interfaceClassDefinition, generationAggr)

    val importPointers: Seq[ClassPointer] = {
      fieldsToGenerate.map(_.classPointer) ++ childrenToSerialize.map(_.classReference) ++
        interfacesToImplement.map(_.origin.reference.classPointer)
    }
    val imports: Set[String] = platform.importStatements(interfaceClassReference, importPointers.toSet)

    val jsonTypeAnnotations = generateJsonTypeAnnotations(childrenToSerialize, jsonTypeInfo)

    val source =
      s"""
        package ${interfaceClassReference.packageName};

        import com.fasterxml.jackson.annotation.*;

        ${imports.mkString("\n")};

        $jsonTypeAnnotations
        ${generateInterfaceSource(interfaceClassReference, interfacesToImplement, fieldsToGenerate, skipFieldName)}
     """

    val sourceFile =
      SourceFile(
        filePath = interfaceClassReference.toFilePath,
        content  = source
      )

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

  private def generateInterfaceSource(toClassReference: ClassReference,
                                      interfacesToImplement: List[TransferObjectInterfaceDefinition],
                                      fieldsToGenerate: Seq[Field],
                                      skipFieldName: Option[String] = None): String = {

    val selectedFields =
      skipFieldName map { skipField =>
        fieldsToGenerate.filterNot(_.fieldName == skipField)
      } getOrElse fieldsToGenerate

    val sortedFields = selectedFields.sortBy(_.safeFieldName) // In Java Pojo's, we sort by field name!

    val getterAndSetters = sortedFields map {
      case fieldRep @ Field(fieldName, classPointer, required) =>
        val fieldNameCap = fieldRep.safeFieldName.capitalize
        s"""
           public ${classPointer.classDefinition} get$fieldNameCap();

           public void set$fieldNameCap(${classPointer.classDefinition} ${fieldRep.safeFieldName});
         """
    }

    val implementsInterfaces = interfacesToImplement.map(classToImpl => s"${classToImpl.origin.reference.classDefinition}")
    val implementsStatement =
      if (implementsInterfaces.nonEmpty) implementsInterfaces.mkString("implements ", ",", "")
      else ""

    val fieldDeclarations = sortedFields.map(_.fieldDeclaration)

    s"""
      public interface ${toClassReference.classDefinition} $implementsStatement {

        ${getterAndSetters.mkString("\n")}

      }
     """
  }

}
