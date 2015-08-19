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

package io.atomicbits.scraml.client.java;

import io.atomicbits.scraml.dsl.java.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by peter on 19/08/15.
 */
public class PathparamResource extends ParamSegment<String> {

    public PathparamResource() {
    }

    public PathparamResource(String value, RequestBuilder requestBuilder) {
        super(value, requestBuilder);
    }

    public PathparamResource withHeader(String key, String value) {
        PathparamResource pathparamResource = this.shallowClone();
        pathparamResource.requestBuilder = pathparamResource.requestBuilder.cloneAddHeader(key, value);
        return pathparamResource;
    }

    public GetSegment<String, String> get(double queryparX, int queryparY, Integer queryParZ) {
        Map<String, HttpParam> queryParams = new HashMap<String, HttpParam>();
        queryParams.put("queryParX", new SingleHttpParam(queryparX));
        queryParams.put("queryparY", new SingleHttpParam(queryparY));
        queryParams.put("queryParZ", new SingleHttpParam(queryParZ));

        List<String> validAcceptHeaders = new ArrayList<String>();
        validAcceptHeaders.add("application/json");

        return new GetSegment<String, String>(queryParams, validAcceptHeaders, this.requestBuilder);
    }

    private PathparamResource shallowClone() {
        // We cannot go through the normal constructor, or we'll change the path of the requestBuilder again.
        PathparamResource pathparamResource = new PathparamResource();
        pathparamResource.value = this.value;
        pathparamResource.requestBuilder = this.requestBuilder;
        return pathparamResource;
    }

}
