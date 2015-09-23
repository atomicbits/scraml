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

package io.atomicbits.scraml.dsl.java;


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
