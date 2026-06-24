/**
 * ANSWER FILE — Medium01_EqualsHashCodeAndImmutability
 * ======================================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — hashCode() Not Overridden in TradeKey (TradeKey class)
 * ─────────────────────────────────────────────────────────
 * What  : equals() is overridden to compare symbol + tradeDate, but hashCode()
 *         is NOT overridden — it inherits Object's identity-based hash.
 * Why   : HashMap first finds the bucket using hashCode(), then confirms with
 *         equals(). k1 and k2 have the same content but different identity
 *         hashes, so they land in DIFFERENT buckets. equals() is never even
 *         called. Result: isCached(k2) returns false, remove(k2) removes
 *         nothing — the map is broken for all lookups and evictions.
 *         This also violates the Java contract: equal objects MUST have
 *         equal hash codes.
 * Fix   : Override hashCode() using the same fields as equals().
 *
 * Review comment:
 *   "TradeKey overrides equals() but not hashCode(). HashMap uses hashCode()
 *    first to locate the bucket — two logically-equal TradeKeys will have
 *    different identity hashes and land in different buckets, so
 *    containsKey() and remove() will always miss. Override hashCode() with
 *    the same fields used in equals()."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — getAuditLog() Returns Direct Internal Reference (Line 52)
 * ─────────────────────────────────────────────────────────
 * What  : The method returns `auditLog` itself — the actual internal list.
 *         Any caller can clear(), add(), or remove() entries without going
 *         through any method on TradeCache.
 * Why   : Encapsulation is broken. The caller in main() calls
 *         cache.getAuditLog().clear() and silently wipes the audit trail —
 *         no validation, no event, no log. In auditing/compliance contexts
 *         this is a critical bug.
 * Fix   : Return Collections.unmodifiableList(auditLog) or List.copyOf(auditLog).
 *
 * Review comment:
 *   "getAuditLog() returns a direct reference to the internal list. Callers
 *    can clear or modify the audit log without going through any TradeCache
 *    method, breaking encapsulation. Suggest returning
 *    Collections.unmodifiableList(auditLog) so the list is read-only to
 *    callers."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — isCached() and removePrice() Are Also Broken (Same Root Cause)
 * ─────────────────────────────────────────────────────────
 * What  : Both rely on HashMap's hashCode-based lookup. Without a correct
 *         hashCode(), containsKey(k2) and remove(k2) silently do nothing for
 *         a key that is logically equal to a stored key. This is the same
 *         root cause as Issue 1 but worth calling out separately in a review
 *         to show the full blast radius.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — HashMap Is Not Thread-Safe (Medium-level awareness)
 * ─────────────────────────────────────────────────────────
 * What  : TradeCache is used as a shared cache (implied by the name), but
 *         HashMap is not thread-safe. Concurrent put() calls can corrupt the
 *         internal linked-list structure during rehashing, causing infinite
 *         loops that peg CPU at 100%.
 * Fix   : Use ConcurrentHashMap.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Medium01_EqualsHashCodeAndImmutability_Answer {

    static class TradeKey {
        private final String symbol;
        private final String tradeDate;

        public TradeKey(String symbol, String tradeDate) {
            this.symbol    = symbol;
            this.tradeDate = tradeDate;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TradeKey)) return false;
            TradeKey other = (TradeKey) o;
            return Objects.equals(symbol, other.symbol) &&
                   Objects.equals(tradeDate, other.tradeDate);
        }

        // Fix 1: always override hashCode() with the same fields as equals()
        @Override
        public int hashCode() {
            return Objects.hash(symbol, tradeDate);
        }
    }

    static class TradeCache {

        // Fix 4: thread-safe map
        private final Map<TradeKey, Double> priceCache = new ConcurrentHashMap<>();
        private final List<String>          auditLog   = new ArrayList<>();

        public void cachePrice(TradeKey key, double price) {
            priceCache.put(key, price);
        }

        public boolean isCached(TradeKey key) {
            return priceCache.containsKey(key);   // now works correctly
        }

        public void addAuditEntry(String entry) {
            auditLog.add(entry);
        }

        // Fix 2: unmodifiable view — callers cannot mutate the internal list
        public List<String> getAuditLog() {
            return Collections.unmodifiableList(auditLog);
        }

        public void removePrice(TradeKey key) {
            priceCache.remove(key);               // now works correctly
        }
    }
}
