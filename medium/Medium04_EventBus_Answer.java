/**
 * ANSWER FILE — Medium04_EventBus
 * =================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Static Collections Not Thread-Safe
 * ─────────────────────────────────────────────────────────
 * What  : handlers is a static HashMap and each value is an ArrayList.
 *         subscribe(), publish(), and unsubscribe() can be called from
 *         different threads simultaneously. Concurrent puts to a HashMap
 *         during rehashing corrupt its internal structure.
 *         Iterating the ArrayList in publish() while subscribe() adds to it
 *         throws ConcurrentModificationException.
 * Fix   : Use ConcurrentHashMap for the outer map and CopyOnWriteArrayList
 *         for each handler list. CopyOnWriteArrayList is ideal here because
 *         reads (iteration during publish) are far more frequent than writes.
 *
 * Review comment:
 *   "handlers is a static HashMap and the inner lists are ArrayLists — neither
 *    is thread-safe. Concurrent subscribe/publish calls cause data corruption.
 *    Use ConcurrentHashMap and CopyOnWriteArrayList."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — One Handler Exception Aborts All Remaining Handlers
 * ─────────────────────────────────────────────────────────
 * What  : If h.handle(event) throws a RuntimeException, the loop terminates
 *         and all subsequent handlers for that event are silently skipped.
 * Why   : Each handler is an independent subscriber with its own concerns.
 *         A bug in OrderAnalyticsHandler should not prevent OrderEmailHandler
 *         from sending the confirmation email.
 * Fix   : Wrap each h.handle(event) in try-catch. Log the failure with the
 *         handler class name so it is diagnosable without crashing others.
 *
 * Review comment:
 *   "If h.handle(event) throws, the loop aborts and remaining handlers are
 *    silently skipped. A bug in one subscriber shouldn't affect others.
 *    Wrap each call in try-catch and log the failure."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Unbounded Thread Creation in publishAsync()
 * ─────────────────────────────────────────────────────────
 * What  : new Thread(...).start() on every call creates one OS thread per
 *         event under load. A spike of 1000 events/second = 1000 threads,
 *         each consuming ~1MB stack — instant OutOfMemoryError.
 * Fix   : Use a shared ExecutorService (e.g. Executors.newFixedThreadPool(N)
 *         or a cached pool) stored as a static field. Submit tasks rather
 *         than creating threads directly.
 *
 * Review comment:
 *   "new Thread().start() for every async event creates unbounded threads under
 *    load. Use a shared ExecutorService with a bounded thread pool."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — unsubscribe() Silently Fails for Lambda/Anonymous Handlers
 * ─────────────────────────────────────────────────────────
 * What  : List.remove() uses equals() to locate the element. Lambda instances
 *         and anonymous classes do not override equals() — each new instance
 *         is a different object even if it represents the same method reference.
 *         unsubscribe() silently does nothing and the handler leaks.
 * Why   : This is a classic observer-pattern memory leak: the event bus holds
 *         a strong reference to every widget/component that subscribed, even
 *         after those components are destroyed.
 * Fix   : Require callers to store the handler instance and pass the exact
 *         same object to unsubscribe(). Document this contract clearly.
 *         Alternatively, return an opaque Subscription token from subscribe()
 *         and use it to cancel.
 *
 * Review comment:
 *   "list.remove(handler) uses equals(). Lambda handlers don't override equals()
 *    — every instance is unique. unsubscribe() silently does nothing, causing a
 *    memory leak. Consider returning a Subscription token from subscribe() and
 *    using it to cancel."
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import java.util.concurrent.*;

public class Medium04_EventBus_Answer {

    // Fix 1: thread-safe outer map + inner lists
    private static final Map<String, List<EventHandler>> handlers = new ConcurrentHashMap<>();
    // Fix 3: shared bounded thread pool
    private static final ExecutorService asyncPool = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    );

    public static void subscribe(String eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(handler);
    }

    // Fix 2: each handler is isolated — one failure doesn't abort the rest
    public static void publish(String eventType, Event event) {
        List<EventHandler> list = handlers.get(eventType);
        if (list == null) return;
        for (EventHandler h : list) {
            try {
                h.handle(event);
            } catch (Exception e) {
                log.error("Handler {} failed for event {}: {}",
                          h.getClass().getSimpleName(), eventType, e.getMessage(), e);
            }
        }
    }

    public static void unsubscribe(String eventType, EventHandler handler) {
        List<EventHandler> list = handlers.get(eventType);
        if (list != null) list.remove(handler);  // caller must pass the exact same instance
    }

    // Fix 3: submit to shared pool — no unbounded thread creation
    public static void publishAsync(String eventType, Event event) {
        asyncPool.submit(() -> publish(eventType, event));
    }
}
