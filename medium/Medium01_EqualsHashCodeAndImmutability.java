/**
 * DIFFICULTY : Medium
 * SNIPPET    : 01 — equals()/hashCode() Contract & Immutability
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Medium01_EqualsHashCodeAndImmutability_Answer.java.
 *
 * Hint: there are 4 distinct issues. Some are subtle.
 */
import java.util.*;

public class Medium01_EqualsHashCodeAndImmutability {

    // ── Domain class ──────────────────────────────────────────────────────────

    static class TradeKey {
        private String symbol;
        private String tradeDate;

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

        // hashCode() is NOT overridden
    }

    // ── Service using TradeKey ────────────────────────────────────────────────

    static class TradeCache {

        private final Map<TradeKey, Double> priceCache = new HashMap<>();
        private final List<String>          auditLog   = new ArrayList<>();

        public void cachePrice(TradeKey key, double price) {
            priceCache.put(key, price);
        }

        public boolean isCached(TradeKey key) {
            return priceCache.containsKey(key);  // issue here
        }

        public void addAuditEntry(String entry) {
            auditLog.add(entry);
        }

        /** Returns the full audit log for reporting. */
        public List<String> getAuditLog() {
            return auditLog;                      // issue here
        }

        public void removePrice(TradeKey key) {
            priceCache.remove(key);               // issue here (same root cause as isCached)
        }
    }

    // ── Caller ───────────────────────────────────────────────────────────────

    public static void main(String[] args) {
        TradeCache cache = new TradeCache();

        TradeKey k1 = new TradeKey("AAPL", "2024-01-15");
        cache.cachePrice(k1, 189.50);

        TradeKey k2 = new TradeKey("AAPL", "2024-01-15");  // logically equal to k1
        System.out.println(cache.isCached(k2));             // prints false — unexpected!

        // Caller mutates the audit log directly
        cache.getAuditLog().clear();                        // wipes the log undetected
    }
}
