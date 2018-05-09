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

package io.atomicbits.scraml.dsl.javajackson.util;

import java.util.List;

/**
 * Created by peter on 19/09/15.
 */
public class ListUtils {

    static public String mkString(List<String> list, String delimiter) {
        return mkStringHelper(list, delimiter, new StringBuilder());
    }

    static private String mkStringHelper(List<String> list, String delimiter, StringBuilder sb) {

        if (list == null || list.isEmpty()) {
            return sb.toString();
        } else if (list.size() == 1) {
            String head = list.get(0);
            return sb.append(head).toString();
        } else {
            String head = list.get(0);
            List<String> tail = list.subList(1, list.size());
            return mkStringHelper(tail, delimiter, sb.append(head).append(delimiter));
        }

    }

}