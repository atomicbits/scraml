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

package io.atomicbits.scraml.generator.platform

import io.atomicbits.scraml.generator.typemodel._

/**
  * Created by peter on 10/01/17.
  */
trait Platform {

  def classDefinition(classPointer: ClassPointer): String

  def className(classPointer: ClassPointer): String

  def packageName(classPointer: ClassPointer): String

  def fullyQualifiedName(classPointer: ClassPointer): String

  def safePackageParts(classPointer: ClassPointer): List[String]

  def canonicalName(classPointer: ClassPointer): String

  def safeFieldName(field: Field): String

  def fieldExpression(field: Field): String

  def toSourceFile(toClassDefinition: TransferObjectClassDefinition): SourceFile

  def toSourceFile(toInterfaceDefinition: TransferObjectInterfaceDefinition): SourceFile

  def toSourceFile(enumDefinition: EnumDefinition): SourceFile

  def toSourceFile(clientClassDefinition: ClientClassDefinition): SourceFile

  def toSourceFile(resourceClassDefinition: ResourceClassDefinition): SourceFile

  def toSourceFile(unionClassDefinition: UnionClassDefinition): SourceFile

  def stringClassReference: ClassReference

  def longClassReference(primitive: Boolean = false): ClassReference

  def doubleClassReference(primitive: Boolean = false): ClassReference

  def booleanClassReference(primitive: Boolean = false): ClassReference

  def arrayClassReference(arrayType: ClassReference): ClassPointer

  def listClassReference(typeParamName: String): ClassReference

  def byteClassReference: ClassReference

  def binaryDataClassReference: ClassReference

  def fileClassReference: ClassReference

  def inputStreamClassReference: ClassReference

  def jsObjectClassReference: ClassReference

  def jsValueClassReference: ClassReference

}

object Platform {

  implicit class PlatformClassPointerOps(val classPointer: ClassPointer) {

    def classDefinition(implicit platform: Platform): String = platform.classDefinition(classPointer)

    def packageName(implicit platform: Platform): String = platform.packageName(classPointer)

    def fullyQualifiedName(implicit platform: Platform): String = platform.fullyQualifiedName(classPointer)

    def safePackageParts(implicit platform: Platform): List[String] = platform.safePackageParts(classPointer)

    def canonicalName(implicit platform: Platform): String = platform.canonicalName(classPointer)

  }

  implicit class PlatformFieldOps(val field: Field) {

    def safeFieldName(implicit platform: Platform): String = platform.safeFieldName(field)

    def fieldExpression(implicit platform: Platform): String = platform.fieldExpression(field)
  }

  implicit class PlatformToClassDefinitionOps(val toClassDefinition: TransferObjectClassDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(toClassDefinition)

  }

  implicit class PlatformToInterfaceDefinitionOps(val toInterfaceDefinition: TransferObjectInterfaceDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(toInterfaceDefinition)

  }

  implicit class PlatformEnumDefinitionOps(val enumDefinition: EnumDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(enumDefinition)

  }

  implicit class PlatformClientClassDefinitionOps(val clientClassDefinition: ClientClassDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(clientClassDefinition)

  }

  implicit class PlatformResourceClassDefinitionOps(val resourceClassDefinition: ResourceClassDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(resourceClassDefinition)

  }

  implicit class PlatformUnionClassDefinitionOps(val unionClassDefinition: UnionClassDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile = platform.toSourceFile(unionClassDefinition)

  }

  implicit class PlatformSourceCodeOps(val sourceCode: SourceDefinition) {

    def toSourceFile(implicit platform: Platform): SourceFile =
      sourceCode match {
        case clientClassDefinition: ClientClassDefinition => new PlatformClientClassDefinitionOps(clientClassDefinition).toSourceFile
        case resourceClassDefinition: ResourceClassDefinition =>
          new PlatformResourceClassDefinitionOps(resourceClassDefinition).toSourceFile
        case toClassDefinition: TransferObjectClassDefinition => new PlatformToClassDefinitionOps(toClassDefinition).toSourceFile
        case toInterfaceDefinition: TransferObjectInterfaceDefinition =>
          new PlatformToInterfaceDefinitionOps(toInterfaceDefinition).toSourceFile
        case enumDefinition: EnumDefinition             => new PlatformEnumDefinitionOps(enumDefinition).toSourceFile
        case unionClassDefinition: UnionClassDefinition => new PlatformUnionClassDefinitionOps(unionClassDefinition).toSourceFile
      }

  }

}
