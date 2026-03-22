package model;

/**
 * Abstract base controller for profile editing.
 *
 * Java principles:
 *  - Abstraction  : defines WHAT a profile controller does, not HOW
 *  - Inheritance  : CustomerProfileController extends this
 *  - Polymorphism : subclasses override saveProfile() per role
 *  - Encapsulation: validation logic hidden here, reused by all subclasses
 */
public abstract class ProfileController {

    protected final AccountService accountService;

    protected ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ── Abstract — subclasses must implement ─────────────────────
    public abstract User    getCurrentUser();
    public abstract boolean saveProfile(String newName, String newEmail);
    public abstract boolean deleteAccount();

    // ── Shared validation (encapsulated, inherited by all subclasses) ─

    /**
     * Returns null if valid, or an error message string if not.
     */
    public String validate(String newName, String newEmail) {
        if (newName == null || newName.trim().isEmpty())
            return "Name cannot be empty.";
        if (!newName.trim().matches("[a-zA-Z ]{2,50}"))
            return "Name must be 2–50 characters and contain only letters.";
        if (newEmail == null || newEmail.trim().isEmpty())
            return "Email cannot be empty.";
        if (!newEmail.trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$"))
            return "Please enter a valid email address.";
        User current = getCurrentUser();
        if (current != null
                && !newEmail.trim().equalsIgnoreCase(current.getEmail())
                && accountService.emailExists(newEmail.trim()))
            return "An account with this email already exists.";
        return null; // no error
    }

    /**
     * Returns true if the new values are identical to the current values.
     * Used to detect "no changes made".
     */
    public boolean hasNoChanges(String newName, String newEmail) {
        User current = getCurrentUser();
        if (current == null) return true;
        return current.getName().equals(newName.trim())
                && current.getEmail().equals(newEmail.trim());
    }
}
