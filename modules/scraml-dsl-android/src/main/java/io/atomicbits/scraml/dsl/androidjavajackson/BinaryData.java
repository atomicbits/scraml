/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public void writeToFile(File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        InputStream inputStream = asStream();
        FileOutputStream outputStream = new FileOutputStream(file);
        try {
            int read = 0;
            byte[] bytes = new byte[1024];

            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            outputStream.flush();
            outputStream.close();
        }
    }

}
