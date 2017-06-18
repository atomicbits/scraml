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
 * Created by peter on 22/04/16.
 */
public abstract class HeaderOp {

    private final String key;
    private final List<String> values;

    public HeaderOp(String key, String value) {
        this.key = key;
        List<String> values = new ArrayList<>();
        values.add(value);
        this.values = values;
    }

    public HeaderOp(String key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public abstract void process(HeaderMap headerMap);

    public String getKey() {
        return key;
    }

    public List<String> getValues() {
        return values;
    }
}
