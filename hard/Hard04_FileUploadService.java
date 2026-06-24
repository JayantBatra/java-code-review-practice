/**
 * DIFFICULTY : Hard
 * SNIPPET    : 04 — File Upload Service
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Hard04_FileUploadService_Answer.java.
 *
 * Context: A REST API endpoint accepts file uploads from end-users and stores
 *          them on the server filesystem. Downloads are served back on request.
 * Hint: there are 5 distinct issues — several are security vulnerabilities.
 */
import java.io.*;
import java.nio.file.*;

public class Hard04_FileUploadService {

    private static final String UPLOAD_DIR = "/var/app/uploads/";

    /**
     * Stores an uploaded file to disk.
     *
     * @param filename  the original filename from the user
     * @param content   the raw file bytes
     * @param userId    the uploader's ID
     * @return the path where the file was stored
     */
    public String upload(String filename, byte[] content, String userId) throws IOException {
        String destPath = UPLOAD_DIR + userId + "/" + filename;    // filename from user

        File dest = new File(destPath);
        dest.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(dest);
        fos.write(content);
        fos.close();

        log.info("User {} uploaded {} ({} bytes)", userId, filename, content.length);
        return destPath;
    }

    /**
     * Returns the bytes of a previously uploaded file.
     */
    public byte[] download(String filename, String userId) throws IOException {
        String filePath = UPLOAD_DIR + userId + "/" + filename;    // filename from user
        return Files.readAllBytes(Paths.get(filePath));
    }

    /**
     * Returns true if the file type is permitted.
     */
    public boolean isAllowedFile(String filename, byte[] content) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1);
        return ext.equals("jpg") || ext.equals("png") || ext.equals("pdf");
    }
}
