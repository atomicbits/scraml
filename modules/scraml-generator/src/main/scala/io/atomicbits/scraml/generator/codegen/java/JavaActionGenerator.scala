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

package io.atomicbits.scraml.generator.codegen.java

import io.atomicbits.scraml.generator.model.{RichResource, ClassRep}

/**
 * Created by peter on 25/09/15.
 */
object JavaActionGenerator {

  case class JavaActionFunctionResult(imports: Set[String] = Set.empty,
                                      fields: List[String] = List.empty,
                                      classes: List[ClassRep] = List.empty) {

    def ++(other: JavaActionFunctionResult): JavaActionFunctionResult =
      JavaActionFunctionResult(imports ++ other.imports, fields ++ other.fields, classes ++ other.classes)

  }


  def generateActionFunctions(resource: RichResource): JavaActionFunctionResult = {
    ???
  }

}
