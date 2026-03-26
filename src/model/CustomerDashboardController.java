package model;

import java.awt.Frame;
import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
 */
public class CustomerDashboardController {

    // ── Services used to read/write data files ────────────────────
    private final VehicleService          vehicleService;
    private final ProfilePicStorage       profilePicStorage;
    private final BackgroundImageStorage  backgroundImageStorage;
    private final CustomerProfileController profileController;

    // ── Callback interface so this controller can update the UI ──
    // (without importing any Swing/view classes directly)
    private final DashboardView view;

    /**
     * DashboardView is a contract (interface) that lists everything this
     * controller needs to update in the UI.
     *
     * CustomerDashboard implements this interface and provides the
     * actual UI update code. This way, the controller (model) never
     * imports CustomerDashboard (view) — keeping the layers clean.
     */
    public interface DashboardView {

        // ── Session access ────────────────────────────────────────
        /** Returns the full logged-in User object. */
        User   getLoggedInUserObj();

        /** Returns the logged-in user's display name. */
        String getLoggedInUser();

        /** Updates the logged-in name in the session. */
        void   setLoggedInUser(String name);

        /** Updates the full User object in the session. */
        void   setLoggedInUserObj(User user);

        // ── UI update methods called by the controller ────────────

        /** Updates the name shown in the header. */
        void updateHeaderName(String name);

        /** Updates the avatar icon index shown in the header circle. */
        void updateAvatarIndex(int index);

        /** Updates the name label on the profile card. */
        void updateProfileNameLabel(String name);

        /** Updates the email label on the profile card. */
        void updateProfileEmailLabel(String email);

        /** Updates the role label on the profile card. */
        void updateProfileRoleLabel(String role);

        /** Sets the profile picture image shown on the profile page and header. */
        void setProfileImage(BufferedImage image);

        /** Sets the banner/background image shown on the profile page. */
        void setBannerImage(BufferedImage image);

        /** Repaints the profile picture circle. */
        void repaintProfilePic();

        /** Repaints the banner/background area. */
        void repaintBanner();

        /** Repaints the small header avatar circle. */
        void repaintAvatar();

        /** Switches to edit mode (shows text fields, hides labels). */
        void showEditMode();

        /** Switches back to read mode (shows labels, hides text fields). */
        void showReadMode();

        /** Returns the text currently typed in the name field. */
        String getNameFieldText();

        /** Returns the text currently typed in the email field. */
        String getEmailFieldText();

        /** Fills the name field with the given value. */
        void setNameFieldText(String name);

        /** Fills the email field with the given value. */
        void setEmailFieldText(String email);

        /** Rebuilds the vehicle list panel with current data. */
        void rebuildVehicleList(List<String[]> vehicles);

        /** Shows a popup message to the user. */
        void showMessage(String message, String title, int messageType);

        /** Returns the main application window (used as parent for dialogs). */
        java.awt.Window getWindow();

        /** Navigates back to the login/onboarding screen. */
        void navigateToLogin();
    }

    // ── Constructor ───────────────────────────────────────────────

    /**
     * Creates the controller with all required services.
     *
     * @param view                   the dashboard UI (implements DashboardView)
     * @param vehicleService         reads/writes vehicles.txt
     * @param profilePicStorage      saves/loads profile pictures
     * @param backgroundImageStorage saves/loads background images
     * @param profileController      handles profile save/validation
     */
    /** Creates the controller and connects it to all the services and the view. */
    public CustomerDashboardController(
            DashboardView view,
            VehicleService vehicleService,
            ProfilePicStorage profilePicStorage,
            BackgroundImageStorage backgroundImageStorage,
            CustomerProfileController profileController) {

        this.view                    = view;
        this.vehicleService          = vehicleService;
        this.profilePicStorage       = profilePicStorage;
        this.backgroundImageStorage  = backgroundImageStorage;
        this.profileController       = profileController;
    }

    // ═══════════════════════════════════════════════════════════════
    // USER REFRESH
    // ═══════════════════════════════════════════════════════════════

    /**
     * Refreshes all information displayed on the dashboard.
     * Called after login and after any profile changes.
     *
     * Steps:
     *  1. Update the header name
     *  2. Update the avatar icon
     *  3. Update profile labels (name, email, role)
     *  4. Load and display saved profile picture + background image
     *  5. Reload the vehicle list
     */
    /** Reloads all user information and updates every label and image on the dashboard. */
    public void refreshUser() {
        // Step 1: Update the name shown in the header
        String name = view.getLoggedInUser();
        if (name == null || name.isEmpty()) {
            name = "Customer"; // fallback if name is missing
        }
        view.updateHeaderName(name);

        // Step 2: Update the avatar icon index
        User user = view.getLoggedInUserObj();
        if (user != null) {
            view.updateAvatarIndex(user.getProfilePicture());
        }
        view.repaintAvatar();

        // Step 3: Update the profile labels
        if (user != null) {
            view.updateProfileNameLabel(user.getName());
            view.updateProfileEmailLabel(user.getEmail());

            // Capitalise the first letter of the role e.g. "customer" → "Customer"
            String role = user.getRole();
            String capitalisedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
            view.updateProfileRoleLabel(capitalisedRole);

            // Step 4: Load the saved images from disk and update the UI
            BufferedImage profileImage = profilePicStorage.loadImage(user.getEmail());
            BufferedImage bannerImage  = backgroundImageStorage.loadImage(user.getEmail());

            view.setProfileImage(profileImage);
            view.setBannerImage(bannerImage);
            view.repaintProfilePic();
            view.repaintBanner();
            view.repaintAvatar();
        }

        // Step 5: Reload the vehicle list
        refreshVehicleList();
    }

