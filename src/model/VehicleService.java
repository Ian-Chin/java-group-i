package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VehicleService manages all vehicle-related data stored in vehicles.txt.
 *
 * It can:
 *  - Read all vehicles belonging to a customer (getVehiclesByEmail)
 *  - Add a new vehicle (addVehicle)
 *  - Delete a vehicle (deleteVehicle)
 *
 * File format — each line has exactly 6 values separated by commas:
 *   vehicleID , email , plateNumber , brand , year , colour
 *
 * Example lines in vehicles.txt:
 *   V1,bo@gmail.com,WXY1234,Toyota Vios,2021,White
 *   V2,t@gmail.com,ABC5678,Honda City,2019,Silver
 *   V3,lin@gmail.com,LIN110,Mercedes AMG Coupe,2025,White
 */
public class VehicleService {

    // Path to the vehicles data file
    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "vehicles.txt";

    // Each line must have exactly 6 columns
    private static final int EXPECTED_COLUMNS = 6;

    /**
     * Reads vehicles.txt and returns all vehicles that belong to the given email.
     *
     * Each vehicle is returned as a String array with 5 elements:
     *   [0] vehicleID  e.g. "V1"
     *   [1] plate      e.g. "WXY1234"
     *   [2] brand      e.g. "Toyota Vios"
     *   [3] year       e.g. "2021"
     *   [4] colour     e.g. "White"
     *
     * Note: the email (column 1) is not included in the returned array
     * because the caller already knows the email.
     *
     * @param email  the customer's email address to search for
     * @return a list of vehicle arrays (empty list if no vehicles found)
     */
    public List<String[]> getVehiclesByEmail(String email) {
        List<String[]> result = new ArrayList<>();

        File file = new File(FILE_PATH);

        // Return empty list if the file does not exist yet
        if (!file.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip blank lines
                if (line.isBlank()) continue;

                // Split the line into columns (maximum 6 parts)
                String[] columns = line.split(",", EXPECTED_COLUMNS);

                // Skip lines that do not have exactly 6 columns
                if (columns.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: skipping line " + lineNumber
                            + " — expected 6 columns but got " + columns.length + ": " + line);
                    continue;
                }

                // Column index 1 is the email — check if it matches
                String emailInFile = columns[1].trim();
                if (emailInFile.equalsIgnoreCase(email)) {
                    // Build the 5-element array to return
                    String[] vehicle = new String[]{
                            columns[0].trim(), // vehicleID
                            columns[2].trim(), // plate number
                            columns[3].trim(), // brand / model
                            columns[4].trim(), // year
                            columns[5].trim()  // colour
                    };
                    result.add(vehicle);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Adds a new vehicle to vehicles.txt.
     * A vehicle ID is generated automatically (e.g. V1, V2, V3...).
     *
     * Throws IllegalArgumentException if any field is blank.
     *
     * @param email   the customer's email address
     * @param plate   the car plate number (e.g. "WXY1234")
     * @param brand   the car brand/model (e.g. "Toyota Vios")
     * @param year    the year of manufacture (e.g. "2021")
     * @param colour  the colour of the car (e.g. "White")
     * @return true if the vehicle was saved successfully
     */
    public boolean addVehicle(String email, String plate, String brand,
                              String year, String colour) {
        // Make sure none of the fields are empty
        if (isAnyBlank(email, plate, brand, year, colour)) {
            throw new IllegalArgumentException("All vehicle fields are required.");
        }

        try {
            File file = new File(FILE_PATH);

            // Create the TxtFile folder if it does not exist
            file.getParentFile().mkdirs();

            // Generate the next vehicle ID (e.g. if V3 exists, the next is V4)
            String newVehicleID = generateNextID();

            // Append a new line to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(newVehicleID + ","
                        + email.trim()  + ","
                        + plate.trim()  + ","
                        + brand.trim()  + ","
                        + year.trim()   + ","
                        + colour.trim());
                writer.newLine(); // move to the next line
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a vehicle from vehicles.txt.
     * The vehicle is identified by matching both the email AND the plate number.
     *
     * How it works:
     *  1. Read all lines from the file
     *  2. Keep every line EXCEPT the one that matches email + plate
     *  3. Write the remaining lines back to the file
     *
     * @param email  the customer's email address
     * @param plate  the car plate number of the vehicle to remove
     * @return true if the vehicle was found and removed, false if not found
     */
    public boolean deleteVehicle(String email, String plate) {
        File file = new File(FILE_PATH);

        // Cannot delete from a file that does not exist
        if (!file.exists()) return false;

        List<String> linesToKeep = new ArrayList<>();
        boolean vehicleWasFound = false;
        int lineNumber = 0;

        // Step 1: Read all lines, skip the one that matches email + plate
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) continue;

                String[] columns = line.split(",", EXPECTED_COLUMNS);

                // Skip malformed lines
                if (columns.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: skipping malformed line "
                            + lineNumber + " during delete: " + line);
                    continue;
                }

                String emailInFile = columns[1].trim();
                String plateInFile = columns[2].trim();

                // Check if this is the vehicle to delete
                boolean isMatchingVehicle = emailInFile.equalsIgnoreCase(email)
                        && plateInFile.equalsIgnoreCase(plate);

                if (isMatchingVehicle) {
                    vehicleWasFound = true;
                    // Do NOT add this line to linesToKeep — this deletes it
                } else {
                    linesToKeep.add(line); // keep all other vehicles
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // If the vehicle was not found, nothing to delete
        if (!vehicleWasFound) return false;

        // Step 2: Write the remaining lines back to the file (overwrite mode)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String lineToWrite : linesToKeep) {
                writer.write(lineToWrite);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    /**
     * Updates an existing vehicle record in vehicles.txt IN PLACE.
     * This means the vehicle stays in the same position in the file —
     * the order of vehicles does not change after editing.
     *
     * The vehicle to update is found by matching both email AND the OLD plate number.
     * Once found, that line is replaced with the new values.
     *
     * @param email     the customer's email address
     * @param oldPlate  the original plate number (used to find the record)
     * @param newPlate  the new plate number
     * @param newBrand  the updated brand / model
     * @param newYear   the updated year
     * @param newColour the updated colour
     * @return true if the vehicle was found and updated, false if not found
     */
    public boolean updateVehicle(String email, String oldPlate,
                                 String newPlate, String newBrand,
                                 String newYear, String newColour) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> updatedLines = new ArrayList<>();
        boolean vehicleWasFound = false;
        int lineNumber = 0;

        // Step 1: Read every line and replace the matching one with updated values
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) continue;

                String[] columns = line.split(",", EXPECTED_COLUMNS);

                if (columns.length != EXPECTED_COLUMNS) {
                    System.err.println("VehicleService: skipping malformed line "
                            + lineNumber + " during update: " + line);
                    continue;
                }

                String emailInFile = columns[1].trim();
                String plateInFile = columns[2].trim();

                // Check if this is the vehicle to update
                boolean isMatchingVehicle = emailInFile.equalsIgnoreCase(email)
                        && plateInFile.equalsIgnoreCase(oldPlate);

                if (isMatchingVehicle) {
                    // Keep the same vehicleID, just replace the other fields
                    String vehicleID  = columns[0].trim();
                    String updatedLine = vehicleID        + ","
                            + email.trim()    + ","
                            + newPlate.trim() + ","
                            + newBrand.trim() + ","
                            + newYear.trim()  + ","
                            + newColour.trim();
                    updatedLines.add(updatedLine);
                    vehicleWasFound = true;
                } else {
                    // Keep all other lines exactly as they are
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!vehicleWasFound) return false;

        // Step 2: Write all lines back in the same order (overwrite the file)
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (String updatedLine : updatedLines) {
                writer.write(updatedLine);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════

    /**
     * Generates the next vehicle ID by finding the highest existing
     * number and adding 1.
     *
     * Example: if vehicles.txt has V1, V2, V3 → returns "V4"
     * Example: if vehicles.txt is empty → returns "V1"
     */
    private String generateNextID() {
        int highestNumber = 0;

        File file = new File(FILE_PATH);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;

                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) continue;

                    // Only read the first column (the vehicle ID)
                    String[] columns = line.split(",", 2);

                    if (columns.length >= 1) {
                        String vehicleID = columns[0].trim();

                        // Check if the ID matches the pattern "V" followed by a number
                        // e.g. "V1", "V2", "V10" — yes. "ABC" — no.
                        if (vehicleID.matches("V\\d+")) {
                            // Extract the number part (remove the "V")
                            int number = Integer.parseInt(vehicleID.substring(1));

                            // Track the highest number found
                            if (number > highestNumber) {
                                highestNumber = number;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Next ID is one more than the highest found
        return "V" + (highestNumber + 1);
    }

    /**
     * Checks if any of the given String values are null or blank (empty/whitespace).
     * Returns true if at least one value is blank, false if all are filled.
     *
     * Example: isAnyBlank("lin@gmail.com", "", "Toyota") → true (second value is blank)
     *          isAnyBlank("lin@gmail.com", "V123", "Toyota") → false (all filled)
     */
    private boolean isAnyBlank(String... values) {
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                return true; // found a blank value
            }
        }
        return false; // all values are filled
    }
}
