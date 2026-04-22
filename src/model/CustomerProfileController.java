package model;

/**
 * ============================================================
 * CustomerProfileController.java — Personal Information Logic
 * ============================================================
 *
 * Handles ALL business logic for the Personal Information card
 * in ViewProfile.java. ViewProfile.java only calls these methods
 * and handles the resulting UI changes.
 *
 * Methods moved FROM ViewProfile.java:
 *   - getCurrentName()       : returns the logged-in user's current name
 *   - getCurrentEmail()      : returns the logged-in user's current email
 *   - hasNoChanges()         : checks whether name/email actually changed
 *   - saveProfile()          : validates and persists name + email changes
 *
 * Validation rules (moved from inline ViewProfile logic):
 *   - Name  : cannot be blank
 *   - Email : cannot be blank, must contain '@', must not already exist
 *             for a DIFFERENT account
 *
 * Called by ViewProfile.java:
 *   enterEditMode()      -> getCurrentName() / getCurrentEmail()
 *   exitEditMode()       -> getCurrentName() / getCurrentEmail()
 *   saveProfileChanges() -> hasNoChanges() / saveProfile()
 *
 * Called by CustomerDashboard.java (constructor):
 *   new CustomerProfileController(accountService, appFrameAccessor)
 * ============================================================
 */
public class CustomerProfileController {

    // =========================================================
    // AppFrameAccessor INTERFACE
    // =========================================================

    /**
     * Provides CustomerProfileController with access to the
     * AppFrame state it needs (logged-in user object and name)
     * without creating a hard dependency on AppFrame itself.
     *
     * Implemented as an anonymous class in CustomerDashboard's
     * constructor, delegating to app.getLoggedInUserObj() etc.
     *
     * Used by CustomerDashboard:
     *   profileController = new CustomerProfileController(
     *       app.getAccountService(),
     *       new CustomerProfileController.AppFrameAccessor() {
     *           public User   getLoggedInUserObj()       { return app.getLoggedInUserObj(); }
     *           public String getLoggedInUser()          { return app.getLoggedInUser(); }
     *           public void   setLoggedInUser(String n)  { app.setLoggedInUser(n); }
     *           public void   setLoggedInUserObj(User u) { app.setLoggedInUserObj(u); }
     *       }
     *   );
     */
    public interface AppFrameAccessor {
        User   getLoggedInUserObj();
        String getLoggedInUser();
        void   setLoggedInUser(String name);
        void   setLoggedInUserObj(User user);
    }

    // =========================================================
    // FIELDS
    // =========================================================

    private final AccountService   accountService;
    private final AppFrameAccessor accessor;       // access to AppFrame state

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    /**
     * @param accountService  reads/writes accounts.txt
     * @param accessor        provides access to AppFrame logged-in user state
     */
    public CustomerProfileController(AccountService accountService,
                                     AppFrameAccessor accessor) {
        this.accountService = accountService;
        this.accessor       = accessor;
    }

    // =========================================================
    // READ HELPERS — used by enterEditMode() and exitEditMode()
    // =========================================================

    /**
     * Returns the current display name for the logged-in user.
     * Used to pre-populate the edit field when entering edit mode,
     * and to restore it when cancelling.
     *
     * @return name string, or empty string if no user is logged in
     */
    public String getCurrentName() {
        User user = accessor.getLoggedInUserObj();
        return (user != null && user.getName() != null) ? user.getName() : "";
    }

    /**
     * Returns the current email address for the logged-in user.
     * Used to pre-populate the edit field when entering edit mode,
     * and to restore it when cancelling.
     *
     * @return email string, or empty string if no user is logged in
     */
    public String getCurrentEmail() {
        User user = accessor.getLoggedInUserObj();
        return (user != null && user.getEmail() != null) ? user.getEmail() : "";
    }

    // =========================================================
    // CHANGE DETECTION — used before trying to save
    // =========================================================

    /**
     * Returns true if the submitted name and email are identical
     * to the currently stored values (i.e. the user changed nothing).
     *
     * ViewProfile calls this inside saveProfileChanges() to show
     * the "No changes were made." dialog and skip the file write.
     *
     * @param newName   trimmed text from the name edit field
     * @param newEmail  trimmed text from the email edit field
     * @return true if nothing changed, false if at least one field differs
     */
    public boolean hasNoChanges(String newName, String newEmail) {
        return getCurrentName().equals(newName) && getCurrentEmail().equals(newEmail);
    }

    // =========================================================
    // SAVE — validates and persists the profile changes
    // =========================================================

    /**
     * Validates the new name and email, then persists them via
     * AccountService if they pass. Updates the AppFrame's logged-in
     * user reference on success via the AppFrameAccessor.
     *
     * Validation rules:
     *   1. Name  — must not be blank
     *   2. Email — must not be blank
     *   3. Email — must contain the '@' character
     *   4. Email — must not already belong to a DIFFERENT account
     *              (case-insensitive check via accountService.emailExists())
     *
     * @param newName   trimmed text from the name edit field
     * @param newEmail  trimmed text from the email edit field
     * @return true if saved successfully, false on persistence failure
     * @throws IllegalArgumentException if any validation rule is violated
     *         (the message is shown directly to the user in a dialog)
     */
    public boolean saveProfile(String newName, String newEmail) {
        // ── Validation ──────────────────────────────────────────────
        if (newName.isEmpty())
            throw new IllegalArgumentException("Name cannot be empty.");
        if (newEmail.isEmpty())
            throw new IllegalArgumentException("Email cannot be empty.");
        if (!newEmail.contains("@"))
            throw new IllegalArgumentException("Please enter a valid email address.");

        // Only check email uniqueness when the email actually changed
        boolean emailChanged = !newEmail.equalsIgnoreCase(getCurrentEmail());
        if (emailChanged && accountService.emailExists(newEmail))
            throw new IllegalArgumentException(
                    "This email is already in use by another account.");

        // ── Persist ─────────────────────────────────────────────────
        User currentUser = accessor.getLoggedInUserObj();
        if (currentUser == null) return false;

        // Build an updated User object preserving all other fields
        User updated = new User(
                currentUser.getUserId(),
                newName,
                newEmail,
                currentUser.getPassword(),
                currentUser.getRole(),
                currentUser.getProfilePicture()
        );

        String originalEmail = getCurrentEmail();
        boolean saved = accountService.updateUser(originalEmail, updated);
        if (saved) {
            // Keep AppFrame's logged-in user references in sync so that
            // getCurrentName() / getCurrentEmail() return fresh values,
            // and the header name label updates immediately.
            accessor.setLoggedInUserObj(updated);
            accessor.setLoggedInUser(newName);
        }
        return saved;
    }
}