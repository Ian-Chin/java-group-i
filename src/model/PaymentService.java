package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PaymentService reads and writes payment data from payments.txt.
 *
 * NEW File format — each line now has 9 values (vehicleID was added):
 *   PaymentID , CustomerID , ServiceHistoryID , AppointmentID ,
 *   VehicleID , Amount , PaymentDate , Method , Status
 *
 * Example:
 *   PY1,C1,SH1,AP1,V1,120.00,2026-03-05,Cash,Paid
 *
 * CHANGE FROM OLD FORMAT:
 *   Old (8 cols): PaymentID, CustomerID, SH_ID, ApptID, Amount, Date, Method, Status
 *   New (9 cols): PaymentID, CustomerID, SH_ID, ApptID, VehicleID, Amount, Date, Method, Status
 *   VehicleID was inserted at position 4, pushing Amount to position 5.
 */
public class PaymentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "payments.txt";

    // CHANGE: was 8 columns, now 9 because VehicleID was added
    private static final int EXPECTED_COLUMNS = 9;

    // ─────────────────────────────────────────────────────────────
    // getAllPayments() — reads every payment row from payments.txt
    //
    // Each returned String[] has 9 elements:
    //   [0] paymentID      [1] customerID      [2] serviceHistoryID
    //   [3] appointmentID  [4] vehicleID        [5] amount
    //   [6] paymentDate    [7] method           [8] status
    // ─────────────────────────────────────────────────────────────
    public List<String[]> getAllPayments() {
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
    // getPaidAppointmentIds() — returns a set of appointment IDs
    // that a given customer has already paid for.
    //
    // Used to filter out already-paid appointments from the
    // Pending Payment card.
    // ─────────────────────────────────────────────────────────────
    public Set<String> getPaidAppointmentIds(String userId) {
        Set<String> paidIds = new HashSet<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return paidIds;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] columns = line.split(",", EXPECTED_COLUMNS);
                if (columns.length != EXPECTED_COLUMNS) continue;

                // [1] = CustomerID, [3] = AppointmentID, [8] = Status
                String customerIdInFile = columns[1].trim();
                String appointmentId    = columns[3].trim();
                String status           = columns[8].trim();

                if (customerIdInFile.equalsIgnoreCase(userId)
                        && status.equalsIgnoreCase("Paid")) {
                    paidIds.add(appointmentId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return paidIds;
    }

    // ─────────────────────────────────────────────────────────────
    // getPaymentByAppointmentId() — finds one payment row by its
    // appointment ID. Returns null if not found.
    // ─────────────────────────────────────────────────────────────
    public String[] getPaymentByAppointmentId(String appointmentId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] columns = line.split(",", EXPECTED_COLUMNS);
                if (columns.length != EXPECTED_COLUMNS) continue;
                // [3] = AppointmentID
                if (columns[3].trim().equalsIgnoreCase(appointmentId)) {
                    return columns;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // generateNextPaymentId() — returns the next PY number.
    // Scans payments.txt for the highest existing PY number, adds 1.
    // Example: if PY7 is the highest, returns "PY8".
    // ─────────────────────────────────────────────────────────────
    public String generateNextPaymentId() {
        int highestNumber = 0;
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || line.trim().startsWith("#")) continue;
                    String[] columns = line.split(",", 2);
                    if (columns.length >= 1) {
                        String paymentId = columns[0].trim();
                        if (paymentId.matches("PY\\d+")) {
                            int number = Integer.parseInt(paymentId.substring(2));
                            if (number > highestNumber) highestNumber = number;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "PY" + (highestNumber + 1);
    }

    // ─────────────────────────────────────────────────────────────
    // savePayment() — saves a new payment to payments.txt and
    //                 updates the matching row in serviceHistory.txt.
    //
    // Steps:
    //   1. Generate the next PY ID (e.g. "PY8")
    //   2. Find the SH ID for this appointment from serviceHistory.txt
    //   3. Append a new row to payments.txt (now 9 columns with vehicleID)
    //   4. Update serviceHistory.txt PaymentID from "NULL" to the PY ID
    //
    // Parameters:
    //   userId        - customer ID e.g. "C3"
    //   appointmentId - the appointment being paid e.g. "AP4"
    //   vehicleId     - the vehicle ID e.g. "V4"   ← NEW
    //   amount        - amount string e.g. "1050.00"
    //   method        - "Cash", "Card", or "Online"
    // ─────────────────────────────────────────────────────────────
    public boolean savePayment(String userId, String appointmentId,
                               String vehicleId, String amount, String method) {
        File file = new File(FILE_PATH);
        try {
            file.getParentFile().mkdirs();

            // Today's date in Malaysia time
            java.time.LocalDate today = java.time.LocalDate.now(
                    java.time.ZoneId.of("Asia/Kuala_Lumpur"));
            String paymentDate = today.toString(); // e.g. "2026-03-27"

            String newPaymentId = generateNextPaymentId(); // e.g. "PY8"

            // Get the SH ID for this appointment
            String shId = findServiceHistoryId(appointmentId);

            // Append to payments.txt — now 9 columns including vehicleID
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Format: PY8,C3,SH4,AP4,V4,1050.00,2026-03-27,Cash,Paid
                writer.write(newPaymentId  + ","
                           + userId        + ","
                           + shId          + ","   // "SH4" or "NULL"
                           + appointmentId + ","
                           + vehicleId     + ","   // NEW: vehicleID
                           + amount        + ","
                           + paymentDate   + ","
                           + method        + ","
                           + "Paid");
                writer.newLine();
            }

            // Update serviceHistory.txt: change "NULL" paymentID to real PY ID
            ServiceHistoryService shService = new ServiceHistoryService();
            shService.updatePaymentId(appointmentId, newPaymentId);

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findServiceHistoryId() — searches serviceHistory.txt for the
    // SH row that matches a given appointment ID, returns its SH ID.
    //
    // serviceHistory.txt new format (8 columns):
    //   SH1, C1, AP1, V1, PY1, T1, 2026-03-05, Completed
    //   col0  col1  col2  col3  col4  col5  col6  col7
    //   ↑           ↑
    //   SH_ID       AppointmentID (column 2)
    //
    // Returns "NULL" if no matching record is found.
    // ─────────────────────────────────────────────────────────────
    private String findServiceHistoryId(String appointmentId) {
        String shFilePath = "src" + File.separator
                + "TxtFile" + File.separator + "serviceHistory.txt";
        File shFile = new File(shFilePath);
        if (!shFile.exists()) return "NULL";

        try (BufferedReader reader = new BufferedReader(new FileReader(shFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] cols = line.split(",", 8); // 8 columns now
                if (cols.length >= 3) {
                    // Column 2 = AppointmentID
                    if (cols[2].trim().equalsIgnoreCase(appointmentId)) {
                        return cols[0].trim(); // Column 0 = ServiceHistoryID e.g. "SH4"
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "NULL";
    }
}