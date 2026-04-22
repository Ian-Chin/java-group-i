package model;

import java.util.List;

/**
 * ============================================================
 * VehicleSectionController.java — My Vehicles Business Logic
 * ============================================================
 *
 * Handles ALL business logic for the My Vehicles card in
 * ViewProfile.java and the Customer Dashboard.
 *
 * ViewProfile.java only calls these methods and handles the
 * resulting UI changes.
 *
 * Vehicle array from VehicleService has 6 elements:
 *   [0] vehicleID   [1] vehicleType  [2] plate
 *   [3] brand       [4] year         [5] colour
 *
 * handleAdd / handleEdit fields array has 5 elements:
 *   [0] vehicleType  [1] plate  [2] brand  [3] year  [4] colour
 *
 * Methods moved FROM ViewProfile.java (inline logic):
 *   - loadVehiclesForUser()  : reads vehicles.txt for the logged-in user
 *   - handleAdd()            : validates + saves a new vehicle
 *   - handleEdit()           : validates + updates an existing vehicle
 *   - handleDelete()         : deletes a vehicle after confirmation
 *   - validateFields()       : shared validation for add and edit
 *
 * Methods that were already here (unchanged):
 *   - refreshList()              : tells the UI to rebuild the list
 *   - getAllVehiclesForUser()     : returns every vehicle for a userId
 *   - getVehicleLabel()          : "Car · LIN110" style label
 *
 * refreshList() passes ALL vehicles to the view — no limit here.
 * CustomerDashboard.rebuildVehicleList() shows only the first 2 on
 * screen and adds a "View All" button when there are more than 2.
 * ============================================================
 */
public class VehicleSectionController implements SectionController {

    // =========================================================
    // FIELDS
    // =========================================================

    private final VehicleService vehicleService;
    private final SectionView    view;

    // =========================================================
    // SectionView INTERFACE
    // =========================================================

    public interface SectionView {
        User            getLoggedInUser();
        void            rebuildList(List<String[]> items);
        void            showMessage(String message, String title, int messageType);
        java.awt.Window getWindow();
    }

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public VehicleSectionController(VehicleService vehicleService, SectionView view) {
        this.vehicleService = vehicleService;
        this.view           = view;
    }

    // =========================================================
    // LOAD — reads vehicles.txt and returns rows for the user
    // =========================================================

