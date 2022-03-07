/*
 * Copyright 2003-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.champeau.gradle.japicmp.report;

import java.util.Optional;

@FunctionalInterface
public interface ViolationTransformer {
    /**
     * Transforms the current violation.
     * @param type the type on which the violation was found
     * @param violation the violation
     * @return a transformed violation. If the violation should be suppressed, return Optional.empty()
     */
    Optional<Violation> transform(String type, Violation violation);
}
