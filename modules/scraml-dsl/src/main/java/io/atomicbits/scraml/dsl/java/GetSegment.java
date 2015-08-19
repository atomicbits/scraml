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
public class GetSegment<B, R> extends MethodSegment<B, R> {

    public GetSegment(Map<String, HttpParam> queryParams, List<String> validAcceptHeaders, RequestBuilder requestBuilder) {
        super(null);

        RequestBuilder req = requestBuilder.shallowClone();
        req.method = Method.GET;
        req.queryParameters = removeNullParams(queryParams);
        req.validAcceptHeaders = validAcceptHeaders;

        this.body = null;
        this.requestBuilder = req;
    }

    public Future<Response<R>> call() {
        return callToTypeResponse();
    }

    private Map<String, HttpParam> removeNullParams(Map<String, HttpParam> map) {
        Map<String, HttpParam> cleanedMap = new HashMap<String, HttpParam>();
        for (String key : map.keySet()) {
            HttpParam value = map.get(key);
            if (value != null) {
                cleanedMap.put(key, value);
            }
        }
        return cleanedMap;
    }

}
