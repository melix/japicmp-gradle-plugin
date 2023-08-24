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

import java.util.Map;

public class ViolationTransformerConfiguration extends RuleConfiguration<ViolationTransformer> {
    public ViolationTransformerConfiguration(Class<? extends ViolationTransformer> ruleClass, Map<String, String> arguments) {
        super(ruleClass, arguments);
    }

    /**
     * This constructor allows to override entries in the {@code japiCmp.richReport.rules } collection exposed by {@link RuleConfiguration#getNormalizedArguments()} } () getNormalizedArguments()}.
     * Although {@link RuleConfiguration#getArguments()} getArguments()} } collection contains the rule arguments, {@link RuleConfiguration#getNormalizedArguments()} } () getNormalizedArguments()} is the collection used as Gradle task input.
     * This allows to apply some normalization when inputs are volatile (ie. absolute paths in rule).
     *
     * @param ruleClass rule class
     * @param arguments rule arguments
     * @param normalizedArguments normalized rule arguments
     */
    public ViolationTransformerConfiguration(Class<? extends ViolationTransformer> ruleClass, Map<String, String> arguments, Map<String, String> normalizedArguments) {
        super(ruleClass, arguments, normalizedArguments);
    }
}
