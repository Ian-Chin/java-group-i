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
         *
         * IMPORTANT: The FULL list is passed here (not just top 3).
         * CustomerDashboard decides how many to show on screen (top 3)
         * and whether to show a "View All" button.
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
     *
     * KEY FIX: We now pass ALL vehicles (no limit here).
     * The CustomerDashboard.rebuildVehicleList() method decides to show
     * only the first 3, and shows a "View All" button if there are more.
     *
     * Before this fix, we called allVehicles.subList(0, limit) here,
     * which meant rebuildVehicleList() only ever got 3 or fewer items
     * and could never know the total count to show "View All".
     *
     * Uses getUserId() instead of getEmail() to look up vehicles.
     */
    @Override
    public void refreshList() {
        User user = view.getLoggedInUser();

        // No user logged in — pass empty list so UI shows "No vehicles registered."
        if (user == null) {
            view.rebuildList(new java.util.ArrayList<>());
            return;
        }

        // Read ALL vehicles belonging to this user from vehicles.txt.
        // We do NOT limit to 3 here. CustomerDashboard handles the display limit.
        List<String[]> allVehicles = vehicleService.getVehiclesByUserId(user.getUserId());

        // Pass the FULL list to the dashboard
        view.rebuildList(allVehicles);
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
        boolean saved = vehicleService.addVehicle(
                user.getUserId(), // customer ID e.g. "C3"
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

        // Step 3: Update the record in place
        boolean updated = vehicleService.updateVehicle(
                user.getUserId(), // customer ID e.g. "C3"
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
        int choice = javax.swing.JOptionPane.showConfirmDialog(
                view.getWindow(),
                "Are you sure you want to remove this vehicle?",
                "Confirm Remove",
                javax.swing.JOptionPane.YES_NO_OPTION,
                javax.swing.JOptionPane.QUESTION_MESSAGE);

        if (choice == javax.swing.JOptionPane.YES_OPTION) {
            User user = view.getLoggedInUser();
            if (user != null && vehicleService.deleteVehicle(user.getUserId(), id)) {
                refreshList();
            } else {
                view.showMessage("Failed to remove vehicle.", "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Validates all four vehicle fields.
     *
     * Rules:
     *  fields[0] — Car Plate  : must have BOTH letters AND numbers, no special characters
     *  fields[1] — Brand/Model: letters and/or numbers only, no special characters
     *  fields[2] — Year       : exactly 4 digits, numbers only
     *  fields[3] — Colour     : letters and spaces only
     *
     * @return error message string if invalid, null if all fields are valid
     */
    @Override
    public String validateFields(String[] fields) {
        String plate  = fields[0];
        String brand  = fields[1];
        String year   = fields[2];
        String colour = fields[3];

        if (plate.isEmpty()) return "Car Plate cannot be empty.";
        if (!plate.matches("[a-zA-Z0-9 ]+"))
            return "Car Plate can only contain letters and numbers (no special characters).";
        boolean plateHasLetter = plate.matches(".*[a-zA-Z].*");
        boolean plateHasNumber = plate.matches(".*[0-9].*");
        if (!plateHasLetter || !plateHasNumber)
            return "Car Plate must contain both letters and numbers (e.g. WXY1234).";

        if (brand.isEmpty()) return "Brand / Model cannot be empty.";
        if (!brand.matches("[a-zA-Z0-9 ]+"))
            return "Brand / Model can only contain letters and numbers (no special characters).";

        if (year.isEmpty()) return "Year cannot be empty.";
        if (!year.matches("\\d{4}")) return "Year must be exactly 4 digits (e.g. 2025).";

        if (colour.isEmpty()) return "Colour cannot be empty.";
        if (!colour.matches("[a-zA-Z ]+"))
            return "Colour can only contain letters (e.g. White, Dark Blue).";

        return null; // all fields are valid
    }
}