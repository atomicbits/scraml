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

/**
  * Created by peter on 7/02/17.
  *
  * A class name is the final platform-specific class name for a generated class.
  * It is similar to a canonical name (which is a RAML parser term in our context), but then platform specific and matching the
  * final fully qualified class name.
  */
case class ClassName(name: String, packagePath: List[String] = List.empty)
