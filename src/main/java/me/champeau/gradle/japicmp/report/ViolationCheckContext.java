/*
 * Copyright 2003-2012 the original author or authors.
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
package me.champeau.gradle.japicmp.report;

import java.util.Map;

public interface ViolationCheckContext {
    /**
     * Returns the fully-qualified class name of the class being currently checked
     * @return the fully-qualified class name of the class being currently checked, or null if it's a pre/post rule
     */
    String getClassName();

    /**
     * Returns a map that can be used by the writer of a rule to read and write arbitrary data.
     * @return a user-data map, never null
     */
    Map<String, ?> getUserData();
}
