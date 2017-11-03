/*
 *
 *  (C) Copyright 2015 Atomic BITS (http://atomicbits.io).
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the GNU Affero General Public License
 *  (AGPL) version 3.0 which accompanies this distribution, and is available in
 *  the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 *  Alternatively, you may also use this code under the terms of the
 *  Scraml Commercial License, see http://scraml.io
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Affero General Public License or the Scraml Commercial License for more
 *  details.
 *
 *  Contributors:
 *      Peter Rigole
 *
 */

package io.atomicbits.scraml.dsl.androidjavajackson;

import io.atomicbits.scraml.dsl.androidjavajackson.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Created by peter on 19/08/15.
 */
public class RequestBuilder {

    private Client client;
    private List<String> path = new ArrayList<String>();
    private Method method = Method.GET;
    private Map<String, HttpParam> queryParameters = new HashMap<String, HttpParam>();
    private Map<String, HttpParam> formParameters = new HashMap<String, HttpParam>();
    private List<BodyPart> multipartParams = new ArrayList<BodyPart>(1);
    private BinaryRequest binaryRequest = null;
    private HeaderMap headerMap = new HeaderMap();
    private List<HeaderOp> headerOps = new ArrayList<>(1);

    RequestBuilder parentRequestBuilder;


    public RequestBuilder() {
    }

    public RequestBuilder(Client client) {
        this.client = client;
    }


    /**
     * Fold all properties of this requestbuilder's parents and itself recursively into a new requestbuilder.
     */
    public RequestBuilder fold() {
        RequestBuilder folded;
        if (getParentRequestBuilder() != null) {
            folded = getParentRequestBuilder().fold();
        } else {
            folded = new RequestBuilder();
        }
        if (getClient() != null) {
            folded.setClient(getClient());
        }
        path.forEach(folded::appendPathElement);
        if (method != null) {
            folded.setMethod(method);
        }
        queryParameters.forEach(folded::addQueryParameter);
        formParameters.forEach(folded::addFormParameter);
        multipartParams.forEach(folded::addMultipartParameter);
        if (binaryRequest != null) {
            folded.setBinaryRequest(binaryRequest);
        }
        for (HeaderOp headerOp : this.getHeaderOps()) {
            headerOp.process(folded.getHeaderMap());
        }
        return folded;
    }

    public Client getClient() {
        return client;
    }

    public Map<String, HttpParam> getFormParameters() {
        return formParameters;
    }

    public HeaderMap getHeaderMap() {
        return headerMap;
    }

    public List<HeaderOp> getHeaderOps() {
        return headerOps;
    }

    public void addHeader(String key, String value) {
        HeaderAdd headerAdd = new HeaderAdd(key, value);
        getHeaderOps().add(headerAdd);
    }

    public void setHeader(String key, String value) {
        HeaderSet headerSet = new HeaderSet(key, value);
        getHeaderOps().add(headerSet);
    }

    public Method getMethod() {
        return method;
    }

    public List<BodyPart> getMultipartParams() {
        return multipartParams;
    }

    public BinaryRequest getBinaryRequest() {
        return binaryRequest;
    }

    public void setBinaryRequest(BinaryRequest binaryRequest) {
        this.binaryRequest = binaryRequest;
    }

    public Map<String, HttpParam> getQueryParameters() {
        return queryParameters;
    }

    public List<String> getPath() {
        return path;
    }

    public RequestBuilder getParentRequestBuilder() {
        return parentRequestBuilder;
    }

    public void setParentRequestBuilder(RequestBuilder parentRequestBuilder) {
        this.parentRequestBuilder = parentRequestBuilder;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public void setFormParameters(Map<String, HttpParam> formParameters) {
        if (formParameters == null) {
            this.formParameters = new HashMap<>();
        } else {
            this.formParameters = formParameters;
        }
    }


    public void setMethod(Method method) {
        this.method = method;
    }

    public void setMultipartParams(List<BodyPart> multipartParams) {
        if (multipartParams == null) {
            this.multipartParams = new ArrayList<>();
        } else {
            this.multipartParams = multipartParams;
        }
    }

    public void setPath(List<String> path) {
        if (path == null) {
            this.path = new ArrayList<>();
        } else {
            this.path = path;
        }
    }

    public void setQueryParameters(Map<String, HttpParam> queryParameters) {
        if (queryParameters == null) {
            this.queryParameters = new HashMap<>();
        } else {
            this.queryParameters = queryParameters;
        }
    }

    public void addQueryParameter(String key, HttpParam value) {
        getQueryParameters().put(key, value);
    }

    public void addFormParameter(String key, HttpParam value) {
        getFormParameters().put(key, value);
    }

    public void addMultipartParameter(BodyPart bodyPart) {
        getMultipartParams().add(bodyPart);
    }

    public String getRelativePath() {
        return ListUtils.mkString(path, "/");
    }

    public void appendPathElement(String pathElement) {
        this.path.add(pathElement);
    }

    public CompletableFuture<Response<String>> callToStringResponse(String body) {
        return client.callToStringResponse(this, body);
    }

    public CompletableFuture<Response<BinaryData>> callToBinaryResponse(String body) {
        return client.callToBinaryResponse(this, body);
    }

    public <R> CompletableFuture<Response<R>> callToTypeResponse(String body, String canonicalResponseType) {
        return client.callToTypeResponse(this, body, canonicalResponseType);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("client:\t\t" + client + "\n");
        sb.append("path:\t\t" + listToString(path) + "\n");
        sb.append("parent:\t\t" + parentRequestBuilder);
        return sb.toString();
    }

    private String listToString(List list) {
        String txt = "";
        for (Object o : list) {
            txt += o.toString() + ", ";
        }
        return txt;
    }

}
