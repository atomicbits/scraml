/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.generator.platform.typescript

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform, SourceGenerator }
import io.atomicbits.scraml.generator.typemodel.{ TransferObjectClassDefinition, TransferObjectInterfaceDefinition }
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

    val fieldDefinitions: Seq[String] = toInterfaceDefinition.fields.map(_.fieldDeclaration)

    def childTypeDiscriminatorValues: List[String] = {
      val originalToCanonicalName         = toInterfaceDefinition.origin.reference.canonicalName
      val childNames: List[CanonicalName] = generationAggr.allChildren(originalToCanonicalName)
      childNames.flatMap { childName =>
        val childDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(childName, sys.error(s"Expected to find $childName in the generation aggregate."))
        childDefinition.typeDiscriminatorValue
      }
    }

    val typeDiscriminatorFieldDefinition: Seq[String] = {

      val typeDiscriminatorFieldDefOpt =
        for {
          typeDiscriminatorValue <- toInterfaceDefinition.origin.typeDiscriminatorValue
          typeDiscriminator          = toInterfaceDefinition.discriminator
          allTypeDiscriminatorValues = typeDiscriminatorValue :: childTypeDiscriminatorValues
        } yield {
          val typeDiscriminatorUnionString = allTypeDiscriminatorValues.map(CleanNameTools.quoteString).mkString(" | ")
          Seq(s"${CleanNameTools.quoteString(typeDiscriminator)}: $typeDiscriminatorUnionString,")
        }

      typeDiscriminatorFieldDefOpt.getOrElse(Seq.empty)
    }

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
