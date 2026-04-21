package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PaymentService reads and writes payment data from payments.txt.
 *
 * File format — each line has 9 values:
 *   PaymentID , CustomerID , ServiceHistoryID , AppointmentID ,
 *   VehicleID , Amount , PaymentDate , Method , Status
 *
 * Example:
 *   PY1,C1,SH1,AP1,V1,120.00,2026-03-05,Cash,Paid
 *
 * METHODS MOVED FROM CustomerDashboard.java:
 *  - calcPendingAmount()  : totals RM due across all unpaid appointment rows
 *  - calcTotalSpent()     : totals RM for completed + paid appointments (for charts)
 *
 * METHODS MOVED FROM PaymentHistoryPage.java:
 *  - readPaymentsFromFile()    : reads all payment rows from payments.txt
 *  - getPaymentsForCustomer()  : filters rows by customer ID
 *  - calcTotalPaidAmount()     : sums RM for all "Paid" rows
 *  - countPaidRows()           : counts rows with status "Paid"
 *  - countPendingRows()        : counts rows with status "Pending"
 *  - getPreferredMethod()      : finds the most-used payment method
 *
 * CHANGE: findServiceHistoryId() has been removed from this class.
 *   It was a private method that read serviceHistory.txt — that file is owned
 *   by ServiceHistoryService, so the method now lives there as a public method.
 *   savePayment() now calls serviceHistoryService.findServiceHistoryId() instead.
 */
