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

import io.atomicbits.scraml.dsl.androidjavajackson.util.ListUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        // Java 1.7 specific
        for (String pathElement : path) {
            folded.appendPathElement(pathElement);
        }
        if (method != null) {
            folded.setMethod(method);
        }
        // Java 1.7 specific
        for (Map.Entry<String, HttpParam> entry : queryParameters.entrySet()) {
            folded.addQueryParameter(entry.getKey(), entry.getValue());
        }
        // Java 1.7 specific
        for (Map.Entry<String, HttpParam> entry : formParameters.entrySet()) {
            folded.addFormParameter(entry.getKey(), entry.getValue());
        }
        // Java 1.7 specific
        for (BodyPart bodyPart : multipartParams) {
            folded.addMultipartParameter(bodyPart);
        }
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

    public void callToStringResponse(String body, Callback<String> callback) {
        client.callToStringResponse(this, body, callback);
    }

    public void callToBinaryResponse(String body, Callback<BinaryData> callback) {
        client.callToBinaryResponse(this, body, callback);
    }

    public <R> void callToTypeResponse(String body, String canonicalResponseType, Callback<R> callback) {
        client.callToTypeResponse(this, body, canonicalResponseType, callback);
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
