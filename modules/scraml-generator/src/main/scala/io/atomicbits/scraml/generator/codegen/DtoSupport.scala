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

package io.atomicbits.scraml.generator.codegen

import io.atomicbits.scraml.generator.model.{TypedClassReference, ClassReference, ClassPointer, ClassRep}

/**
 * Created by peter on 2/10/15.
 */
trait DtoSupport {

  /**
   * Collect all type imports for a given class and its generic types, but not its parent or child classes.
   */
  def collectImports(collectClassRep: ClassRep): Set[String] = {

    val ownPackage = collectClassRep.packageName

    /**
     * Collect the type imports for the given class rep.
     */
    def collectTypeImports(collected: Set[String], classPtr: ClassPointer): Set[String] = {

      def collectFromClassReference(classRef: ClassReference): Set[String] = {
        if (classRef.packageName != ownPackage && !classRef.predef) collected + s"import ${classRef.fullyQualifiedName}"
        else Set.empty[String]
      }

      val collectedFromClassPtr =
        classPtr match {
          case typedClassReference: TypedClassReference =>
            typedClassReference.types.values.foldLeft(collectFromClassReference(typedClassReference.classReference))(collectTypeImports)
          case classReference: ClassReference           => collectFromClassReference(classReference)
          case _                                        => Set.empty[String]
        }

      collectedFromClassPtr ++ collected
    }

    val ownTypesImport = collectTypeImports(Set.empty, collectClassRep.classRef)

    collectClassRep.fields.map(_.classPointer).foldLeft(ownTypesImport)(collectTypeImports)
  }

}
