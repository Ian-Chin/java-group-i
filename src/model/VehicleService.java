package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages vehicle records in src/TxtFile/vehicles.txt.
 *
 * File format — strictly 6 columns per line, comma-separated:
 *   email,plateNumber,brand,year,colour,carType
 *
 * Example:
 *   bo@gmail.com,WXY1234,Toyota Vios,2021,White,Sedan
 *   lin@gmail.com,LIN110,Mercedes AMG Coupe,2025,White,Coupe
 *
 * Note: plateNumber is stored for backend operations (edit/delete by plate)
 *       but is NOT displayed in the UI.
 */
public class VehicleService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "vehicles.txt";

    private static final int EXPECTED_COLUMNS = 6;

    /**
     * Returns all vehicles for the given customer email.
     * Each entry: [plate, brand, year, colour, carType]
     * Lines that do not have exactly 6 columns are skipped and logged.
     */
    public List<String[]> getVehiclesByEmail(String email) {
        List<String[]> result = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return result;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNum = 0;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                String[] parts = line.split(",", EXPECTED_COLUMNS);
                if (parts.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: skipping malformed line "
                            + lineNum + " (expected " + EXPECTED_COLUMNS
                            + " columns, got " + parts.length + "): " + line);
                    continue;
                }
                if (parts[0].trim().equalsIgnoreCase(email)) {
                    result.add(new String[]{
                            parts[1].trim(), // plate
                            parts[2].trim(), // brand
                            parts[3].trim(), // year
                            parts[4].trim(), // colour
                            parts[5].trim()  // carType
                    });
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Appends a new vehicle record.
     * All 6 fields are required — throws IllegalArgumentException if any is blank.
     */
    public boolean addVehicle(String email, String plate, String brand,
                              String year, String colour, String carType) {
        if (isAnyBlank(email, plate, brand, year, colour, carType)) {
            throw new IllegalArgumentException(
                    "All vehicle fields (email, plate, brand, year, colour, carType) are required.");
        }
        try {
            File file = new File(FILE_PATH);
            file.getParentFile().mkdirs();
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, true))) {
                bw.write(email.trim() + "," + plate.trim() + "," + brand.trim()
                        + "," + year.trim() + "," + colour.trim() + "," + carType.trim());
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes the vehicle matching the given email + plate number.
     * Returns true if a matching record was found and deleted.
     */
    public boolean deleteVehicle(String email, String plate) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> lines   = new ArrayList<>();
        boolean      found   = false;
        int          lineNum = 0;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lineNum++;
                if (line.isBlank()) continue;
                String[] parts = line.split(",", EXPECTED_COLUMNS);
                if (parts.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: skipping malformed line "
                            + lineNum + " during delete: " + line);
                    continue; // drop malformed lines too
                }
                if (parts[0].trim().equalsIgnoreCase(email)
                        && parts[1].trim().equalsIgnoreCase(plate)) {
                    found = true; // skip this line — deletes the record
                } else {
                    lines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!found) return false;

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // ── Helper ───────────────────────────────────────────────────
    private boolean isAnyBlank(String... values) {
        for (String v : values) {
            if (v == null || v.trim().isEmpty()) return true;
        }
        return false;
    }
}
