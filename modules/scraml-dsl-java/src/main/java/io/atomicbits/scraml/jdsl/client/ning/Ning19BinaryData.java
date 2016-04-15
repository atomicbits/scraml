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

package io.atomicbits.scraml.jdsl.client.ning;

import io.atomicbits.scraml.jdsl.BinaryData;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by peter on 22/01/16.
 */
public class Ning19BinaryData extends BinaryData {

    private com.ning.http.client.Response innerResponse;

    public Ning19BinaryData(com.ning.http.client.Response innerResponse) {
        this.innerResponse = innerResponse;
    }

    @Override
    public byte[] asBytes() throws IOException {
        return innerResponse.getResponseBodyAsBytes();
    }

    @Override
    public InputStream asStream() throws IOException {
        return innerResponse.getResponseBodyAsStream();
    }

    @Override
    public String asString() throws IOException {
        return innerResponse.getResponseBody();
    }

    @Override
    public String asString(String charset) throws IOException {
        return innerResponse.getResponseBody(charset);
    }

}
