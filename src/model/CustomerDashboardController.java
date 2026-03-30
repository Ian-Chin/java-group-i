package model;

import java.awt.Frame;
import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

/**
 * CustomerDashboardController handles ALL the business logic for the Customer Dashboard.
 *
 * The CustomerDashboard (view) only builds the UI layout and calls methods here.
 * This class decides WHAT happens when buttons are clicked.
 *
 * Java OOP principles used:
 *  - Class        : groups all dashboard logic into one organised place
 *  - Object       : CustomerDashboard creates ONE instance of this controller
 *                   and uses it throughout
 *  - Encapsulation: all logic is hidden inside this class — the dashboard
 *                   never directly reads/writes files or validates data itself
 *  - Abstraction  : the dashboard only calls simple methods like handleSave()
 *                   or chooseProfileImage() without knowing the details
 *  - Inheritance  : uses CustomerProfileController which extends ProfileController
 *  - Polymorphism : saveProfile() and deleteAccount() behave differently per role
 *                   (defined in ProfileController, overridden in CustomerProfileController)
 *
 * CHANGE: vehicles.txt now has 7 columns — vehicleType (Car/Motor) was added.
 *   handleAddVehicle()    now accepts vehicleType as the first parameter.
 *   handleUpdateVehicle() now accepts newType as a parameter.
 *   validateVehicleFields() now validates 5 fields (type, plate, brand, year, colour).
 */
public class CustomerDashboardController {

    // ── Services used to read/write data files ────────────────────
    private final VehicleService            vehicleService;
    private final ProfilePicStorage         profilePicStorage;
    private final BackgroundImageStorage    backgroundImageStorage;
    private final CustomerProfileController profileController;

    // ── Callback interface so this controller can update the UI ──
    private final DashboardView view;

    /**
     * DashboardView is a contract (interface) that lists everything this
     * controller needs to update in the UI.
     *
     * CustomerDashboard implements this interface and provides the
     * actual UI update code. This way, the controller never imports
     * CustomerDashboard directly — keeping model and view separate.
     */
    public interface DashboardView {

        // ── Session access ────────────────────────────────────────

        /** Returns the full logged-in User object. */
        User getLoggedInUserObj();

        /** Returns the logged-in user's display name. */
        String getLoggedInUser();

        /** Updates the logged-in name stored in the session. */
        void setLoggedInUser(String name);

        /** Updates the full User object stored in the session. */
        void setLoggedInUserObj(User user);

        // ── UI update methods called by the controller ────────────

        /** Updates the name shown in the top header bar. */
        void updateHeaderName(String name);

        /** Updates which avatar icon is shown in the header circle. */
        void updateAvatarIndex(int index);

        /** Updates the name label on the profile card (read mode). */
        void updateProfileNameLabel(String name);

        /** Updates the email label on the profile card (read mode). */
        void updateProfileEmailLabel(String email);

        /** Updates the role label on the profile card (read mode). */
        void updateProfileRoleLabel(String role);

        /** Sets the profile picture image shown on the profile page and header. */
        void setProfileImage(BufferedImage image);

        /** Sets the banner/background image shown at the top of the profile page. */
        void setBannerImage(BufferedImage image);

        /** Repaints the circular profile picture on the profile page. */
        void repaintProfilePic();

        /** Repaints the banner/background area on the profile page. */
        void repaintBanner();

        /** Repaints the small circular avatar in the header bar. */
        void repaintAvatar();

        /** Switches the profile card to edit mode (shows text fields, hides labels). */
        void showEditMode();

        /** Switches the profile card back to read mode (shows labels, hides text fields). */
        void showReadMode();

        /** Returns the text currently typed in the name text field. */
        String getNameFieldText();

        /** Returns the text currently typed in the email text field. */
        String getEmailFieldText();

        /** Pre-fills the name text field with the given value. */
        void setNameFieldText(String name);

        /** Pre-fills the email text field with the given value. */
        void setEmailFieldText(String email);

