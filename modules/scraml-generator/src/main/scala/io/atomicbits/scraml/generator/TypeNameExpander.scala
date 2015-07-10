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

package io.atomicbits.scraml.generator

import io.atomicbits.scraml.jsonschemaparser.{TypeClassRep, PlainClassRep, ClassRep}

import scala.reflect.macros.whitebox
import scala.language.experimental.macros


/**
 * Created by peter on 24/06/15. 
 */
object TypeNameExpander {

  def expand(classRep: ClassRep, c: whitebox.Context): c.universe.Tree = {

    import c.universe._

    classRep match {
      case plainClassRep: PlainClassRep =>
        // Don't ask my why we have to do it this way to get a TypeName into a Tree without ending up with strings later on.
        val className = TypeName(plainClassRep.name)
        val q"val foo: $classType" = q"val foo: $className"
        classType
      case typeClassRep: TypeClassRep   =>
        val className = TypeName(typeClassRep.name)
        val q"val foo: $typedClassType" = q"val foo: ${className}[..${typeClassRep.types.map(expand(_, c))}]"
        typedClassType
    }

  }

}
