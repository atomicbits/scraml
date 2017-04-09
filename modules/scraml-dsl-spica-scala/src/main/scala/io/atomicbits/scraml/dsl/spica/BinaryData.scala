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
import _root_.java.nio.file._

import scala.util.{ Success, Try }

/**
  * Created by peter on 20/01/16.
  */
/**
  * The BinaryData object is the Response result type when binary data is received from the server.
  */
trait BinaryData {

  def asBytes: Array[Byte]

  /**
    * Request the binary data as a stream. This is convenient when there is a large amount of data to receive.
    * You can only request the input stream once because the data is not stored along the way!
    * Do not close the stream after use.
    *
    * @return An inputstream for reading the binary data.
    */
  def asStream: InputStream

  def asString: String

  def asString(charset: String): String

  /**
    * Write the binary data as a stream to the given file. This call can only be executed once!
    *
    * @param path The path (file) to write the binary data to.
    * @param options The copy options.
    * @return A Success if the copying was successful, a Failure otherwise.
    */
  def writeToFile(path: Path, options: CopyOption*): Try[_] = {
    val directoriesCreated =
      Option(path.getParent) collect {
        case parent => Try(Files.createDirectories(parent))
      } getOrElse Success(Unit)

    directoriesCreated flatMap { _ =>
      Try(Files.copy(asStream, path, options: _*))
    }
  }

  /**
    * Write the binary data as a stream to the given file. This call can only be executed once!
    *
    * @param file The file to write the binary data to.
    * @return A Success if the copying was successful, a Failure otherwise.
    */
  def writeToFile(file: File): Try[_] = writeToFile(file.toPath)

}
