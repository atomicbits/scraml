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

package io.atomicbits.scraml.jdsl.spica;

import java.util.List;
import java.util.Map;

/**
 * Created by peter on 19/08/15.
 */
public class Response<T> {

    private int status;
    private String stringBody;
    // private JsValue jsonBody;
    private T body;
    private Map<String, List<String>> headers;

    public Response(String stringBody, T body, int status, Map<String, List<String>> headers) {
        this.body = body;
        this.headers = headers;
        this.status = status;
        this.stringBody = stringBody;
    }

    public T getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public int getStatus() {
        return status;
    }

    public String getStringBody() {
        return stringBody;
    }

    public String toString() {
        return "Status: " + status + "\n" + "body:\n" + stringBody;
    }

}
