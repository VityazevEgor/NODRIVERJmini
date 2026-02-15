package com.vityazev_egor.Core;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A task that waits until a given condition is met or a timeout occurs.
 * Instead of requiring an abstract method, this class accepts a lambda function.
 * <p>
 * This class can be created either with a constructor (using default timeout settings)
 * or with {@link #builder()} for custom timeout and polling delay values.
 */
public class LambdaWaitTask {
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int DEFAULT_DELAY_MILLIS = 100;

    private final Supplier<Boolean> condition;
    private final int timeOutSeconds;
    private final int delayMillis;

    /**
     * Constructs a new LambdaWaitTask with the given condition and default settings.
     *
     * @param condition A {@link Supplier} that returns {@code true} when the desired condition is met.
     *                  Default timeout is 5 seconds and default delay is 100 milliseconds.
     */
    public LambdaWaitTask(Supplier<Boolean> condition) {
        this(condition, DEFAULT_TIMEOUT_SECONDS, DEFAULT_DELAY_MILLIS);
    }

    private LambdaWaitTask(Supplier<Boolean> condition, int timeOutSeconds, int delayMillis) {
        this.condition = Objects.requireNonNull(condition, "condition must not be null");
        this.timeOutSeconds = timeOutSeconds;
        this.delayMillis = delayMillis;
    }

    /**
     * Creates a fluent builder for {@link LambdaWaitTask}.
     *
     * @return a new {@link Builder} instance.
     */
    public static Builder builder() {
        return new Builder();
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
     * Executes the wait task using timeout and delay configured in this instance.
     *
     * @return {@code true} if the condition was met within the timeout, {@code false} otherwise.
     */
    public Boolean execute() {
        return execute(this.timeOutSeconds, this.delayMillis);
    }

    /**
     * Executes the wait task with explicit timeout settings.
     * <p>
     * If either argument is {@code null}, the value configured in this instance is used.
     *
     * @param timeOutSeconds The maximum time to wait in seconds, or {@code null} to use the configured value.
     * @param delayMillis    The delay between condition checks in milliseconds, or {@code null} to use the configured value.
     * @return {@code true} if the condition was met within the timeout, {@code false} otherwise.
     */
    public Boolean execute(Integer timeOutSeconds, Integer delayMillis) {
        int timeout = timeOutSeconds != null ? timeOutSeconds : this.timeOutSeconds;
        int delay = delayMillis != null ? delayMillis : this.delayMillis;
        long startTime = System.currentTimeMillis();
        while (true) {
            if (condition.get()) {
                return true;
            }
            if (System.currentTimeMillis() - startTime >= timeout * 1000L) {
                break;
            }
            Shared.sleep(delay);
        }
        return false;
    }

    /**
     * Builder for {@link LambdaWaitTask}.
     */
    public static class Builder {
        private Supplier<Boolean> condition;
        private int timeOutSeconds = DEFAULT_TIMEOUT_SECONDS;
        private int delayMillis = DEFAULT_DELAY_MILLIS;

        /**
         * Sets the condition to evaluate.
         *
         * @param condition a supplier that returns {@code true} when waiting can stop.
         * @return this builder instance.
         */
        public Builder condition(Supplier<Boolean> condition) {
            this.condition = condition;
            return this;
        }

        /**
         * Sets timeout in seconds.
         *
         * @param timeOutSeconds maximum wait duration in seconds.
         * @return this builder instance.
         */
        public Builder timeOutSeconds(int timeOutSeconds) {
            this.timeOutSeconds = timeOutSeconds;
            return this;
        }

        /**
         * Sets delay between condition checks in milliseconds.
         *
         * @param delayMillis polling delay in milliseconds.
         * @return this builder instance.
         */
        public Builder delayMillis(int delayMillis) {
            this.delayMillis = delayMillis;
            return this;
        }

        /**
         * Builds a new {@link LambdaWaitTask}.
         *
         * @return configured {@link LambdaWaitTask}.
         * @throws IllegalStateException if condition was not provided.
         */
        public LambdaWaitTask build() {
            if (condition == null) {
                throw new IllegalStateException("condition must be provided");
            }
            return new LambdaWaitTask(condition, timeOutSeconds, delayMillis);
        }
    }
}

