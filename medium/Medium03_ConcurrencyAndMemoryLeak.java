/**
 * DIFFICULTY : Medium
 * SNIPPET    : 03 — Concurrency & Memory Leak
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Medium03_ConcurrencyAndMemoryLeak_Answer.java.
 *
 * Context: This runs in a web application with a Tomcat thread pool.
 * Hint: there are 5 distinct issues — 2 concurrency, 2 memory leak, 1 correctness.
 */
import java.util.*;

public class Medium03_ConcurrencyAndMemoryLeak {

    // ── Shared request counter (used across threads) ──────────────────────────

    static class RequestStats {
        private int totalRequests   = 0;
        private int failedRequests  = 0;

        public void recordSuccess() { totalRequests++; }
        public void recordFailure() { totalRequests++; failedRequests++; }

        public double getFailureRate() {
            return (double) failedRequests / totalRequests;
        }
    }

    // ── Per-request user context (Tomcat reuses threads) ──────────────────────

    static class RequestContext {
        private static ThreadLocal<UserContext> current = new ThreadLocal<>();

        public static void set(UserContext ctx)  { current.set(ctx); }
        public static UserContext get()          { return current.get(); }
        // No remove() method
    }

    // ── In-memory session registry ────────────────────────────────────────────

    static class SessionRegistry {
        private static final Map<String, Session> activeSessions = new HashMap<>();

        public static void register(String sessionId, Session session) {
            activeSessions.put(sessionId, session);
        }

        public static Session lookup(String sessionId) {
            Session s = activeSessions.get(sessionId);
            if (activeSessions.containsKey(sessionId)) {    // redundant / TOCTOU issue
                s.markAccessed();
            }
            return s;
        }

        public static void deregister(String sessionId) {
            // Intentionally omitted — sessions are never removed
        }
    }

    // ── Servlet filter wiring it all together ──────────────────────────────────

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
            }
            // no finally block
        }
    }
}
