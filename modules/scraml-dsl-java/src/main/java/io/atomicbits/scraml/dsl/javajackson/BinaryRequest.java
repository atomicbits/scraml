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
