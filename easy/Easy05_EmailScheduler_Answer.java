/**
 * ANSWER FILE — Easy05_EmailScheduler
 * =====================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — ConcurrentModificationException: Removing While Iterating (Lines 31-33)
 * ─────────────────────────────────────────────────────────
 * What  : pendingEmails.remove(email) is called inside a for-each loop over
 *         pendingEmails. ArrayList's iterator detects the structural modification
 *         and throws ConcurrentModificationException on the very next iteration.
 * Why   : The enhanced for-each loop uses an iterator internally. Any structural
 *         modification to the list (add/remove) while the iterator is active
 *         immediately invalidates it.
 * Fix A : Use Iterator explicitly: while (it.hasNext()) { if sent → it.remove(); }
 * Fix B : Collect sent emails and remove after the loop:
 *         List<Email> sent = new ArrayList<>(); ... sent.add(email); ...
 *         pendingEmails.removeAll(sent);
 * Fix C : Use removeIf(): pendingEmails.removeIf(e -> tryToSend(e));
 *
 * Review comment:
 *   "pendingEmails.remove() inside a for-each loop throws
 *    ConcurrentModificationException — ArrayList's iterator detects the
 *    structural modification. Use an explicit Iterator and call iterator.remove(),
 *    or collect removals after the loop."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Non-final "Constant" MAX_RETRIES (Line 18)
 * ─────────────────────────────────────────────────────────
 * What  : MAX_RETRIES follows the ALL_CAPS naming convention for constants,
 *         but it is not declared final — any code can reassign it at runtime.
 * Why   : The convention ALL_CAPS signals "this will never change". Violating
 *         that contract confuses readers and creates a subtle bug surface —
 *         a caller or subclass can silently change the retry threshold.
 * Fix   : private static final int MAX_RETRIES = 3;
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — String Concatenation in Loop — O(n²) (Lines 42-44)
 * ─────────────────────────────────────────────────────────
 * What  : log += ... inside the loop creates a new String on every iteration,
 *         copying all prior content. O(n²) total work for large address lists.
 * Fix   : Use StringBuilder or String.join("\n", addresses).
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — System.out Instead of a Proper Logger (Line 45)
 * ─────────────────────────────────────────────────────────
 * What  : System.out.println() writes to stdout with no log level, no timestamp,
 *         no class/method context, and no ability to be filtered or shipped to
 *         a log aggregator (Splunk, ELK, CloudWatch).
 * Why   : In most production environments stdout is either discarded or not
 *         monitored. Log output from SLF4J/Log4j flows to the configured
 *         appenders and is visible to operators.
 * Fix   : Use log.info("Bulk send complete: {} addresses", addresses.size());
 *         Log the count, not every address — PII concern.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Easy05_EmailScheduler_Answer {

    private static final Logger log = LoggerFactory.getLogger(Easy05_EmailScheduler_Answer.class);

    private final List<Email> pendingEmails = new ArrayList<>();
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;    // Fix 2: final enforces immutability

    public void schedule(Email email) {
        if (email != null) pendingEmails.add(email);
    }

    // Fix 1: collect successes, remove after loop — no ConcurrentModificationException
    public void processPending() {
        List<Email> sent = new ArrayList<>();
        for (Email email : pendingEmails) {
            try {
                emailClient.send(email);
                sent.add(email);
            } catch (EmailException e) {
                retryCount++;
                log.warn("Failed to send email to {}: {}", email.getTo(), e.getMessage());
            }
        }
        pendingEmails.removeAll(sent);
    }

    // Fix 3: StringBuilder; Fix 4: proper logger, no PII in log
    public void sendBulk(List<String> addresses, String subject, String body) {
        for (String addr : addresses) {
            emailClient.send(new Email(addr, subject, body));
        }
        log.info("Bulk send complete: {} addresses", addresses.size());
    }
}
