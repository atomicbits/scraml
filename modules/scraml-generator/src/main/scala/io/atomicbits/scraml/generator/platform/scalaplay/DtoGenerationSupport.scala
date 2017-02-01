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

import io.atomicbits.scraml.generator.platform.Platform
import io.atomicbits.scraml.generator.typemodel._

/**
  * Created by peter on 1/02/17.
  */
trait DtoGenerationSupport {

  def collectImports(toClassReference: ClassReference, fields: Seq[Field] = Seq.empty, dependencies: Seq[ClassReference] = Seq.empty)(
      implicit platform: Platform): Set[String] = {

    import Platform._

    val ownPackage = toClassReference.packageName

    def collectTypeImports(collected: Set[String], classPtr: ClassPointer): Set[String] = {

      def importFromClassReference(classRef: ClassReference): Option[String] = {
        if (classRef.packageName != ownPackage && !classRef.predef) Some(s"import ${classRef.fullyQualifiedName}")
        else None
      }

      val classReferene = classPtr.native
      val collectedWithClassRef =
        importFromClassReference(classReferene).map(classRefImport => collected + classRefImport).getOrElse(collected)

      classReferene.typeParamValues.values.toSet.foldLeft(collectedWithClassRef)(collectTypeImports)
    }

    val typeImports: Set[String] = collectTypeImports(Set.empty, toClassReference)

    val typeAndFieldImports: Set[String] = fields.map(_.classPointer).foldLeft(typeImports)(collectTypeImports)

    // Remember that the fields of the traits are already covered.
    val typeAndFieldAndTraitImports: Set[String] = dependencies.foldLeft(typeAndFieldImports)(collectTypeImports)

    typeAndFieldAndTraitImports
  }

}
