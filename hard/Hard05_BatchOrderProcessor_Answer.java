/**
 * ANSWER FILE — Hard05_BatchOrderProcessor
 * ==========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — [CRITICAL] SQL Injection in processBatch() (Lines 28-29)
 * ─────────────────────────────────────────────────────────
 * What  : order.getStatus() and order.getId() are concatenated directly into
 *         the SQL string. A crafted status like "PENDING'; DROP TABLE orders;--"
 *         executes arbitrary SQL, destroying the orders table.
 * Why   : String-concatenated SQL is OWASP #1. In a batch processor running
 *         against millions of rows the damage is total and irreversible.
 * Fix   : Use PreparedStatement:
 *           PreparedStatement stmt = conn.prepareStatement(
 *               "UPDATE orders SET status = ? WHERE id = ?");
 *           stmt.setString(1, order.getStatus());
 *           stmt.setLong(2, order.getId());
 *           stmt.executeUpdate();
 *
 * Severity: CRITICAL / BLOCKER
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Exception Swallowed — Root Cause Permanently Lost (Lines 32-33)
 * ─────────────────────────────────────────────────────────
 * What  : The catch block increments errorCount but discards the exception.
 *         Operators see "142 errors" with zero information about why —
 *         no SQL state, no order ID, no stack trace.
 * Why   : Debugging a batch that processed 10 million rows with 142 silent
 *         failures is nearly impossible without any logged context.
 * Fix   : log.error("Failed to process orderId={}", order.getId(), e);
 *         This captures the order ID, SQL error message, and full stack trace.
 *
 * Review comment:
 *   "The catch block silently increments errorCount. The root cause is
 *    permanently lost — no SQL state, no order ID, no stack trace. Add
 *    log.error with the order ID and the exception to make failures diagnosable."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — Non-Atomic Counters Shared Across Parallel Threads (Lines 20-21)
 * ─────────────────────────────────────────────────────────
 * What  : processedCount++ and errorCount++ are called from parallelStream()
 *         worker threads simultaneously. The ++ operator is read-modify-write
 *         across three bytecode instructions — not atomic. Under concurrent
 *         access, increments are silently lost and the report is wrong.
 * Fix   : Use AtomicInteger and incrementAndGet().
 *
 * Review comment:
 *   "processedCount++ is not atomic. parallelStream() calls processBatch()
 *    from multiple threads — under concurrent access, increments are silently
 *    lost. Use AtomicInteger."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — JDBC Connection Shared Across Parallel Threads (Line 41)
 * ─────────────────────────────────────────────────────────
 * What  : A single Connection is passed to parallelStream() which distributes
 *         batches across multiple threads. JDBC Connection is not thread-safe —
 *         concurrent Statement creation and execution on the same Connection
 *         produces undefined behaviour (statement state corruption, wrong
 *         results, or exceptions).
 * Why   : Connection objects maintain internal state (transaction, cursor
 *         position, auto-commit mode) that is not designed for concurrent access.
 * Fix   : Each batch should obtain its own Connection from a DataSource:
 *           batches.parallelStream().forEach(batch -> {
 *               try (Connection c = dataSource.getConnection()) {
 *                   processBatch(batch, c);
 *               }
 *           });
 *
 * Review comment:
 *   "A single JDBC Connection is shared across parallelStream() threads.
 *    Connection is not thread-safe — concurrent Statement operations cause
 *    undefined behaviour. Each thread needs its own connection from the pool."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — System.out for Validation Warning (Line 48)
 * ─────────────────────────────────────────────────────────
 * What  : System.out.println() is used for an error condition (negative total).
 *         In production, stdout is either discarded or not monitored.
 *         The warning is invisible to operators.
 * Why   : Beyond visibility, a negative total is likely a data integrity bug
 *         that should fail loudly (throw a ValidationException) rather than
 *         silently continue processing.
 * Fix   : Throw a ValidationException so the order is skipped with a logged
 *         error, or use log.warn() so the warning appears in monitoring.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import java.sql.*;
import java.util.concurrent.atomic.*;
import javax.sql.DataSource;

public class Hard05_BatchOrderProcessor_Answer {

    // Fix 3: atomic counters — safe under concurrent access
    private final AtomicInteger processedCount = new AtomicInteger(0);
    private final AtomicInteger errorCount     = new AtomicInteger(0);
    private final DataSource    dataSource;

    public Hard05_BatchOrderProcessor_Answer(DataSource ds) {
        this.dataSource = ds;
    }

    public void processBatch(List<Order> orders, Connection conn) throws SQLException {
        // Fix 1: PreparedStatement — parameterized, injection-proof
        String sql = "UPDATE orders SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Order order : orders) {
                try {
                    validateOrder(order);
                    stmt.setString(1, order.getStatus());
                    stmt.setLong(2, order.getId());
                    stmt.executeUpdate();
                    processedCount.incrementAndGet();
                } catch (Exception e) {
                    // Fix 2: log with context — root cause preserved
                    log.error("Failed to process orderId={}, status={}",
                              order.getId(), order.getStatus(), e);
                    errorCount.incrementAndGet();
                }
            }
        }
    }

    // Fix 4: each thread obtains its own connection from the pool
    public void processAllBatches(List<List<Order>> batches) {
        batches.parallelStream().forEach(batch -> {
            try (Connection conn = dataSource.getConnection()) {
                processBatch(batch, conn);
            } catch (SQLException e) {
                log.error("Failed to obtain connection for batch", e);
            }
        });
    }

    public Report generateReport() {
        return new Report(processedCount.get(), errorCount.get());
    }

    private void validateOrder(Order order) {
        // Fix 5: throw rather than silently print — fail fast with context
        if (order.getTotal() < 0)
            throw new ValidationException("Negative total for orderId=" + order.getId());
    }
}