    // ═══════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION — EDIT / SAVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Switches the profile card to edit mode.
     * Fills in the text fields with the current name and email.
     */
    /** Tells the view to show the text fields so the user can edit their name and email. */
    public void enterEditMode() {
        // Pre-fill the text fields with current values
        view.setNameFieldText(profileController.getCurrentName());
        view.setEmailFieldText(profileController.getCurrentEmail());

        // Tell the UI to show the text fields (and hide the labels)
        view.showEditMode();
    }

    /**
     * Switches back to read mode without saving.
     */
    /** Tells the view to hide the text fields and go back to showing labels. */
    public void exitEditMode() {
        // Tell the UI to show the labels (and hide the text fields)
        view.showReadMode();
    }

    /**
     * Handles the Save button click on the Personal Information card.
     *
     * Steps:
     *  1. Read the new values from the text fields
     *  2. Check if anything actually changed
     *  3. Try to save via the profile controller
     *  4. Show a success or error message
     */
    /** Reads the typed values, checks for changes, validates, and saves the profile. */
    public void handleSave() {
        // Step 1: Get the values typed by the user
        String newName  = view.getNameFieldText().trim();
        String newEmail = view.getEmailFieldText().trim();

        // Step 2: Check if anything actually changed
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
                refreshUser(); // update all displayed labels with new values
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
     * Reads vehicles from vehicles.txt and tells the UI to rebuild the list.
     * Shows a maximum of 3 vehicles.
     * Called on login and after add/update/remove.
     */
    /** Reads the vehicle list from file and tells the view to rebuild the vehicle panel. */
    public void refreshVehicleList() {
        User user = view.getLoggedInUserObj();

        if (user == null) {
            // No user logged in — pass empty list so UI shows "No vehicles registered."
            view.rebuildVehicleList(new java.util.ArrayList<>());
            return;
        }

        // Read this user's vehicles from vehicles.txt
        List<String[]> allVehicles = vehicleService.getVehiclesByUserId(user.getUserId());

        // Only pass the first 3 to the UI
        int limit = Math.min(3, allVehicles.size());
        List<String[]> vehiclesToShow = allVehicles.subList(0, limit);

        view.rebuildVehicleList(vehiclesToShow);
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE ADD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and adds a new vehicle.
     * Called when the Save button on the Add Vehicle form is clicked.
     *
     * @param plate   car plate number entered by the user
     * @param brand   brand/model entered by the user
     * @param year    year entered by the user
     * @param colour  colour entered by the user
     * @return true if the vehicle was added successfully, false otherwise
     */
    /** Validates the new vehicle fields and saves the vehicle to vehicles.txt if valid. */
    public boolean handleAddVehicle(String plate, String brand, String year, String colour) {
        // Validate all fields first
        String validationError = validateVehicleFields(plate, brand, year, colour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Get the logged-in user's email
        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        // Save the vehicle to vehicles.txt
        boolean saved = vehicleService.addVehicle(
                user.getUserId(), plate, brand, year, colour);

        if (saved) {
            refreshVehicleList(); // reload so the new vehicle appears
        } else {
            view.showMessage("Failed to add vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE EDIT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and updates an existing vehicle in place.
     * Called when the Save button on an Edit Vehicle row is clicked.
     *
     * "In place" means the vehicle stays in the same position in vehicles.txt
     * — the order does not change after editing.
     *
     * @param oldPlate  the original plate (used to find the record)
     * @param newPlate  the new plate number
     * @param newBrand  the updated brand/model
     * @param newYear   the updated year
     * @param newColour the updated colour
     * @return true if updated successfully, false otherwise
     */
    /** Validates the edited vehicle fields and updates the record in place in vehicles.txt. */
    public boolean handleUpdateVehicle(String oldPlate, String newPlate,
                                       String newBrand, String newYear, String newColour) {
        // Validate all fields first
        String validationError = validateVehicleFields(newPlate, newBrand, newYear, newColour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Get the logged-in user's email
        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        // Update the record in place in vehicles.txt
        boolean updated = vehicleService.updateVehicle(
        		user.getUserId(), oldPlate, newPlate, newBrand, newYear, newColour);

        if (updated) {
            refreshVehicleList(); // reload to show the updated vehicle
        } else {
            view.showMessage("Failed to update vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return updated;
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE REMOVE
    // ═══════════════════════════════════════════════════════════════

    /**
     * Asks for confirmation then deletes a vehicle from vehicles.txt.
     * Called when the Remove button on a vehicle row is clicked.
     *
     * @param plate  the car plate of the vehicle to remove
     */
    /** Shows a confirmation dialog and deletes the vehicle from vehicles.txt if confirmed. */
    public void handleRemoveVehicle(String plate) {
        // Show a confirmation dialog before deleting
        int choice = JOptionPane.showConfirmDialog(
                view.getWindow(),
                "Are you sure you want to remove this vehicle?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        // Only delete if the user clicked Yes
        if (choice == JOptionPane.YES_OPTION) {
            User user = view.getLoggedInUserObj();

            if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                refreshVehicleList(); // reload to remove the deleted vehicle from the list
            } else {
                view.showMessage("Failed to remove vehicle.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        // If the user clicked No, nothing happens
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE CHOOSERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens the native OS file chooser so the user can pick a profile picture.
     * Saves the image to src/ProfilePic/{email}.jpg
     * and updates the profile picture circle and header avatar immediately.
     */
    /** Opens the OS file chooser, saves the picked image to ProfilePic folder, updates the UI. */
    public void chooseProfileImage() {
        User user = view.getLoggedInUserObj();
        if (user == null) return;

        // Open the native Windows/Mac file chooser (not a Java-style one)
        FileDialog fileChooser = new FileDialog(
                (Frame) view.getWindow(),
                "Choose Profile Picture",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp"); // show only image files
        fileChooser.setVisible(true);

        // If the user cancelled the dialog, do nothing
        if (fileChooser.getFile() == null) return;

        try {
            // Read the selected file into a BufferedImage
            java.io.File selectedFile = new java.io.File(
                    fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                view.showMessage("Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save to src/ProfilePic/{email}.jpg via ProfilePicStorage
            boolean saved = profilePicStorage.saveImage(user.getEmail(), image);
            if (!saved) {
                view.showMessage("Failed to save profile picture.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update the UI immediately so the new picture shows right away
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
     * Opens the native OS file chooser so the user can pick a background image.
     * Saves the image to src/BackgroundImg/{email}.jpg
     * and updates the banner immediately.
     */
    /** Opens the OS file chooser, saves the picked image to BackgroundImg folder, updates the UI. */
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

            // Save to src/BackgroundImg/{email}.jpg via BackgroundImageStorage
            boolean saved = backgroundImageStorage.saveImage(user.getEmail(), image);
            if (!saved) {
                view.showMessage("Failed to save background image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update the banner immediately
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
     * Validates all four vehicle fields.
     * Returns an error message string if any field is invalid.
     * Returns null if ALL fields are valid.
     *
     * Rules:
     *  Car Plate   — must have BOTH letters AND numbers, no special characters
     *                ✅ "WXY1234"     ❌ "ABC"    ❌ "1234"   ❌ "WXY-123"
     *  Brand/Model — letters and/or numbers only, no special characters
     *                ✅ "Toyota Vios" ✅ "BMW i5" ❌ "BMW-i5"
     *  Year        — exactly 4 digits, numbers only
     *                ✅ "2025"        ❌ "25"     ❌ "202A"
     *  Colour      — letters and spaces only, no numbers or special characters
     *                ✅ "White"       ✅ "Dark Blue" ❌ "Blue2" ❌ "Blue-Red"
     *
     * @return error message string if invalid, or null if all valid
     */
    /** Checks all four vehicle fields against the rules and returns an error message or null. */
    public String validateVehicleFields(String plate, String brand,
                                        String year, String colour) {
        // ── Car Plate ─────────────────────────────────────────────
        if (plate.isEmpty()) {
            return "Car Plate cannot be empty.";
        }
        // Only letters, digits and spaces allowed — no dashes, dots, etc.
        if (!plate.matches("[a-zA-Z0-9 ]+")) {
            return "Car Plate can only contain letters and numbers (no special characters).";
        }
        // Must contain at least one letter AND at least one number
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
        // Must be exactly 4 digits e.g. 2025
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

        return null; // null means all fields passed validation
    }

    // ═══════════════════════════════════════════════════════════════
    // CONVENIENCE GETTERS (used by the view to read current user data)
    // ═══════════════════════════════════════════════════════════════

    /** Returns the profile controller (used by view to get current name/email). */
    /** Returns the profile controller so the view can call getCurrentName() and getCurrentEmail(). */
    public CustomerProfileController getProfileController() {
        return profileController;
    }

    /** Returns the vehicle service (used by view to call add/update/delete). */
    /** Returns the vehicle service so the view can use it if needed. */
    public VehicleService getVehicleService() {
        return vehicleService;
    }
}