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

/**
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Affero General Public License
 * (AGPL) version 3.0 which accompanies this distribution, and is available in
 * the LICENSE file or at http://www.gnu.org/licenses/agpl-3.0.en.html
 * <p>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 */


package io.atomicbits.scraml.client.java;

import com.fasterxml.jackson.annotation.*;

public class Dog extends Animal {

    @JsonProperty(value = "canBark")
    private Boolean canBark;


    @JsonProperty(value = "gender")
    private String gender;


    @JsonProperty(value = "name")
    private String name;


    public Dog() {
    }


    public Dog(Boolean canBark, String gender, String name) {
        this.setCanBark(canBark);
        this.setGender(gender);
        this.setName(name);
    }


    public Boolean getCanBark() {
        return canBark;
    }

    public void setCanBark(Boolean canBark) {
        this.canBark = canBark;
    }


    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
     
     