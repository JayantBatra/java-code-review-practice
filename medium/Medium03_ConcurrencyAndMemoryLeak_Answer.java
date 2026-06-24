/**
 * ANSWER FILE — Medium03_ConcurrencyAndMemoryLeak
 * =================================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Race Condition on int Fields (RequestStats)
 * ─────────────────────────────────────────────────────────
 * What  : totalRequests++ and failedRequests++ are NOT atomic. Each is a
 *         read-modify-write across three bytecode instructions. Two threads
 *         can both read 99, both compute 100, and both write 100 — one
 *         increment is silently lost.
 * Why   : In a Tomcat thread pool with hundreds of concurrent requests, the
 *         counter will drift significantly. Metrics dashboards show incorrect
 *         failure rates, which can mask production incidents.
 * Fix   : Use AtomicInteger or AtomicLong for each counter.
 *
 * Review comment:
 *   "totalRequests++ is not atomic — read-modify-write is three operations.
 *    Under concurrent load the counter will drift. Suggest replacing int with
 *    AtomicInteger and using incrementAndGet()."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — HashMap in Shared Static Field (SessionRegistry)
 * ─────────────────────────────────────────────────────────
 * What  : activeSessions is a static HashMap shared across all threads.
 *         Concurrent put() calls during rehashing can corrupt the internal
 *         linked-list, causing an infinite loop that pegs a CPU core at 100%.
 * Why   : This is one of the most notorious Java production bugs — nearly
 *         impossible to reproduce locally, catastrophic in prod.
 * Fix   : Use ConcurrentHashMap.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — TOCTOU Race in lookup() (SessionRegistry)
 * ─────────────────────────────────────────────────────────
 * What  : lookup() calls get() first, then containsKey() — two separate
 *         operations. In a concurrent environment another thread can remove
 *         the session between get() and containsKey(), causing s.markAccessed()
 *         to throw NPE on a stale reference.
 *         Also, get() already returns null for missing keys — the containsKey()
 *         check is redundant and adds a TOCTOU window.
 * Fix   : Use a single get() call and null-check the result.
 *
 * Review comment:
 *   "lookup() calls get() and then containsKey() — two separate operations
 *    with a TOCTOU gap. Between them, another thread could remove the session,
 *    making s.markAccessed() throw NPE. Simplify to a single get() call and
 *    null-check the result."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Sessions Never Removed (Memory Leak)
 * ─────────────────────────────────────────────────────────
 * What  : deregister() is a commented-out stub. Sessions are added on every
 *         login but never removed. activeSessions grows unbounded.
 * Why   : A busy server handling thousands of logins per day will exhaust
 *         heap memory. Sessions for users who logged out hours ago stay in
 *         memory forever.
 * Fix   : Implement deregister() and call it on logout/session-expiry.
 *         Alternatively, use a cache with TTL eviction (Caffeine/Guava).
 *
 * Review comment:
 *   "deregister() is a stub — sessions are added but never removed.
 *    activeSessions grows without bound and will eventually cause
 *    OutOfMemoryError. Suggest implementing deregister() and calling it
 *    on logout, or switching to a TTL-based cache."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — ThreadLocal Never Cleared (Memory Leak + Security Bug)
 * ─────────────────────────────────────────────────────────
 * What  : RequestContext.set() is called in the filter but there is no
 *         remove() and no finally block. When doFilter() exits, the UserContext
 *         stays in the thread's ThreadLocal slot.
 * Why   : (a) Memory leak: Tomcat reuses threads from a pool. Old UserContext
 *         objects accumulate and are never GC'd.
 *         (b) Security bug: The NEXT request handled by this thread will
 *         inherit the PREVIOUS user's context — a user-data leakage bug.
 * Fix   : Always call ThreadLocal.remove() in a finally block.
 *
 * Review comment:
 *   "RequestContext.set() is called but there is no matching remove(), and
 *    no finally block. In Tomcat's thread pool, the UserContext from request N
 *    will still be present for request N+1 handled by the same thread —
 *    leaking one user's data into another user's request. Call
 *    RequestContext.clear() in a finally block."
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Medium03_ConcurrencyAndMemoryLeak_Answer {

    // Fix 1: atomic counters — lock-free, correct under concurrent access
    static class RequestStats {
        private final AtomicInteger totalRequests  = new AtomicInteger(0);
        private final AtomicInteger failedRequests = new AtomicInteger(0);

        public void recordSuccess() { totalRequests.incrementAndGet(); }
        public void recordFailure() { totalRequests.incrementAndGet(); failedRequests.incrementAndGet(); }

        public double getFailureRate() {
            int total = totalRequests.get();
            return total == 0 ? 0.0 : (double) failedRequests.get() / total;
        }
    }

    static class RequestContext {
        private static final ThreadLocal<UserContext> current = new ThreadLocal<>();

        public static void set(UserContext ctx) { current.set(ctx); }
        public static UserContext get()         { return current.get(); }
        // Fix 5: expose a clear() method
        public static void clear()              { current.remove(); }
    }

    static class SessionRegistry {
        // Fix 2: ConcurrentHashMap — thread-safe
        private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

        public static void register(String id, Session session) { activeSessions.put(id, session); }

        // Fix 3: single atomic get() — no TOCTOU window
        public static Session lookup(String id) {
            Session s = activeSessions.get(id);
            if (s != null) s.markAccessed();
            return s;
        }

        // Fix 4: actually remove the session
        public static void deregister(String id) { activeSessions.remove(id); }
    }

    static class AuthFilter {
        private final RequestStats stats = new RequestStats();

        public void doFilter(Request request, Response response, FilterChain chain)
                throws Exception {
            try {
                UserContext ctx = resolveUser(request);
                RequestContext.set(ctx);
                chain.doFilter(request, response);
                stats.recordSuccess();
            } catch (AuthException e) {
                stats.recordFailure();
                response.setStatus(401);
            } finally {
                RequestContext.clear();   // Fix 5: always runs — no leak, no bleed
            }
        }
    }
}
