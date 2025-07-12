package com.runtime.deadcode;

import com.runtime.deadcode.config.DeadCodeAgentProperties;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.description.type.TypeDescription;

import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;

public class DeadCodeAgent {

    private static String userPrefix = null;
    private static DeadCodeAgentProperties props = null;
    private static final java.util.concurrent.atomic.AtomicBoolean initialized = new java.util.concurrent.atomic.AtomicBoolean(false);


    public static void premain(String agentArgs, Instrumentation inst) {
        if (!initialized.compareAndSet(false, true)) {
            System.out.println("[Agent] Already initialized. Skipping duplicate premain.");
            return;
        }

        try {
            System.out.println("[Agent] premain() called from: " + Arrays.toString(Thread.currentThread().getStackTrace()));
          //  injectBootstrapJars(inst);
            props = AgentBootstrap.init();
            System.out.println("[Agent] premain() called and TrackExecutionTime is :"+ props.isTrackExecutionTime() + " and are Tracking Invocation Count"+props.isTrackInvocationCount());
            TrackerRegistry.configureTiming(props.isTrackExecutionTime());
            TrackerRegistry.configureCounting(props.isTrackInvocationCount());
            Dumper.start(props);
            Dumper.startScheduledStatsPosting(props);
            install(inst);
        } catch (Exception e) {
            System.err.println("[Agent] Error in premain:");
            e.printStackTrace();
        }
    }

    private static void install(Instrumentation inst) {
        userPrefix = System.getProperty("deadcode.prefix");
        List<String> basePackages = props.getBasePackages();
        for (String s : basePackages) {
            System.out.println("[Agent] Base Package:::: " + s);
        }

        // Attempt to load TrackerAdvice from bootstrap classloader
        Class<?> adviceClass;
        try {
            adviceClass = Class.forName("com.runtime.deadcode.TrackerAdvice");; // null -> bootstrap classloader
            System.out.println("[Agent] Loaded TrackerAdvice from classloader: " + adviceClass.getClassLoader());

            System.out.println("[Debug] Scanning TrackerAdvice for advice methods...");
            for (java.lang.reflect.Method method : adviceClass.getDeclaredMethods()) {
                for (Annotation annotation : method.getAnnotations()) {
                    String annName = annotation.annotationType().getName();
                    if (annName.equals("net.bytebuddy.asm.Advice$OnMethodEnter")) {
                        System.out.println("[Debug] Found @OnMethodEnter: " + method);
                    }
                    if (annName.equals("net.bytebuddy.asm.Advice$OnMethodExit")) {
                        System.out.println("[Debug] Found @OnMethodExit: " + method);
                    }
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[Agent] Could not load TrackerAdvice from bootstrap classloader", e);
        }

        // Build a combined hasSuperType matcher for Spring proxy classes
        ElementMatcher<? super TypeDescription> superTypeMatcher = ElementMatchers.hasSuperType(
                basePackages.stream()
                        .map(ElementMatchers::nameStartsWith)
                        .reduce(ElementMatchers.none(), ElementMatcher.Junction::or)
        );

        AgentBuilder.RawMatcher matcher = (typeDescription, classLoader, module, classBeingRedefined, protectionDomain) -> {
            String className = typeDescription.getName();
            if (className == null || className.contains("$$") || className.contains("$Proxy") || typeDescription.isInterface()) {
            //    System.out.println("[Agent] Skipping proxy/interface: " + className);
                return false;
            }

            boolean directMatch = basePackages.stream().anyMatch(className::startsWith);
            boolean superMatch = typeDescription.getSuperClass() != null &&
                    basePackages.stream().anyMatch(pkg -> typeDescription.getSuperClass().getTypeName().startsWith(pkg));

            if (directMatch || superMatch) {
                System.out.println("[Agent] Will instrument: " + className);
                return true;
            }

            return false;
        };

        new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.Listener.StreamWriting.toSystemOut().withTransformationsOnly())
                .ignore(ElementMatchers.nameStartsWith("java.")
                        .or(ElementMatchers.nameStartsWith("javax."))
                        .or(ElementMatchers.nameStartsWith("sun."))
                        .or(ElementMatchers.nameStartsWith("com.sun."))
                        .or(ElementMatchers.nameStartsWith("kotlin."))
                        .or(ElementMatchers.nameStartsWith("org.hibernate."))
                        .or(ElementMatchers.nameStartsWith("org.apache."))
                        .or(ElementMatchers.nameStartsWith("org.slf4j."))
                        .or(ElementMatchers.nameStartsWith("ch.qos.")))
                .type(matcher)
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(adviceClass).on(
                                ElementMatchers.isMethod()
                                        .and(ElementMatchers.not(ElementMatchers.isAbstract()))
                                        .and(ElementMatchers.not(ElementMatchers.isSynthetic()))
                                        .and(ElementMatchers.not(ElementMatchers.isNative()))
                        ))
                )
                .installOn(inst);

        for (Class<?> loadedClass : inst.getAllLoadedClasses()) {
            if (inst.isModifiableClass(loadedClass)) {
                String className = loadedClass.getName();
                if (basePackages.stream().anyMatch(className::startsWith)) {
                    try {
                        System.out.println("[Agent] Retransforming already loaded class: " + className);
                        inst.retransformClasses(loadedClass);
                    } catch (Exception e) {
                        System.err.println("[Agent] Failed to retransform " + className + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}
