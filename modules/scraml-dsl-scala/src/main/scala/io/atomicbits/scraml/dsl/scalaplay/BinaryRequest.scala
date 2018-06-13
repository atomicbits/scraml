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

package io.atomicbits.scraml.dsl.scalaplay

import _root_.java.io.{ InputStream, File }

/**
  * Created by peter on 15/01/16.
  */
/**
  * The Binary Request object is an internal type of the DSL and is not directly visible for the user.
  */
sealed trait BinaryRequest

case class FileBinaryRequest(file: File) extends BinaryRequest

case class InputStreamBinaryRequest(inputStream: InputStream) extends BinaryRequest

case class ByteArrayBinaryRequest(byteArray: Array[Byte]) extends BinaryRequest

case class StringBinaryRequest(text: String) extends BinaryRequest

object BinaryRequest {

  def apply(file: File): BinaryRequest = FileBinaryRequest(file)

  def apply(inputStream: InputStream): BinaryRequest = InputStreamBinaryRequest(inputStream)

  def apply(byteArray: Array[Byte]): BinaryRequest = ByteArrayBinaryRequest(byteArray)

  def apply(text: String): BinaryRequest = StringBinaryRequest(text)

}
