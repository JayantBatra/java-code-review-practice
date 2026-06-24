/**
 * DIFFICULTY : Hard
 * SNIPPET    : 05 — Order Batch Processor
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Hard05_BatchOrderProcessor_Answer.java.
 *
 * Context: A nightly job processes millions of orders in parallel batches.
 *          Each batch runs in its own thread. A Report is generated after
 *          all batches complete.
 * Hint: there are 5 distinct issues — some interact with each other.
 */
import java.util.*;
import java.sql.*;

public class Hard05_BatchOrderProcessor {

    private int processedCount = 0;
    private int errorCount     = 0;

    /**
     * Processes a single batch of orders. Each order status is updated in the DB.
     */
    public void processBatch(List<Order> orders, Connection conn) {
        for (Order order : orders) {
            try {
                String sql = "UPDATE orders SET status = '" + order.getStatus()
                           + "' WHERE id = " + order.getId();
                conn.createStatement().executeUpdate(sql);
                processedCount++;
            } catch (Exception e) {
                errorCount++;
            }
        }
    }

    /**
     * Runs all batches in parallel on the common fork-join pool.
     */
    public void processAllBatches(List<List<Order>> batches, Connection conn) {
        batches.parallelStream().forEach(batch -> processBatch(batch, conn));
    }

    public Report generateReport() {
        return new Report(processedCount, errorCount);
    }

    private void validateOrder(Order order) {
        if (order.getTotal() < 0) {
            System.out.println("Warning: negative total for order " + order.getId());
        }
    }
}
