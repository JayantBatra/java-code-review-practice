/**
 * ANSWER FILE — Hard01_SecurityAndLogging
 * =========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — SQL Injection in authenticate() [CRITICAL]
 * ─────────────────────────────────────────────────────────
 * What  : The query is built by concatenating username and password directly
 *         into the SQL string. An attacker can supply:
 *           username = admin' --
 *         Making the query:  WHERE username = 'admin' --' AND password = '...'
 *         The password check is commented out — attacker logs in as any user.
 *         With username = '; DROP TABLE users; -- they can destroy the DB.
 * Why   : String-concatenated SQL is OWASP #1. It allows arbitrary SQL to be
 *         injected into the query, bypassing authentication and enabling data
 *         exfiltration or destruction.
 * Fix   : Use PreparedStatement with parameterized placeholders. The driver
 *         treats parameters as data, never as SQL syntax — injection is impossible.
 *
 * Severity: CRITICAL / BLOCKER
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Password Logged in Plaintext [CRITICAL]
 * ─────────────────────────────────────────────────────────
 * What  : Line: log.info("Login attempt — username: {}, password: {}", username, password)
 *         The raw password is written to the application log.
 * Why   : Log files are stored on disk, shipped to aggregators (Splunk, ELK),
 *         and often accessible to ops/infra teams. Plaintext passwords in logs
 *         are a GDPR violation, a PCI-DSS violation, and a direct credential
 *         leak if logs are compromised.
 * Fix   : Never log passwords, tokens, CVVs, or SSNs. Log only the username
 *         and a correlation ID.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Session Token Logged After Login [HIGH]
 * ─────────────────────────────────────────────────────────
 * What  : Line: log.info("Login success for user: {}, token: {}", user.getEmail(), user.getSessionToken())
 *         The session token is written to the log.
 * Why   : A session token is equivalent to a password for the duration of the
 *         session. Logging it means anyone with log access can impersonate users.
 *         This is a session hijacking risk and likely a GDPR violation.
 * Fix   : Log only the user ID or a masked token prefix. Never the full token.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — SQL Injection in searchUsers() [CRITICAL]
 * ─────────────────────────────────────────────────────────
 * What  : nameQuery is concatenated into a LIKE clause with no sanitization.
 *           nameQuery = "' UNION SELECT id,username,password,role FROM users --"
 *         This UNION injection returns all usernames and passwords.
 * Why   : Same root cause as Issue 1 — string-concatenated SQL.
 * Fix   : Use PreparedStatement with ? for the LIKE value, passing "%" + nameQuery + "%"
 *         as the parameter.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — Passwords Stored/Compared in Plaintext + Swallowed Exception
 * ─────────────────────────────────────────────────────────
 * What  : (a) The password column stores and compares plaintext — no hashing.
 *             Anyone with DB read access sees all user passwords.
 *         (b) searchUsers() swallows the SQLException silently — a DB failure
 *             returns an empty list with no log, making incidents invisible.
 * Why   : Plaintext passwords are a catastrophic breach risk. Every password
 *         dump is immediately usable. Passwords must be hashed with a slow
 *         algorithm (BCrypt, Argon2).
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.sql.*;
import org.mindrot.jbcrypt.BCrypt;

public class Hard01_SecurityAndLogging_Answer {

    private static final Logger log = LoggerFactory.getLogger(Hard01_SecurityAndLogging_Answer.class);
    private final Connection dbConnection;

    // Fix 1 + 2 + 3: parameterized query; log only username, no password/token
    public User authenticate(String username, String password) {
        log.info("Login attempt — username: {}", username);   // no password in log

        // Fix 5a: compare against BCrypt hash — never store/compare plaintext
        String sql = "SELECT * FROM users WHERE username = ?";

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                User user = mapUser(rs);
                String storedHash = rs.getString("password_hash");

                if (BCrypt.checkpw(password, storedHash)) {
                    // Fix 3: log only user ID, never the session token
                    log.info("Login success for userId={}", user.getId());
                    return user;
                }
            }
        } catch (SQLException e) {
            log.error("DB error during login for username={}", username, e);
        }
        return null;
    }

    // Fix 4: PreparedStatement for LIKE — parameter is data, never SQL
    public List<User> searchUsers(String nameQuery) {
        String sql = "SELECT id, name, email, role FROM users WHERE name LIKE ?";
        List<User> results = new ArrayList<>();

        try (PreparedStatement stmt = dbConnection.prepareStatement(sql)) {
            stmt.setString(1, "%" + nameQuery + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) results.add(mapUser(rs));
        } catch (SQLException e) {
            // Fix 5b: log the failure — don't swallow it
            log.error("searchUsers failed for query={}", nameQuery, e);
            throw new UserSearchException("User search failed", e);
        }
        return results;
    }

    // Fix 1 + 2 + 5a: parameterized; no passwords in logs; hash before storing
    public boolean updatePassword(String userId, String oldPassword, String newPassword) {
        log.debug("Password update requested — userId={}", userId);  // no passwords

        String selectSql = "SELECT password_hash FROM users WHERE id = ?";
        String updateSql = "UPDATE users SET password_hash = ? WHERE id = ?";

        try (PreparedStatement sel = dbConnection.prepareStatement(selectSql)) {
            sel.setString(1, userId);
            ResultSet rs = sel.executeQuery();
            if (!rs.next()) return false;

            String storedHash = rs.getString("password_hash");
            if (!BCrypt.checkpw(oldPassword, storedHash)) return false;   // verify old pw

            try (PreparedStatement upd = dbConnection.prepareStatement(updateSql)) {
                upd.setString(1, BCrypt.hashpw(newPassword, BCrypt.gensalt(12)));
                upd.setString(2, userId);
                return upd.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            log.error("Password update failed for userId={}", userId, e);
            return false;
        }
    }
}
