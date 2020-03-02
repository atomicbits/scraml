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
      } getOrElse Success(())

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
