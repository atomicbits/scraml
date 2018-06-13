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
