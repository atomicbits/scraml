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

package io.atomicbits.scraml.dsl.androidjavajackson.client.okhttp;

import io.atomicbits.scraml.dsl.androidjavajackson.BinaryData;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by peter on 3/11/17.
 */
public class OkHttpScramlBinaryData extends BinaryData {

    private ResponseBody innerResponse;

    public OkHttpScramlBinaryData(ResponseBody innerResponse) {
        this.innerResponse = innerResponse;
    }

    @Override
    public byte[] asBytes() throws IOException {
        return innerResponse.bytes();
    }

    @Override
    public InputStream asStream() throws IOException {
        return innerResponse.byteStream();
    }

    @Override
    public String asString() throws IOException {
        return innerResponse.string();
    }

    @Override
    public String asString(String charset) throws IOException {
        return innerResponse.string();
    }

}
