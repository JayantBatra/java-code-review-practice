/**
 * ANSWER FILE — Hard02_DeepConcurrencyAndDesign
 * ===============================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — RateLimiter Is Not Thread-Safe (Race Condition)
 * ─────────────────────────────────────────────────────────
 * What  : requestCount and windowStart are plain int/long fields. allow() reads
 *         and writes both without synchronization. Under 200 concurrent threads:
 *         - Two threads can both read requestCount = 99, both increment to 100,
 *           both write 100 — one increment is lost.
 *         - Two threads can simultaneously see an expired window, both reset
 *           windowStart and requestCount, effectively resetting twice.
 *         - The check-then-act on (requestCount <= maxRequests) is not atomic.
 * Why   : Rate limiting relies on precise counts. A race here means the limiter
 *         allows 2× or 3× the intended traffic, defeating its purpose.
 * Fix   : Synchronize the entire allow() method, or use AtomicInteger +
 *         AtomicLong with CAS (compare-and-swap) for a lock-free version.
 *         For production, consider a Semaphore or a dedicated library
 *         (Bucket4j, Resilience4j).
 *
 * Review comment:
 *   "RateLimiter.allow() reads and writes requestCount/windowStart without
 *    synchronization. Under concurrent load, multiple threads can simultaneously
 *    reset the window or lose increments, allowing far more traffic than
 *    intended. Synchronize allow() or use AtomicInteger + AtomicLong with CAS."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — CircuitBreaker Is Not Thread-Safe (Multiple Race Conditions)
 * ─────────────────────────────────────────────────────────
 * What  : state, failureCount, and openedAt are unsynchronized plain fields.
 *         Specific races:
 *         (a) isOpen() reads state, then writes state = HALF_OPEN in a
 *             non-atomic check-then-act. Two threads can both see OPEN, both
 *             flip to HALF_OPEN and both probe the downstream service.
 *         (b) recordFailure() increments failureCount (not atomic) and then
 *             writes state = OPEN — a non-atomic compound action. Two threads
 *             can both reach the threshold and both write openedAt, but with
 *             slightly different timestamps.
 *         (c) recordSuccess() resets state without visibility guarantees —
 *             other threads may see a stale OPEN state.
 * Why   : A circuit breaker that is not thread-safe either fails to open
 *         (allowing a cascading failure) or fails to close (causing unnecessary
 *         downtime). Both are production incidents.
 * Fix   : Use synchronized on all state-mutating methods, or model state
 *         transitions with AtomicReference<State> + AtomicInteger.
 *
 * Review comment:
 *   "CircuitBreaker's state, failureCount, and openedAt are unsynchronized.
 *    isOpen() performs a non-atomic check-then-act: two threads can both see
 *    OPEN, both flip to HALF_OPEN, and both send probe requests. recordFailure()
 *    has the same issue with failureCount. Synchronize all state-mutating
 *    methods, or use AtomicReference + AtomicInteger."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — catch(Exception) in charge() Is Too Broad
 * ─────────────────────────────────────────────────────────
 * What  : The catch-all at the bottom catches everything — including
 *         RateLimitException and CircuitOpenException thrown earlier in the
 *         same method, NPEs, OutOfMemoryError (if catching Throwable).
 *         It also catches InterruptedException without restoring the flag.
 * Why   : This masks programming errors and makes debugging extremely hard.
 *         If the payment request itself has a bug causing NPE, it's silently
 *         wrapped in PaymentException — the circuit breaker also opens
 *         incorrectly because recordFailure() is not called in the catch-all.
 * Fix   : Remove the catch-all. Let unexpected exceptions propagate.
 *         Only catch what you can meaningfully handle.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Retry Logic Retries Non-Retryable Exceptions
 * ─────────────────────────────────────────────────────────
 * What  : chargeWithRetry() retries on ALL PaymentException subclasses,
 *         including RateLimitException and CircuitOpenException.
 * Why   : Retrying a RateLimitException immediately makes the rate limit
 *         situation WORSE — 3 retries = 3× the pressure on the limiter.
 *         Retrying when the circuit is OPEN guarantees N failures before
 *         giving up, delaying recovery.
 * Fix   : Check the exception type (or a retryable flag) before retrying.
 *         Add exponential backoff with jitter for retryable failures.
 *
 * Review comment:
 *   "chargeWithRetry() retries on all PaymentExceptions, including
 *    RateLimitException. Retrying a rate-limit failure immediately makes
 *    the problem worse. Suggest checking a retryable flag on the exception
 *    (or matching specific types) and adding exponential backoff."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — Unreachable return null Masks a Logic Gap
 * ─────────────────────────────────────────────────────────
 * What  : The `return null` at the end of chargeWithRetry() is described as
 *         "unreachable" — but the compiler requires it because the loop can
 *         technically exit without returning. If the logic is ever changed,
 *         null could be returned to a caller that doesn't null-check.
 * Fix   : Throw an AssertionError("unreachable") or restructure the loop to
 *         use a do-while, making it truly impossible to fall through.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE (key changes only)
 * ─────────────────────────────────────────────────────────
 */
