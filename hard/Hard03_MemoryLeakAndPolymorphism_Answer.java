/**
 * ANSWER FILE — Hard03_MemoryLeakAndPolymorphism
 * ================================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — Static HashMap Growing Unbounded (Memory Leak)
 * ─────────────────────────────────────────────────────────
 * What  : activeJobs is static. generate() adds a job, then removes it on
 *         success. But if generatePdf/Csv/Excel() throws an exception, the
 *         removal at the bottom never executes — the job stays in the map forever.
 *         In a long-running service generating thousands of reports, failed jobs
 *         accumulate and the map grows without bound.
 * Why   : Exception paths are the classic source of resource/memory leaks.
 *         The "happy path" cleanup at the end of a method is the #1 mistake.
 * Fix   : Use a try/finally block: put() before the try, remove() in finally.
 *         This guarantees cleanup regardless of success or failure.
 *
 * Review comment:
 *   "activeJobs.remove() is called after generate() completes, but if
 *    generatePdf/Csv/Excel() throws, the job is never removed from the static
 *    map — it leaks. In a long-running service this will grow unbounded.
 *    Suggest moving activeJobs.remove() to a finally block."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Static Listener List + No Deregister = Memory Leak
 * ─────────────────────────────────────────────────────────
 * What  : listeners is a static list. DashboardWidget registers itself in its
 *         constructor with no way to unregister. If a DashboardWidget is
 *         created per user session, every old widget is held alive by the
 *         static list — sessions that ended hours ago are never GC'd.
 * Why   : A static list holding instance references is one of the most common
 *         Java memory leaks. The GC cannot collect the widget because the
 *         static list still references it.
 * Fix   : Add a removeListener() method and call it when the widget is destroyed
 *         (e.g., in a @PreDestroy or close() method). Alternatively use
 *         WeakReference<ReportListener> in the list.
 *
 * Review comment:
 *   "DashboardWidget registers itself with the static listener list but there
 *    is no deregisterListener() method. If widgets are created per user session,
 *    expired sessions will never be GC'd — the static list holds them alive.
 *    Suggest adding removeListener() and calling it from the widget's cleanup
 *    lifecycle."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — getActiveJobs() Returns Live Internal Map
 * ─────────────────────────────────────────────────────────
 * What  : Returns `activeJobs` directly — the actual static map. A caller can
 *         clear() it, remove arbitrary jobs, or iterate it while another thread
 *         is modifying it (concurrent modification).
 * Why   : Encapsulation is broken. The map is also not thread-safe (HashMap),
 *         so concurrent iteration + modification = ConcurrentModificationException.
 * Fix   : Return Collections.unmodifiableMap(new HashMap<>(activeJobs)) —
 *         a snapshot copy wrapped as unmodifiable. Also switch to ConcurrentHashMap.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — NPE in getTotalOutputSize() (Logic Error)
 * ─────────────────────────────────────────────────────────
 * What  : j.getOutput() returns null for jobs that haven't completed yet
 *         (output is only set after generation). .length on null is NPE.
 *         Since activeJobs only contains IN-PROGRESS jobs (removed on
 *         completion), output will always be null here — the method always throws.
 * Why   : This reveals a design confusion: the method claims to measure
 *         "completed outputs" but queries a map of in-progress jobs.
 * Fix   : Either filter for non-null outputs, or maintain a separate completed
 *         jobs structure. Add a null guard: j.getOutput() != null.
 *
 * Review comment:
 *   "getTotalOutputSize() calls j.getOutput().length but output is null for
 *    in-progress jobs — this always throws NPE. activeJobs only contains
 *    running jobs (completed ones are removed), so the method can never
 *    return a meaningful result. Suggest a null guard and clarifying whether
 *    this should track completed or in-progress jobs."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — Polymorphism Opportunity: format type-checks
 * ─────────────────────────────────────────────────────────
 * What  : The if/else chain checking job.getFormat() will grow with every new
 *         format. HTML and WORD are already planned — this chain will get longer.
 *         The silent `return new byte[0]` for unknown formats hides misconfiguration.
 * Why   : Open/Closed Principle violation — same as all format/type-check chains.
 *         The silent empty-byte fallback is also dangerous: a misconfigured format
 *         produces an empty report with no error.
 * Fix   : Define a ReportFormat interface with a generate(ReportJob) method.
 *         Use a Map<String, ReportFormat> registry. Throw for unknown formats.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 6 — HashMap Not Thread-Safe for Static Shared State
 * ─────────────────────────────────────────────────────────
 * What  : Both activeJobs and listeners are static and mutated from concurrent
 *         threads (one per report request). HashMap.put() during rehashing +
 *         concurrent access = corrupted internal state or infinite loop.
 * Fix   : Use ConcurrentHashMap for activeJobs and
 *         CopyOnWriteArrayList for listeners.
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE (structure)
 * ─────────────────────────────────────────────────────────
 */
import java.util.*;
import java.util.concurrent.*;

public class Hard03_MemoryLeakAndPolymorphism_Answer {

    // Fix 5: interface replaces format type-checks
    interface ReportFormat {
        byte[] generate(ReportJob job);
    }

    static class ReportGenerator {

        // Fix 6: thread-safe collections
        private static final Map<String, ReportJob>    activeJobs = new ConcurrentHashMap<>();
        private static final List<ReportListener>      listeners  = new CopyOnWriteArrayList<>();

        // Fix 5: registry — add new formats without changing generate()
        private static final Map<String, ReportFormat> formats = Map.of(
            "PDF",   job -> pdfRenderer.render(job),
            "CSV",   job -> csvWriter.write(job),
            "EXCEL", job -> excelWriter.write(job)
        );

        public void addListener(ReportListener l)    { listeners.add(l); }
        public void removeListener(ReportListener l) { listeners.remove(l); }   // Fix 2

        public byte[] generate(ReportJob job) {
            activeJobs.put(job.getJobId(), job);
            try {                                               // Fix 1: finally guarantees cleanup
                ReportFormat fmt = formats.get(job.getFormat());
                if (fmt == null) throw new IllegalArgumentException("Unknown format: " + job.getFormat());

                byte[] output = fmt.generate(job);
                job.setOutput(output);
                job.setStatus("COMPLETE");
                listeners.forEach(l -> l.onComplete(job));
                return output;

            } finally {
                activeJobs.remove(job.getJobId());              // Fix 1: always runs
            }
        }

        // Fix 3: snapshot + unmodifiable — no live reference exposed
        public Map<String, ReportJob> getActiveJobs() {
            return Collections.unmodifiableMap(new HashMap<>(activeJobs));
        }

        // Fix 4: guard against null output; activeJobs only has in-progress jobs
        public long getTotalOutputSize() {
            return activeJobs.values().stream()
                             .filter(j -> j.getOutput() != null)
                             .mapToLong(j -> j.getOutput().length)
                             .sum();
        }
    }

    static class DashboardWidget implements ReportListener, AutoCloseable {
        private final ReportGenerator generator;
        private int completedCount = 0;

        public DashboardWidget(ReportGenerator gen) {
            this.generator = gen;
            gen.addListener(this);
        }

        @Override public void onComplete(ReportJob job) { completedCount++; }
        @Override public void onFailure(ReportJob job, Exception cause) {}

        // Fix 2: deregister when widget is closed — called by Spring @PreDestroy
        @Override public void close() { generator.removeListener(this); }
    }
}
