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

package io.atomicbits.scraml.jdsl.spica;

import java.io.File;
import java.io.InputStream;

/**
 * Created by peter on 17/01/16.
 */
public abstract class BinaryRequest {

    public boolean isFile() {
        return false;
    }

    public boolean isInputStream() {
        return false;
    }

    public boolean isByteArray() {
        return false;
    }

    public boolean isString() {
        return false;
    }

    public static BinaryRequest create(File file) {
        return new FileBinaryRequest(file);
    }

    public static BinaryRequest create(InputStream inputStream) {
        return new InputStreamBinaryRequest(inputStream);
    }

    public static BinaryRequest create(byte[] bytes) {
        return new ByteArrayBinaryRequest(bytes);
    }

    public static BinaryRequest create(String text) {
        return new StringBinaryRequest(text);
    }

}
