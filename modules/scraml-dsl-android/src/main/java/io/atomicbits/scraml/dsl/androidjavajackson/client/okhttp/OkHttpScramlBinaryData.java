/*
 *
 *  (C) Copyright 2017 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml End-User License Agreement, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml End-User License Agreement for
 *  more details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson.client.okhttp;

import io.atomicbits.scraml.dsl.androidjavajackson.BinaryData;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by peter on 3/11/17.
 */
public class OkHttpScramlBinaryData extends BinaryData {

    @Override
    public byte[] asBytes() throws IOException {
        return new byte[0];
    }

    @Override
    public InputStream asStream() throws IOException {
        return null;
    }

    @Override
    public String asString() throws IOException {
        return null;
    }

    @Override
    public String asString(String charset) throws IOException {
        return null;
    }

}
