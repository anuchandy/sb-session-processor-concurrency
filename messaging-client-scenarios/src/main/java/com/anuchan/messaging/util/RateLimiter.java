package com.anuchan.messaging.util;

import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter implements AutoCloseable {
    private final int maxConcurrency;
    private final AtomicInteger inFlight = new AtomicInteger(0);
    private final AtomicInteger bucket = new AtomicInteger(0);
    private final Timer replenishTimer;
    private final boolean enabled;

    public RateLimiter(int secondsInPeriod, int maxLimitPerSecond, int maxConcurrency) {
        this.maxConcurrency = maxConcurrency;
        this.enabled = maxLimitPerSecond > 0;
        this.replenishTimer = new Timer("replenish");
        if (enabled) {
            replenishTimer.schedule(new TimerTask() {
                public void run() {
                    bucket.set(maxLimitPerSecond);
                }
            }, 0, secondsInPeriod * 1000L);
        }
    }

    public Mono<Boolean> acquire() {
        if (tryAcquire()) {
            return Mono.just(true);
        }

        return Mono.delay(Duration.ofMillis(10)).repeat(() -> !tryAcquire()).then().thenReturn(true);
    }

    public boolean tryAcquire() {
        if (!enabled) {
            return true;
        }

        int concurrency;
        do {
            concurrency = inFlight.get();
            if (concurrency == maxConcurrency) {
                return false;
            }

        } while (!inFlight.compareAndSet(concurrency, concurrency + 1));

        int remaining;
        do {
            remaining = bucket.get();
            if (remaining == 0) {
                inFlight.decrementAndGet();
                return false;
            }
        } while (!bucket.compareAndSet(remaining, remaining - 1));

        return true;
    }

    public void release() {
        inFlight.decrementAndGet();
    }

    @Override
    public void close() {
        replenishTimer.cancel();
    }
}