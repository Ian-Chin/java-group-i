package model;

import java.awt.Frame;
import java.awt.FileDialog;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class CustomerDashboardController {

    // ── Services used to read/write data files ────────────────────────────
    private final VehicleService            vehicleService;
    private final ProfilePicStorage         profilePicStorage;
    private final BackgroundImageStorage    backgroundImageStorage;
    private final CustomerProfileController profileController;

    // ── Callback interface so this controller can update the UI ───────────
    private final DashboardView view;

    // ═══════════════════════════════════════════════════════════════════════
    //  VIEW INTERFACE  — one method per UI element, grouped by screen area
    // ═══════════════════════════════════════════════════════════════════════
    public interface DashboardView {

        // ── (A) SESSION — who is logged in ───────────────────────────────

        /** Returns the full logged-in User object. */
        User getLoggedInUserObj();

        /** Returns the logged-in user's display name. */
        String getLoggedInUser();

        /** Updates the logged-in name stored in the session. */
        void setLoggedInUser(String name);

        /** Updates the full User object stored in the session. */
        void setLoggedInUserObj(User user);

        // ── (B) HEADER — top bar (name label + avatar circle) ────────────

        /** Updates the name shown in the top header bar. */
        void updateHeaderName(String name);

        /** Updates which avatar colour/icon is shown in the header circle. */
        void updateAvatarIndex(int index);

        /** Repaints the small circular avatar in the header bar. */
        void repaintAvatar();

        // ── (C) PROFILE PAGE — View Profile (opened from header dropdown) ─

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

        /** Switches the profile card to edit mode (shows text fields, hides labels). */
        void showEditMode();

        /** Switches the profile card back to read mode (shows labels, hides fields). */
        void showReadMode();

        /** Returns the text currently typed in the name text field. */
        String getNameFieldText();

        /** Returns the text currently typed in the email text field. */
        String getEmailFieldText();

        /** Pre-fills the name text field with the given value. */
        void setNameFieldText(String name);

        /** Pre-fills the email text field with the given value. */
        void setEmailFieldText(String email);

        // ── (D) VEHICLE LIST — inside Profile Page ────────────────────────

        /** Tells the view to rebuild the vehicle list panel with the given vehicles. */
        void rebuildVehicleList(List<String[]> vehicles);

        // ── (E) SHARED UTILITIES ──────────────────────────────────────────

        /** Shows a popup message dialog to the user. */
        void showMessage(String message, String title, int messageType);

        /** Returns the main application window (used as a parent for dialogs). */
        java.awt.Window getWindow();

        /** Navigates back to the login/onboarding screen. */
        void navigateToLogin();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════════════
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

    // ═══════════════════════════════════════════════════════════════════════
    //  (A) SESSION / USER REFRESH
    // ═══════════════════════════════════════════════════════════════════════

    public void refreshUser() {

        // Step 1: HEADER — update the name shown in the top bar
        String name = view.getLoggedInUser();
        if (name == null || name.isEmpty()) {
            name = "Customer";
        }
        view.updateHeaderName(name);

        // Step 2: HEADER — update the avatar colour circle
        User user = view.getLoggedInUserObj();
        if (user != null) {
            view.updateAvatarIndex(user.getProfilePicture());
        }
        view.repaintAvatar();

        // Step 3: PROFILE PAGE — update name / email / role labels
        if (user != null) {
            view.updateProfileNameLabel(user.getName());
            view.updateProfileEmailLabel(user.getEmail());

            // Capitalise first letter of role: "customer" → "Customer"
            String role = user.getRole();
            String capitalisedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
            view.updateProfileRoleLabel(capitalisedRole);

            // Step 4: PROFILE PAGE — load saved images from disk and push to UI
            BufferedImage profileImage = profilePicStorage.loadImage(user.getUserId());
            BufferedImage bannerImage  = backgroundImageStorage.loadImage(user.getUserId());

            view.setProfileImage(profileImage);
            view.setBannerImage(bannerImage);
            view.repaintProfilePic();
            view.repaintBanner();
            view.repaintAvatar();
        }

        // Step 5: VEHICLE LIST — reload from vehicles.txt
        refreshVehicleList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  (B) HEADER — PROFILE DROPDOWN
    // ═══════════════════════════════════════════════════════════════════════

    public void handleViewProfile() {
        
    }

    public void handleLogout() {
        view.setLoggedInUser("");
        view.setLoggedInUserObj(null);
        view.navigateToLogin();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  (C) PROFILE PAGE — PERSONAL INFORMATION (Edit / Save / Cancel)
    // ═══════════════════════════════════════════════════════════════════════

    public void enterEditMode() {
        view.setNameFieldText(profileController.getCurrentName());
        view.setEmailFieldText(profileController.getCurrentEmail());
        view.showEditMode();
    }

    /**
     * Called when the user clicks "Cancel" — discards changes, returns to read mode.
     */
    public void exitEditMode() {
        view.showReadMode(); 
    }

    /**
     * Called when the user clicks "Save" on the profile card.
     * Validates the new values and writes them to accounts.txt if changed.
     */
    public void handleSave() {
        // Step 1: Read what the user typed
        String newName  = view.getNameFieldText().trim();
        String newEmail = view.getEmailFieldText().trim();

        // Step 2: If nothing changed, inform the user and exit edit mode
        if (profileController.hasNoChanges(newName, newEmail)) {
            view.showMessage("No changes were made.", "No Changes",
                    JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }

        // Step 3: Try to save — profileController handles all validation rules
        try {
            boolean saved = profileController.saveProfile(newName, newEmail);

            if (saved) {
                exitEditMode();
                refreshUser();          // update every displayed label with the new values
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

    // ═══════════════════════════════════════════════════════════════════════
    //  (C) PROFILE PAGE — IMAGE UPLOADERS
    // ═══════════════════════════════════════════════════════════════════════

    public void chooseProfileImage() {
        User user = view.getLoggedInUserObj();
        if (user == null) return;

        // Open the native file picker (Windows / macOS file dialog)
        FileDialog fileChooser = new FileDialog(
                (Frame) view.getWindow(),
                "Choose Profile Picture",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");  // image files only
        fileChooser.setVisible(true);

        if (fileChooser.getFile() == null) return;              // user cancelled

        try {
            java.io.File selectedFile = new java.io.File(
                    fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                view.showMessage("Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save to disk using userId as filename (e.g. "C3.png")
            boolean saved = profilePicStorage.saveImage(user.getUserId(), image);
            if (!saved) {
                view.showMessage("Failed to save profile picture.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Push to UI immediately — no restart required
            view.setProfileImage(image);
            view.repaintProfilePic();
            view.repaintAvatar();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            view.showMessage("Failed to read the selected image.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

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

            // Save using userId — consistent with how other storage services work
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

    // ═══════════════════════════════════════════════════════════════════════
    //  (D) VEHICLE LIST — inside Profile Page
    // ═══════════════════════════════════════════════════════════════════════

    public void refreshVehicleList() {
        User user = view.getLoggedInUserObj();

        if (user == null) {
            // No user logged in — show empty list ("No vehicles registered.")
            view.rebuildVehicleList(new java.util.ArrayList<>());
            return;
        }

        List<String[]> allVehicles = vehicleService.getVehiclesByUserId(user.getUserId());
        view.rebuildVehicleList(allVehicles);
    }

    public boolean handleAddVehicle(String vehicleType, String plate,
                                    String brand, String year, String colour) {

        // Validate all 5 fields before saving
        String validationError = validateVehicleFields(vehicleType, plate, brand, year, colour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        boolean saved = vehicleService.addVehicle(
                user.getUserId(),   // e.g. "C3"
                vehicleType,        // "Car" or "Motor"
                plate, brand, year, colour);

        if (saved) {
            refreshVehicleList();   // reload so the new vehicle appears immediately
        } else {
            view.showMessage("Failed to add vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return saved;
    }

    public boolean handleUpdateVehicle(String oldPlate, String newType, String newPlate,
                                       String newBrand, String newYear, String newColour) {

        String validationError = validateVehicleFields(newType, newPlate, newBrand, newYear, newColour);
        if (validationError != null) {
            view.showMessage(validationError, "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }

        User user = view.getLoggedInUserObj();
        if (user == null) return false;

        boolean updated = vehicleService.updateVehicle(
                user.getUserId(), oldPlate,
                newType, newPlate, newBrand, newYear, newColour);

        if (updated) {
            refreshVehicleList();   // reload to show the updated vehicle immediately
        } else {
            view.showMessage("Failed to update vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
        }

        return updated;
    }

    public void handleRemoveVehicle(String plate) {
        int choice = JOptionPane.showConfirmDialog(
                view.getWindow(),
                "Are you sure you want to remove this vehicle?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            User user = view.getLoggedInUserObj();

            if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                refreshVehicleList();
            } else {
                view.showMessage("Failed to remove vehicle.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        // If user clicked NO → do nothing, vehicle stays
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  (E) VEHICLE FIELD VALIDATION — shared by Add and Edit
    //  All 5 fields must pass before any write to disk happens.
    // ═══════════════════════════════════════════════════════════════════════

    public String validateVehicleFields(String vehicleType, String plate,
                                        String brand, String year, String colour) {

        // ── Vehicle Type ──────────────────────────────────────────────────
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            return "Please select a vehicle type (Car or Motor).";
        }
        if (!vehicleType.equals("Car") && !vehicleType.equals("Motor")) {
            return "Vehicle type must be either 'Car' or 'Motor'.";
        }

        // ── Car Plate ─────────────────────────────────────────────────────
        if (plate.isEmpty()) {
            return "Car Plate cannot be empty.";
        }
        if (!plate.matches("[a-zA-Z0-9 ]+")) {
            return "Car Plate can only contain letters and numbers (no special characters).";
        }
        boolean plateHasLetter = plate.matches(".*[a-zA-Z].*");
        boolean plateHasNumber = plate.matches(".*[0-9].*");
        if (!plateHasLetter || !plateHasNumber) {
            return "Car Plate must contain both letters and numbers (e.g. WXY1234).";
        }

        // ── Brand / Model ─────────────────────────────────────────────────
        if (brand.isEmpty()) {
            return "Brand / Model cannot be empty.";
        }
        if (!brand.matches("[a-zA-Z0-9 ]+")) {
            return "Brand / Model can only contain letters and numbers (no special characters).";
        }

        // ── Year ──────────────────────────────────────────────────────────
        if (year.isEmpty()) {
            return "Year cannot be empty.";
        }
        if (!year.matches("\\d{4}")) {
            return "Year must be exactly 4 digits (e.g. 2025).";
        }

        // ── Colour ────────────────────────────────────────────────────────
        if (colour.isEmpty()) {
            return "Colour cannot be empty.";
        }
        if (!colour.matches("[a-zA-Z ]+")) {
            return "Colour can only contain letters (e.g. White, Dark Blue).";
        }

        return null;    // ALL fields passed — safe to save
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONVENIENCE GETTERS
    // ═══════════════════════════════════════════════════════════════════════

    /** Returns the profile controller (used by the view for direct profile access). */
    public CustomerProfileController getProfileController() {
        return profileController;
    }

    /** Returns the vehicle service (used by the view e.g. for "View All" dialogs). */
    public VehicleService getVehicleService() {
        return vehicleService;
    }
}