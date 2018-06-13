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
case class TraitGenerator(scalaPlay: ScalaPlay) extends SourceGenerator {

  implicit val platform: ScalaPlay = scalaPlay

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

    val dslBasePackage = platform.rewrittenDslBasePackage.mkString(".")

    val source =
      s"""
        package ${toInterfaceDefinition.classReference.packageName}

        import play.api.libs.json._
        import $dslBasePackage.json.TypedJson._

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

    generationAggr.addSourceFile(sourceFile)
  }

}
