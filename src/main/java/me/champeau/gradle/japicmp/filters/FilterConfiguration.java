/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle.japicmp.filters;

import japicmp.filter.Filter;

import java.io.Serializable;

public class FilterConfiguration implements Serializable {

    protected final Class<? extends Filter> filterClass;

    public FilterConfiguration(Class<? extends Filter> filterClass) {
        this.filterClass = filterClass;
    }

    public Class<? extends Filter> getFilterClass() {
        return filterClass;
    }
}
