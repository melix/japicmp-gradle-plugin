package me.champeau.gradle.japicmp;

import org.gradle.workers.WorkAction;

public abstract class JApiCmpWorkAction implements WorkAction<JapiCmpWorkParameters> {

    @Override
    public void execute() {
        new JApiCmpWorkerAction(getParameters().getConfiguration().get()).run();
    }
}
