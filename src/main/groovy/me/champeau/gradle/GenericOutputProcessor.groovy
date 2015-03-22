package me.champeau.gradle

import japicmp.model.JApiClass

public class GenericOutputProcessor {

    private final List<JApiClass> jApiClasses
    private final List<Closure> classVisitors
    private final List<Closure> methodVisitors
    private final List<Closure> constructorVisitors
    private final List<Closure> beforeVisitors
    private final List<Closure> afterVisitors

    GenericOutputProcessor(
            final List<Closure> classVisitors,
            final List<Closure> methodVisitors,
            final List<Closure> constructorVisitors,
            final List<Closure> beforeVisitors,
            final List<Closure> afterVisitors,
            final List<JApiClass> jApiClasses) {
        this.classVisitors = classVisitors
        this.methodVisitors = methodVisitors
        this.constructorVisitors = constructorVisitors
        this.beforeVisitors = beforeVisitors
        this.afterVisitors = afterVisitors
        this.jApiClasses = jApiClasses
    }

    public void processOutput() {
        beforeVisitors*.call(jApiClasses)
        for (JApiClass jApiClass : jApiClasses) {
            processClass(jApiClass)
            processMethods(jApiClass)
        }
        afterVisitors*.call(jApiClasses)
    }

    private void processMethods(JApiClass jApiClass) {
        jApiClass.methods.each { method ->
            processMethodOrConstructor(methodVisitors, method, jApiClass)
        }
        jApiClass.constructors.each { method ->
            processMethodOrConstructor(constructorVisitors, method, jApiClass)
        }
    }

    private static Iterable<Closure> processMethodOrConstructor(List<Closure> visitors, method, JApiClass jApiClass) {
        visitors.each { Closure v ->
            if (v.maximumNumberOfParameters == 1) {
                v.call(method)
            } else if (v.maximumNumberOfParameters == 2) {
                v.call(jApiClass, method)
            }
        }
    }

    private void processClass(JApiClass jApiClass) {
        classVisitors*.call(jApiClass)
    }
}
