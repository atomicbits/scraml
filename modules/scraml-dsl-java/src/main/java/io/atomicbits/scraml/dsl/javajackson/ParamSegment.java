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

/**
 * Created by peter on 19/08/15.
 */
public abstract class ParamSegment<T> extends Segment {

    // We have to initialize it empty and fill it in later to get the resource segments initialized as fields and not methods.
    protected RequestBuilder _requestBuilder = new RequestBuilder();

    public ParamSegment(){}

    public ParamSegment(RequestBuilder parentRequestBuilder){
        this._requestBuilder.setParentRequestBuilder(parentRequestBuilder);
    }

    public ParamSegment(T value, RequestBuilder parentRequestBuilder) {
        // The preceding part of the path will be prepended on fold().
        this._requestBuilder.appendPathElement(value.toString());
        this._requestBuilder.setParentRequestBuilder(parentRequestBuilder);
    }

    @Override
    protected RequestBuilder getRequestBuilder() {
        return this._requestBuilder;
    }

}
