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

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RuleConfiguration<T> implements Serializable {

    protected final Class<? extends T> ruleClass;
    protected final Map<String, String> arguments;
    protected final Map<String, String> normalizedArguments;

    public RuleConfiguration(final Class<? extends T> ruleClass, final Map<String, String> arguments) {
        this.ruleClass = ruleClass;
        this.arguments = arguments;
        this.normalizedArguments = null;
    }

    public RuleConfiguration(final Class<? extends T> ruleClass, final Map<String, String> arguments, final Map<String, String> normalizedArguments) {
        this.ruleClass = ruleClass;
        this.arguments = arguments;
        this.normalizedArguments = normalizedArguments;
    }

    @Input
    public Class<? extends T> getRuleClass() {
        return ruleClass;
    }

    @Internal
    public Map<String, String> getArguments() {
        return arguments;
    }

    @Input
    @Optional
    public Map<String, String> getNormalizedArguments() {
        Map<String, String> mergedArguments = new HashMap<>();
        if(null != arguments) {
            mergedArguments.putAll(arguments);
        }
        if(null != normalizedArguments) {
            mergedArguments.putAll(normalizedArguments);
        }
        return mergedArguments;
    }

}
