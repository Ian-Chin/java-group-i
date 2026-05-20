package model;

import java.util.List;

/**
 * ============================================================
 * VehicleSectionController.java — My Vehicles Business Logic
 * ============================================================
 *
 * Vehicle array from VehicleService has 6 elements:
 *   [0] vehicleID   [1] vehicleType  [2] plate
 *   [3] brand       [4] year         [5] colour
 *
 * handleAdd / handleEdit fields array has 5 elements:
 *   [0] vehicleType  [1] plate  [2] brand  [3] year  [4] colour
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
     * Each String[] has 6 elements:
     *   [0] vehicleID  [1] vehicleType  [2] plate
     *   [3] brand      [4] year         [5] colour
     */
    public List<String[]> loadVehiclesForUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return vehicleService.getVehiclesByUserId(userId);
    }

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
     * fields layout:
     *   [0] vehicleType   e.g. "Car" or "Motor"
     *   [1] plate         e.g. "WXY1234"
     *   [2] brand         e.g. "Toyota Vios"
     *   [3] year          e.g. "2022"
     *   [4] colour        e.g. "White"
     */
    @Override
    public boolean handleAdd(String[] fields) {
        // ── Validate all five fields ─────────────────────────────
        String error = validateFields(fields);
        if (error != null) { // if there is an error message
            view.showMessage(error, "Validation Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }

        // ── Get logged-in user ───────────────────────────────────
        User user = view.getLoggedInUser();
        if (user == null) return false;

        boolean saved = vehicleService.addVehicle(
                user.getUserId(),
                fields[0], // vehicleType
                fields[1], // plate
                fields[2], // brand
                fields[3], // year
                fields[4]  // colour
        );

        if (saved) {
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
     * This overload is used by ViewProfile where the confirmation
     * dialog is shown inline (JOptionPane.showConfirmDialog) in
     * the button listener before calling this method.
     */
    public boolean deleteVehicleDirectly(String userId, String plate) {
        return vehicleService.deleteVehicle(userId, plate);
    }

    // =========================================================
    // VALIDATE — checks all 5 vehicle fields
    // =========================================================

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