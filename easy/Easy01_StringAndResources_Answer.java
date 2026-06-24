/**
 * ANSWER FILE — Easy01_StringAndResources
 * ========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Unclosed Resource (Lines 22-24)
 * ─────────────────────────────────────────────────────────
 * What  : FileInputStream and BufferedReader are opened but never closed.
 *         If reader.readLine() or anything after throws an exception, the
 *         streams are leaked — the file handle is never released.
 * Why   : Java's GC does not close I/O streams. Accumulated open handles
 *         eventually cause "Too many open files" OS errors in production.
 * Fix   : Wrap in try-with-resources — both streams are AutoCloseable.
 *
 * Review comment:
 *   "fis and reader are opened but there is no close() call in exception
 *    paths. If readLine() throws, the streams leak. Suggest wrapping in
 *    try-with-resources, which guarantees cleanup regardless of the outcome."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — String Compared with == Instead of .equals() (Lines 26 & 36)
 * ─────────────────────────────────────────────────────────
 * What  : Both `storedKey == inputKey` and `role == ROLE_ADMIN` use reference
 *         equality (same object in memory), not content equality.
 * Why   : String literals in the same class may be interned, making == work
 *         intermittently — but Strings from I/O, databases, or `new String()`
 *         are never interned. The bug is silent and environment-dependent.
 * Fix   : Use .equals() or equalsIgnoreCase(). Put the literal on the left
 *         to guard against NPE: `ROLE_ADMIN.equals(role)`.
 *
 * Review comment:
 *   "storedKey == inputKey compares references, not content. This works for
 *    interned literals but will silently fail for Strings from file I/O or
 *    DB queries. Use storedKey.equals(inputKey) or
 *    Objects.equals(storedKey, inputKey) for null-safety."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — String Concatenation in Loop O(n²) (Lines 44-46)
 * ─────────────────────────────────────────────────────────
 * What  : `summary = summary + id + ", "` inside a loop creates a brand-new
 *         String object on every iteration, copying all previous content.
 * Why   : For n order IDs the first copy is length 1, the second length 2…
 *         total work is O(n²). At 10,000 orders this becomes visibly slow.
 * Fix   : Use StringBuilder, or String.join() for this exact use case.
 *
 * Review comment:
 *   "String concatenation inside a loop is O(n²) — each += copies the entire
 *    string built so far. Suggest StringBuilder.append() or
 *    String.join(\", \", orderIds) which is both faster and clearer."
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
public class Easy01_StringAndResources_Answer {

    private static final String ROLE_ADMIN = "ADMIN";

    // Fix 1: try-with-resources closes both streams automatically
    public boolean isKeyValid(String filePath, String inputKey) throws Exception {
        try (FileInputStream fis = new FileInputStream(filePath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            String storedKey = reader.readLine();
            return Objects.equals(storedKey, inputKey);   // Fix 2: null-safe equals
        }
    }

    // Fix 2: literal on left → NPE-safe
    public boolean isAdmin(User user) {
        return ROLE_ADMIN.equals(user.getRole());
    }

    // Fix 3: String.join is O(n) and idiomatic
    public String buildOrderSummary(List<String> orderIds) {
        return String.join(", ", orderIds);
    }
}
