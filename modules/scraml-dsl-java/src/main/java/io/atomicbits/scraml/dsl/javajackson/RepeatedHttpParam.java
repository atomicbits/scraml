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

}
