package com.runtime.deadcode;

import net.bytebuddy.asm.Advice;

public class TrackerAdvice {

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Origin("#t") String className,
                               @Advice.Origin("#m") String methodName) {
        TrackerRegistry.markInvocation(className, methodName);
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Origin("#t") String className,
                              @Advice.Origin("#m") String methodName,
                              @Advice.Enter long startTime) {
        long duration = System.nanoTime() - startTime;
        TrackerRegistry.markExecutionTime(className, methodName, duration);
    }

}
