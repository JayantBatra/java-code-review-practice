/**
 * ANSWER FILE — Hard04_FileUploadService
 * ========================================
 * Do NOT read this before attempting the review yourself.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 1 — [CRITICAL] Path Traversal Vulnerability (Lines 29, 44)
 * ─────────────────────────────────────────────────────────
 * What  : filename is concatenated directly into the filesystem path without
 *         sanitization. An attacker supplies:
 *           filename = "../../etc/passwd"
 *         The resolved path becomes /var/app/uploads/user123/../../etc/passwd
 *         which normalizes to /etc/passwd — overwriting a system file.
 *         In download(), the same attack reads arbitrary files from disk.
 * Why   : Path Traversal (OWASP A01 — Broken Access Control) allows an attacker
 *         to read config files, private keys, and other users' uploads,
 *         or to overwrite system files causing full server compromise.
 * Fix   : Resolve and normalize the path, then assert it is still inside the
 *         upload directory:
 *           Path resolved = Paths.get(UPLOAD_DIR, userId, filename).normalize();
 *           if (!resolved.startsWith(UPLOAD_DIR)) throw new SecurityException();
 *
 * Severity: CRITICAL / BLOCKER
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 2 — Unclosed FileOutputStream on Exception Path (Lines 33-35)
 * ─────────────────────────────────────────────────────────
 * What  : If fos.write(content) throws (e.g. disk full, I/O error),
 *         fos.close() is never called. The file descriptor leaks.
 *         Under repeated failures this exhausts OS file descriptor limits.
 * Fix   : Wrap in try-with-resources — FileOutputStream implements AutoCloseable.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 3 — File Type Check Is Extension-Only — Bypassable (Lines 51-53)
 * ─────────────────────────────────────────────────────────
 * What  : The check reads only the filename extension. An attacker renames
 *         malware.exe to malware.jpg — isAllowedFile() returns true and the
 *         malware is stored on the server.
 * Why   : File extensions are user-controlled metadata. The actual content
 *         type is determined by the file's magic bytes (first 4-16 bytes).
 *         A JPEG always starts with FF D8 FF; a PDF always starts with %PDF.
 * Fix   : Read the first 8 bytes and compare against known magic byte signatures.
 *         Or use Apache Tika for robust MIME detection:
 *           new Tika().detect(content) // returns "image/jpeg", etc.
 *
 * Review comment:
 *   "isAllowedFile() checks only the filename extension. Renaming malware.exe
 *    to malware.jpg bypasses it. Check the file's magic bytes (first 4-8 bytes)
 *    to verify the actual content type."
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 4 — Entire File Loaded into Memory (Line 45)
 * ─────────────────────────────────────────────────────────
 * What  : Files.readAllBytes() reads the entire file into a single byte[].
 *         For a 2 GB upload this allocates 2 GB on the heap — instant
 *         OutOfMemoryError, and the JVM cannot GC it until the method returns.
 * Fix   : Stream the file in chunks using Files.newInputStream() and write
 *         incrementally to the HTTP response OutputStream.
 *
 * ─────────────────────────────────────────────────────────
 * ISSUE 5 — No File Size Validation (Lines 29, 33)
 * ─────────────────────────────────────────────────────────
 * What  : content.length is never checked before writing. An attacker uploads
 *         a 100 GB file to exhaust disk space — a denial-of-service vector.
 * Fix   : Check content.length against a configured MAX_UPLOAD_BYTES before
 *         proceeding: if (content.length > MAX_UPLOAD_BYTES) throw new FileTooLargeException()
 *
 * ─────────────────────────────────────────────────────────
 * FIXED CODE
 * ─────────────────────────────────────────────────────────
 */
import java.io.*;
import java.nio.file.*;

public class Hard04_FileUploadService_Answer {

    private static final String UPLOAD_DIR     = "/var/app/uploads/";
    private static final long   MAX_BYTES      = 10L * 1024 * 1024;  // 10 MB

    // Magic bytes for allowed types
    private static final byte[] JPEG_MAGIC = {(byte)0xFF, (byte)0xD8, (byte)0xFF};
    private static final byte[] PNG_MAGIC  = {(byte)0x89, 0x50, 0x4E, 0x47};
    private static final byte[] PDF_MAGIC  = {0x25, 0x50, 0x44, 0x46};   // %PDF

    public String upload(String filename, byte[] content, String userId) throws IOException {
        // Fix 5: size check before any I/O
        if (content.length > MAX_BYTES)
            throw new FileTooLargeException("Upload exceeds " + MAX_BYTES + " bytes");

        // Fix 1: resolve + normalize + assert still inside upload dir
        Path base     = Paths.get(UPLOAD_DIR, userId).normalize();
        Path resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base))
            throw new SecurityException("Invalid filename: " + filename);

        resolved.getParent().toFile().mkdirs();

        // Fix 2: try-with-resources — stream closed even on exception
        try (FileOutputStream fos = new FileOutputStream(resolved.toFile())) {
            fos.write(content);
        }

        log.info("User {} uploaded {} bytes", userId, content.length);  // no filename in log (PII)
        return resolved.toString();
    }

    // Fix 4: stream in chunks — never loads the full file into memory
    public void download(String filename, String userId, OutputStream out) throws IOException {
        Path base     = Paths.get(UPLOAD_DIR, userId).normalize();
        Path resolved = base.resolve(filename).normalize();
        if (!resolved.startsWith(base))
            throw new SecurityException("Invalid filename: " + filename);

        try (InputStream in = Files.newInputStream(resolved)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        }
    }

    // Fix 3: check magic bytes, not just extension
    public boolean isAllowedFile(String filename, byte[] content) {
        if (content == null || content.length < 4) return false;
        return startsWith(content, JPEG_MAGIC)
            || startsWith(content, PNG_MAGIC)
            || startsWith(content, PDF_MAGIC);
    }

    private boolean startsWith(byte[] data, byte[] magic) {
        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i]) return false;
        }
        return true;
    }
}
