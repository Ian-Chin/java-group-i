package model;

/**
 * CustomerProfileController handles all profile-related actions for customers.
 *
 * It acts as the "middle layer" between the CustomerDashboard (view) and
 * the AccountService (data layer). The dashboard never talks to AccountService
 * directly — it always goes through this controller.
 *
 * Java OOP principles used:
 *  - Inheritance  : extends ProfileController, which provides validate() and
 *                   hasNoChanges() methods for free
 *  - Polymorphism : overrides saveProfile() and deleteAccount() from ProfileController
 *                   so they work specifically for customer accounts
 *  - Encapsulation: CustomerDashboard never reads or writes accounts.txt directly —
 *                   it just calls saveProfile() or deleteAccount() here
 *  - Abstraction  : the dashboard only knows WHAT these methods do, not HOW
 *                   the file reading/writing works underneath
 */
public class CustomerProfileController extends ProfileController {

    /**
     * AppFrameAccessor is a small interface (a "contract") that lets this
     * model class get and update the logged-in user session WITHOUT needing
     * to import the AppFrame class from the view package.
     *
     * This keeps the model layer independent of the view layer — a key
     * principle of good software design.
     *
     * CustomerDashboard creates an anonymous implementation of this interface
     * when it creates the controller.
     */
    public interface AppFrameAccessor {
        User   getLoggedInUserObj();           // get the full User object
        String getLoggedInUser();              // get just the logged-in name
        void   setLoggedInUser(String name);   // update the session name
        void   setLoggedInUserObj(User user);  // update the full User object
    }

    // Reference to the app session (set in the constructor)
    private final AppFrameAccessor appAccessor;

    // ── Constructor ───────────────────────────────────────────────

    /**
     * Creates a new CustomerProfileController.
     *
     * @param accountService  used to read/write the accounts.txt file
     * @param appAccessor     used to get/set the currently logged-in user
     */
    public CustomerProfileController(AccountService accountService,
                                     AppFrameAccessor appAccessor) {
        super(accountService); // pass accountService to the parent class (ProfileController)
        this.appAccessor = appAccessor;
    }

    // ── Overridden methods from ProfileController ─────────────────

    /**
     * Returns the currently logged-in User object.
     * This overrides the abstract method in ProfileController.
     */
    @Override
    public User getCurrentUser() {
        return appAccessor.getLoggedInUserObj();
    }

    /**
     * Saves the customer's updated name and email to accounts.txt.
     *
     * Steps:
     *  1. Trim whitespace from the inputs
     *  2. Validate using the parent class validate() method
     *  3. Build an updated User object with the new name/email
     *  4. Save via AccountService
     *  5. If email changed, rename the image files so they still load correctly
     *
     * Throws IllegalArgumentException with a user-facing message if validation fails.
     * Returns true on success, false if the file could not be written.
     */
    @Override
    public boolean saveProfile(String newName, String newEmail) {
        // Remove extra spaces from both ends
        newName  = newName.trim();
        newEmail = newEmail.trim();

        // Validate the inputs using the method inherited from ProfileController
        String validationError = validate(newName, newEmail);
        if (validationError != null) {
            // Throw an error that the dashboard will catch and show to the user
            throw new IllegalArgumentException(validationError);
        }

        // Get the currently logged-in user
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        // Remember the original email before making changes
        String originalEmail = currentUser.getEmail();

        // Build a new User object with the updated name and email
        // (password, role, and profile picture index stay the same)
        User updatedUser = new User(
                newName,
                newEmail,
                currentUser.getPassword(),
                currentUser.getRole(),
                currentUser.getProfilePicture()
        );

        // Save the updated user to accounts.txt
        boolean saveSuccessful = accountService.updateUser(originalEmail, updatedUser);

        if (saveSuccessful) {
            // Update the session so the header shows the new name immediately
            appAccessor.setLoggedInUser(newName);
            appAccessor.setLoggedInUserObj(updatedUser);

            // If the email address changed, rename the image files so they
            // still load correctly on the next login
            // e.g. lin@gmail.com.jpg → newlin@gmail.com.jpg
            boolean emailChanged = !originalEmail.equalsIgnoreCase(newEmail);
            if (emailChanged) {
                renameImageFile(
                        "src" + java.io.File.separator + "ProfilePic",
                        originalEmail, newEmail);
                renameImageFile(
                        "src" + java.io.File.separator + "BackgroundImg",
                        originalEmail, newEmail);
            }
        }

        return saveSuccessful;
    }

