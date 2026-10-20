package com.rnm;

public final class RnmTpsEstimator {
    private static long lastGameTime = -1L;
    private static long lastReceiveNanos = -1L;
    private static long lastUpdateNanos = -1L;
    private static double tps = -1.0;

    public static synchronized void onTimeUpdate(long gameTime) {
        long now = System.nanoTime();

        if (lastGameTime >= 0 && lastReceiveNanos >= 0) {
            long elapsedNanos = now - lastReceiveNanos;
            long tickDifference = gameTime - lastGameTime;

            if (elapsedNanos > 0 && tickDifference > 0) {
                double seconds = elapsedNanos / 1_000_000_000.0;
                double estimate = tickDifference / seconds;

                tps = Math.max(0.0, Math.min(20.0, estimate));
                lastUpdateNanos = now;
            }
        }

        lastGameTime = gameTime;
        lastReceiveNanos = now;
    }

    public static synchronized double getTps() {
        if (lastUpdateNanos < 0) {
            return -1.0;
        }

        // 超過 10 秒沒有新資料，就視為目前無法估算。
        if (System.nanoTime() - lastUpdateNanos > 10_000_000_000L) {
            return -1.0;
        }

        return tps;
    }

    public static synchronized void reset() {
        lastGameTime = -1L;
        lastReceiveNanos = -1L;
        lastUpdateNanos = -1L;
        tps = -1.0;
    }
}