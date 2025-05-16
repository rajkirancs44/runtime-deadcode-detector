package com.runtime.deadcode;

import net.bytebuddy.asm.Advice;

public class TrackerAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(@Advice.Origin("#t") String className,
                               @Advice.Origin("#m") String methodName) {
        System.out.println("[Advice] Method entered: " + className + "#" + methodName);
        TrackerRegistry.markInvocation(className, methodName);
    }

}