    /**
     * Renames {folder}/{oldEmail}.jpg to {folder}/{newEmail}.jpg.
     *
     * This is called when the user changes their email address so that
     * the profile picture and background image still load correctly
     * on the next login (since the filename is based on the email).
     *
     * Example:
     *   old: src/ProfilePic/lin@gmail.com.jpg
     *   new: src/ProfilePic/newlin@gmail.com.jpg
     *
     * @param folder    the folder containing the image (e.g. "src/ProfilePic")
     * @param oldEmail  the old email address (current filename)
     * @param newEmail  the new email address (new filename)
     */
    private void renameImageFile(String folder, String oldEmail, String newEmail) {
        // Make both emails safe to use as filenames
        // (replaces any invalid characters with underscore)
        String safeOldEmail = oldEmail.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");
        String safeNewEmail = newEmail.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");

        // Build the full paths for the old and new files
        java.io.File folder_dir = new java.io.File(System.getProperty("user.dir"), folder);
        java.io.File oldFile    = new java.io.File(folder_dir, safeOldEmail + ".jpg");
        java.io.File newFile    = new java.io.File(folder_dir, safeNewEmail + ".jpg");

        // Only rename if the old file actually exists
        if (oldFile.exists()) {
            boolean renameSuccessful = oldFile.renameTo(newFile);
            System.out.println("[CustomerProfileController] Renamed image file: "
                    + oldFile.getName() + " → " + newFile.getName()
                    + "  success=" + renameSuccessful);
        }
    }

    /**
     * Deletes the customer's account from accounts.txt.
     *
     * Steps:
     *  1. Read every line from accounts.txt
     *  2. Keep every line EXCEPT the one matching this customer's email
     *  3. Write the remaining lines back to the file
     *  4. Clear the session so the app goes back to the login screen
     *
     * Returns true if the account was found and deleted, false otherwise.
     */
    @Override
    public boolean deleteAccount() {
        // Get the currently logged-in user
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        // Path to the accounts file
        String filePath = "src" + java.io.File.separator
                + "TxtFile" + java.io.File.separator + "accounts.txt";
        java.io.File accountsFile = new java.io.File(filePath);

        if (!accountsFile.exists()) return false;

        // List to hold all lines we want to keep
        java.util.List<String> linesToKeep = new java.util.ArrayList<>();
        boolean accountWasFound = false;

        // Step 1: Read all lines, skip the one matching this user's email
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(accountsFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] columns = line.split(",", 5);

                // Email is in column index 1 (e.g. name,email,password,role,pic)
                boolean isThisAccount = columns.length >= 2
                        && columns[1].trim().equalsIgnoreCase(currentUser.getEmail());

                if (isThisAccount) {
                    accountWasFound = true;
                    // Do NOT add this line — this is how we "delete" the account
                } else {
                    linesToKeep.add(line); // keep all other accounts
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        // If the account was not found, nothing to delete
        if (!accountWasFound) return false;

        // Step 2: Write the remaining lines back to the file (overwrite mode)
        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(accountsFile, false))) {
            for (String lineToWrite : linesToKeep) {
                writer.write(lineToWrite);
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        // Step 3: Clear the session — app goes back to the login screen
        appAccessor.setLoggedInUser("");
        appAccessor.setLoggedInUserObj(null);

        return true;
    }

    // ── Convenience getters ───────────────────────────────────────
    // These allow the dashboard to easily get the current user's details
    // without needing to call getCurrentUser() and then call getName() etc.

    /** Returns the current user's name, or empty string if not logged in. */
    public String getCurrentName() {
        User user = getCurrentUser();
        return user != null ? user.getName() : "";
    }

    /** Returns the current user's email, or empty string if not logged in. */
    public String getCurrentEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : "";
    }

    /** Returns the current user's role with the first letter capitalised.
     *  e.g. "customer" → "Customer". Returns empty string if not logged in. */
    public String getCurrentRole() {
        User user = getCurrentUser();
        if (user == null) return "";
        String role = user.getRole();
        return role.substring(0, 1).toUpperCase() + role.substring(1);
    }
}
