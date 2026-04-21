package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VehicleService — reads and writes vehicles.txt.
 *
 * File format (7 columns):
 *   vehicleID , userID , vehicleType , plate , brand , year , colour
 *
 * Example:
 *   V3,C3,Car,LIN110,Mercedes AMG Coupe,2025,White
 *   V7,C3,Motor,AJH1312,Yamaha,2013,Blue
 *
 * getVehiclesByUserId() returns a 6-element array per vehicle:
 *   [0] vehicleID    [1] vehicleType  [2] plate
 *   [3] brand        [4] year         [5] colour
 *
 * METHODS MOVED FROM AppointmentSectionController.java:
 *  - getVehicleLabel()      : converts a vehicleId to "Car · LIN110" display string
 *                             (was a pass-through wrapper — belongs here because it
 *                              reads vehicles.txt via getVehiclePlate())
 *  - buildVehicleSubtitle() : builds "Cars", "Motorcycles", or "Cars & motorcycles"
 *                             subtitle for the Registered Vehicles stat card
 *                             (belongs here because it only inspects vehicle type
 *                              data from this service — no appointment dependency)
 */
public class VehicleService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "vehicles.txt";

    private static final int EXPECTED_COLUMNS = 7;

    // ─────────────────────────────────────────────────────────────
    // getVehiclesByUserId — returns all vehicles for a user
    // ─────────────────────────────────────────────────────────────
    public List<String[]> getVehiclesByUserId(String userId) {
        List<String[]> result = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: bad line " + lineNum + ": " + line);
                    continue;
                }
                if (cols[1].trim().equalsIgnoreCase(userId)) {
                    result.add(new String[]{
                            cols[0].trim(), // [0] vehicleID
                            cols[2].trim(), // [1] vehicleType
                            cols[3].trim(), // [2] plate
                            cols[4].trim(), // [3] brand
                            cols[5].trim(), // [4] year
                            cols[6].trim()  // [5] colour
                    });
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // getVehicleIdByUserAndPlate — looks up the vehicleID for a
    // given userId + plate combination. Returns null if not found.
    // ─────────────────────────────────────────────────────────────
    public String getVehicleIdByUserAndPlate(String userId, String plate) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length != EXPECTED_COLUMNS) continue;

                boolean userMatches  = cols[1].trim().equalsIgnoreCase(userId);
                boolean plateMatches = cols[3].trim().equalsIgnoreCase(plate);

                if (userMatches && plateMatches) {
                    return cols[0].trim();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // getVehiclePlate — looks up a vehicle by ID and returns a
    // short display label like "Car · LIN110".
    // ─────────────────────────────────────────────────────────────
    public String getVehiclePlate(String vehicleId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return vehicleId;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length == EXPECTED_COLUMNS
                        && cols[0].trim().equalsIgnoreCase(vehicleId)) {
                    return cols[2].trim() + " \u00B7 " + cols[3].trim();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        return vehicleId;
    }

    // ═══════════════════════════════════════════════════════════════
    // DASHBOARD VEHICLE DISPLAY HELPERS
    // (moved from AppointmentSectionController.java)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns a short display label for a vehicle given its ID.
     * Example:  "Car · LIN110"  or  "Motor · AJH1312"
     *
     * MOVED FROM: AppointmentSectionController.getVehicleLabel()
     * Reason: purely a vehicle data lookup — reads vehicles.txt via
     *         getVehiclePlate(), so it belongs here in VehicleService.
     *
     * Called by:
     *   CustomerDashboard.buildUpcomingRow()         — vehicle label in appointment row
     *   CustomerDashboard.showPaymentInvoiceDialog() — vehicle label on invoice
     *
     * @param vehicleId  e.g. "V4"
     * @return display label, or the vehicleId itself as a fallback if not found
     */
    public String getVehicleLabel(String vehicleId) {
        return getVehiclePlate(vehicleId);
    }

    /**
     * Builds the subtitle text for the "Registered Vehicles" stat card
     * on the Customer Dashboard.
     *
     * Examples:
     *   one car only        → "Car"
     *   multiple cars       → "Cars"
     *   one motorcycle      → "Motorcycle"
     *   multiple bikes      → "Motorcycles"
     *   mix of both         → "Cars & motorcycles"
     *
     * MOVED FROM: AppointmentSectionController.buildVehicleSubtitle()
     * Reason: only reads vehicle type data ([1] vehicleType) from vehicle
     *         rows — no appointment dependency at all. Belongs here.
     *
     * Called by:
     *   CustomerDashboard.buildStatCardsRow() — subtitle under vehicle count card
     *
     * @param vehicles  list of vehicle rows from getVehiclesByUserId()
     * @return subtitle string, or empty string if the list is empty
     */
    public String buildVehicleSubtitle(List<String[]> vehicles) {
        boolean hasCar   = vehicles.stream().anyMatch(v -> "Car".equalsIgnoreCase(v[1]));
        boolean hasMotor = vehicles.stream().anyMatch(v -> "Motor".equalsIgnoreCase(v[1]));
        if (hasCar && hasMotor) return "Cars & motorcycles";
        if (hasCar)   return "Car"        + (vehicles.size() > 1 ? "s" : "");
        if (hasMotor) return "Motorcycle" + (vehicles.size() > 1 ? "s" : "");
        return "";
    }

    // ─────────────────────────────────────────────────────────────
    // addVehicle — appends a new vehicle line to vehicles.txt
    // ─────────────────────────────────────────────────────────────
    public boolean addVehicle(String userId, String vehicleType, String plate,
                              String brand, String year, String colour) {
        if (isAnyBlank(userId, vehicleType, plate, brand, year, colour))
            throw new IllegalArgumentException("All vehicle fields are required.");

        try {
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();
            String newId = generateNextID();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(newId + "," + userId.trim() + "," + vehicleType.trim()
                        + "," + plate.trim() + "," + brand.trim()
                        + "," + year.trim() + "," + colour.trim());
                writer.newLine();
            }
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
    }

    // ─────────────────────────────────────────────────────────────
    // deleteVehicle — removes the vehicle from vehicles.txt AND
    //                 cancels any Pending appointments for it.
    // ─────────────────────────────────────────────────────────────
    public boolean deleteVehicle(String userId, String plate) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        String vehicleId = getVehicleIdByUserAndPlate(userId, plate);

        List<String> keep = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length == EXPECTED_COLUMNS
                        && cols[1].trim().equalsIgnoreCase(userId)
                        && cols[3].trim().equalsIgnoreCase(plate)) {
                    found = true;
                } else {
                    keep.add(line);
                }
            }
        } catch (IOException e) { e.printStackTrace(); return false; }

        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String ln : keep) {
                writer.write(ln);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); return false; }

        if (vehicleId != null) {
            AppointmentService appointmentService = new AppointmentService();
            appointmentService.deletePendingByVehicleId(vehicleId);
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────
    // updateVehicle — replaces the line matching userID + oldPlate
    // ─────────────────────────────────────────────────────────────
    public boolean updateVehicle(String userId, String oldPlate,
                                 String newType, String newPlate,
                                 String newBrand, String newYear, String newColour) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> updated = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length == EXPECTED_COLUMNS
                        && cols[1].trim().equalsIgnoreCase(userId)
                        && cols[3].trim().equalsIgnoreCase(oldPlate)) {
                    updated.add(cols[0].trim() + "," + userId.trim()
                            + "," + newType.trim() + "," + newPlate.trim()
                            + "," + newBrand.trim() + "," + newYear.trim()
                            + "," + newColour.trim());
                    found = true;
                } else {
                    updated.add(line);
                }
            }
        } catch (IOException e) { e.printStackTrace(); return false; }

        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String ln : updated) {
                writer.write(ln);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); return false; }
        return true;
    }

    // ── Private helpers ───────────────────────────────────────────

    private String generateNextID() {
        int highest = 0;
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;
                    String[] cols = line.split(",", 2);
                    if (cols.length >= 1) {
                        String id = cols[0].trim();
                        if (id.matches("V\\d+")) {
                            int n = Integer.parseInt(id.substring(1));
                            if (n > highest) highest = n;
                        }
                    }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }
        return "V" + (highest + 1);
    }

    private boolean isAnyBlank(String... values) {
        for (String v : values) if (v == null || v.trim().isEmpty()) return true;
        return false;
    }
}