        /**
         * Tells the view to rebuild the vehicle list panel with the given vehicles.
         *
         * Each vehicle array has 6 elements:
         *   [0] vehicleID
         *   [1] vehicleType  ("Car" or "Motor")  ← NEW
         *   [2] plate
         *   [3] brand
         *   [4] year
         *   [5] colour
         */
        void rebuildVehicleList(List<String[]> vehicles);

        /** Shows a popup message dialog to the user. */
        void showMessage(String message, String title, int messageType);

        /** Returns the main application window (used as a parent for dialogs). */
        java.awt.Window getWindow();

        /** Navigates back to the login/onboarding screen. */
        void navigateToLogin();
    }

    // ── Constructor ───────────────────────────────────────────────

    /**
     * Creates the controller and connects it to all the required services and the view.
     *
     * @param view                   the dashboard UI (implements DashboardView)
     * @param vehicleService         reads/writes vehicles.txt
     * @param profilePicStorage      saves/loads profile pictures from disk
     * @param backgroundImageStorage saves/loads background/banner images from disk
     * @param profileController      handles profile save and validation logic
     */
    public CustomerDashboardController(
            DashboardView view,
            VehicleService vehicleService,
            ProfilePicStorage profilePicStorage,
            BackgroundImageStorage backgroundImageStorage,
            CustomerProfileController profileController) {

        this.view                   = view;
        this.vehicleService         = vehicleService;
        this.profilePicStorage      = profilePicStorage;
        this.backgroundImageStorage = backgroundImageStorage;
        this.profileController      = profileController;
    }

    // ═══════════════════════════════════════════════════════════════
    // USER REFRESH
    // ═══════════════════════════════════════════════════════════════

    /**
     * Refreshes ALL information shown on the dashboard.
     * Called when the user first logs in and after any profile change.
     *
     * Steps:
     *  1. Update the header name
     *  2. Update the avatar icon index
     *  3. Update the profile labels (name, email, role)
     *  4. Load and show the saved profile picture and background image
     *  5. Reload the vehicle list from vehicles.txt
     */
    public void refreshUser() {

        // Step 1: Update the name shown in the top header
        String name = view.getLoggedInUser();
        if (name == null || name.isEmpty()) {
            name = "Customer"; // use a safe fallback if no name is stored
        }
        view.updateHeaderName(name);

        // Step 2: Update the avatar icon
        User user = view.getLoggedInUserObj();
        if (user != null) {
            view.updateAvatarIndex(user.getProfilePicture());
        }
        view.repaintAvatar();

        // Step 3: Update the profile card labels (name, email, role)
        if (user != null) {
            view.updateProfileNameLabel(user.getName());
            view.updateProfileEmailLabel(user.getEmail());

            // Capitalise the first letter of the role, e.g. "customer" → "Customer"
            String role = user.getRole();
            String capitalisedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
            view.updateProfileRoleLabel(capitalisedRole);

            // Step 4: Load the saved images from disk and push them to the UI
            // NOTE: images are stored by userId (e.g. "C3"), NOT by email
            BufferedImage profileImage = profilePicStorage.loadImage(user.getUserId());
            BufferedImage bannerImage  = backgroundImageStorage.loadImage(user.getUserId());

            view.setProfileImage(profileImage);
            view.setBannerImage(bannerImage);
            view.repaintProfilePic();
            view.repaintBanner();
            view.repaintAvatar();
        }

        // Step 5: Reload the vehicle list from vehicles.txt
        refreshVehicleList();
    }

    // ═══════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION — EDIT / SAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Switches the Personal Information card to edit mode.
     * Pre-fills the text fields with the current saved name and email.
     */
    public void enterEditMode() {
        // Pre-fill the text fields with the current values from accounts.txt
        view.setNameFieldText(profileController.getCurrentName());
        view.setEmailFieldText(profileController.getCurrentEmail());

        // Tell the UI to show the text fields (and hide the read-only labels)
        view.showEditMode();
    }

    /**
     * Switches back to read mode WITHOUT saving any changes.
     * The original name and email remain unchanged.
     */
    public void exitEditMode() {
        // Tell the UI to show the labels again (and hide the text fields)
        view.showReadMode();
    }