import java.util.concurrent.atomic.*;

public class Hard02_DeepConcurrencyAndDesign_Answer {

    // Fix 1 & 2: synchronized methods — all state transitions are atomic
    static class RateLimiter {
        private final int  maxRequests;
        private final long windowMs;
        private int  requestCount = 0;
        private long windowStart  = System.currentTimeMillis();

        public RateLimiter(int max, long windowMs) { this.maxRequests = max; this.windowMs = windowMs; }

        public synchronized boolean allow() {
            long now = System.currentTimeMillis();
            if (now - windowStart > windowMs) { windowStart = now; requestCount = 0; }
            return ++requestCount <= maxRequests;
        }
    }

    static class CircuitBreaker {
        enum State { CLOSED, OPEN, HALF_OPEN }

        private volatile State state       = State.CLOSED;
        private final AtomicInteger fails  = new AtomicInteger(0);
        private volatile long openedAt     = 0;

        private static final int  THRESHOLD     = 5;
        private static final long RECOVERY_MS   = 30_000;

        public synchronized boolean isOpen() {
            if (state == State.OPEN &&
                System.currentTimeMillis() - openedAt > RECOVERY_MS) {
                state = State.HALF_OPEN;
            }
            return state == State.OPEN;
        }

        public synchronized void recordSuccess() { fails.set(0); state = State.CLOSED; }

        public synchronized void recordFailure() {
            if (fails.incrementAndGet() >= THRESHOLD) {
                state    = State.OPEN;
                openedAt = System.currentTimeMillis();
            }
        }
    }

    static class PaymentGatewayProxy {
        private final RateLimiter    rateLimiter   = new RateLimiter(100, 1000);
        private final CircuitBreaker circuit       = new CircuitBreaker();

        public PaymentResult charge(PaymentRequest request) {
            if (!rateLimiter.allow())  throw new RateLimitException("Rate limit exceeded");
            if (circuit.isOpen())      throw new CircuitOpenException("Service unavailable");

            try {
                PaymentResult result = externalGateway.charge(request);
                circuit.recordSuccess();
                return result;
            } catch (GatewayException e) {
                circuit.recordFailure();
                throw new PaymentException("Charge failed", e);
            }
            // Fix 3: no catch-all — let unexpected exceptions propagate
        }
    }

    static class RetryablePaymentService {
        // Fix 4: only retry retryable exceptions; add backoff
        public PaymentResult chargeWithRetry(PaymentRequest request, int maxRetries) {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    return gateway.charge(request);
                } catch (RateLimitException | CircuitOpenException e) {
                    throw e;   // not retryable — fail immediately
                } catch (PaymentException e) {
                    if (attempt == maxRetries) throw e;
                    sleep(100L * (1 << attempt));   // exponential backoff: 200ms, 400ms...
                }
            }
            throw new AssertionError("unreachable");   // Fix 5: compiler happy, intent clear
        }
    }
}
