package com.btech.productivitytracker.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ExecutionTimeUtil {
    private ExecutionTimeUtil() {
    }

    public static Object invokeWithTiming(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            boolean shouldLog = method.isAnnotationPresent(ExecutionTimeLogger.class);
            long start = System.nanoTime();
            Object result = method.invoke(target, args);
            long durationMs = (System.nanoTime() - start) / 1_000_000;

            if (shouldLog) {
                ExecutionTimeLogger logger = method.getAnnotation(ExecutionTimeLogger.class);
                String label = logger.value().isBlank() ? methodName : logger.value();
                System.out.println("[ExecutionTimeLogger] " + label + " executed in " + durationMs + " ms");
            }
            return result;
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Unable to invoke method: " + methodName, e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException("Method invocation failed: " + methodName, cause);
        }
    }
}
