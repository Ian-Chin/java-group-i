package model;

import java.util.List;

/**
 * VehicleSectionController handles all logic for the My Vehicle section.
 *
 * Java OOP principles used:
 *  - Inheritance  : implements SectionController interface
 *  - Polymorphism : the four methods have vehicle-specific behaviour
 *  - Encapsulation: vehicle validation rules and file operations are hidden
 *  - Abstraction  : the dashboard only calls handleAdd(fields) without
 *                   knowing HOW vehicles are validated or saved
 *
 * Field order for add and edit:
 *   fields[0] = plate        (car plate number, e.g. "WXY1234")
 *   fields[1] = brand        (brand / model,    e.g. "Toyota Vios")
 *   fields[2] = year         (year of manufacture, e.g. "2025")
 *   fields[3] = colour       (colour,            e.g. "White")
 */
public class VehicleSectionController implements SectionController {

    // ── Services and view callback ────────────────────────────────
    private final VehicleService vehicleService;
    private final SectionView    view;

    /**
     * SectionView is a small callback interface so this controller can
     * update the UI without importing any Swing classes.
     *
     * CustomerDashboard creates an anonymous implementation of this
     * when it creates the VehicleSectionController.
     */
    public interface SectionView {

        /** Returns the currently logged-in user, or null if not logged in. */
        User getLoggedInUser();

        /**
         * Rebuilds the vehicle list panel with the given vehicles.
         * Each String array contains: [vehicleID, plate, brand, year, colour]
         */
        void rebuildList(List<String[]> items);

        /** Shows a popup message to the user. */
        void showMessage(String message, String title, int messageType);

        /** Returns the main application window (used as parent for dialogs). */
        java.awt.Window getWindow();
    }

    // ── Constructor ───────────────────────────────────────────────

    /**
     * Creates the VehicleSectionController.
     *
     * @param vehicleService  reads and writes vehicles.txt
     * @param view            callback used to update the vehicle list on screen
     */
    public VehicleSectionController(VehicleService vehicleService, SectionView view) {
        this.vehicleService = vehicleService;
        this.view           = view;
    }

    // ═══════════════════════════════════════════════════════════════
    // SectionController INTERFACE — all four methods implemented below
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads vehicles.txt and tells the UI to rebuild the vehicle list.
     * Only shows the first 3 vehicles (top 3).
     *
     * Uses getUserId() instead of getEmail() to look up vehicles,
     * because vehicles.txt now stores user ID (e.g. "C3") not email.
     */
    @Override
    public void refreshList() {
        User user = view.getLoggedInUser();

        // No user logged in — pass empty list so UI shows "No vehicles registered."
        if (user == null) {
            view.rebuildList(new java.util.ArrayList<>());
            return;
        }

        // Read all vehicles belonging to this user from vehicles.txt
        // Using getUserId() (e.g. "C3") instead of getEmail()
        List<String[]> allVehicles = vehicleService.getVehiclesByUserId(user.getUserId());

        // Only show the first 3
        int limit = Math.min(3, allVehicles.size());
        view.rebuildList(allVehicles.subList(0, limit));
    }

