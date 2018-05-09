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

package io.atomicbits.scraml.generator.platform.typescript

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ Field, TransferObjectClassDefinition, TransferObjectInterfaceDefinition }
import Platform._
import io.atomicbits.scraml.ramlparser.model.canonicaltypes.CanonicalName
import io.atomicbits.scraml.ramlparser.parser.SourceFile

/**
  * Created by peter on 15/12/17.
  */
case class InterfaceGenerator(typeScript: TypeScript) extends SourceGenerator {

  implicit val platform: TypeScript = typeScript

  def generate(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr = {

    val classReference = toInterfaceDefinition.classReference

    val extendsInterfaces: String = {
      val parentClassDefs = toInterfaceDefinition.origin.parents.map(_.classDefinition)
      parentClassDefs match {
        case Nil          => ""
        case nonEmptyList => s"extends ${nonEmptyList.mkString(", ")}"
      }
    }

    val originalToCanonicalName = toInterfaceDefinition.origin.reference.canonicalName

    def childTypeDiscriminatorValues: List[String] = {
      val childNames: List[CanonicalName] = generationAggr.allChildren(originalToCanonicalName)
      childNames.map { childName =>
        val childDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(childName, sys.error(s"Expected to find $childName in the generation aggregate."))
        childDefinition.actualTypeDiscriminatorValue
      }
    }

    val (typeDiscriminatorFieldDefinition, fields): (Seq[String], List[Field]) = {
      if (generationAggr.isInHierarchy(originalToCanonicalName)) {
        val typeDiscriminator            = toInterfaceDefinition.discriminator
        val typeDiscriminatorValue       = toInterfaceDefinition.origin.actualTypeDiscriminatorValue
        val allTypeDiscriminatorValues   = typeDiscriminatorValue :: childTypeDiscriminatorValues
        val typeDiscriminatorUnionString = allTypeDiscriminatorValues.map(CleanNameTools.quoteString).mkString(" | ")
        val typeDiscFieldDef             = Seq(s"${CleanNameTools.quoteString(typeDiscriminator)}: $typeDiscriminatorUnionString,")
        val fields = toInterfaceDefinition.fields.collect {
          case field if field.fieldName != typeDiscriminator => field
        }
        (typeDiscFieldDef, fields)
      } else {
        (Seq.empty, toInterfaceDefinition.fields)
      }
    }

    val fieldDefinitions: Seq[String] = fields.map(_.fieldDeclaration)

    val source =
      s"""
         |export interface ${classReference.classDefinition} $extendsInterfaces {
         |  ${(fieldDefinitions ++ typeDiscriminatorFieldDefinition).mkString(",\n  ")}
         |}
       """.stripMargin

    val sourceFile =
      SourceFile(
        filePath = classReference.toFilePath,
        content  = source
      )

    generationAggr.addSourceFile(sourceFile)
  }

}
