package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * VehicleService manages all vehicle-related data stored in vehicles.txt.
 *
 * File format — each line has exactly 6 values separated by commas:
 *   vehicleID , userID , plateNumber , brand , year , colour
 *
 * Example lines in vehicles.txt:
 *   V1,C1,WXY1234,Toyota Vios,2021,White
 *   V2,C2,ABC5678,Honda City,2019,Silver
 *   V3,C3,LIN110,Mercedes AMG Coupe,2025,White
 *
 * NOTE: The second column is now userID (e.g. "C1") instead of email.
 */
public class VehicleService {

    // Path to the vehicles data file
    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "vehicles.txt";

    // Each line must have exactly 6 columns
    private static final int EXPECTED_COLUMNS = 6;

    /**
     * Returns all vehicles belonging to the given userID.
     *
     * Each vehicle is returned as a String array with 5 elements:
     *   [0] vehicleID  e.g. "V1"
     *   [1] plate      e.g. "WXY1234"
     *   [2] brand      e.g. "Toyota Vios"
     *   [3] year       e.g. "2021"
     *   [4] colour     e.g. "White"
     *
     * Note: the userID (column 1) is not included in the returned array
     * because the caller already knows the userID.
     *
     * @param userId  the customer's user ID to search for (e.g. "C3")
     * @return a list of vehicle arrays (empty list if no vehicles found)
     */
    public List<String[]> getVehiclesByUserId(String userId) {
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

                // Column index 1 is now userID — check if it matches
                String userIdInFile = columns[1].trim();
                if (userIdInFile.equalsIgnoreCase(userId)) {
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
     * Adds a new vehicle to vehicles.txt using userID instead of email.
     * A vehicle ID is generated automatically (e.g. V1, V2, V3...).
     *
     * @param userId  the customer's user ID (e.g. "C3")
     * @param plate   the car plate number (e.g. "WXY1234")
     * @param brand   the car brand/model (e.g. "Toyota Vios")
     * @param year    the year of manufacture (e.g. "2021")
     * @param colour  the colour of the car (e.g. "White")
     * @return true if the vehicle was saved successfully
     */
    public boolean addVehicle(String userId, String plate, String brand,
                              String year, String colour) {
        // Make sure none of the fields are empty
        if (isAnyBlank(userId, plate, brand, year, colour)) {
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
                        + userId.trim()  + ","
                        + plate.trim()   + ","
                        + brand.trim()   + ","
                        + year.trim()    + ","
                        + colour.trim());
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Removes a vehicle from vehicles.txt, matched by userID + plate number.
     *
     * How it works:
     *  1. Read all lines from the file
     *  2. Keep every line EXCEPT the one that matches userID + plate
     *  3. Write the remaining lines back to the file
     *
     * @param userId  the customer's user ID (e.g. "C3")
     * @param plate   the car plate number of the vehicle to remove
     * @return true if the vehicle was found and removed, false if not found
     */
    public boolean deleteVehicle(String userId, String plate) {
        File file = new File(FILE_PATH);

        // Cannot delete from a file that does not exist
        if (!file.exists()) return false;

        List<String> linesToKeep = new ArrayList<>();
        boolean vehicleWasFound = false;
        int lineNumber = 0;

        // Step 1: Read all lines, skip the one that matches userID + plate
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

                String userIdInFile = columns[1].trim();
                String plateInFile  = columns[2].trim();

                // Check if this is the vehicle to delete (match userID + plate)
                boolean isMatchingVehicle = userIdInFile.equalsIgnoreCase(userId)
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
     * The vehicle is found by matching both userID AND the OLD plate number.
     * Once found, that line is replaced with the new values.
     *
     * @param userId    the customer's user ID (e.g. "C3")
     * @param oldPlate  the original plate number (used to find the record)
     * @param newPlate  the new plate number
     * @param newBrand  the updated brand / model
     * @param newYear   the updated year
     * @param newColour the updated colour
     * @return true if the vehicle was found and updated, false if not found
     */
    public boolean updateVehicle(String userId, String oldPlate,
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

                String userIdInFile = columns[1].trim();
                String plateInFile  = columns[2].trim();

                // Check if this is the vehicle to update (match userID + old plate)
                boolean isMatchingVehicle = userIdInFile.equalsIgnoreCase(userId)
                        && plateInFile.equalsIgnoreCase(oldPlate);

                if (isMatchingVehicle) {
                    // Keep the same vehicleID, just replace the other fields
                    String vehicleID   = columns[0].trim();
                    String updatedLine = vehicleID         + ","
                            + userId.trim()    + ","
                            + newPlate.trim()  + ","
                            + newBrand.trim()  + ","
                            + newYear.trim()   + ","
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

    // ═══════════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

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
                        if (vehicleID.matches("V\\d+")) {
                            int number = Integer.parseInt(vehicleID.substring(1));
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

        return "V" + (highestNumber + 1);
    }

    /**
     * Checks if any of the given String values are null or blank.
     * Returns true if at least one value is blank, false if all are filled.
     */
    private boolean isAnyBlank(String... values) {
        for (String value : values) {
            if (value == null || value.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
