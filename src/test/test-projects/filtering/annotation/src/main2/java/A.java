package me.champeau.gradle.japicmp;

import me.champeau.gradle.japicmp.BetaApi;
import me.champeau.gradle.japicmp.StableApi;

public class A {
    @BetaApi
    public void betaMethod() {}

    @StableApi
    public void stableMethod() {}
}
