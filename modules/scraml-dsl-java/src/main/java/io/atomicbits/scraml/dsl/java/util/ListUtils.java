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

package io.atomicbits.scraml.dsl.java.util;

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