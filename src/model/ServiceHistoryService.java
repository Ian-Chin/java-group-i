package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceHistoryService handles reading and writing records in serviceHistory.txt.
 *
 * File format — each line has 7 values:
 *   ServiceHistoryID , CustomerID , AppointmentID , PaymentID ,
 *   TechnicianID , ServiceDate , Status
 *
 * Example:
 *   SH1,C1,AP1,PY1,T1,2026-03-05,Completed
 *   SH3,C3,AP3,NULL,T3,2026-03-20,Completed   ← NULL means not paid yet
 *
 * NOTE: PaymentID is "NULL" when no payment has been made yet.
 *       It gets updated to a real PY number once the customer pays.
 */
public class ServiceHistoryService {

    // Path to the serviceHistory.txt file
    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "serviceHistory.txt";

    // Each line must have exactly 7 columns
    private static final int EXPECTED_COLUMNS = 7;

    // ─────────────────────────────────────────────────────────────
    // getAllRecords() — read every row from serviceHistory.txt
    //
    // Returns a list of String arrays.
    // Each array has 7 elements matching the 7 columns in the file.
    // ─────────────────────────────────────────────────────────────
    public List<String[]> getAllRecords() {
        List<String[]> list = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return list; // return empty list if file doesn't exist yet

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip blank lines and comment lines starting with #
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                // Split by comma into at most EXPECTED_COLUMNS parts
                String[] columns = line.split(",", EXPECTED_COLUMNS);
                if (columns.length == EXPECTED_COLUMNS) {
                    list.add(columns);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // appointmentAlreadyRecorded() — check if an appointment already
    // has a serviceHistory entry, so we don't write it twice.
    //
    // Example: if SH3 already records AP3, return true for "AP3".
    // ─────────────────────────────────────────────────────────────
    public boolean appointmentAlreadyRecorded(String appointmentId) {
        for (String[] row : getAllRecords()) {
            // Column index 2 = AppointmentID
            if (row[2].trim().equalsIgnoreCase(appointmentId)) {
                return true; // already in the file — don't add again
            }
        }
        return false; // not found — safe to add a new record
    }

    // ─────────────────────────────────────────────────────────────
    // generateNextId() — generates the next SH number.
    //
    // Looks at all existing IDs like "SH1", "SH11", finds the highest,
    // and returns the next one (e.g. "SH12").
    // ─────────────────────────────────────────────────────────────
    public String generateNextId() {
        int highestNumber = 0;

        for (String[] row : getAllRecords()) {
            String shId = row[0].trim(); // e.g. "SH11"
            if (shId.matches("SH\\d+")) {
                // Remove "SH" prefix and parse the number
                int number = Integer.parseInt(shId.substring(2));
                if (number > highestNumber) {
                    highestNumber = number;
                }
            }
        }

        return "SH" + (highestNumber + 1); // e.g. "SH12"
    }

    // ─────────────────────────────────────────────────────────────
    // addRecord() — appends one new row to serviceHistory.txt.
    //
    // Called in two situations:
    //   1. When an appointment is auto-completed (paymentId = "NULL")
    //   2. When a customer pays (paymentId = actual PY number)
    //
    // Parameters:
    //   customerId    e.g. "C3"
    //   appointmentId e.g. "AP4"
    //   paymentId     e.g. "PY9" or "NULL"
    //   technicianId  e.g. "T3"
    //   serviceDate   e.g. "2026-03-26"  (just the date part, no time)
    // ─────────────────────────────────────────────────────────────
    public boolean addRecord(String customerId, String appointmentId,
                              String paymentId, String technicianId,
                              String serviceDate) {
        File file = new File(FILE_PATH);
        try {
            // Create the TxtFile folder if it doesn't exist yet
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            String newId = generateNextId(); // e.g. "SH12"

            // Append to the file (true = don't overwrite, just add at the end)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Format: SH12,C3,AP4,NULL,T3,2026-03-26,Completed
                writer.write(newId + ","
                           + customerId + ","
                           + appointmentId + ","
                           + paymentId + ","    // "NULL" or "PY9"
                           + technicianId + ","
                           + serviceDate + ","
                           + "Completed");
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updatePaymentId() — when a customer pays, update the PaymentID
    // column from "NULL" to the real payment ID (e.g. "PY9").
    //
    // This reads the whole file, finds the row with the matching
    // appointmentId, updates its PaymentID column, then rewrites the file.
    // ─────────────────────────────────────────────────────────────
    public boolean updatePaymentId(String appointmentId, String newPaymentId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> updatedLines = new ArrayList<>();
        boolean foundAndUpdated = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Keep blank and comment lines unchanged
                if (line.isBlank() || line.trim().startsWith("#")) {
                    updatedLines.add(line);
                    continue;
                }

                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length == EXPECTED_COLUMNS
                        && cols[2].trim().equalsIgnoreCase(appointmentId)) {
                    // This row matches — update column index 3 (PaymentID)
                    // Rebuild the line with the new PaymentID
                    String updatedLine = cols[0].trim() + ","   // SH ID
                                      + cols[1].trim() + ","   // customerID
                                      + cols[2].trim() + ","   // appointmentID
                                      + newPaymentId + ","     // NEW paymentID
                                      + cols[4].trim() + ","   // technicianID
                                      + cols[5].trim() + ","   // serviceDate
                                      + cols[6].trim();        // status
                    updatedLines.add(updatedLine);
                    foundAndUpdated = true;
                } else {
                    updatedLines.add(line); // keep the line as-is
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!foundAndUpdated) return false;

        // Write all lines back to the file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (int i = 0; i < updatedLines.size(); i++) {
                writer.write(updatedLines.get(i));
                if (i < updatedLines.size() - 1) writer.newLine();
            }
            writer.newLine(); // trailing newline
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }
}