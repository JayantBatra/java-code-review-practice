/**
 * DIFFICULTY : Hard
 * SNIPPET    : 02 — Deep Concurrency, Design & Exception Handling
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Hard02_DeepConcurrencyAndDesign_Answer.java.
 *
 * Context: A rate-limiter + circuit-breaker used by a payments API gateway.
 *          Called by ~200 concurrent threads in production.
 * Hint: there are 5 issues — 3 concurrency, 1 design, 1 exception handling.
 */
import java.util.*;
import java.util.concurrent.atomic.*;

public class Hard02_DeepConcurrencyAndDesign {

    // ── Rate Limiter ──────────────────────────────────────────────────────────

    static class RateLimiter {
        private final int     maxRequests;
        private final long    windowMs;
        private int           requestCount   = 0;
        private long          windowStart    = System.currentTimeMillis();

        public RateLimiter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs    = windowMs;
        }

        public boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) {
                windowStart    = now;
                requestCount   = 0;       // reset
            }
            requestCount++;
            return requestCount <= maxRequests;
        }
    }

    // ── Circuit Breaker ───────────────────────────────────────────────────────

    static class CircuitBreaker {
        enum State { CLOSED, OPEN, HALF_OPEN }

        private State   state          = State.CLOSED;
        private int     failureCount   = 0;
        private long    openedAt       = 0;

        private static final int  FAILURE_THRESHOLD   = 5;
        private static final long RECOVERY_TIMEOUT_MS = 30_000;

        public boolean isOpen() {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - openedAt > RECOVERY_TIMEOUT_MS) {
                    state = State.HALF_OPEN;  // allow one probe request through
                }
            }
            return state == State.OPEN;
        }

        public void recordSuccess() {
            failureCount = 0;
            state        = State.CLOSED;
        }

        public void recordFailure() {
            failureCount++;
            if (failureCount >= FAILURE_THRESHOLD) {
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
            }
        }
    }

    // ── Payment Gateway Proxy ─────────────────────────────────────────────────

    static class PaymentGatewayProxy {
        private final RateLimiter     rateLimiter     = new RateLimiter(100, 1000);
        private final CircuitBreaker  circuitBreaker  = new CircuitBreaker();

        public PaymentResult charge(PaymentRequest request) {
            if (!rateLimiter.allow()) {
                throw new RateLimitException("Rate limit exceeded");
            }
            if (circuitBreaker.isOpen()) {
                throw new CircuitOpenException("Service unavailable");
            }

            try {
                PaymentResult result = externalGateway.charge(request);
                circuitBreaker.recordSuccess();
                return result;

            } catch (GatewayException e) {
                circuitBreaker.recordFailure();
                throw new PaymentException("Charge failed", e);

            } catch (Exception e) {
                // catch-all
                throw new PaymentException("Unexpected error", e);
            }
        }
    }

    // ── Retry Wrapper ──────────────────────────────────────────────────────────

    static class RetryablePaymentService {

        public PaymentResult chargeWithRetry(PaymentRequest request, int maxRetries) {
            int attempts = 0;
            while (attempts < maxRetries) {
                try {
                    return gateway.charge(request);
                } catch (PaymentException e) {
                    attempts++;
                    if (attempts == maxRetries) {
                        throw e;
                    }
                }
            }
            return null;  // unreachable, but compiler complains
        }
    }
}
