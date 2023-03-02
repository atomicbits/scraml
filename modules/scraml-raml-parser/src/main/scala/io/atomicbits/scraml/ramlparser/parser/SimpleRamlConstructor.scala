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

package io.atomicbits.scraml.ramlparser.parser

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.constructor.{ AbstractConstruct, Construct, SafeConstructor }
import org.yaml.snakeyaml.nodes.{ Node, ScalarNode, Tag }

/**
  * Created by peter on 6/02/16.
  */
class SimpleRamlConstructor extends SafeConstructor(new LoaderOptions()) {

  def addYamlConstructor(tag: Tag, construct: Construct): Unit = {
    this.yamlConstructors.put(tag, construct)
  }

}

class ConstructInclude extends AbstractConstruct {

  @SuppressWarnings(Array("unchecked"))
  def construct(node: Node): Object = {
    val snode: ScalarNode = node.asInstanceOf[ScalarNode]
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
