package me.champeau.gradle.japicmp;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.util.GradleVersion;

/**
 * @author Cedric Champeau
 */
public class JapicmpPlugin implements Plugin<Project> {
    private final static GradleVersion GRADLE_MIN = GradleVersion.version("6.0");
    private final static boolean IS_GRADLE_MIN = GradleVersion.current().compareTo(GRADLE_MIN) >= 0;

    public void apply(Project project) {
        // this plugin doesn't create any task by default
        if (!IS_GRADLE_MIN) {
            throw new RuntimeException("This version of the JApicmp Gradle plugin requires" + GRADLE_MIN.getVersion() +
                    "you are using" + GradleVersion.current().getVersion() + "." +
                    "Please upgrade Gradle or use an older version of the JMH Gradle plugin.");
        }
    }
}
