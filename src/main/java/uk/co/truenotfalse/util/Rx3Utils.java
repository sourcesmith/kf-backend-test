package uk.co.truenotfalse.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Function;
import org.reactivestreams.Publisher;


/**
 *  Java RX 3 related utility methods.
 */
public final class Rx3Utils
{
    /**
     *  Returns a function implementing an exponential back off delay using a power function for retries which can be
     *  used with the retryWhen method of Observables.
     *
     * @param firstDelay  The duration of the first delay.
     * @param unit  The time unit the delay is specified in.
     * @param factor  The exponent to apply to the power function to generate increasing durations for delays.
     * @param maxRetries  The number of retries on errors to attempt.
     *
     * @return  A function implementing an exponential backoff delay retry policy.
     */
    public static Function<? super Flowable<Throwable>,
                           ? extends Publisher<@NonNull ?>> exponentialBackoff(final long firstDelay,
                                                                               final TimeUnit unit,
                                                                               final double factor,
                                                                               final int maxRetries)
    {
        return errors ->
        {
            if(firstDelay < 1L)
            {
                throw new IllegalArgumentException("A delay is required.");
            }
            if(maxRetries < 1L)
            {
                throw new IllegalArgumentException("A number of retries must be specified");
            }

            final AtomicInteger counter = new AtomicInteger();

            return errors.takeWhile(error -> counter.getAndIncrement() <= maxRetries).
                          flatMap(error ->
                                  {
                                      if(counter.get() > maxRetries)
                                      {
                                          return Flowable.error(error);
                                      }

                                      return Flowable.timer(Math.round(Math.pow(factor, counter.get() - 1) * firstDelay), unit);
                                  });
        };
    }


    private Rx3Utils()
    {
        throw new UnsupportedOperationException("Instance of RxUtils may not be instantiated.");
    }
}