    /**
     * Reads ALL vehicles from vehicles.txt for the currently
     * logged-in user and returns them as a list of String arrays.
     *
     * MOVED FROM: ViewProfile.loadVehicles() (was an inline call
     * directly to vehicleService inside ViewProfile)
     *
     * Each String[] has 6 elements:
     *   [0] vehicleID  [1] vehicleType  [2] plate
     *   [3] brand      [4] year         [5] colour
     *
     * Returns an empty list when no user is logged in or when the
     * user has no registered vehicles.
     *
     * Called by ViewProfile.java:
     *   - after the page builds (SwingUtilities.invokeLater)
     *   - after a successful Add, Edit, or Delete
     *   - inside refreshUser() to keep the list in sync
     *
     * @param userId  the logged-in user's ID (e.g. "C3")
     * @return list of vehicle rows; never null
     */
    public List<String[]> loadVehiclesForUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return vehicleService.getVehiclesByUserId(userId);
    }

    // =========================================================
    // REFRESH — reads vehicles.txt and tells the UI to update
    // =========================================================

    /**
     * Reads ALL vehicles from vehicles.txt and passes the full list
     * to the view for display. The view decides how many to show
     * (top 2 on the dashboard) and whether to add a "View All" button.
     */
    @Override
    public void refreshList() {
        User user = view.getLoggedInUser();
        if (user == null) {
            view.rebuildList(new java.util.ArrayList<>());
            return;
        }
        // Pass ALL — no subList limit here
        view.rebuildList(vehicleService.getVehiclesByUserId(user.getUserId()));
    }

    // =========================================================
    // GET ALL VEHICLES — used by the "View All" dialog
    // =========================================================

    /**
     * Returns ALL vehicles for the given userId.
     * Used by CustomerDashboard to populate the "All My Vehicles" dialog.
     *
     * Each String[] has 6 elements:
     *   [0] vehicleID  [1] vehicleType  [2] plate  [3] brand  [4] year  [5] colour
     *
     * @param userId  the logged-in user's ID e.g. "C3"
     * @return list of vehicle rows
     */
    public List<String[]> getAllVehiclesForUser(String userId) {
        return vehicleService.getVehiclesByUserId(userId);
    }

    // =========================================================
    // VEHICLE LABEL — converts vehicleId to a display string
    // =========================================================

    /**
     * Looks up a vehicle by its ID and returns a short display label.
     * Example: "Car · LIN110" or "Motor · AJH1312"
     * Returns the vehicleId itself as a fallback if not found.
     *
     * @param vehicleId  e.g. "V4"
     * @return display label string
     */
    public String getVehicleLabel(String vehicleId) {
        return vehicleService.getVehiclePlate(vehicleId);
    }

    // =========================================================
    // ADD — validates and saves a new vehicle
    // =========================================================

    /**
     * Validates and adds a new vehicle for the logged-in user.
     *
     * BUSINESS LOGIC MOVED FROM: ViewProfile.buildAddForm() —
     * the doSave Runnable that was wired to the Save button
     * inside buildAddForm() has been extracted here so that
     * ViewProfile only handles UI (showing/hiding the form,
     * scrolling, clearing fields).
     *
     * fields layout:
     *   [0] vehicleType   e.g. "Car" or "Motor"
     *   [1] plate         e.g. "WXY1234"
     *   [2] brand         e.g. "Toyota Vios"
     *   [3] year          e.g. "2022"
     *   [4] colour        e.g. "White"
     *
     * @param fields  5-element String array from the add form
     * @return true if the vehicle was saved successfully
     */
    @Override
    public boolean handleAdd(String[] fields) {
        // ── Validate all five fields ─────────────────────────────
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // ── Get logged-in user ───────────────────────────────────
        User user = view.getLoggedInUser();
        if (user == null) return false;

        // ── Persist to vehicles.txt via VehicleService ───────────
        boolean saved = vehicleService.addVehicle(
                user.getUserId(),
                fields[0], // vehicleType
                fields[1], // plate
                fields[2], // brand
                fields[3], // year
                fields[4]  // colour
        );

        if (saved) {
            // refreshList() tells the view to reload and rebuild the vehicle rows
            refreshList();
        } else {
            view.showMessage("Failed to add vehicle.", "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return saved;
    }

    // =========================================================
    // EDIT — validates and updates an existing vehicle
    // =========================================================

    /**
     * Validates and updates an existing vehicle.
     *
     * BUSINESS LOGIC MOVED FROM: ViewProfile.buildVehicleRow() —
     * the doSave Runnable that was wired to the Save button in
     * each vehicle row's edit card has been extracted here.
     * ViewProfile only handles flipping the CardLayout card back
     * to "display" and reloading the list.
     *
     * @param id      old plate number — identifies which record to update
     * @param fields  5-element String array with the new values
     *                (same layout as handleAdd)
     * @return true if the vehicle was updated successfully
     */
    @Override
    public boolean handleEdit(String id, String[] fields) {
        // ── Validate all five fields ─────────────────────────────
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // ── Get logged-in user ───────────────────────────────────
        User user = view.getLoggedInUser();
        if (user == null) return false;

        // ── Persist update to vehicles.txt via VehicleService ────
        boolean updated = vehicleService.updateVehicle(
                user.getUserId(),
                id,        // old plate number
                fields[0], // new vehicleType
                fields[1], // new plate
                fields[2], // new brand
                fields[3], // new year
                fields[4]  // new colour
        );

        if (updated) {
            // refreshList() tells the view to reload and rebuild
            refreshList();
        } else {
            view.showMessage("Failed to update vehicle.", "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
        return updated;
    }

    // =========================================================
    // DELETE — confirms then removes a vehicle
    // =========================================================

    /**
     * Shows a confirmation dialog, then deletes the vehicle with
     * the given plate number from vehicles.txt.
     *
     * BUSINESS LOGIC MOVED FROM: ViewProfile.buildVehicleRow() —
     * the removeBtn ActionListener that called JOptionPane +
     * vehicleService.deleteVehicle() directly inside ViewProfile
     * has been extracted here.
     *
     * ViewProfile still wires the Remove button listener but calls
     * this method instead of performing deletion itself.
     *
     * @param id  the plate number of the vehicle to delete
     *            (used as the unique identifier in vehicles.txt)
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
     * Deletes a vehicle directly (no confirmation dialog).
     *
     * MOVED FROM: ViewProfile.buildVehicleRow() — the removeBtn
     * listener that called vehicleService.deleteVehicle() with
     * userId + plate directly.
     *
     * This overload is used by ViewProfile where the confirmation
     * dialog is shown inline (JOptionPane.showConfirmDialog) in
     * the button listener before calling this method.
     *
     * @param userId  the logged-in user's ID
     * @param plate   the plate number of the vehicle to delete
     * @return true if deleted successfully
     */
    public boolean deleteVehicleDirectly(String userId, String plate) {
        return vehicleService.deleteVehicle(userId, plate);
    }

    // =========================================================
    // VALIDATE — checks all 5 vehicle fields
    // =========================================================

    /**
     * Validates all 5 vehicle fields.
     * Returns an error message string, or null if everything is valid.
     *
     * Rules:
     *   vehicleType — must be "Car" or "Motor"
     *   plate       — letters + numbers only, must have at least one of each
     *   brand       — letters, numbers and spaces only
     *   year        — exactly 4 digits
     *   colour      — letters and spaces only
     *
     * Called by both handleAdd() and handleEdit() before persisting.
     *
     * @param fields  5-element array: [type, plate, brand, year, colour]
     * @return error message string, or null if all fields are valid
     */
    @Override
    public String validateFields(String[] fields) {
        String type   = fields[0];
        String plate  = fields[1];
        String brand  = fields[2];
        String year   = fields[3];
        String colour = fields[4];

        // ── Vehicle Type ──────────────────────────────────────────
        if (type == null || type.isEmpty())
            return "Please select a vehicle type (Car or Motor).";
        if (!type.equals("Car") && !type.equals("Motor"))
            return "Vehicle type must be either 'Car' or 'Motor'.";

        // ── Car Plate ─────────────────────────────────────────────
        if (plate.isEmpty())
            return "Car Plate cannot be empty.";
        if (!plate.matches("[a-zA-Z0-9 ]+"))
            return "Car Plate can only contain letters and numbers (no special characters).";
        if (!plate.matches(".*[a-zA-Z].*") || !plate.matches(".*[0-9].*"))
            return "Car Plate must contain both letters and numbers (e.g. WXY1234).";

        // ── Brand / Model ─────────────────────────────────────────
        if (brand.isEmpty())
            return "Brand / Model cannot be empty.";
        if (!brand.matches("[a-zA-Z0-9 ]+"))
            return "Brand / Model can only contain letters and numbers (no special characters).";

        // ── Year ──────────────────────────────────────────────────
        if (year.isEmpty())
            return "Year cannot be empty.";
        if (!year.matches("\\d{4}"))
            return "Year must be exactly 4 digits (e.g. 2025).";

        // ── Colour ────────────────────────────────────────────────
        if (colour.isEmpty())
            return "Colour cannot be empty.";
        if (!colour.matches("[a-zA-Z ]+"))
            return "Colour can only contain letters (e.g. White, Dark Blue).";

        return null; // null = all valid — safe to save
    }
}