/**
 * ANSWER FILE — Easy02_NullAndMagicNumbers
 * ==========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Unconsumed Nullable Return Value (Line 19)
 * ─────────────────────────────────────────────────────────
 * What  : findDisplayName() can return null (documented above), but the result
 *         is used directly: name.toUpperCase() — instant NPE when name is null.
 * Why   : NPEs are runtime errors; the compiler won't warn you. This will
 *         silently crash for any user without a display name set.
 * Fix   : Guard with a null check, or use Optional.
 *
 * Review comment:
 *   "findDisplayName() can return null, but the result is immediately
 *    dereferenced with .toUpperCase(). This throws NPE for any user without
 *    a display name. Suggest Optional.ofNullable(name).map(String::toUpperCase)
 *    .orElse(\"Anonymous\") or a null guard before the call."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Chained Method Calls With No Null Checks (Lines 29-32)
 * ─────────────────────────────────────────────────────────
 * What  : getUser(), getProfile(), getAddress(), and getCity() can each return
 *         null. Any single null in the chain causes an NPE — and the stack
 *         trace won't tell you WHICH step failed.
 * Why   : Each `.` is a potential NPE. Partial profile data is common (users
 *         may not fill address fields), so these nulls are expected at runtime.
 * Fix   : Use Optional.map() — it short-circuits on null and returns
 *         Optional.empty() instead of throwing.
 *
 * Review comment:
 *   "This chain has four consecutive calls without null checks. Any one of
 *    getUser(), getProfile(), getAddress(), getCity() can return null, and
 *    the NPE message won't identify which step failed. Suggest Optional.map()
 *    to flatten the chain safely."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Magic Number 5 (Line 39)
 * ─────────────────────────────────────────────────────────
 * What  : The literal 5 has no name — a reader cannot know this is the
 *         account-lockout threshold without reading surrounding context.
 * Why   : If the business rule changes to 3 or 10, you must hunt every
 *         occurrence. Magic numbers also make unit tests unreadable.
 * Fix   : Extract to a named constant.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Magic Number 1800000 (Line 46)
 * ─────────────────────────────────────────────────────────
 * What  : 1800000 milliseconds = 30 minutes, but no reader can tell that
 *         at a glance. This is a maintenance and readability risk.
 * Fix   : Use TimeUnit.MINUTES.toMillis(30) and assign to a named constant.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
public class Easy02_NullAndMagicNumbers_Answer {

    private static final int    MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final long   SESSION_TIMEOUT_MS        = TimeUnit.MINUTES.toMillis(30);

    // Fix 1: Optional handles the nullable return gracefully
    public String getDisplayName(User user) {
        return Optional.ofNullable(userRepository.findDisplayName(user.getId()))
                       .map(String::toUpperCase)
                       .orElse("Anonymous");
    }

    // Fix 2: Optional.map() chain — short-circuits on any null, no NPE
    public Optional<String> getUserCity(String userId) {
        return Optional.ofNullable(userService.getUser(userId))
                       .map(User::getProfile)
                       .map(Profile::getAddress)
                       .map(Address::getCity);
    }

    // Fix 3 & 4: named constants — self-documenting, single place to change
    public boolean shouldLockAccount(User user) {
        return user.getFailedAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS;
    }

    public boolean isSessionExpired(long lastActivityMs) {
        return System.currentTimeMillis() - lastActivityMs > SESSION_TIMEOUT_MS;
    }
}
