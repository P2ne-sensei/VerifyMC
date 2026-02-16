package team.kitemc.verifymc.service;

import java.util.concurrent.ConcurrentHashMap;

public class RateLimitService {
    private final ConcurrentHashMap<String, WindowRateLimitRecord> questionnaireRateLimitStore = new ConcurrentHashMap<>();

    public RateLimitDecision checkQuestionnaireRateLimit(String key, int limit, long windowMs) {
        if (key == null || key.isBlank() || limit <= 0 || windowMs <= 0) {
            return RateLimitDecision.allowed();
        }
        long now = System.currentTimeMillis();
        WindowRateLimitRecord rec = questionnaireRateLimitStore.compute(key, (k, old) -> {
            if (old == null || now - old.windowStart >= windowMs) {
                return new WindowRateLimitRecord(1, now);
            }
            old.count++;
            return old;
        });
        if (rec.count > limit) {
            long retryAfterMs = windowMs - (now - rec.windowStart);
            return RateLimitDecision.blocked(Math.max(1L, retryAfterMs));
        }
        return RateLimitDecision.allowed();
    }

    private static class WindowRateLimitRecord {
        private int count;
        private long windowStart;

        private WindowRateLimitRecord(int count, long windowStart) {
            this.count = count;
            this.windowStart = windowStart;
        }
    }

    public static class RateLimitDecision {
        private final boolean allowed;
        private final long retryAfterMs;

        private RateLimitDecision(boolean allowed, long retryAfterMs) {
            this.allowed = allowed;
            this.retryAfterMs = retryAfterMs;
        }

        public static RateLimitDecision allowed() {
            return new RateLimitDecision(true, 0L);
        }

        public static RateLimitDecision blocked(long retryAfterMs) {
            return new RateLimitDecision(false, retryAfterMs);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRetryAfterMs() {
            return retryAfterMs;
        }
    }
}
