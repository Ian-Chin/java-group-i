package model;

/**
 * CustomerProfileController.java — Personal Information Logic
 */
public class CustomerProfileController {

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

    public CustomerProfileController(AccountService accountService,
                                     AppFrameAccessor accessor) {
        this.accountService = accountService;
        this.accessor       = accessor;
    }

    public String getCurrentName() {
        User user = accessor.getLoggedInUserObj();
        return (user != null && user.getName() != null) ? user.getName() : "";
    }

    public String getCurrentEmail() {
        User user = accessor.getLoggedInUserObj();
        return (user != null && user.getEmail() != null) ? user.getEmail() : "";
    }

    public boolean hasNoChanges(String newName, String newEmail) {
        return getCurrentName().equals(newName) && getCurrentEmail().equals(newEmail);
    }

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
            accessor.setLoggedInUserObj(updated);
            accessor.setLoggedInUser(newName);
        }
        return saved;
    }
}