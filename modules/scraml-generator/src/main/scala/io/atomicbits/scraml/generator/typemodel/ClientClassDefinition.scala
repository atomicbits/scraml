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

package io.atomicbits.scraml.generator.typemodel

import io.atomicbits.scraml.generator.platform.{ CleanNameTools, Platform }

/**
  * Created by peter on 13/01/17.
  *
  * In a client class definition, we collect all information that is needed to generate a client class, independent from
  * the target language.
  */
case class ClientClassDefinition(apiName: String,
                                 baseUri: Option[String],
                                 basePackage: List[String],
                                 topLevelResourceDefinitions: List[ResourceClassDefinition],
                                 title: String       = "",
                                 version: String     = "",
                                 description: String = "") // ToDo: insert title, version and description from the RAML model
    extends SourceDefinition {

  override def classReference(implicit platform: Platform): ClassReference = {
    val apiClassName = CleanNameTools.cleanClassNameFromFileName(apiName)
    ClassReference(name = apiClassName, packageParts = basePackage)
  }

}
