package me.champeau.gradle.japicmp;

import org.gradle.api.provider.Property;
import org.gradle.workers.WorkParameters;

public interface JapiCmpWorkParameters extends WorkParameters {

    Property<JapiCmpWorkerConfiguration> getConfiguration();
}
