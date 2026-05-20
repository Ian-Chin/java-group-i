package model;

public abstract class ProfileController {

    /**
     * AccountService is used to read from and write to accounts.txt.
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
    //Returns the currently logged-in user.
    
    public abstract User getCurrentUser();

    // Saves the user's updated name and email.
    
    public abstract boolean saveProfile(String newName, String newEmail);

    // Deletes the current user's account.
    
    public abstract boolean deleteAccount();

    // Validates the name and email entered by the user.

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
    
    public boolean hasNoChanges(String newName, String newEmail) {
        User currentUser = getCurrentUser();

        // If there is no logged-in user, treat it as "no changes"
        if (currentUser == null) return true;

        boolean nameIsTheSame  = currentUser.getName().equals(newName.trim());
        boolean emailIsTheSame = currentUser.getEmail().equals(newEmail.trim());

        return nameIsTheSame && emailIsTheSame;
    }
}
