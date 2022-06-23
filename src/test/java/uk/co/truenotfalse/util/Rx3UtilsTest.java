package uk.co.truenotfalse.util;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;


class Rx3UtilsTest {
    @Test
    void exponentialBackoff() {
        final int maxRetries = 5;
        final double factor = 2.0;
        final long firstDelay = 10000000L;
        final TimeUnit timeUnit = TimeUnit.NANOSECONDS;
        final Exception ex = new RuntimeException("bang");
        final TestSubscriber<Integer> subscriber = TestSubscriber.create();
        final AtomicInteger errorCount = new AtomicInteger();
        final long[] timestamps = new long[maxRetries + 1];

        Flowable.just(1, 2, 3).
                // Force error after 3 emissions.
                        concatWith(Flowable.error(ex)).
                // Record error timestamps.
                        doOnError(error -> timestamps[errorCount.getAndIncrement()] = System.nanoTime()).
                // Retry with backoff.
                        retryWhen(Rx3Utils.exponentialBackoff(firstDelay, timeUnit, factor, maxRetries)).
                // Go.
                        blockingSubscribe(subscriber);

        subscriber.assertError(ex);
        assertEquals(maxRetries + 1, errorCount.get(), "Unexpected number of retries.");

        subscriber.assertValues(Collections.nCopies(maxRetries + 1, IntStream.rangeClosed(1, 3).boxed().toList()).stream().flatMap(Collection::stream).toArray(Integer[]::new));

        final long firstDelayAsNanos = timeUnit.toNanos(firstDelay);
        // Skip the first interval as with small intervals the first interval can overrun disproportionally to its
        // target length.  This can be avoided by increasing the first interval but then the entire test take a
        // second or more to run.
        for (int i = 2; i <= maxRetries; i++) {
            final long expectedDelay = Math.round(Math.pow(factor, i - 1) * firstDelayAsNanos);
            final long actualDelayNoJitter =
                    (Math.round((timestamps[i] - timestamps[i - 1]) / (double) firstDelayAsNanos) * firstDelayAsNanos);

            assertEquals(expectedDelay, actualDelayNoJitter, "Unexpected retry delay.");
        }
    }
}