    /**
     * Handles the Save button on the Personal Information card.
     *
     * Steps:
     *  1. Read the values typed in the text fields
     *  2. Check if anything actually changed — if not, show "No Changes" message
     *  3. Try to save via profileController (which validates and writes to accounts.txt)
     *  4. Show a success message, or show the error if validation failed
     */
    public void handleSave() {
        // Step 1: Read what the user typed
        String newName  = view.getNameFieldText().trim();
        String newEmail = view.getEmailFieldText().trim();

        // Step 2: If nothing changed, tell the user and exit edit mode
        if (profileController.hasNoChanges(newName, newEmail)) {
            view.showMessage("No changes were made.", "No Changes",
                    JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }

        // Step 3: Try to save — profileController handles validation
        try {
            boolean saved = profileController.saveProfile(newName, newEmail);

            if (saved) {
                exitEditMode();
                refreshUser(); // update all displayed labels with the new values
                view.showMessage("Profile updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                view.showMessage("Failed to save. Please try again.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

        } catch (IllegalArgumentException ex) {
            // profileController threw a validation error — show it to the user
            view.showMessage(ex.getMessage(), "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE LIST
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads ALL vehicles from vehicles.txt for the logged-in user,
     * then tells the UI to rebuild the vehicle list panel.
     *
     * We pass ALL vehicles — the view (CustomerDashboard) decides
     * how many to show on screen (top 3) and when to show "View All".
     *
     * Each vehicle array returned has 6 elements:
     *   [0] vehicleID, [1] vehicleType, [2] plate, [3] brand, [4] year, [5] colour
     */
    public void refreshVehicleList() {
        User user = view.getLoggedInUserObj();

        if (user == null) {
            // No user logged in — pass an empty list so the UI shows
            // "No vehicles registered."
            view.rebuildVehicleList(new java.util.ArrayList<>());
            return;
        }

        // Read ALL this user's vehicles from vehicles.txt (no limit here)
        // The view will decide to show only the first 3 and add a "View All" button
        List<String[]> allVehicles = vehicleService.getVehiclesByUserId(user.getUserId());
        view.rebuildVehicleList(allVehicles);
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE ADD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and adds a new vehicle to vehicles.txt.
     * Called when the Save button on the Add Vehicle form is clicked.
     *
     * CHANGE: vehicleType ("Car" or "Motor") is now the FIRST parameter.
     *
     * @param vehicleType "Car" or "Motor" — chosen from the dropdown
     * @param plate       car/motor plate number entered by the user
     * @param brand       brand/model entered by the user
     * @param year        year entered by the user
     * @param colour      colour entered by the user
     * @return true if the vehicle was added successfully, false otherwise
     */
    public boolean handleAddVehicle(String vehicleType, String plate,
                                    String brand, String year, String colour) {

        // Validate all 5 fields first (type, plate, brand, year, colour)
        String validationError = validateVehicleFields(vehicleType, plate, brand, year, colour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false; // stop here — do not save if any field is invalid
        }

        // Get the logged-in user's ID (e.g. "C3")
        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        // Save the new vehicle to vehicles.txt via VehicleService
        // VehicleService.addVehicle() now accepts vehicleType as the 2nd parameter
        boolean saved = vehicleService.addVehicle(
                user.getUserId(), // e.g. "C3"
                vehicleType,      // "Car" or "Motor"
                plate,
                brand,
                year,
                colour);

        if (saved) {
            refreshVehicleList(); // reload so the new vehicle appears on screen immediately
        } else {
            view.showMessage("Failed to add vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE EDIT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and updates an existing vehicle record in vehicles.txt.
     * Called when the Save button on a vehicle's Edit form is clicked.
     *
     * "In place" means the vehicle stays in the same position in the file
     * — only the field values change, the order does not change.
     *
     * CHANGE: newType ("Car" or "Motor") is now the 2nd parameter.
     *
     * @param oldPlate  the ORIGINAL plate number — used to FIND the record in vehicles.txt
     * @param newType   the updated vehicle type ("Car" or "Motor")
     * @param newPlate  the new plate number (may be the same as oldPlate)
     * @param newBrand  the updated brand/model
     * @param newYear   the updated year
     * @param newColour the updated colour
     * @return true if updated successfully, false otherwise
     */
    public boolean handleUpdateVehicle(String oldPlate, String newType, String newPlate,
                                       String newBrand, String newYear, String newColour) {

        // Validate all 5 fields first (type, plate, brand, year, colour)
        String validationError = validateVehicleFields(newType, newPlate, newBrand, newYear, newColour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false; // stop here — do not save if any field is invalid
        }

        // Get the logged-in user's ID (e.g. "C3")
        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        // Update the record in place inside vehicles.txt via VehicleService
        // VehicleService.updateVehicle() now accepts newType as a parameter
        boolean updated = vehicleService.updateVehicle(
                user.getUserId(), // e.g. "C3"
                oldPlate,         // used to locate the correct line in the file
                newType,          // "Car" or "Motor"
                newPlate,
                newBrand,
                newYear,
                newColour);

        if (updated) {
            refreshVehicleList(); // reload to show the updated vehicle on screen
        } else {
            view.showMessage("Failed to update vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return updated;
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE REMOVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Shows a confirmation popup, then deletes the vehicle from vehicles.txt.
     * Called when the Remove button on a vehicle row is clicked.
     *
     * @param plate the plate number of the vehicle to remove
     *              (used to locate the correct line in vehicles.txt)
     */
    public void handleRemoveVehicle(String plate) {
        // Ask the user to confirm before permanently deleting
        int choice = JOptionPane.showConfirmDialog(
                view.getWindow(),
                "Are you sure you want to remove this vehicle?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        // Only delete if the user clicked YES
        if (choice == JOptionPane.YES_OPTION) {
            User user = view.getLoggedInUserObj();

            if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                refreshVehicleList(); // reload to remove the vehicle from the displayed list
            } else {
                view.showMessage("Failed to remove vehicle.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        // If the user clicked NO, nothing happens — the vehicle stays
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE CHOOSERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens the native OS file chooser so the user can pick a profile picture.
     * Saves the selected image to the ProfilePic folder using the user's ID as filename.
     * Updates the profile picture circle and header avatar immediately.
     */
    public void chooseProfileImage() {
        User user = view.getLoggedInUserObj();
        if (user == null) return;

        // Open the native OS file chooser (shows the Windows/Mac file picker)
        FileDialog fileChooser = new FileDialog(
                (Frame) view.getWindow(),
                "Choose Profile Picture",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp"); // show only image files
        fileChooser.setVisible(true);

        // If the user cancelled the dialog without choosing a file, stop here
        if (fileChooser.getFile() == null) return;

        try {
            // Read the selected file into a BufferedImage object
            java.io.File selectedFile = new java.io.File(
                    fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                view.showMessage("Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save the image to disk using the user's ID (e.g. "C3") as the filename
            // NOTE: uses userId (not email) to match how VehicleService and other
            // services identify the user
            boolean saved = profilePicStorage.saveImage(user.getUserId(), image);
            if (!saved) {
                view.showMessage("Failed to save profile picture.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Push the new image to the UI so it shows immediately — no need to restart
            view.setProfileImage(image);
            view.repaintProfilePic();
            view.repaintAvatar();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            view.showMessage("Failed to read the selected image.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the native OS file chooser so the user can pick a background/banner image.
     * Saves the selected image to the BackgroundImg folder using the user's ID as filename.
     * Updates the banner immediately.
     */
    public void chooseBannerImage() {
        User user = view.getLoggedInUserObj();
        if (user == null) return;

        FileDialog fileChooser = new FileDialog(
                (Frame) view.getWindow(),
                "Choose Background Image",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fileChooser.setVisible(true);

        if (fileChooser.getFile() == null) return;

        try {
            java.io.File selectedFile = new java.io.File(
                    fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                view.showMessage("Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save using userId (e.g. "C3") — consistent with how other services work
            boolean saved = backgroundImageStorage.saveImage(user.getUserId(), image);
            if (!saved) {
                view.showMessage("Failed to save background image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update the banner on screen immediately
            view.setBannerImage(image);
            view.repaintBanner();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            view.showMessage("Failed to read the selected image.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE VALIDATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates all FIVE vehicle fields before saving.
     *
     * CHANGE: vehicleType is now validated as the FIRST field.
     *
     * Rules:
     *
     *  vehicleType — must be exactly "Car" or "Motor"
     *                ✅ "Car"          ✅ "Motor"       ❌ ""   ❌ "Truck"
     *
     *  Car Plate   — must contain BOTH letters AND numbers, no special characters
     *                ✅ "WXY1234"      ❌ "ABC"  ❌ "1234"  ❌ "WXY-123"
     *
     *  Brand/Model — letters and/or numbers and spaces only, no special characters
     *                ✅ "Toyota Vios"  ✅ "BMW i5"           ❌ "BMW-i5"
     *
     *  Year        — exactly 4 digits
     *                ✅ "2025"         ❌ "25"   ❌ "202A"
     *
     *  Colour      — letters and spaces only, no numbers or special characters
     *                ✅ "White"        ✅ "Dark Blue"         ❌ "Blue2"
     *
     * @return an error message string if any field is invalid,
     *         or null if ALL fields are valid (null = no error = safe to save)
     */
    public String validateVehicleFields(String vehicleType, String plate,
                                        String brand, String year, String colour) {

        // ── Vehicle Type ──────────────────────────────────────────
        // Must be selected — cannot be empty
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return "Please select a vehicle type (Car or Motor).";
        }
        // Must be exactly "Car" or "Motor" — nothing else is accepted
        if (!vehicleType.equals("Car") && !vehicleType.equals("Motor")) {
            return "Vehicle type must be either 'Car' or 'Motor'.";
        }

        // ── Car Plate ─────────────────────────────────────────────
        // Cannot be empty
        if (plate.isEmpty()) {
            return "Car Plate cannot be empty.";
        }
        // Only letters, digits, and spaces are allowed — no dashes, dots, etc.
        if (!plate.matches("[a-zA-Z0-9 ]+")) {
            return "Car Plate can only contain letters and numbers (no special characters).";
        }
        // Must have at least one letter AND at least one number
        boolean plateHasLetter = plate.matches(".*[a-zA-Z].*");
        boolean plateHasNumber = plate.matches(".*[0-9].*");
        if (!plateHasLetter || !plateHasNumber) {
            return "Car Plate must contain both letters and numbers (e.g. WXY1234).";
        }

        // ── Brand / Model ─────────────────────────────────────────
        if (brand.isEmpty()) {
            return "Brand / Model cannot be empty.";
        }
        // Letters, digits and spaces only
        if (!brand.matches("[a-zA-Z0-9 ]+")) {
            return "Brand / Model can only contain letters and numbers (no special characters).";
        }

        // ── Year ──────────────────────────────────────────────────
        if (year.isEmpty()) {
            return "Year cannot be empty.";
        }
        // Must be exactly 4 digits, e.g. "2025"
        if (!year.matches("\\d{4}")) {
            return "Year must be exactly 4 digits (e.g. 2025).";
        }

        // ── Colour ────────────────────────────────────────────────
        if (colour.isEmpty()) {
            return "Colour cannot be empty.";
        }
        // Letters and spaces only — no numbers or special characters
        if (!colour.matches("[a-zA-Z ]+")) {
            return "Colour can only contain letters (e.g. White, Dark Blue).";
        }

        return null; // null = ALL fields passed — safe to save
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVENIENCE GETTERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the profile controller.
     * The view uses this to call getCurrentName() and getCurrentEmail()
     * when pre-filling the edit form.
     */
    public CustomerProfileController getProfileController() {
        return profileController;
    }

    /**
     * Returns the vehicle service.
     * The view can use this directly if needed (e.g. for the "View All" dialog).
     */
    public VehicleService getVehicleService() {
        return vehicleService;
    }
}