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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created by peter on 19/08/15.
 */
public abstract class MethodSegment<B, R> extends Segment {

    private B body;
    private RequestBuilder requestBuilder;

    protected MethodSegment(Method method,
                            B theBody,
                            Map<String, HttpParam> queryParams,
                            Map<String, HttpParam> formParams,
                            List<BodyPart> multipartParams,
                            BinaryBody binaryBody,
                            String expectedAcceptHeader,
                            String expectedContentTypeHeader,
                            RequestBuilder req) {

        this.body = theBody;

        RequestBuilder requestBuilder = req.shallowClone();
        requestBuilder.setMethod(method);
        requestBuilder.setQueryParameters(removeNullParams(queryParams));
        requestBuilder.setFormParameters(removeNullParams(formParams));
        requestBuilder.setMultipartParams(multipartParams);
        requestBuilder.setBinaryBody(binaryBody);

        if (expectedAcceptHeader != null && !requestBuilder.getHeaders().hasKey("Accept")) {
            requestBuilder.addHeader("Accept", expectedAcceptHeader);
        }

        if (expectedContentTypeHeader != null && !requestBuilder.getHeaders().hasKey("Content-Type")) {
            requestBuilder.addHeader("Content-Type", expectedContentTypeHeader);
        }

        this.requestBuilder = requestBuilder;
    }


    protected Future<Response<String>> callToStringResponse(String canonicalRequestType) {
        return requestBuilder.callToStringResponse(getBody(), canonicalRequestType);
    }

    protected Future<Response<R>> callToTypeResponse(String canonicalRequestType, String canonicalResponseType) {
        return requestBuilder.callToTypeResponse(getBody(), canonicalRequestType, canonicalResponseType);
    }

    protected B getBody() {
        return body;
    }

    protected RequestBuilder getRequestBuilder() {
        return requestBuilder;
    }

    private Map<String, HttpParam> removeNullParams(Map<String, HttpParam> map) {
        Map<String, HttpParam> cleanedMap = new HashMap<String, HttpParam>();
        if (map != null) {
            for (String key : map.keySet()) {
                HttpParam value = map.get(key);
                if (value != null) {
                    cleanedMap.put(key, value);
                }
            }
        }
        return cleanedMap;
    }

}
