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

package io.atomicbits.scraml.dsl.spica

import _root_.java.io.{ File, InputStream }

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
