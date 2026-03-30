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
 *   [0] vehicleID    e.g. "V3"
 *   [1] vehicleType  e.g. "Car" or "Motor"
 *   [2] plate        e.g. "LIN110"
 *   [3] brand        e.g. "Mercedes AMG Coupe"
 *   [4] year         e.g. "2025"
 *   [5] colour       e.g. "White"
 *
 * CHANGE in deleteVehicle():
 *   After removing the vehicle from vehicles.txt, it now also calls
 *   AppointmentService.deletePendingByVehicleId() to automatically
 *   cancel any "Pending" or "In Progress" appointments for that vehicle.
 *
 *   "Completed" appointments are intentionally left alone — the customer
 *   still owes payment for services that were already performed, even if
 *   they no longer own or have registered that vehicle.
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
    // getVehicleIdByUserAndPlate — looks up the vehicleID for a given
    // userId + plate combination.
    //
    // Used inside deleteVehicle() so we know WHICH vehicleID (e.g. "V4")
    // to pass to AppointmentService when cancelling pending appointments.
    //
    // Returns null if no match is found.
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

                // cols[1] = userID,  cols[3] = plate,  cols[0] = vehicleID
                boolean userMatches  = cols[1].trim().equalsIgnoreCase(userId);
                boolean plateMatches = cols[3].trim().equalsIgnoreCase(plate);

                if (userMatches && plateMatches) {
                    return cols[0].trim(); // return the vehicleID e.g. "V4"
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        return null; // not found
    }

    // ─────────────────────────────────────────────────────────────
    // getVehiclePlate — looks up a vehicle by its ID and returns
    // a short display label like "Car · LIN110"
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
                    return cols[2].trim() + " · " + cols[3].trim();
                }
            }
        } catch (IOException e) { e.printStackTrace(); }

        return vehicleId; // fallback if vehicle not found
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
    //
    // CHANGE: After deleting the vehicle row, this method now calls
    //   AppointmentService.deletePendingByVehicleId(vehicleId)
    //   to automatically remove all "Pending" / "In Progress"
    //   appointments for that vehicle.
    //
    //   "Completed" appointments are intentionally preserved so that
    //   the Pending Payment card still shows them — the customer owes
    //   payment for services already performed.
    //
    // Step-by-step:
    //   1. Find the vehicleID (e.g. "V4") for the given userId + plate
    //   2. Remove the vehicle row from vehicles.txt
    //   3. Cancel pending appointments in appointments.txt for that vehicleID
    // ─────────────────────────────────────────────────────────────
    public boolean deleteVehicle(String userId, String plate) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        // ── Step 1: Find the vehicleID before deleting ────────────
        // We need the vehicleID (e.g. "V4") to cancel appointments.
        // We look it up BEFORE modifying the file so we can still read it.
        String vehicleId = getVehicleIdByUserAndPlate(userId, plate);

        // ── Step 2: Remove the vehicle row from vehicles.txt ──────
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
                    found = true; // this is the vehicle to delete — skip it
                } else {
                    keep.add(line); // keep everything else
                }
            }
        } catch (IOException e) { e.printStackTrace(); return false; }

        if (!found) return false; // vehicle not found — nothing to delete

        // Rewrite vehicles.txt without the deleted vehicle
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String ln : keep) {
                writer.write(ln);
                writer.newLine();
            }
        } catch (IOException e) { e.printStackTrace(); return false; }

        // ── Step 3: Cancel pending appointments for this vehicle ──
        // Only do this if we found a valid vehicleID in Step 1.
        // If vehicleId is null (vehicle was not in the file), skip this step.
        if (vehicleId != null) {
            AppointmentService appointmentService = new AppointmentService();
            appointmentService.deletePendingByVehicleId(vehicleId);
            // Note: we do NOT delete Completed appointments here.
            // Completed appointments stay so the customer can still pay
            // for services that were already performed on the vehicle.
        }

        return true;
    }

    // ─────────────────────────────────────────────────────────────
    // updateVehicle — replaces the line matching userID + oldPlate
    //                 with updated values
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
                    // Keep same vehicleID, replace everything else
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