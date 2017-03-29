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

package io.atomicbits.scraml.jdsl;

import io.atomicbits.scraml.jdsl.json.Json;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by peter on 23/09/15.
 */
public class TypeMethodSegment<B, R> extends MethodSegment<B, R> {

    private String canonicalContentType;
    private String canonicalResponseType;

    public TypeMethodSegment(Method method,
                             B theBody,
                             Map<String, HttpParam> queryParams,
                             Map<String, HttpParam> formParams,
                             List<BodyPart> multipartParams,
                             BinaryRequest binaryRequest,
                             String expectedAcceptHeader,
                             String expectedContentTypeHeader,
                             RequestBuilder req,
                             String canonicalContentType,
                             String canonicalResponseType) {
        super(method, theBody, queryParams, formParams, multipartParams, binaryRequest, expectedAcceptHeader, expectedContentTypeHeader, req);

        this.canonicalContentType = canonicalContentType;
        this.canonicalResponseType = canonicalResponseType;
    }


    public CompletableFuture<Response<R>> call() {
        return getRequestBuilder().callToTypeResponse(getStringBody(canonicalContentType), canonicalResponseType);
    }

}
