package me.champeau.gradle.japicmp;

public interface Interface {

	boolean methodA();

	default boolean notMethodA() {
		return !methodA();
	}

}
