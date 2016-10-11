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

package io.atomicbits.scraml.ramlparser.parser

import org.yaml.snakeyaml.constructor.{Construct, AbstractConstruct, SafeConstructor}
import org.yaml.snakeyaml.nodes.{ScalarNode, Node, Tag}


/**
  * Created by peter on 6/02/16.
  */
class SimpleRamlConstructor extends SafeConstructor {

  def addYamlConstructor(tag: Tag, construct: Construct): Unit = {
    this.yamlConstructors.put(tag, construct)
  }

}


class ConstructInclude extends AbstractConstruct {

  @SuppressWarnings(Array("unchecked"))
  def construct(node: Node): Object  = {
    val snode: ScalarNode  =  node.asInstanceOf[ScalarNode]
    Include(snode.getValue)
  }

}


object SimpleRamlConstructor {

  def apply(): SimpleRamlConstructor = {
    val constructor = new SimpleRamlConstructor
    constructor.addYamlConstructor(new Tag("!include"), new ConstructInclude())
    constructor
  }

}
