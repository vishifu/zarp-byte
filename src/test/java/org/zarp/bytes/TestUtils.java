package org.zarp.bytes;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public final class TestUtils {

    private TestUtils() {}

    /**
     * Wraps await of CountDownLatch, hides exception
     */
    public static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

    /**
     * Wraps await of CyclicBarrier, hides exception
     */
    public static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (BrokenBarrierException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(ex);
        }
    }

}
