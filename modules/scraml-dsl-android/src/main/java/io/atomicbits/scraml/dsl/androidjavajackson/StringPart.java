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

package io.atomicbits.scraml.dsl.androidjavajackson;


import java.nio.charset.Charset;

/**
 * Created by peter on 19/09/15.
 */
public class StringPart implements BodyPart {

    private String name;
    private String value;
    private String contentType;
    private Charset charset = Charset.forName("UTF8");
    private String contentId;
    private String transferEncoding;

    public StringPart(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public StringPart(String name, String value, String contentId, Charset charset, String contentType, String transferEncoding) {
        this.name = name;
        this.value = value;
        this.contentId = contentId;
        this.charset = charset;
        this.contentType = contentType;
        this.transferEncoding = transferEncoding;
    }

    public Charset getCharset() {
        return charset;
    }

    public String getContentId() {
        return contentId;
    }

    public String getContentType() {
        return contentType;
    }

    public String getName() {
        return name;
    }

    public String getTransferEncoding() {
        return transferEncoding;
    }

    public String getValue() {
        return value;
    }

    @Override
    public Boolean isString() {
        return true;
    }

    @Override
    public Boolean isFile() {
        return false;
    }

    @Override
    public Boolean isByteArray() {
        return false;
    }

}
