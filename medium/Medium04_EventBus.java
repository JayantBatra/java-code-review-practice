/**
 * DIFFICULTY : Medium
 * SNIPPET    : 04 — In-App Event Bus
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Medium04_EventBus_Answer.java.
 *
 * Context: Used in a Spring Boot service where components publish and subscribe
 *          to domain events (OrderPlaced, PaymentReceived, etc.) across threads.
 * Hint: there are 4 distinct issues.
 */
import java.util.*;

public class Medium04_EventBus {

    private static final Map<String, List<EventHandler>> handlers = new HashMap<>();

    public static void subscribe(String eventType, EventHandler handler) {
        handlers.computeIfAbsent(eventType, k -> new ArrayList<>()).add(handler);
    }

    /**
     * Publishes an event to all registered handlers for that type.
     */
    public static void publish(String eventType, Event event) {
        List<EventHandler> list = handlers.get(eventType);
        if (list != null) {
            for (EventHandler h : list) {
                h.handle(event);              // what if one throws?
            }
        }
    }

    /**
     * Removes a previously registered handler.
     */
    public static void unsubscribe(String eventType, EventHandler handler) {
        List<EventHandler> list = handlers.get(eventType);
        if (list != null) {
            list.remove(handler);
        }
    }

    /**
     * Publishes the event asynchronously on a new thread.
     */
    public static void publishAsync(String eventType, Event event) {
        new Thread(() -> publish(eventType, event)).start();
    }
}
