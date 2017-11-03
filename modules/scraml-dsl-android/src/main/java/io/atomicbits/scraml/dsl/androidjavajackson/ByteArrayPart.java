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

import java.nio.charset.Charset;

/**
 * Created by peter on 19/09/15.
 */
public class ByteArrayPart implements BodyPart {

    private String name;
    private byte[] bytes;
    private String contentType;
    private Charset charset = Charset.forName("UTF8");
    private String contentId;
    private String fileName;
    private String transferEncoding;

    public ByteArrayPart(String name, byte[] bytes) {
        this.name = name;
        this.bytes = bytes;
    }

    public ByteArrayPart(String name, byte[] bytes, String contentId, Charset charset, String fileName, String contentType, String transferEncoding) {
        this.name = name;
        this.bytes = bytes;
        this.contentId = contentId;
        this.charset = charset;
        this.fileName = fileName;
        this.contentType = contentType;
        this.transferEncoding = transferEncoding;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public Charset getCharset() {
        return charset;
    }

    public String getFileName() {
        return fileName;
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

    @Override
    public Boolean isString() {
        return false;
    }

    @Override
    public Boolean isFile() {
        return false;
    }

    @Override
    public Boolean isByteArray() {
        return true;
    }

}
