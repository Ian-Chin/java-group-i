package model;

/**
 * Concrete profile controller for customer accounts.
 *
 * Java principles:
 *  - Inheritance     : extends ProfileController
 *  - Polymorphism    : overrides saveProfile() and deleteAccount()
 *  - Encapsulation   : CustomerDashboard never calls AccountService directly
 *  - Abstraction     : dashboard only calls saveProfile() / deleteAccount(),
 *                      not the underlying file I/O
 */
public class CustomerProfileController extends ProfileController {

    /**
     * Minimal callback interface so this model class never imports AppFrame.
     * Keeps model layer independent of view layer.
     */
    public interface AppFrameAccessor {
        User   getLoggedInUserObj();
        String getLoggedInUser();
        void   setLoggedInUser(String name);
        void   setLoggedInUserObj(User user);
    }

    private final AppFrameAccessor appAccessor;

    // ── Constructor ──────────────────────────────────────────────
    public CustomerProfileController(AccountService accountService,
                                     AppFrameAccessor appAccessor) {
        super(accountService);
        this.appAccessor = appAccessor;
    }

    // ── Abstract method implementations ─────────────────────────

    @Override
    public User getCurrentUser() {
        return appAccessor.getLoggedInUserObj();
    }

    /**
     * Validates then saves name + email to accounts.txt.
     * Throws IllegalArgumentException with a user-facing message if validation fails.
     * Returns true on success, false on file-write failure.
     */
    @Override
    public boolean saveProfile(String newName, String newEmail) {
        newName  = newName.trim();
        newEmail = newEmail.trim();

        // Reuse base-class validation (inheritance)
        String error = validate(newName, newEmail);
        if (error != null) throw new IllegalArgumentException(error);

        User current = getCurrentUser();
        if (current == null) return false;

        String originalEmail = current.getEmail();
        User updated = new User(
                newName, newEmail,
                current.getPassword(),
                current.getRole(),
                current.getProfilePicture()
        );

        boolean success = accountService.updateUser(originalEmail, updated);
        if (success) {
            appAccessor.setLoggedInUser(newName);
            appAccessor.setLoggedInUserObj(updated);

            // If email changed, rename image files so they still load correctly
            if (!originalEmail.equalsIgnoreCase(newEmail)) {
                renameImageFile("src" + java.io.File.separator + "ProfilePic",
                        originalEmail, newEmail);
                renameImageFile("src" + java.io.File.separator + "BackgroundImg",
                        originalEmail, newEmail);
            }
        }
        return success;
    }

    /**
     * Renames {folder}/{oldEmail}.jpg to {folder}/{newEmail}.jpg
     * so profile picture and background still load after email change.
     */
    private void renameImageFile(String folder, String oldEmail, String newEmail) {
        String sanitised_old = oldEmail.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");
        String sanitised_new = newEmail.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");

        java.io.File root = new java.io.File(System.getProperty("user.dir"), folder);
        java.io.File oldFile = new java.io.File(root, sanitised_old + ".jpg");
        java.io.File newFile = new java.io.File(root, sanitised_new + ".jpg");

        if (oldFile.exists()) {
            boolean renamed = oldFile.renameTo(newFile);
            System.out.println("[CustomerProfileController] Renamed image: "
                    + oldFile.getName() + " → " + newFile.getName()
                    + "  success=" + renamed);
        }
    }

    /**
     * Deletes this customer's account directly from accounts.txt.
     * AccountService is not modified — file I/O handled here to keep
     * AccountService focused on authentication and registration.
     */
    @Override
    public boolean deleteAccount() {
        User current = getCurrentUser();
        if (current == null) return false;

        String filePath = "src" + java.io.File.separator
                + "TxtFile" + java.io.File.separator + "accounts.txt";
        java.io.File file = new java.io.File(filePath);
        if (!file.exists()) return false;

        java.util.List<String> lines = new java.util.ArrayList<>();
        boolean found = false;

        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",", 5);
                // Match by email (second column)
                if (parts.length >= 2
                        && parts[1].trim().equalsIgnoreCase(current.getEmail())) {
                    found = true; // skip this line — deletes the account
                } else {
                    lines.add(line);
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!found) return false;

        try (java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(file, false))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        // Clear session on success
        appAccessor.setLoggedInUser("");
        appAccessor.setLoggedInUserObj(null);
        return true;
    }

    // ── Convenience getters (encapsulation — caller gets only what it needs) ─

    public String getCurrentName()  {
        User u = getCurrentUser();
        return u != null ? u.getName() : "";
    }

    public String getCurrentEmail() {
        User u = getCurrentUser();
        return u != null ? u.getEmail() : "";
    }

    public String getCurrentRole()  {
        User u = getCurrentUser();
        if (u == null) return "";
        String r = u.getRole();
        return r.substring(0, 1).toUpperCase() + r.substring(1);
    }
}
