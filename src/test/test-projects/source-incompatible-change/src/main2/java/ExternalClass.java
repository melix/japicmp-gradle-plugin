package me.champeau.gradle.japicmp;

public abstract class ExternalClass {
    /** Adding a new abstract method breaks existing implementations. */
    public abstract void breakingChange();
}