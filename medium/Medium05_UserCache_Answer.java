/**
 * ANSWER FILE — Medium05_UserCache
 * ==================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Static HashMaps Not Thread-Safe
 * ─────────────────────────────────────────────────────────
 * What  : Both cache and accessTimes are static HashMaps mutated from multiple
 *         request threads. Concurrent put() calls during rehashing corrupt the
 *         internal linked-list structure — the JVM enters an infinite loop
 *         traversing a circular chain, pinning a CPU core at 100%.
 * Why   : This is one of the most notorious Java production bugs. It is
 *         nearly impossible to reproduce locally but reliably triggers under
 *         load in production.
 * Fix   : Use ConcurrentHashMap for both maps.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — TOCTOU Race: containsKey() + get() Are Two Operations
 * ─────────────────────────────────────────────────────────
 * What  : Between containsKey() (returns true) and cache.get() (line 30),
 *         another thread can evict the entry. get() then returns null — a
 *         value the caller did not expect after the containsKey() check passed.
 * Why   : The check and the read are two separate operations with no atomicity
 *         guarantee. This is the classic Time-Of-Check-Time-Of-Use race.
 * Fix   : Use a single cache.get(userId) call and null-check the result.
 *         ConcurrentHashMap.get() is atomic.
 *
 * Review comment:
 *   "containsKey() + get() is a TOCTOU race — another thread can evict the
 *    entry between the two calls. Use a single cache.get(userId) and null-check
 *    the result."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — ConcurrentModificationException in evictStale()
 * ─────────────────────────────────────────────────────────
 * What  : cache.remove(key) inside a cache.keySet() for-each throws
 *         ConcurrentModificationException — the HashMap iterator detects
 *         the structural modification and fails immediately.
 * Fix   : Collect keys to evict first, then call cache.keySet().removeAll().
 *         Or use ConcurrentHashMap.entrySet().removeIf() which is atomic.
 *
 * Review comment:
 *   "cache.remove(key) inside a keySet() for-each throws
 *    ConcurrentModificationException. Collect stale keys in a separate list
 *    and call removeAll() after the loop, or use entrySet().removeIf()."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Cache Is Unbounded — No Maximum Size or Automatic Eviction
 * ─────────────────────────────────────────────────────────
 * What  : There is no cap on how many entries the cache can hold. evictStale()
 *         must be called explicitly — if the caller forgets or the scheduler
 *         stops, the cache grows until OutOfMemoryError.
 * Why   : In a busy service with millions of unique user IDs, an unbounded
 *         cache consumes all available heap. Caches must always have a size
 *         limit and a TTL.
 * Fix   : Use Caffeine: CacheBuilder.newBuilder().maximumSize(10_000)
 *         .expireAfterAccess(30, TimeUnit.MINUTES).build()
 *         This handles eviction automatically and is fully thread-safe.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import java.util.concurrent.*;
import com.github.benmanes.caffeine.cache.*;

public class Medium05_UserCache_Answer {

    // Fix 1, 2, 3, 4 — Caffeine replaces all four custom implementations
    private static final Cache<String, User> cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build();

    public static void put(String userId, User user) {
        cache.put(userId, user);
    }

    // Fix 2: single atomic get() — null-safe, no TOCTOU
    public static User get(String userId) {
        return cache.getIfPresent(userId);   // returns null if absent — no race possible
    }

    // Fix 3 & 4: Caffeine handles eviction automatically — no manual evictStale() needed

    public static long size() {
        return cache.estimatedSize();
    }
}

// ── Alternative if Caffeine is unavailable: manual ConcurrentHashMap fix ──

class Medium05_UserCache_ManualFix {

    // Fix 1: thread-safe maps
    private static final ConcurrentHashMap<String, User> cache       = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Long> accessTimes = new ConcurrentHashMap<>();

    public static void put(String userId, User user) {
        cache.put(userId, user);
        accessTimes.put(userId, System.currentTimeMillis());
    }

    // Fix 2: single atomic get()
    public static User get(String userId) {
        User u = cache.get(userId);
        if (u != null) accessTimes.put(userId, System.currentTimeMillis());
        return u;
    }

    // Fix 3: removeIf() is atomic on ConcurrentHashMap
    public static void evictStale(long maxAgeMs) {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(e -> {
            Long t = accessTimes.get(e.getKey());
            return t != null && now - t > maxAgeMs;
        });
    }
}
