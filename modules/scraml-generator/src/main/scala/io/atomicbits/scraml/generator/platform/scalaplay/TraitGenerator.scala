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
object TraitGenerator extends ScalaPlaySourceGenerator {

  implicit val platform: Platform = ScalaPlay

  def generate(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition): GenerationAggr = {

    val toCanonicalName = toInterfaceDefinition.origin.reference.canonicalName

    val parentNames: List[CanonicalName] = generationAggr.allParents(toCanonicalName)

    val fields: Seq[Field] =
      parentNames.foldLeft(toInterfaceDefinition.origin.fields) { (collectedFields, parentName) =>
        val parentDefinition: TransferObjectClassDefinition =
          generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
        collectedFields ++ parentDefinition.fields
      }

    val traitsToImplement =
      generationAggr
        .directParents(toCanonicalName)
        .foldLeft(Seq.empty[TransferObjectClassDefinition]) { (traitsToImpl, parentName) =>
          val parentDefinition =
            generationAggr.toMap.getOrElse(parentName, sys.error(s"Expected to find $parentName in the generation aggregate."))
          traitsToImpl :+ parentDefinition
        }
        .map(TransferObjectInterfaceDefinition(_, toInterfaceDefinition.discriminator))

    val implementingNonLeafClassNames                                  = generationAggr.nonLeafChildren(toCanonicalName) + toCanonicalName
    val implementingLeafClassNames                                     = generationAggr.leafChildren(toCanonicalName)
    val implementingNonLeafClasses: Set[TransferObjectClassDefinition] = implementingNonLeafClassNames.map(generationAggr.toMap)
    val implementingLeafClasses: Set[TransferObjectClassDefinition]    = implementingLeafClassNames.map(generationAggr.toMap)

    generateTrait(fields, traitsToImplement, toInterfaceDefinition, implementingNonLeafClasses, implementingLeafClasses, generationAggr)
  }

  private def generateTrait(fields: Seq[Field],
                            traitsToImplement: Seq[TransferObjectInterfaceDefinition],
                            toInterfaceDefinition: TransferObjectInterfaceDefinition,
                            implementingNonLeafClasses: Set[TransferObjectClassDefinition],
                            implementingLeafClasses: Set[TransferObjectClassDefinition],
                            generationAggr: GenerationAggr): GenerationAggr = {

    def childClassRepToWithTypeHintExpression(childClassRep: TransferObjectClassDefinition, isLeaf: Boolean): String = {
      val discriminatorValue = childClassRep.actualTypeDiscriminatorValue
      val childClassName =
        if (isLeaf) childClassRep.reference.name
        else childClassRep.implementingInterfaceReference.name

      s"""$childClassName.jsonFormatter.withTypeHint("$discriminatorValue")"""
    }

    val imports: Set[String] =
      platform
        .importStatements(
          toInterfaceDefinition.classReference,
          fields.map(_.classPointer).toSet ++
            implementingNonLeafClasses.map(_.implementingInterfaceReference) ++
            implementingLeafClasses.map(_.reference) ++
            traitsToImplement.map(_.classReference)
        )

    val fieldDefinitions = fields.map(_.fieldDeclaration).map(fieldExpr => s"def $fieldExpr")

    val typeHintExpressions =
      implementingNonLeafClasses.map(childClassRepToWithTypeHintExpression(_, isLeaf = false)) ++
        implementingLeafClasses.map(childClassRepToWithTypeHintExpression(_, isLeaf  = true))

    val extendedTraitDefs = traitsToImplement.map(_.classReference.classDefinition)

    val extendsExpression =
      if (extendedTraitDefs.nonEmpty) extendedTraitDefs.mkString("extends ", " with ", "")
      else ""

    val source =
      s"""
        package ${toInterfaceDefinition.classReference.packageName}

        import play.api.libs.json._
        import $dslBasePackageString.json.TypedJson._

        ${imports.mkString("\n")}
        
        trait ${toInterfaceDefinition.classReference.classDefinition} $extendsExpression {

          ${fieldDefinitions.mkString("\n\n")}

        }

        object ${toInterfaceDefinition.classReference.name} {

          implicit val jsonFormat: Format[${toInterfaceDefinition.classReference.classDefinition}] =
            TypeHintFormat(
              "${toInterfaceDefinition.discriminator}",
              ${typeHintExpressions.mkString(",\n")}
            )
        }
      """

    val sourceFile =
      SourceFile(
        filePath = toInterfaceDefinition.classReference.toFilePath,
        content  = source
      )

    generationAggr.copy(sourceFilesGenerated = sourceFile +: generationAggr.sourceFilesGenerated)
  }

}
