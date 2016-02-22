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

package io.atomicbits.scraml.jdsl;

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
