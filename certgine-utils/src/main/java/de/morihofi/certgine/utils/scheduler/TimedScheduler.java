/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.scheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import lombok.extern.slf4j.Slf4j;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Simple scheduler based on cron expressions. Tasks registered here are executed
 * when the cron expression matches the current time.
 */
@Slf4j
public class TimedScheduler {
    private final ScheduledExecutorService executor;
    private final CopyOnWriteArrayList<ScheduledHandle> tasks = new CopyOnWriteArrayList<>();
    private final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
    private final Clock clock;

    /**
     * Creates a scheduler using a single-threaded executor.
     */
    public TimedScheduler() {
        this(Executors.newSingleThreadScheduledExecutor(), Clock.systemDefaultZone());
    }

    /**
     * Creates a scheduler using the provided executor.
     *
     * @param executor executor used to schedule tasks
     */
    public TimedScheduler(ScheduledExecutorService executor) {
        this(executor, Clock.systemDefaultZone());
    }

    /**
     * Creates a scheduler using the provided executor and clock.
     *
     * @param executor executor used to schedule tasks
     * @param clock    clock providing the current time
     */
    public TimedScheduler(ScheduledExecutorService executor, Clock clock) {
        this.executor = executor;
        this.clock = clock;
    }

    /**
     * Creates a scheduler using a single-threaded executor and the provided clock.
     *
     * @param clock clock providing the current time
     */
    public TimedScheduler(Clock clock) {
        this(Executors.newSingleThreadScheduledExecutor(), clock);
    }

    /**
     * Registers a new task to be executed according to the given cron expression.
     *
     * @param cronExpression cron expression following the UNIX format
     * @param task           runnable to execute
     * @return handle for the scheduled task which can be used to cancel future executions
     */
    public ScheduledHandle schedule(String cronExpression, Runnable task) {
        Cron cron = parser.parse(cronExpression);
        cron.validate();
        ScheduledTask scheduled = new ScheduledTask(cron, task);
        tasks.add(scheduled);
        scheduleNextExecution(scheduled);
        return scheduled;
    }

    private void scheduleNextExecution(ScheduledTask task) {
        if (task.cancelled) {
            return;
        }
        ExecutionTime executionTime = ExecutionTime.forCron(task.cron());
        Instant instant = clock.instant();
        ZonedDateTime now = ZonedDateTime.ofInstant(instant, clock.getZone());
        Optional<ZonedDateTime> next = executionTime.nextExecution(now);
        if (next.isEmpty()) {
            log.warn("Cron expression {} does not yield future execution", task.cron().asString());
            return;
        }
        Duration delay = Duration.between(instant, next.get().toInstant());
        ScheduledFuture<?> future = executor.schedule(() -> {
            try {
                task.task().run();
            } finally {
                scheduleNextExecution(task);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
        task.setFuture(future);
        if (task.cancelled) {
            future.cancel(false);
        }
    }

    /**
     * Stops the scheduler and clears all registered tasks.
     */
    public void shutdown() {
        tasks.forEach(ScheduledHandle::cancel);
        executor.shutdownNow();
        tasks.clear();
    }

    /**
     * Handle allowing cancellation of a scheduled task.
     */
    public interface ScheduledHandle {
        /**
         * Cancel future executions of the task.
         */
        void cancel();
    }

    private class ScheduledTask implements ScheduledHandle {
        private final Cron cron;
        private final Runnable task;
        private volatile boolean cancelled = false;
        private volatile ScheduledFuture<?> future;

        ScheduledTask(Cron cron, Runnable task) {
            this.cron = cron;
            this.task = task;
        }

        Cron cron() {
            return cron;
        }

        Runnable task() {
            return task;
        }

        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

        @Override
        public void cancel() {
            cancelled = true;
            if (future != null) {
                future.cancel(false);
            }
            tasks.remove(this);
        }
    }
}
