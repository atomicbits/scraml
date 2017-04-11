/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.ramlparser.model.canonicaltypes

/**
  * Created by peter on 11/12/16.
  */
case class UnionType(types: Set[TypeReference]) extends NonPrimitiveType {

  lazy val canonicalName: CanonicalName = {

    val classNames = types.map(_.refers.name).toList.sorted

    val packages = types.map(_.refers.packagePath).toList

    val commonPackage = findCommonPackage(packages)

    CanonicalName.create(name = s"UnionOf${classNames.mkString("With")}", packagePath = commonPackage)

  }

  private def findCommonPackage(packages: List[List[String]]): List[String] = {

    def findCommon(packs: List[List[String]], common: List[String] = List.empty): List[String] = {

      val headOpts = packs.map(_.headOption)

      if (headOpts.contains(None)) {
        common
      } else {
        val heads     = headOpts.map(_.get)
        val firstHead = heads.head
        if (heads.tail.exists(_ != firstHead)) {
          common
        } else {
          val tails = packs.map(_.tail)
          findCommon(tails, common :+ firstHead)
        }
      }

    }

    findCommon(packages)

  }

}
