/**
 * DIFFICULTY : Easy
 * SNIPPET    : 01 — String Comparison & Resource Handling
 *
 * Instructions
 * ------------
 * Review the code below as if you were doing a professional code review.
 * Find ALL issues, explain WHY each is a problem, and describe the fix.
 * Try to spot every issue before looking at Easy01_StringAndResources_Answer.java.
 *
 * Hint: there are 3 distinct issues hidden in this class.
 */
public class Easy01_StringAndResources {

    private static final String ROLE_ADMIN = "ADMIN";

    /**
     * Reads the first line from a file and checks if the content matches a key.
     */
    public boolean isKeyValid(String filePath, String inputKey) throws Exception {
        FileInputStream fis = new FileInputStream(filePath);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

        String storedKey = reader.readLine();

        if (storedKey == inputKey) {         // comparison here
            return true;
        }
        return false;
    }

    /**
     * Checks whether the given user has admin privileges.
     */
    public boolean isAdmin(User user) {
        String role = user.getRole();
        if (role == ROLE_ADMIN) {            // comparison here
            return true;
        }
        return false;
    }

    /**
     * Builds a comma-separated summary of all order IDs.
     */
    public String buildOrderSummary(List<String> orderIds) {
        String summary = "";
        for (String id : orderIds) {
            summary = summary + id + ", ";   // string building here
        }
        return summary;
    }
}
