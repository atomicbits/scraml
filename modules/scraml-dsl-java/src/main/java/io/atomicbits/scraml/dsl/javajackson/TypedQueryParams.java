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

import io.atomicbits.scraml.dsl.javajackson.json.Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 14/05/17.
 */
public class TypedQueryParams {

    private Map<String, HttpParam> params;

    public <T> TypedQueryParams(T typedQueryParams) {
        this.params = Json.toFormUrlEncoded(typedQueryParams);
    }

    public Map<String, HttpParam> getParams() {
        return params;
    }

}
