package com.runtime.deadcode;

import com.runtime.deadcode.config.DeadCodeAgentProperties;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.description.type.TypeDescription;

import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import com.runtime.deadcode.TrackerAdvice;

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
            Dumper.start(props);
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




    private static void injectBootstrapJars(Instrumentation inst) {
        try {
            CodeSource codeSource = DeadCodeAgent.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                System.err.println("[Agent] Cannot locate agent JAR");
                return;
            }

            File agentJar = new File(codeSource.getLocation().toURI());
            try (JarFile jar = new JarFile(agentJar)) {
                // 1. Inject tracker-bootstrap
                JarEntry trackerEntry = jar.getJarEntry("lib/tracker-bootstrap-0.1.0.jar");
                if (trackerEntry != null) {
                    Path tmp = Files.createTempFile("tracker-bootstrap", ".jar");
                    try (InputStream is = jar.getInputStream(trackerEntry)) {
                        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                        inst.appendToBootstrapClassLoaderSearch(new JarFile(tmp.toFile()));
                        System.out.println("[Agent] Loaded tracker-bootstrap into bootstrap classloader.");
                    }
                }

                // 2. Inject byte-buddy
                JarEntry bbEntry = jar.getJarEntry("lib/byte-buddy-1.14.12.jar");
                if (bbEntry != null) {
                    Path tmp = Files.createTempFile("byte-buddy", ".jar");
                    try (InputStream is = jar.getInputStream(bbEntry)) {
                        Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
                        inst.appendToBootstrapClassLoaderSearch(new JarFile(tmp.toFile()));
                        System.out.println("[Agent] Loaded byte-buddy into bootstrap classloader.");
                    }
                }

            }
        } catch (Exception e) {
            System.err.println("[Agent] Failed to inject bootstrap jars");
            e.printStackTrace();
        }
    }





    private static boolean shouldInstrument(TypeDescription typeDescription) {
        String className = typeDescription.getCanonicalName();
        if (className == null) return false;
        if (userPrefix != null) {
            return className.startsWith(userPrefix);
        }

        return !(className.startsWith("java.") ||
                className.startsWith("javax.") ||
                className.startsWith("sun.") ||
                className.startsWith("com.sun.") ||
                className.startsWith("org.springframework.") ||
                className.startsWith("org.hibernate.") ||
                className.startsWith("kotlin.") ||
                className.startsWith("scala.") ||
                className.startsWith("ch.qos.logback.") ||
                className.startsWith("org.slf4j.") ||
                className.startsWith("org.apache."));
    }
}
