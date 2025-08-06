package de.morihofi.certgine.core.modules;

import de.morihofi.certgine.types.modules.ModuleScheduledTask;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link ModuleTaskScheduler} verifying task cancellation behaviour.
 */
class ModuleTaskSchedulerTest {

    @Test
    void cancelModuleTasksInvokesTaskCancellation() {
        ModuleTaskScheduler scheduler = new ModuleTaskScheduler();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        ModuleScheduledTask task = new ModuleScheduledTask() {
            @Override
            public Runnable task() {
                return () -> {};
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };
        scheduler.registerModuleTasks("mod", Map.of("* * * * *", task));
        scheduler.cancelModuleTasks("mod");
        assertTrue(cancelled.get());
        scheduler.shutdown();
    }

    @Test
    void shutdownCancelsRegisteredTasks() {
        ModuleTaskScheduler scheduler = new ModuleTaskScheduler();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        ModuleScheduledTask task = new ModuleScheduledTask() {
            @Override
            public Runnable task() {
                return () -> {};
            }

            @Override
            public void cancel() {
                cancelled.set(true);
            }
        };
        scheduler.registerModuleTasks("mod", Map.of("* * * * *", task));
        scheduler.shutdown();
        assertTrue(cancelled.get());
    }
}
