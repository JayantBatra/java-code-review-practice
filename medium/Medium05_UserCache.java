/**
 * DIFFICULTY : Medium
 * SNIPPET    : 05 — User Cache Layer
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Medium05_UserCache_Answer.java.
 *
 * Context: Used in a REST API service. Multiple request threads call get()
 *          and put() concurrently. evictStale() is called by a background job.
 * Hint: there are 4 distinct issues.
 */
import java.util.*;

public class Medium05_UserCache {

    private static final Map<String, User> cache       = new HashMap<>();
    private static final Map<String, Long> accessTimes = new HashMap<>();

    public static void put(String userId, User user) {
        cache.put(userId, user);
        accessTimes.put(userId, System.currentTimeMillis());
    }

    public static User get(String userId) {
        if (cache.containsKey(userId)) {
            accessTimes.put(userId, System.currentTimeMillis());
            return cache.get(userId);          // two operations — not atomic
        }
        return null;
    }

    /**
     * Removes cache entries not accessed within maxAgeMs.
     */
    public static void evictStale(long maxAgeMs) {
        for (String key : cache.keySet()) {
            Long t = accessTimes.get(key);
            if (t != null && System.currentTimeMillis() - t > maxAgeMs) {
                cache.remove(key);             // modifying map while iterating
            }
        }
    }

    public static int size() {
        return cache.size();
    }
}
