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

/**
  * Created by peter on 14/01/17.
  */
object TraitGenerator extends SourceGenerator {

  implicit val platform: Platform = ScalaPlay

  def generate(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr = {

    val toCanonicalName = toInterfaceDefinition.origin.reference.canonicalName

    val fields: Seq[Field] = toInterfaceDefinition.origin.fields

    val implementingClassNames                                  = generationAggr.children(toCanonicalName) + toCanonicalName
    val implementingClasses: Set[TransferObjectClassDefinition] = implementingClassNames.map(generationAggr.toMap)

    generateTrait(fields, toInterfaceDefinition, implementingClasses, generationAggr)
  }

  private def generateTrait(fields: Seq[Field],
                            toInterfaceDefinition: TransferObjectInterfaceDefinition,
                            implementingClasses: Set[TransferObjectClassDefinition],
                            generationAggr: GenerationAggr): GenerationAggr = {

    def leafClassRepToWithTypeHintExpression(leafClassRep: TransferObjectClassDefinition): String = {
      val discriminatorValue =
        leafClassRep.jsonTypeInfo.map(_.discriminatorValue).getOrElse(toInterfaceDefinition.origin.reference.name)
      s"""${leafClassRep.reference.name}.jsonFormatter.withTypeHint("$discriminatorValue")"""
    }

    val imports: Set[String] =
      platform
        .importStatements(toInterfaceDefinition.classReference, fields.map(_.classPointer).toSet ++ implementingClasses.map(_.reference))

    val fieldDefinitions = fields.map(_.fieldExpression).map(fieldExpr => s"def $fieldExpr")

    val source =
      s"""
        package ${toInterfaceDefinition.classReference.packageName}

        import play.api.libs.json._
        import io.atomicbits.scraml.dsl.json.TypedJson._

        ${imports.mkString("\n")}
        
        trait ${toInterfaceDefinition.classReference.classDefinition} {

          ${fieldDefinitions.mkString("\n\n")}

        }

        object ${toInterfaceDefinition.classReference.name} {

          implicit val jsonFormat: Format[${toInterfaceDefinition.classReference.classDefinition}] =
            TypeHintFormat(
              "${toInterfaceDefinition.discriminator}",
              ${implementingClasses.map(leafClassRepToWithTypeHintExpression).mkString(",\n")}
            )
        }
      """

    val sourceFile =
      SourceFile(
        filePath = platform.classReferenceToFilePath(toInterfaceDefinition.classReference),
        content  = source
      )

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

}
