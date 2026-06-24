/**
 * DIFFICULTY : Easy
 * SNIPPET    : 03 — Exception Handling & Code Duplication
 *
 * Instructions
 * ------------
 * Review the code below. Find ALL issues, explain WHY each is a problem,
 * and describe the fix.
 * Try to spot every issue before looking at Easy03_ExceptionAndDuplication_Answer.java.
 *
 * Hint: there are 4 distinct issues hidden in this class.
 */
public class Easy03_ExceptionAndDuplication {

    /**
     * Loads a user by ID. Returns null if anything goes wrong.
     */
    public User loadUser(String userId) {
        try {
            return userRepository.findById(userId);
        } catch (Exception e) {
            // something went wrong
            return null;
        }
    }

    /**
     * Saves a report to disk.
     */
    public void saveReport(String content, String path) {
        try {
            File file = new File(path);
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
        }
    }

    /**
     * Registers a new user account after validating email and password.
     */
    public void registerUser(String email, String password, String username) {
        if (email == null || !email.contains("@") || email.length() > 255) {
            throw new ValidationException("Invalid email");
        }
        if (password == null || password.length() < 8) {
            throw new ValidationException("Password too short");
        }
        userRepository.create(email, password, username);
    }

    /**
     * Updates the email address for an existing account.
     */
    public void updateEmail(String userId, String newEmail) {
        if (newEmail == null || !newEmail.contains("@") || newEmail.length() > 255) {
            throw new ValidationException("Invalid email");     // duplicated from registerUser
        }
        userRepository.updateEmail(userId, newEmail);
    }

    /**
     * Sends a password-reset link after validating the new password.
     */
    public void resetPassword(String userId, String newPassword) {
        if (newPassword == null || newPassword.length() < 8) {
            throw new ValidationException("Password too short"); // duplicated from registerUser
        }
        userRepository.updatePassword(userId, newPassword);
    }
}
