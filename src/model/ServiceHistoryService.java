package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ServiceHistoryService handles reading and writing records in serviceHistory.txt.
 *
 * NEW File format — each line now has 8 values (vehicleID was added):
 *   ServiceHistoryID , CustomerID , AppointmentID , VehicleID ,
 *   PaymentID , TechnicianID , ServiceDate , Status
 *
 * Example:
 *   SH1,C1,AP1,V1,PY1,T1,2026-03-05,Completed
 *   SH3,C3,AP3,V3,NULL,T3,2026-03-20,Completed  ← NULL = not paid yet
 *
 * CHANGE FROM OLD FORMAT:
 *   Old (7 cols): SH_ID, CustomerID, AppointmentID, PaymentID, TechnicianID, Date, Status
 *   New (8 cols): SH_ID, CustomerID, AppointmentID, VehicleID, PaymentID, TechnicianID, Date, Status
 *   VehicleID was inserted at position 3, pushing PaymentID to position 4.
 */
public class ServiceHistoryService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "serviceHistory.txt";

    // CHANGE: was 7 columns, now 8 because VehicleID was added
    private static final int EXPECTED_COLUMNS = 8;

    // ─────────────────────────────────────────────────────────────
    // getAllRecords() — reads every row from serviceHistory.txt
    //
    // Each returned String[] has 8 elements:
    //   [0] serviceHistoryID  [1] customerID    [2] appointmentID
    //   [3] vehicleID          [4] paymentID     [5] technicianID
    //   [6] serviceDate        [7] status
    // ─────────────────────────────────────────────────────────────
    public List<String[]> getAllRecords() {
        List<String[]> list = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
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
    // appointmentAlreadyRecorded() — checks if an appointment ID
    // already has a record in serviceHistory.txt.
    // Prevents writing the same appointment twice.
    // ─────────────────────────────────────────────────────────────
    public boolean appointmentAlreadyRecorded(String appointmentId) {
        for (String[] row : getAllRecords()) {
            // Column index 2 = AppointmentID (unchanged position)
            if (row[2].trim().equalsIgnoreCase(appointmentId)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // generateNextId() — returns the next SH number.
    // Finds the highest existing SH number and adds 1.
    // ─────────────────────────────────────────────────────────────
    public String generateNextId() {
        int highestNumber = 0;
        for (String[] row : getAllRecords()) {
            String shId = row[0].trim();
            if (shId.matches("SH\\d+")) {
                int number = Integer.parseInt(shId.substring(2));
                if (number > highestNumber) highestNumber = number;
            }
        }
        return "SH" + (highestNumber + 1);
    }

    // ─────────────────────────────────────────────────────────────
    // addRecord() — appends one new row to serviceHistory.txt.
    //
    // CHANGE: vehicleId parameter was added.
    //
    // Parameters:
    //   customerId    e.g. "C3"
    //   appointmentId e.g. "AP4"
    //   vehicleId     e.g. "V4"     ← NEW
    //   paymentId     e.g. "PY9" or "NULL"
    //   technicianId  e.g. "T3"
    //   serviceDate   e.g. "2026-03-26"
    // ─────────────────────────────────────────────────────────────
    public boolean addRecord(String customerId, String appointmentId,
                              String vehicleId, String paymentId,
                              String technicianId, String serviceDate) {
        File file = new File(FILE_PATH);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            String newId = generateNextId();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Format: SH12,C3,AP4,V4,NULL,T3,2026-03-26,Completed
                writer.write(newId        + ","
                           + customerId   + ","
                           + appointmentId+ ","
                           + vehicleId    + ","   // NEW: vehicleID
                           + paymentId    + ","
                           + technicianId + ","
                           + serviceDate  + ","
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
    // updatePaymentId() — when a customer pays, updates the PaymentID
    // column in serviceHistory.txt from "NULL" to the real PY ID.
    //
    // Reads the whole file, finds the row with the matching appointmentId,
    // updates its PaymentID column, then rewrites the file.
    // ─────────────────────────────────────────────────────────────
    public boolean updatePaymentId(String appointmentId, String newPaymentId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> updatedLines = new ArrayList<>();
        boolean foundAndUpdated = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) {
                    updatedLines.add(line);
                    continue;
                }

                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length == EXPECTED_COLUMNS
                        && cols[2].trim().equalsIgnoreCase(appointmentId)) {
                    // Rebuild the line with the new PaymentID at column index 4
                    // New format: [0]SH_ID, [1]custID, [2]apptID, [3]vehicleID,
                    //             [4]paymentID, [5]techID, [6]date, [7]status
                    String updatedLine = cols[0].trim() + ","   // SH ID
                                      + cols[1].trim() + ","   // customerID
                                      + cols[2].trim() + ","   // appointmentID
                                      + cols[3].trim() + ","   // vehicleID (NEW)
                                      + newPaymentId   + ","   // paymentID (was index 3, now 4)
                                      + cols[5].trim() + ","   // technicianID
                                      + cols[6].trim() + ","   // serviceDate
                                      + cols[7].trim();        // status
                    updatedLines.add(updatedLine);
                    foundAndUpdated = true;
                } else {
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!foundAndUpdated) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (int i = 0; i < updatedLines.size(); i++) {
                writer.write(updatedLines.get(i));
                if (i < updatedLines.size() - 1) writer.newLine();
            }
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}