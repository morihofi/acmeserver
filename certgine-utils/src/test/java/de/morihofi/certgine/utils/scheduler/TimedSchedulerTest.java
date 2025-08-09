package de.morihofi.certgine.utils.scheduler;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TimedScheduler} verifying handle cancellation behaviour.
 */
class TimedSchedulerTest {

    @Test
    void cancelHandleStopsFutureExecutions() throws Exception {
        Clock clock = Clock.fixed(
                Instant.parse("2024-01-01T00:00:55Z"), ZoneId.systemDefault());
        TimedScheduler scheduler = new TimedScheduler(clock);
        AtomicInteger firstCount = new AtomicInteger();
        AtomicInteger secondCount = new AtomicInteger();
        CountDownLatch firstRunLatch = new CountDownLatch(2);

        TimedScheduler.ScheduledHandle first = scheduler.schedule("* * * * *", () -> {
            firstCount.incrementAndGet();
            firstRunLatch.countDown();
        });
        scheduler.schedule("* * * * *", () -> {
            secondCount.incrementAndGet();
            firstRunLatch.countDown();
        });

        assertTrue(firstRunLatch.await(15, TimeUnit.SECONDS));
        first.cancel();

        Thread.sleep(TimeUnit.SECONDS.toMillis(10));

        assertEquals(1, firstCount.get());
        assertTrue(secondCount.get() >= 2);
        scheduler.shutdown();
    }
}
