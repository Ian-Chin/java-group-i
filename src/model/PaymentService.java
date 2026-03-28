package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PaymentService reads and writes payment data from payments.txt.
 *
 * File format — each line has 8 values:
 *   PaymentID , CustomerID , ServiceHistoryID , AppointmentID ,
 *   Amount , PaymentDate , Method , Status
 *
 * Example:
 *   PY1,C1,SH1,AP1,120.00,2026-03-05,Cash,Paid
 *
 * NOTE: ServiceHistoryID is column index 2.
 *       AppointmentID is at index 3, Amount at index 4, etc.
 */
public class PaymentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "payments.txt";

    private static final int EXPECTED_COLUMNS = 8;

    // ─────────────────────────────────────────────────────────────
    // getAllPayments() — read every payment row from payments.txt
    //
    // Each returned String[] has 8 elements:
    //   [0] paymentID     [1] customerID      [2] serviceHistoryID
    //   [3] appointmentID [4] amount           [5] paymentDate
    //   [6] method        [7] status
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
    // that have already been paid by a given customer.
    //
    // Used by CustomerDashboard to filter out already-paid appointments
    // from the Pending Payment list.
    //
    // Example: C3 paid for AP3 → returns {"AP3"}
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

                // columns[1] = CustomerID, columns[3] = AppointmentID, columns[7] = Status
                String customerIdInFile = columns[1].trim();
                String appointmentId    = columns[3].trim();
                String status           = columns[7].trim();

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
    // getPaymentByAppointmentId() — find one payment record
    // by its appointment ID. Returns null if not found.
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

                // columns[3] = AppointmentID
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
    // Scans payments.txt for the highest PY number and adds 1.
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
    // savePayment() — saves a new payment to payments.txt AND
    //                 updates the matching row in serviceHistory.txt.
    //
    // Steps:
    //   1. Generate the next PY ID (e.g. "PY8")
    //   2. Find the matching SH row in serviceHistory.txt for this appointment
    //   3. Append a new row to payments.txt
    //   4. Update serviceHistory.txt PaymentID column from "NULL" → "PY8"
    //
    // Parameters:
    //   userId        - customer ID e.g. "C3"
    //   appointmentId - the appointment being paid e.g. "AP4"
    //   amount        - amount string e.g. "1050.00"
    //   method        - "Cash", "Card", or "Online"
    //
    // Returns true if the payment was saved successfully.
    // ─────────────────────────────────────────────────────────────
    public boolean savePayment(String userId, String appointmentId,
                               String amount, String method) {
        File file = new File(FILE_PATH);
        try {
            // Create the TxtFile folder if it doesn't exist
            file.getParentFile().mkdirs();

            // Get today's date in Malaysia time (UTC+8)
            java.time.LocalDate today = java.time.LocalDate.now(
                    java.time.ZoneId.of("Asia/Kuala_Lumpur"));
            String paymentDate = today.toString(); // e.g. "2026-03-27"

            // Generate the next payment ID, e.g. "PY8"
            String newPaymentId = generateNextPaymentId();

            // Find the SH ID for this appointment from serviceHistory.txt
            // We need it to fill column 2 in the payments.txt row.
            String shId = findServiceHistoryId(appointmentId);

            // Append the new payment line to payments.txt
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                // Format: PY8,C3,SH4,AP4,1050.00,2026-03-27,Cash,Paid
                writer.write(newPaymentId   + ","
                           + userId         + ","
                           + shId           + ","  // "SH4" or "NULL" if no history yet
                           + appointmentId  + ","
                           + amount         + ","
                           + paymentDate    + ","
                           + method         + ","
                           + "Paid");
                writer.newLine();
            }

            // Update serviceHistory.txt: change the "NULL" PaymentID to the real PY ID.
            // This links the service history record to this new payment record.
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
    // SH ID that matches a given appointment ID.
    //
    // serviceHistory.txt format:
    //   SH1,C1,AP1,PY1,T1,2026-03-05,Completed
    //   ↑              ↑
    //   column 0       column 2 = AppointmentID
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
                String[] cols = line.split(",", 7);
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

        return "NULL"; // no matching SH record found
    }
}