package de.morihofi.certgine.utils.scheduler;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link TimedScheduler} verifying handle cancellation behaviour.
 */
@Disabled // TODO: Enable again when we filter out long taking tests
class TimedSchedulerTest {

    @Test
    void cancelHandleStopsFutureExecutions() throws Exception {
        TimedScheduler scheduler = new TimedScheduler();
        AtomicInteger firstCount = new AtomicInteger();
        AtomicInteger secondCount = new AtomicInteger();
        CountDownLatch firstRunLatch = new CountDownLatch(2);

        waitUntilNearNextMinute();

        TimedScheduler.ScheduledHandle first = scheduler.schedule("* * * * *", () -> {
            firstCount.incrementAndGet();
            firstRunLatch.countDown();
        });
        scheduler.schedule("* * * * *", () -> {
            secondCount.incrementAndGet();
            firstRunLatch.countDown();
        });

        assertTrue(firstRunLatch.await(70, TimeUnit.SECONDS));
        first.cancel();

        Thread.sleep(TimeUnit.SECONDS.toMillis(65));

        assertEquals(1, firstCount.get());
        assertTrue(secondCount.get() >= 2);
        scheduler.shutdown();
    }

    private static void waitUntilNearNextMinute() throws InterruptedException {
        int sec = ZonedDateTime.now().getSecond();
        if (sec < 55) {
            Thread.sleep((55 - sec) * 1000L);
        }
    }
}
