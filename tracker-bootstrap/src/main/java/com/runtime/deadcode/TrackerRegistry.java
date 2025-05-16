package com.runtime.deadcode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public class TrackerRegistry {

    private static final ConcurrentMap<String, Set<String>> invocationMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> methodCounts = new ConcurrentHashMap<>();
    private static volatile boolean trackCounts = false;

    public static void configureCounting(boolean enabled) {
        trackCounts = enabled;
    }

    public static void markInvocation(String className, String methodName) {
        if (trackCounts) {
            methodCounts.computeIfAbsent(className + "#" + methodName, k -> new LongAdder()).increment();
        } else {
            invocationMap.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(methodName);
        }
    }

    public static ConcurrentMap<String, Set<String>> getSnapshot() {
        return invocationMap;
    }

    public static ConcurrentMap<String, LongAdder> getMethodCounts() {
        return methodCounts;
    }
}
