/**
 * DIFFICULTY : Easy
 * SNIPPET    : 02 — Null Handling & Magic Numbers
 *
 * Instructions
 * ------------
 * Review the code below. Find ALL issues, explain WHY each is a problem,
 * and describe the fix.
 * Try to spot every issue before looking at Easy02_NullAndMagicNumbers_Answer.java.
 *
 * Hint: there are 4 distinct issues hidden in this class.
 */
public class Easy02_NullAndMagicNumbers {

    /**
     * Returns the display name for a user.
     * Falls back to email prefix if the user has no display name set.
     */
    public String getDisplayName(User user) {
        String name = userRepository.findDisplayName(user.getId());  // can return null
        return name.toUpperCase();
    }

    /**
     * Fetches the city from a user's profile.
     * Profile and address fields may be partially filled.
     */
    public String getUserCity(String userId) {
        return userService.getUser(userId)
                          .getProfile()
                          .getAddress()
                          .getCity();
    }

    /**
     * Determines if an account should be locked after failed login attempts.
     */
    public boolean shouldLockAccount(User user) {
        return user.getFailedAttempts() >= 5;    // magic number
    }

    /**
     * Checks if the session has expired.
     */
    public boolean isSessionExpired(long lastActivityMs) {
        return System.currentTimeMillis() - lastActivityMs > 1800000;  // magic number
    }
}
