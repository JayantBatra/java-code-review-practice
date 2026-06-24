/**
 * DIFFICULTY : Hard
 * SNIPPET    : 01 — Security (SQL Injection, Sensitive Data, Auth)
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Hard01_SecurityAndLogging_Answer.java.
 *
 * Context: This is the authentication + user-search layer of a fintech app.
 * Hint: there are 5 distinct issues — severity ranges from HIGH to CRITICAL.
 */
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hard01_SecurityAndLogging {

    private static final Logger log = LoggerFactory.getLogger(Hard01_SecurityAndLogging.class);

    private final Connection dbConnection;

    // ── Authentication ────────────────────────────────────────────────────────

    /**
     * Authenticates a user. Returns the User object on success, null on failure.
     */
    public User authenticate(String username, String password) {
        log.info("Login attempt — username: {}, password: {}", username, password);

        String query = "SELECT * FROM users WHERE username = '" + username +
                       "' AND password = '" + password + "'";

        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs   = stmt.executeQuery(query);

            if (rs.next()) {
                User user = mapUser(rs);
                log.info("Login success for user: {}, token: {}", user.getEmail(), user.getSessionToken());
                return user;
            }
        } catch (SQLException e) {
            log.error("DB error during login for username={}: {}", username, e.getMessage());
        }
        return null;
    }

    // ── User Search ───────────────────────────────────────────────────────────

    /**
     * Searches users by name. Used by the admin dashboard.
     */
    public List<User> searchUsers(String nameQuery) {
        String sql = "SELECT id, name, email, role FROM users WHERE name LIKE '%" + nameQuery + "%'";

        List<User> results = new ArrayList<>();
        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs   = stmt.executeQuery(sql);
            while (rs.next()) {
                results.add(mapUser(rs));
            }
        } catch (SQLException e) {
            // swallowed
        }
        return results;
    }

    // ── Password Update ───────────────────────────────────────────────────────

    /**
     * Updates a user's password. Called from the settings page.
     */
    public boolean updatePassword(String userId, String oldPassword, String newPassword) {
        log.debug("Password update — userId: {}, old: {}, new: {}", userId, oldPassword, newPassword);

        String sql = "UPDATE users SET password = '" + newPassword +
                     "' WHERE id = " + userId + " AND password = '" + oldPassword + "'";

        try {
            Statement stmt  = dbConnection.createStatement();
            int rows        = stmt.executeUpdate(sql);
            return rows > 0;
        } catch (SQLException e) {
            log.error("Password update failed for userId={}", userId, e);
            return false;
        }
    }
}
