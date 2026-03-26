package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PaymentService reads payment data from payments.txt.
 *
 * File format — each line has 7 values separated by commas:
 *   PaymentID , UserID , AppointmentID , Amount , PaymentDate , Method , Status
 *
 * Example:
 *   PY1,C1,AP1,320.00,2025-03-05,Cash,Paid
 */
public class PaymentService {

    // Path to the payments data file
    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "payments.txt";

    private static final int EXPECTED_COLUMNS = 7;

    /**
     * Returns a Set of appointment IDs that have already been Paid.
     * Used to filter out already-paid appointments from the Pending Payment list.
     *
     * Example result: {"AP1", "AP2", "AP3"}
     *
     * @param userId  the customer's user ID
     */
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

                String userIdInFile  = columns[1].trim(); // UserID
                String appointmentId = columns[2].trim(); // AppointmentID
                String status        = columns[6].trim(); // Status (Paid / Unpaid)

                // Only add if it belongs to this user AND status is Paid
                if (userIdInFile.equalsIgnoreCase(userId)
                        && status.equalsIgnoreCase("Paid")) {
                    paidIds.add(appointmentId);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return paidIds;
    }

    /**
     * Returns the payment details for a given appointment ID.
     * Returns null if no payment record exists for that appointment.
     *
     * Returned array:
     *   [0] paymentID    e.g. "PY1"
     *   [1] userId       e.g. "C1"
     *   [2] appointmentId e.g. "AP1"
     *   [3] amount       e.g. "320.00"
     *   [4] paymentDate  e.g. "2025-03-05"
     *   [5] method       e.g. "Cash"
     *   [6] status       e.g. "Paid"
     */
    public String[] getPaymentByAppointmentId(String appointmentId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                String[] columns = line.split(",", EXPECTED_COLUMNS);
                if (columns.length != EXPECTED_COLUMNS) continue;

                if (columns[2].trim().equalsIgnoreCase(appointmentId)) {
                    return columns; // return the full payment row
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null; // no payment record found
    }

    /**
     * Generates the next payment ID.
     * Example: if PY3 exists, returns "PY4"
     */
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
                        // Payment IDs look like "PY1", "PY2", etc.
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

    /**
     * Saves a new payment record to payments.txt.
     *
     * @param userId        the customer's user ID
     * @param appointmentId the appointment being paid for
     * @param amount        the amount paid (e.g. "320.00")
     * @param method        payment method chosen by the user (e.g. "Cash", "Card", "Online")
     * @return true if saved successfully
     */
    public boolean savePayment(String userId, String appointmentId,
                                String amount, String method) {
        File file = new File(FILE_PATH);
        try {
            file.getParentFile().mkdirs();

            // Get today's date in Malaysia time
            java.time.LocalDate today = java.time.LocalDate.now(
                    java.time.ZoneId.of("Asia/Kuala_Lumpur"));
            String paymentDate = today.toString(); // "2025-03-26"

            String newPaymentId = generateNextPaymentId();

            // Write one new line to the file (append mode)
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(newPaymentId + ","
                        + userId + ","
                        + appointmentId + ","
                        + amount + ","
                        + paymentDate + ","
                        + method + ","
                        + "Paid");
                writer.newLine();
            }

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}