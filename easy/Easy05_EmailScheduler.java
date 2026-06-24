/**
 * DIFFICULTY : Easy
 * SNIPPET    : 05 — Email Scheduling Service
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Easy05_EmailScheduler_Answer.java.
 *
 * Hint: there are 4 distinct issues hidden in this class.
 */
import java.util.*;

public class Easy05_EmailScheduler {

    private List<Email> pendingEmails = new ArrayList<>();
    private int retryCount = 0;
    private int MAX_RETRIES = 3;           // should this be mutable?

    public void schedule(Email email) {
        if (email != null) {
            pendingEmails.add(email);
        }
    }

    /**
     * Sends all pending emails and removes successfully sent ones.
     */
    public void processPending() {
        for (Email email : pendingEmails) {
            try {
                emailClient.send(email);
                pendingEmails.remove(email);   // modifying list during iteration
            } catch (EmailException e) {
                retryCount++;
            }
        }
    }

    /**
     * Sends a bulk email to a list of addresses and prints the log.
     */
    public void sendBulk(List<String> addresses, String subject, String body) {
        String log = "";
        for (String addr : addresses) {
            log = log + "Sent to: " + addr + "\n";
        }
        System.out.println(log);
    }
}
