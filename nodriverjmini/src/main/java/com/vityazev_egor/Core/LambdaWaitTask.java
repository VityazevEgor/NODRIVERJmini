package com.vityazev_egor.Core;

import java.util.function.Supplier;

/**
 * A task that waits until a given condition is met or a timeout occurs.
 * Instead of requiring an abstract method, this class accepts a lambda function.
 */
public class LambdaWaitTask {
    private final Supplier<Boolean> condition;

    /**
     * Constructs a new LambdaWaitTask with the given condition.
     *
     * @param condition A {@link Supplier} that returns {@code true} when the desired condition is met.
     */
    public LambdaWaitTask(Supplier<Boolean> condition) {
        this.condition = condition;
    }

    /**
     * Exception thrown when the wait task exceeds the timeout limit.
     */
    public static class TimeOutException extends Exception {
        /**
         * Constructs a new TimeOutException with the specified message.
         *
         * @param message The exception message.
         */
        public TimeOutException(String message) {
            super(message);
        }
    }

    /**
     * Executes the wait task until the condition is met or the timeout expires.
     *
     * @param timeOutSeconds The maximum time to wait in seconds.
     * @param delayMillis    The delay between condition checks in milliseconds.
     * @return {@code true} if the condition was met within the timeout, {@code false} otherwise.
     */
    public Boolean execute(Integer timeOutSeconds, Integer delayMillis) {
        long startTime = System.currentTimeMillis() / 1000;
        while (true) {
            if (condition.get()) {
                return true;
            }
            if ((System.currentTimeMillis() / 1000) - startTime >= timeOutSeconds) {
                break;
            }
            Shared.sleep(delayMillis);
        }
        return false;
    }
}