    /**
     * Validates and adds a new vehicle to vehicles.txt.
     *
     * fields[0] = plate
     * fields[1] = brand
     * fields[2] = year
     * fields[3] = colour
     *
     * @return true if added successfully, false if validation failed or save failed
     */
    @Override
    public boolean handleAdd(String[] fields) {
        // Step 1: Validate all fields using vehicle-specific rules
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Step 2: Get the logged-in user
        User user = view.getLoggedInUser();
        if (user == null) return false;

        // Step 3: Save the new vehicle to vehicles.txt
        // Using getUserId() (e.g. "C3") instead of getEmail()
        boolean saved = vehicleService.addVehicle(
                user.getUserId(), // was user.getEmail()
                fields[0],        // plate
                fields[1],        // brand
                fields[2],        // year
                fields[3]         // colour
        );

        if (saved) {
            refreshList(); // reload the list so the new vehicle appears immediately
        } else {
            view.showMessage("Failed to add vehicle.", "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        return saved;
    }

    /**
     * Validates and updates an existing vehicle in place (preserves order in file).
     *
     * @param id      the OLD plate number — used to find the record to update
     * @param fields  new values: [plate, brand, year, colour]
     * @return true if updated successfully, false otherwise
     */
    @Override
    public boolean handleEdit(String id, String[] fields) {
        // Step 1: Validate the new values
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // Step 2: Get the logged-in user
        User user = view.getLoggedInUser();
        if (user == null) return false;

        // Step 3: Update the record in place — keeps original position in vehicles.txt
        // Using getUserId() (e.g. "C3") instead of getEmail()
        boolean updated = vehicleService.updateVehicle(
                user.getUserId(), // was user.getEmail()
                id,               // old plate (identifies which record to update)
                fields[0],        // new plate
                fields[1],        // new brand
                fields[2],        // new year
                fields[3]         // new colour
        );

        if (updated) {
            refreshList(); // reload to show the updated vehicle
        } else {
            view.showMessage("Failed to update vehicle.", "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }

        return updated;
    }

    /**
     * Asks for confirmation then removes the vehicle with the given plate number.
     *
     * @param id  the plate number of the vehicle to remove
     */
    @Override
    public void handleDelete(String id) {
        // Show a confirmation popup before deleting
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                view.getWindow(),
                "Are you sure you want to remove this vehicle?",
                "Confirm Remove",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);

        if (choice == javax.swing.JOptionPane.YES_OPTION) {
            User user = view.getLoggedInUser();

            // Using getUserId() (e.g. "C3") instead of getEmail()
            if (user != null && vehicleService.deleteVehicle(user.getUserId(), id)) {
                refreshList(); // reload to remove the deleted vehicle from the list
            } else {
                view.showMessage("Failed to remove vehicle.", "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
        // If the user clicked No, the dialog just closes — nothing is deleted
    }

    /**
     * Validates all four vehicle fields.
     *
     * Rules:
     *  fields[0] — Car Plate  : must have BOTH letters AND numbers, no special characters
     *                           ✅ "WXY1234"     ❌ "ABC"    ❌ "1234"   ❌ "WXY-123"
     *  fields[1] — Brand/Model: letters and/or numbers only, no special characters
     *                           ✅ "Toyota Vios" ✅ "BMW i5" ❌ "BMW-i5"
     *  fields[2] — Year       : exactly 4 digits, numbers only
     *                           ✅ "2025"        ❌ "25"     ❌ "202A"
     *  fields[3] — Colour     : letters and spaces only, no numbers or special characters
     *                           ✅ "White"       ✅ "Dark Blue" ❌ "Blue2" ❌ "Blue-Red"
     *
     * @return error message string if invalid, null if all fields are valid
     */
    @Override
    public String validateFields(String[] fields) {
        String plate  = fields[0];
        String brand  = fields[1];
        String year   = fields[2];
        String colour = fields[3];

        // ── Car Plate ─────────────────────────────────────────────
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

        // ── Brand / Model ─────────────────────────────────────────
        if (brand.isEmpty()) {
            return "Brand / Model cannot be empty.";
        }
        if (!brand.matches("[a-zA-Z0-9 ]+")) {
            return "Brand / Model can only contain letters and numbers (no special characters).";
        }

        // ── Year ──────────────────────────────────────────────────
        if (year.isEmpty()) {
            return "Year cannot be empty.";
        }
        if (!year.matches("\\d{4}")) {
            return "Year must be exactly 4 digits (e.g. 2025).";
        }

        // ── Colour ────────────────────────────────────────────────
        if (colour.isEmpty()) {
            return "Colour cannot be empty.";
        }
        if (!colour.matches("[a-zA-Z ]+")) {
            return "Colour can only contain letters (e.g. White, Dark Blue).";
        }

        return null; // null means all fields passed validation — no errors
    }
}