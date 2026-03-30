package model;

import java.util.List;

/**
 * VehicleSectionController — handles all logic for the My Vehicle section.
 *
 * Vehicle array from VehicleService has 6 elements:
 *   [0] vehicleID   [1] vehicleType  [2] plate
 *   [3] brand       [4] year         [5] colour
 *
 * handleAdd / handleEdit fields array has 5 elements:
 *   [0] vehicleType  [1] plate  [2] brand  [3] year  [4] colour
 *
 * refreshList() passes ALL vehicles to the dashboard — no limit here.
 * CustomerDashboard.rebuildVehicleList() shows only the first 2 on screen
 * and adds a "View All" button when there are more than 2.
 *
 * NEW METHODS added (moved from CustomerDashboard):
 *   getAllVehiclesForUser() — returns every vehicle for the logged-in user
 *   getVehicleLabel()      — returns "Car · LIN110" style label for a vehicleId
 */
public class VehicleSectionController implements SectionController {

    private final VehicleService vehicleService;
    private final SectionView    view;

    public interface SectionView {
        User            getLoggedInUser();
        void            rebuildList(List<String[]> items);
        void            showMessage(String message, String title, int messageType);
        java.awt.Window getWindow();
    }

    public VehicleSectionController(VehicleService vehicleService, SectionView view) {
        this.vehicleService = vehicleService;
        this.view           = view;
    }

    // ═══════════════════════════════════════════════════════════════
    // REFRESH — reads vehicles.txt and tells the UI to update
    // ═══════════════════════════════════════════════════════════════

    /**
     * Reads ALL vehicles from vehicles.txt and passes the full list
     * to the dashboard for display. The dashboard decides how many
     * to show (top 2) and whether to add a "View All" button.
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

    // ═══════════════════════════════════════════════════════════════
    // GET ALL VEHICLES — used by the "View All" dialog
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns ALL vehicles for the currently logged-in user.
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

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE LABEL — converts vehicleId to a display string
    // ═══════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════
    // ADD — validates and saves a new vehicle
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and adds a new vehicle.
     * fields[0]=type  fields[1]=plate  fields[2]=brand  fields[3]=year  fields[4]=colour
     */
    @Override
    public boolean handleAdd(String[] fields) {
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
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
        if (saved) refreshList();
        else view.showMessage("Failed to add vehicle.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        return saved;
    }

    // ═══════════════════════════════════════════════════════════════
    // EDIT — validates and updates an existing vehicle
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates and updates an existing vehicle.
     * id = old plate number (identifies which record to update).
     * fields layout is the same as handleAdd.
     */
    @Override
    public boolean handleEdit(String id, String[] fields) {
        String error = validateFields(fields);
        if (error != null) {
            view.showMessage(error, "Validation Error", javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
        User user = view.getLoggedInUser();
        if (user == null) return false;

        boolean updated = vehicleService.updateVehicle(
                user.getUserId(),
                id,        // old plate
                fields[0], // new vehicleType
                fields[1], // new plate
                fields[2], // new brand
                fields[3], // new year
                fields[4]  // new colour
        );
        if (updated) refreshList();
        else view.showMessage("Failed to update vehicle.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
        return updated;
    }

    // ═══════════════════════════════════════════════════════════════
    // DELETE — confirms then removes a vehicle
    // ═══════════════════════════════════════════════════════════════

    /**
     * Confirms then deletes the vehicle with the given plate number.
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

    // ═══════════════════════════════════════════════════════════════
    // VALIDATE — checks all 5 vehicle fields
    // ═══════════════════════════════════════════════════════════════

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