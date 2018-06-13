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
 * Created by peter on 23/09/15.
 */
public class TypeMethodSegment<B, R> extends MethodSegment<B, R> {

    private String canonicalContentType;
    private String canonicalResponseType;
    private Boolean primitiveBody;

    public TypeMethodSegment(Method method,
                             B theBody,
                             Boolean primitiveBody,
                             Map<String, HttpParam> queryParams,
                             TypedQueryParams queryString,
                             Map<String, HttpParam> formParams,
                             List<BodyPart> multipartParams,
                             BinaryRequest binaryRequest,
                             String expectedAcceptHeader,
                             String expectedContentTypeHeader,
                             RequestBuilder req,
                             String canonicalContentType,
                             String canonicalResponseType) {
        super(method, theBody, queryParams, queryString, formParams, multipartParams, binaryRequest, expectedAcceptHeader, expectedContentTypeHeader, req);

        this.canonicalContentType = canonicalContentType;
        this.canonicalResponseType = canonicalResponseType;
        this.primitiveBody = primitiveBody;
    }

    public void call(Callback<R> callback) {
        if (this.primitiveBody) {
            getRequestBuilder().callToTypeResponse(getPlainStringBody(), canonicalResponseType, callback);
        } else {
            getRequestBuilder().callToTypeResponse(jsonBodyToString(canonicalContentType), canonicalResponseType, callback);
        }
    }

}
