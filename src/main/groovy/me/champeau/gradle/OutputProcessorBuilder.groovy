package me.champeau.gradle

import japicmp.model.JApiChangeStatus

class OutputProcessorBuilder {
    final List<Closure> classProcessors = []
    final List<Closure> methodProcessors = []
    final List<Closure> beforeProcessors = []
    final List<Closure> afterProcessors = []

    private final JapicmpAbstractTask task

    OutputProcessorBuilder(final JapicmpAbstractTask task) {
        this.task = task
    }

    private static Closure doWrap(Closure c, JApiChangeStatus status) {
        if (c.maximumNumberOfParameters==1) {
            return { elem ->
                if (elem.changeStatus == status) {
                    c(elem)
                }
            }
        }
        if (c.maximumNumberOfParameters==2) {
            return { clazz, elem ->
                if (elem.changeStatus == status) {
                    c(clazz,elem)
                }
            }
        }
        throw new IllegalArgumentException("Unsupported DSL feature")
    }

    void before(Closure cl) {
        beforeProcessors << cl
    }

    void after(Closure cl) {
        afterProcessors << cl
    }

    def methodMissing(String name, args) {
        if (args instanceof Object[] && args.length==1) {
            def arg = args[0]
            def isClass = name.endsWith('Class')
            def isMethod = name.endsWith('Method')
            if (arg instanceof Closure && (isClass || isMethod)) {
                String status = isClass ? name - 'Class' : name - 'Method'
                def cl = doWrap(arg, JApiChangeStatus.valueOf(status.toUpperCase()))
                if (isClass) {
                    classProcessors << cl
                } else {
                    methodProcessors << cl
                }
                return null
            }
        }
        task.invokeMethod(name, args)
    }

    def propertyMissing(String name) {
        task.getProperty(name)
    }
}
