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

package io.atomicbits.scraml.dsl.javajackson;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by peter on 22/01/16.
 */
public abstract class BinaryData {

    public abstract byte[] asBytes() throws IOException;

    /**
     * Request the binary data as a stream. This is convenient when there is a large amount of data to receive.
     * You can only request the input stream once because the data is not stored along the way!
     * Do not close the stream after use.
     *
     * @return An inputstream for reading the binary data.
     */
    public abstract InputStream asStream() throws IOException;

    public abstract String asString() throws IOException;

    public abstract String asString(String charset) throws IOException;

    public void writeToFile(Path path, CopyOption... options) throws IOException {
        Path parent =  path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.copy(asStream(), path, options);
    }

    public void writeToFile(File file) throws IOException {
        writeToFile(file.toPath());
    }

}
