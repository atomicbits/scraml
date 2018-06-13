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

import io.atomicbits.scraml.dsl.javajackson.json.Json;

/**
 * Created by peter on 7/04/17.
 */
public class ComplexHttpParam implements SingleHttpParam {

    private String parameter;

    public ComplexHttpParam(Object parameter, String canonicalType) {
        if (parameter != null) {
            String parameterFromJson = Json.writeBodyToString(parameter, canonicalType);
            if(parameterFromJson.startsWith("\"") && parameterFromJson.endsWith("\"")) {
                // When writing out a simple JSON-string value, we need to remove the quotes that Jackson has put around it.
                this.parameter = parameterFromJson.substring(1, parameterFromJson.length()-1);
            } else {
                // More complex JSON objects can be kept as they are.
                this.parameter = parameterFromJson;
            }
        }
    }

    @Override
    public String getParameter() {
        return parameter;
    }

    @Override
    public boolean nonEmpty() {
        return parameter != null;
    }

}
