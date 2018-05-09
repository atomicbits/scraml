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

package io.atomicbits.scraml.client.java;

import io.atomicbits.scraml.dsl.javajackson.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by peter on 19/08/15.
 */
public class PathparamResource extends ParamSegment<String> {

    public PathparamResource(){
    }

    public PathparamResource(RequestBuilder requestBuilder, Boolean noPath) {
        super(requestBuilder);
    }

    public PathparamResource(String value, RequestBuilder requestBuilder) {
        super(value, requestBuilder);
    }

    public PathparamResource addHeader(String key, String value) {
        PathparamResource pathparamResource = new PathparamResource(getRequestBuilder(), true);
        // At this point, the request builder has been initialized, so we can clone it and go on.
        pathparamResource._requestBuilder.addHeader(key, value);
        return pathparamResource;
    }

    public TypeMethodSegment<String, Person> get(double queryparX, int queryparY, Integer queryparZ) {
        Map<String, HttpParam> queryParams = new HashMap<String, HttpParam>();
        queryParams.put("queryparX", new SimpleHttpParam(queryparX));
        queryParams.put("queryparY", new SimpleHttpParam(queryparY));
        queryParams.put("queryparZ", new SimpleHttpParam(queryparZ));

        return new TypeMethodSegment<String, Person>(
                Method.GET,
                null,
                false,
                queryParams,
                null,
                null,
                null,
                null,
                "application/json",
                null,
                this.getRequestBuilder(),
                null,
                "io.atomicbits.scraml.client.java.Person"
        );
    }

}