public class PaymentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "payments.txt";

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
    // readPaymentsFromFile() — reads all payment rows from payments.txt.
    // Alias of getAllPayments(); kept as a named method for clarity
    // when called from PaymentHistoryPage.
    //
    // MOVED FROM: PaymentHistoryPage.readPaymentsFromFile()
    // ─────────────────────────────────────────────────────────────
    public List<String[]> readPaymentsFromFile() {
        return getAllPayments();
    }

    // ─────────────────────────────────────────────────────────────
    // getPaymentsForCustomer() — filters all payment rows to only
    // those that belong to the given customer ID.
    //
    // MOVED FROM: PaymentHistoryPage.refresh() (inline filter loop)
    // ─────────────────────────────────────────────────────────────
    public List<String[]> getPaymentsForCustomer(String customerId) {
        List<String[]> result = new ArrayList<>();
        for (String[] row : readPaymentsFromFile()) {
            if (row[1].trim().equalsIgnoreCase(customerId)) {
                result.add(row);
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // calcTotalPaidAmount() — sums the RM amount for every row
    // whose status is "Paid".
    //
    // MOVED FROM: PaymentHistoryPage.updateStatsCards() (inline loop)
    // ─────────────────────────────────────────────────────────────
    public double calcTotalPaidAmount(List<String[]> rows) {
        double total = 0.0;
        for (String[] row : rows) {
            if (row[8].trim().equalsIgnoreCase("Paid")) {
                try {
                    total += Double.parseDouble(row[5].trim());
                } catch (NumberFormatException ignored) {}
            }
        }
        return total;
    }

    // ─────────────────────────────────────────────────────────────
    // countPaidRows() — returns the number of rows with status "Paid".
    //
    // MOVED FROM: PaymentHistoryPage.updateStatsCards() (inline loop)
    // ─────────────────────────────────────────────────────────────
    public int countPaidRows(List<String[]> rows) {
        int count = 0;
        for (String[] row : rows) {
            if (row[8].trim().equalsIgnoreCase("Paid")) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────
    // countPendingRows() — returns the number of rows with status "Pending".
    //
    // MOVED FROM: PaymentHistoryPage.updateStatsCards() (inline loop)
    // ─────────────────────────────────────────────────────────────
    public int countPendingRows(List<String[]> rows) {
        int count = 0;
        for (String[] row : rows) {
            if (row[8].trim().equalsIgnoreCase("Pending")) count++;
        }
        return count;
    }

    // ─────────────────────────────────────────────────────────────
    // getPreferredMethod() — returns the payment method that appears
    // most frequently in the given list of rows.
    // Returns "—" if the list is empty.
    //
    // MOVED FROM: PaymentHistoryPage.updateStatsCards() (inline loop)
    // ─────────────────────────────────────────────────────────────
    public String getPreferredMethod(List<String[]> rows) {
        Map<String, Integer> methodCount = new HashMap<>();
        for (String[] row : rows) {
            String method = row[7].trim();
            methodCount.put(method, methodCount.getOrDefault(method, 0) + 1);
        }
        String favMethod = "—";
        int    favCount  = 0;
        for (Map.Entry<String, Integer> entry : methodCount.entrySet()) {
            if (entry.getValue() > favCount) {
                favMethod = entry.getKey();
                favCount  = entry.getValue();
            }
        }
        return favMethod;
    }

    // ─────────────────────────────────────────────────────────────
    // getPaidAppointmentIds() — returns a set of appointment IDs
    // that a given customer has already paid for.
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
    // ─────────────────────────────────────────────────────────────
    public boolean savePayment(String userId, String appointmentId,
                               String vehicleId, String amount, String method) {
        File file = new File(FILE_PATH);
        try {
            file.getParentFile().mkdirs();

            java.time.LocalDate today = java.time.LocalDate.now(
                    java.time.ZoneId.of("Asia/Kuala_Lumpur"));
            String paymentDate = today.toString();

            String newPaymentId = generateNextPaymentId();
            // ── SH ID lookup delegated to ServiceHistoryService ───
            // findServiceHistoryId() reads serviceHistory.txt — that file
            // is owned by ServiceHistoryService, so the method lives there.
            ServiceHistoryService shService = new ServiceHistoryService();
            String shId = shService.findServiceHistoryId(appointmentId);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(newPaymentId  + ","
                           + userId        + ","
                           + shId          + ","
                           + appointmentId + ","
                           + vehicleId     + ","
                           + amount        + ","
                           + paymentDate   + ","
                           + method        + ","
                           + "Paid");
                writer.newLine();
            }

            // Update serviceHistory.txt: change "NULL" paymentID to real PY ID
            shService.updatePaymentId(appointmentId, newPaymentId);

            return true;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // DASHBOARD PAYMENT STAT HELPERS
    // (moved from CustomerDashboard.java)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Totals the RM amount due across all unpaid appointments.
     * Used by the "Pending Payment" stat card (e.g. shows "RM 1,200").
     *
     * Called from CustomerDashboard.buildDashboardInner() via:
     *   paymentService.calcPendingAmount(unpaid, appointmentController)
     *
     * Each row in unpaid has:
     *   [3] serviceType   [6] duration
     * The amount per row is calculated using AppointmentSectionController.calculateAmount().
     *
     * @param unpaid                list of unpaid appointment rows
     * @param appointmentController used to calculate the RM amount per appointment
     * @return total pending amount as a double
     */
    public double calcPendingAmount(List<String[]> unpaid,
                                    AppointmentSectionController appointmentController) {
        double total = 0;
        for (String[] row : unpaid) {
            try {
                total += Double.parseDouble(
                        appointmentController.calculateAmount(row[3], row[6]));
            } catch (NumberFormatException ignored) {}
        }
        return total;
    }

    /**
     * Calculates the total RM spent on completed AND paid appointments.
     * Used by the Service Activity chart stats row ("Total Spent").
     *
     * Called from CustomerDashboard.buildActivityChartCard() via:
     *   paymentService.calcTotalSpent(allAppts, paidIds, appointmentController)
     *
     * Only appointments with status "Completed" that also appear in paidIds are counted.
     * The amount per appointment is calculated using calculateAmount().
     *
     * @param allAppts              list of all appointment rows for the user
     * @param paidIds               set of appointment IDs that have been paid
     * @param appointmentController used to calculate the RM amount per appointment
     * @return total amount spent as a double
     */
    public double calcTotalSpent(List<String[]> allAppts,
                                  Set<String> paidIds,
                                  AppointmentSectionController appointmentController) {
        double total = 0;
        for (String[] row : allAppts) {
            if (row[4].equalsIgnoreCase("Completed") && paidIds.contains(row[0])) {
                try {
                    total += Double.parseDouble(
                            appointmentController.calculateAmount(row[3], row[6]));
                } catch (NumberFormatException ignored) {}
            }
        }
        return total;
    }
}