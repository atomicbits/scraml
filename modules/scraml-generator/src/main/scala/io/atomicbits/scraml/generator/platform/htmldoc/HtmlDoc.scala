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

package io.atomicbits.scraml.generator.platform.htmldoc

import java.nio.file.{ Path, Paths }

import io.atomicbits.scraml.generator.codegen.GenerationAggr
import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel._

/**
  * Created by peter on 9/05/18.
  */
object HtmlDoc extends Platform {

  val name = "HTML Documentation"

  override def apiBasePackageParts = List.empty[String]

  override def dslBasePackageParts = List.empty[String]

  override def rewrittenDslBasePackage = List.empty[String]

  override def classPointerToNativeClassReference(classPointer: ClassPointer) = ???

  override def implementingInterfaceReference(classReference: ClassReference) = ???

  override def classDefinition(classPointer: ClassPointer, fullyQualified: Boolean) = ???

  override def className(classPointer: ClassPointer) = ???

  override def packageName(classPointer: ClassPointer) = ???

  override def fullyQualifiedName(classPointer: ClassPointer) = ???

  override def safePackageParts(classPointer: ClassPointer) = ???

  override def safeFieldName(field: Field) = ???

  override def fieldDeclarationWithDefaultValue(field: Field) = ???

  override def fieldDeclaration(field: Field) = ???

  override def importStatements(targetClassReference: ClassPointer, dependencies: Set[ClassPointer]) = ???

  override def toSourceFile(generationAggr: GenerationAggr, toClassDefinition: TransferObjectClassDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, toInterfaceDefinition: TransferObjectInterfaceDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, enumDefinition: EnumDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, clientClassDefinition: ClientClassDefinition) =
    IndexDocGenerator.generate(generationAggr, clientClassDefinition)

  override def toSourceFile(generationAggr: GenerationAggr, resourceClassDefinition: ResourceClassDefinition)           = generationAggr
  override def toSourceFile(generationAggr: GenerationAggr, headerSegmentClassDefinition: HeaderSegmentClassDefinition) = generationAggr

  override def toSourceFile(generationAggr: GenerationAggr, unionClassDefinition: UnionClassDefinition) = generationAggr

  val classFileExtension = "html"

  /**
    * Transforms a given class reference to a file path. The given class reference already has clean package and class names.
    *
    * @param classPointer The class reference for which a file path is generated.
    * @return The relative file name for the given class.
    */
  override def toFilePath(classPointer: ClassPointer): Path = {
    classPointer match {
      case classReference: ClassReference =>
        Paths.get("", s"${classReference.name}.$classFileExtension") // This results in a relative path both on Windows as on Linux/Mac
      case _ => sys.error(s"Cannot create a file path from a class pointer that is not a class reference!")
    }
  }

  override def reservedKeywords = Set.empty[String]

}
