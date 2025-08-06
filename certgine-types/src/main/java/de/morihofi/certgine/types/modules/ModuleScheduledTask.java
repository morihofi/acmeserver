/*
 * SPDX-FileCopyrightText: 2025 Moritz Hofmann
 * SPDX-License-Identifier: MIT
 */

package de.morihofi.certgine.types.modules;

/**
 * Represents a task that can be scheduled by the runtime on behalf of a
 * module. Implementations supply the {@link Runnable} to execute and may
 * provide a {@link #cancel()} method to release resources when the module is
 * unloaded.
 */
public interface ModuleScheduledTask {

    /**
     * Runnable to execute when the cron expression of the task matches.
     *
     * @return runnable to execute
     */
    Runnable task();

    /**
     * Cancels the task and releases any resources.
     *
     * <p>The runtime will invoke this method when the owning module is
     * unloaded. Implementations may override this method to perform custom
     * cleanup. The default implementation does nothing.</p>
     */
    default void cancel() {
        // default no-op
    }
}
