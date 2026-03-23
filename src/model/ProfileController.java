package model;

/**
 * ProfileController is an abstract base class for handling profile editing.
 *
 * "Abstract" means this class cannot be used on its own — a subclass must
 * be created that fills in the specific behaviour for each user role.
 * Currently, CustomerProfileController extends this for customer accounts.
 *
 * Java OOP principles used:
 *  - Abstraction  : defines WHAT a profile controller does (save, delete, validate)
 *                   without specifying HOW — subclasses decide the details
 *  - Inheritance  : CustomerProfileController extends this and gets validate()
 *                   and hasNoChanges() for free
 *  - Polymorphism : saveProfile() and deleteAccount() behave differently
 *                   depending on which subclass is used
 *  - Encapsulation: validation rules are written once here and shared by all
 *                   subclasses — they don't need to re-write validation logic
 */
public abstract class ProfileController {

    /**
     * AccountService is used to read from and write to accounts.txt.
     * It is declared "protected" so subclasses can access it directly.
     */
    protected final AccountService accountService;

    /**
     * Constructor — sets up the account service.
     * Subclasses call super(accountService) to run this.
     */
    protected ProfileController(AccountService accountService) {
        this.accountService = accountService;
    }

    // ── Abstract methods — subclasses MUST override these ─────────

    /**
     * Returns the currently logged-in user.
     * Each subclass knows how to get the current user from its own session.
     */
    public abstract User getCurrentUser();

    /**
     * Saves the user's updated name and email.
     * Each subclass handles saving differently depending on the role.
     *
     * @param newName   the updated name entered by the user
     * @param newEmail  the updated email entered by the user
     * @return true if saved successfully, false if something went wrong
     */
    public abstract boolean saveProfile(String newName, String newEmail);

    /**
     * Deletes the current user's account.
     * Each subclass handles this differently depending on the role.
     *
     * @return true if deleted successfully, false if something went wrong
     */
    public abstract boolean deleteAccount();

    // ── Shared methods — inherited by ALL subclasses ──────────────

    /**
     * Validates the name and email entered by the user.
     *
     * Rules:
     *  Name  — cannot be empty, must be 2 to 50 characters, letters only
     *  Email — cannot be empty, must be in valid format (e.g. user@example.com),
     *          must not already be used by another account
     *
     * Returns null if everything is valid.
     * Returns an error message string if something is wrong.
     *
     * Example:
     *   validate("Zhi Lin", "lin@gmail.com") → null (valid)
     *   validate("", "lin@gmail.com")        → "Name cannot be empty."
     *   validate("Zhi Lin", "notanemail")    → "Please enter a valid email address."
     */
    public String validate(String newName, String newEmail) {
        // ── Validate name ─────────────────────────────────────────
        if (newName == null || newName.trim().isEmpty()) {
            return "Name cannot be empty.";
        }
        // Name must be between 2 and 50 characters, letters and spaces only
        if (!newName.trim().matches("[a-zA-Z ]{2,50}")) {
            return "Name must be 2–50 characters and contain only letters.";
        }

        // ── Validate email ────────────────────────────────────────
        if (newEmail == null || newEmail.trim().isEmpty()) {
            return "Email cannot be empty.";
        }
        // Email must be in a valid format e.g. someone@example.com
        if (!newEmail.trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            return "Please enter a valid email address.";
        }

        // Check if the new email is already used by a different account
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            boolean emailIsChanged = !newEmail.trim().equalsIgnoreCase(currentUser.getEmail());
            boolean emailAlreadyExists = accountService.emailExists(newEmail.trim());

            if (emailIsChanged && emailAlreadyExists) {
                return "An account with this email already exists.";
            }
        }

        return null; // null means all validation passed — no errors
    }

    /**
     * Checks whether the user made any actual changes to their name or email.
     * Returns true if NOTHING changed (both name and email are the same as before).
     * Returns false if at least one of them is different.
     *
     * Used by CustomerDashboard to show "No changes were made." if the user
     * clicks Save without editing anything.
     */
    public boolean hasNoChanges(String newName, String newEmail) {
        User currentUser = getCurrentUser();

        // If there is no logged-in user, treat it as "no changes"
        if (currentUser == null) return true;

        boolean nameIsTheSame  = currentUser.getName().equals(newName.trim());
        boolean emailIsTheSame = currentUser.getEmail().equals(newEmail.trim());

        return nameIsTheSame && emailIsTheSame;
    }
}
