/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.ModuleScheduledTask;
import de.morihofi.certgine.utils.scheduler.TimedScheduler;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.NonNull;

/**
 * Manages scheduling of tasks provided by modules.
 */
public class ModuleTaskScheduler {

    private final TimedScheduler timedScheduler = new TimedScheduler();
    private final Map<String, List<TimedScheduler.ScheduledHandle>> scheduledHandles =
            new HashMap<>();
    private final Map<String, List<ModuleScheduledTask>> moduleTasks = new HashMap<>();

    /**
     * Registers all scheduled tasks of a module.
     *
     * @param moduleName name of the module
     * @param tasks      map of cron expressions to tasks
     */
    public void registerModuleTasks(
            @NonNull String moduleName, @NonNull Map<String, ModuleScheduledTask> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        List<TimedScheduler.ScheduledHandle> handles = new ArrayList<>();
        for (Map.Entry<String, ModuleScheduledTask> entry : tasks.entrySet()) {
            TimedScheduler.ScheduledHandle handle =
                    timedScheduler.schedule(entry.getKey(), entry.getValue().task());
            handles.add(handle);
        }
        scheduledHandles.put(moduleName, handles);
        moduleTasks.put(moduleName, new ArrayList<>(tasks.values()));
    }

    /**
     * Cancels all tasks registered for a module.
     *
     * @param moduleName name of the module
     */
    public void cancelModuleTasks(@NonNull String moduleName) {
        List<TimedScheduler.ScheduledHandle> handles = scheduledHandles.remove(moduleName);
        if (handles != null) {
            handles.forEach(TimedScheduler.ScheduledHandle::cancel);
        }
        List<ModuleScheduledTask> tasks = moduleTasks.remove(moduleName);
        if (tasks != null) {
            tasks.forEach(ModuleScheduledTask::cancel);
        }
    }

    /**
     * Shuts down the scheduler and cancels all registered tasks.
     */
    public void shutdown() {
        moduleTasks.values().forEach(list -> list.forEach(ModuleScheduledTask::cancel));
        timedScheduler.shutdown();
        scheduledHandles.clear();
        moduleTasks.clear();
    }
}

