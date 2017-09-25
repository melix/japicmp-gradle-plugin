package me.champeau.gradle.japicmp;

import me.champeau.gradle.japicmp.BetaApi;
import me.champeau.gradle.japicmp.StableApi;

public class A {
    @BetaApi
    public int betaMethod() {
        return 1;
    }

    @StableApi
    public int stableMethod() {
        return 1;
    }
}
