/**
 * ANSWER FILE — Easy03_ExceptionAndDuplication
 * ==============================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Catching Too Broadly + Returning null (Lines 20-23)
 * ─────────────────────────────────────────────────────────
 * What  : catch(Exception) swallows every possible throwable — including
 *         NullPointerException and programming errors — and silently returns
 *         null. The caller has no idea why loading failed.
 * Why   : Catching Exception hides bugs that should crash loudly. Returning
 *         null propagates the problem downstream and causes harder-to-diagnose
 *         NPEs at the call site. The root cause is permanently lost.
 * Fix   : Catch only the specific DB exception, log it with context, and
 *         rethrow as a domain exception. Let unexpected errors propagate.
 *
 * Review comment:
 *   "Catching Exception broadly swallows NPEs and programming bugs, returning
 *    null with no trace. The NPE will surface elsewhere and be very hard to
 *    diagnose. Suggest catching only DatabaseException, logging with the userId,
 *    and rethrowing as a domain exception."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Empty catch Block + Unclosed Resource (Lines 31-37)
 * ─────────────────────────────────────────────────────────
 * What  : (a) The catch block is completely empty — if write fails, the caller
 *         gets no indication. (b) If writer.write() throws, writer.close() is
 *         never called — the FileWriter leaks.
 * Why   : Silent failures in write operations can corrupt files or lose data
 *         with zero log evidence. The FileWriter leak accumulates file handles.
 * Fix   : Use try-with-resources (fixes the leak automatically) and log + rethrow
 *         in the catch block.
 *
 * Review comment:
 *   "Two issues here: the empty catch means a failed write is completely silent,
 *    and if writer.write() throws, writer.close() is never called — FileWriter
 *    leaks. Suggest try-with-resources to guarantee cleanup, and logging + rethrowing
 *    the IOException so callers know the save failed."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Email Validation Duplicated (Lines 44-46 and 55-57)
 * ─────────────────────────────────────────────────────────
 * What  : The email validation block appears in both registerUser() and
 *         updateEmail() — identical logic copy-pasted.
 * Why   : If the email rule changes (e.g. max length → 320, add regex), it
 *         must be updated in both places. Missing one is how bugs reach prod.
 *         This violates DRY (Don't Repeat Yourself).
 * Fix   : Extract into a private validateEmail() method called from both.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Password Validation Duplicated (Lines 48-50 and 62-64)
 * ─────────────────────────────────────────────────────────
 * What  : Same password validation logic duplicated in registerUser() and
 *         resetPassword().
 * Fix   : Extract into a private validatePassword() method.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
public class Easy03_ExceptionAndDuplication_Answer {

    // Fix 1: catch only what you can handle; rethrow as domain exception
    public User loadUser(String userId) {
        try {
            return userRepository.findById(userId);
        } catch (DatabaseException e) {
            log.error("Failed to load user id={}", userId, e);
            throw new UserServiceException("Could not load user: " + userId, e);
        }
    }

    // Fix 2: try-with-resources + log & rethrow
    public void saveReport(String content, String path) {
        try (FileWriter writer = new FileWriter(new File(path))) {
            writer.write(content);
        } catch (IOException e) {
            log.error("Failed to save report to path={}", path, e);
            throw new ReportException("Report save failed", e);
        }
    }

    // Fixes 3 & 4: single source of truth for validation logic
    public void registerUser(String email, String password, String username) {
        validateEmail(email);
        validatePassword(password);
        userRepository.create(email, password, username);
    }

    public void updateEmail(String userId, String newEmail) {
        validateEmail(newEmail);
        userRepository.updateEmail(userId, newEmail);
    }

    public void resetPassword(String userId, String newPassword) {
        validatePassword(newPassword);
        userRepository.updatePassword(userId, newPassword);
    }

    private void validateEmail(String email) {
        if (email == null || !email.contains("@") || email.length() > 255) {
            throw new ValidationException("Invalid email: " + email);
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters");
        }
    }
}
