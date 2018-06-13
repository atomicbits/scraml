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
