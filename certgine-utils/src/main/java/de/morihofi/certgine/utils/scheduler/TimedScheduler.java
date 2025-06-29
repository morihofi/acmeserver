/*
 * SPDX-FileCopyrightText: 2023-2025 Moritz Hofmann <info@morihofi.de>
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.utils.scheduler;

import com.cronutils.model.Cron;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import com.cronutils.model.CronType;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple scheduler based on cron expressions. Tasks registered here are executed
 * when the cron expression matches the current time.
 */
@Slf4j
public class TimedScheduler {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final CopyOnWriteArrayList<ScheduledTask> tasks = new CopyOnWriteArrayList<>();
    private final CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));

    /**
     * Registers a new task to be executed according to the given cron expression.
     *
     * @param cronExpression cron expression following the UNIX format
     * @param task           runnable to execute
     */
    public void schedule(String cronExpression, Runnable task) {
        Cron cron = parser.parse(cronExpression);
        cron.validate();
        ScheduledTask scheduled = new ScheduledTask(cron, task);
        tasks.add(scheduled);
        scheduleNextExecution(scheduled);
    }

    private void scheduleNextExecution(ScheduledTask task) {
        ExecutionTime executionTime = ExecutionTime.forCron(task.cron());
        Optional<ZonedDateTime> next = executionTime.nextExecution(ZonedDateTime.now());
        if (next.isEmpty()) {
            log.warn("Cron expression {} does not yield future execution", task.cron().asString());
            return;
        }
        Duration delay = Duration.between(ZonedDateTime.now(), next.get());
        executor.schedule(() -> {
            try {
                task.task().run();
            } finally {
                scheduleNextExecution(task);
            }
        }, delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the scheduler and clears all registered tasks.
     */
    public void shutdown() {
        executor.shutdownNow();
        tasks.clear();
    }

    private record ScheduledTask(Cron cron, Runnable task) {
    }
}
