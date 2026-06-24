/**
 * DIFFICULTY : Hard
 * SNIPPET    : 03 — Memory Leak, Polymorphism & Mixed Design Issues
 *
 * Instructions
 * ------------
 * This is the hardest snippet. Multiple patterns interact.
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Hard03_MemoryLeakAndPolymorphism_Answer.java.
 *
 * Context: A reporting system that generates PDF/CSV/Excel reports and
 *          tracks active report jobs. Used in a long-running Spring Boot service.
 * Hint: there are 6 distinct issues spanning memory, design, and correctness.
 */
import java.util.*;

public class Hard03_MemoryLeakAndPolymorphism {

    // ── Report types ──────────────────────────────────────────────────────────

    static class ReportJob {
        private String   jobId;
        private String   format;     // "PDF", "CSV", "EXCEL"
        private String   status;
        private byte[]   output;
        private long     createdAt;

        // getters/setters omitted for brevity
    }

    // ── Report generator ──────────────────────────────────────────────────────

    static class ReportGenerator {

        private static final Map<String, ReportJob> activeJobs = new HashMap<>();
        private static final List<ReportListener>   listeners  = new ArrayList<>();

        public void addListener(ReportListener listener) {
            listeners.add(listener);
        }

        /**
         * Generates a report. format can be "PDF", "CSV", or "EXCEL".
         * New formats are planned (HTML, WORD).
         */
        public byte[] generate(ReportJob job) {
            activeJobs.put(job.getJobId(), job);

            byte[] output;
            if (job.getFormat().equals("PDF")) {
                output = generatePdf(job);
            } else if (job.getFormat().equals("CSV")) {
                output = generateCsv(job);
            } else if (job.getFormat().equals("EXCEL")) {
                output = generateExcel(job);
            } else {
                output = new byte[0];
            }

            job.setOutput(output);
            job.setStatus("COMPLETE");

            for (ReportListener l : listeners) {
                l.onComplete(job);
            }

            activeJobs.remove(job.getJobId());   // removed here
            return output;
        }

        /**
         * Returns a snapshot of currently running jobs for the admin dashboard.
         */
        public Map<String, ReportJob> getActiveJobs() {
            return activeJobs;           // returns the live map
        }

        /**
         * Returns the byte count of all completed outputs tracked internally.
         */
        public long getTotalOutputSize() {
            return activeJobs.values()
                             .stream()
                             .mapToLong(j -> j.getOutput().length)  // NPE risk
                             .sum();
        }

        private byte[] generatePdf(ReportJob job)   { return pdfRenderer.render(job); }
        private byte[] generateCsv(ReportJob job)   { return csvWriter.write(job); }
        private byte[] generateExcel(ReportJob job) { return excelWriter.write(job); }
    }

    // ── Event listener registry ───────────────────────────────────────────────

    interface ReportListener {
        void onComplete(ReportJob job);
        void onFailure(ReportJob job, Exception cause);
    }

    /**
     * Dashboard widget — subscribes to report events to update UI counters.
     */
    static class DashboardWidget implements ReportListener {
        private int completedCount = 0;

        public DashboardWidget(ReportGenerator generator) {
            generator.addListener(this);
            // No way to unregister
        }

        @Override public void onComplete(ReportJob job) { completedCount++; }
        @Override public void onFailure(ReportJob job, Exception cause) {}
    }
}
