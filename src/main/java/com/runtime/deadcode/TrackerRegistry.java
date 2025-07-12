package com.runtime.deadcode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public class TrackerRegistry {

    private static final ConcurrentMap<String, Set<String>> invocationMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> methodCounts = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Long> methodMaxTimes = new ConcurrentHashMap<>();

    private static volatile boolean trackCounts = false;
    private static volatile boolean trackTime = false;

    public static void configureCounting(boolean enabled) {
        trackCounts = enabled;
    }

    public static void configureTiming(boolean enabled) {
        trackTime = enabled;
    }

    public static void markInvocation(String className, String methodName) {
        if (trackCounts) {
            System.out.println("[Agent-tracker] Track Counts is enabled: "+trackCounts);
            methodCounts.computeIfAbsent(className + "#" + methodName, k -> new LongAdder()).increment();
        } else {
            System.out.println("[Agent-tracker] Track invocation is enabled: "+trackCounts);
            invocationMap.computeIfAbsent(className, k -> ConcurrentHashMap.newKeySet()).add(methodName);
        }
    }

    public static void markExecutionTime(String className, String methodName, long durationNano) {
        if (trackTime) {
            System.out.println("[Agent-tracker] Track Time is enabled: "+trackTime);
            String key = className + "#" + methodName;
            methodMaxTimes.compute(key, (k, existing) ->
                    (existing == null || durationNano > existing) ? durationNano : existing
            );
        }
    }

    public static ConcurrentMap<String, Long> getMaxExecutionTimes() {
        return methodMaxTimes;
    }

    public static ConcurrentMap<String, Set<String>> getSnapshot() {
        return invocationMap;
    }

    public static ConcurrentMap<String, LongAdder> getMethodCounts() {
        return methodCounts;
    }
}
