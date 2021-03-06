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

import java.util.List;

/**
 * Created by peter on 22/04/16.
 */
public class HeaderAdd extends HeaderOp {

    public HeaderAdd(String key, String value) {
        super(key, value);
    }

    public HeaderAdd(String key, List<String> values) {
        super(key, values);
    }

    @Override
    public void process(HeaderMap headerMap) {
        headerMap.addHeader(getKey(), getValues());
    }

}
