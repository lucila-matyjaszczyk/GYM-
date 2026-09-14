package com.gymcrm.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Cross-cutting logging for any method marked with one of the three
 * annotations in {@code com.gymcrm.aspect.annotation}. On purpose, this
 * class knows nothing about Trainee/Trainer/Training or any gym-crm
 * service -- it only ever deals with generic method metadata
 * (name, arguments, return value), so it could be dropped into any other
 * project unchanged.
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingAspect.class);

    @Before("@annotation(com.gymcrm.aspect.annotation.EnableArgumentLogging)")
    public void logArguments(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        LOGGER.info("Method name: [{}], parameter(s): {}", methodName, Arrays.toString(args));
    }

    @AfterReturning(
            pointcut = "@annotation(com.gymcrm.aspect.annotation.EnableReturnValueLogging)",
            returning = "returnValue")
    public void logReturnValue(JoinPoint joinPoint, Object returnValue) {
        String methodName = joinPoint.getSignature().getName();
        LOGGER.info("Method name: [{}], return value: {}", methodName, returnValue);
    }

    @Around("@annotation(com.gymcrm.aspect.annotation.EnableExecutionTimeLogging)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        long startNanos = System.nanoTime();
        try {
            return joinPoint.proceed();
        } finally {
            long elapsedNanos = System.nanoTime() - startNanos;
            double elapsedMicros = elapsedNanos / 1000.0;
            LOGGER.info("Method name: [{}], execution time = {}µs", methodName,
                    String.format("%.1f", elapsedMicros));
        }
    }
}
