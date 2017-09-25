package me.champeau.gradle.japicmp.internal;

public abstract class InternalClass {
    /** Adding a new abstract method breaks existing implementations. */
    abstract void breakingChange();
}
