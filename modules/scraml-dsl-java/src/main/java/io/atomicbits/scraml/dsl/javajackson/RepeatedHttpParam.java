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

import java.util.ArrayList;
import java.util.List;

/**
 * Created by peter on 19/08/15.
 */
public class RepeatedHttpParam implements HttpParam {

    private List<String> parameters;

    public RepeatedHttpParam(List parameters) {
        List<String> stringParams = new ArrayList<String>(parameters.size());
        if (parameters != null) {
            for (Object param : parameters) {
                stringParams.add(param.toString());
            }
        }
        this.parameters = stringParams;
    }

    public List<String> getParameters() {
        return parameters;
    }

    @Override
    public boolean nonEmpty() {
        return parameters != null && !parameters.isEmpty();
    }

}
