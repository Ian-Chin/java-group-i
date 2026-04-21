package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServiceHistoryService handles reading and writing records in serviceHistory.txt.
 *
 * File format (8 columns):
 *   ServiceHistoryID , CustomerID , AppointmentID , VehicleID ,
 *   PaymentID , TechnicianID , ServiceDate , Status
 *
 * Example:
 *   SH1,C1,AP1,V1,PY1,T1,2026-03-05,Completed
 *   SH3,C3,AP3,V3,NULL,T3,2026-03-20,Completed  ← NULL = not paid yet
 *
 * METHODS ADDED (moved from ServiceHistoryPage.java):
 *  - getRecordsForCustomer()  : filters all records down to one customer's rows.
 *  - buildTableRows()         : converts raw records into display-ready Object[] rows
 *                               for the JTable (resolves IDs → names/plates/types).
 *  - getSummaryStats()        : derives the three stat-card values from a record list.
 *  - resolveTechnicianName()  : looks up a technician ID and returns the user's name.
 *  - resolveVehicleType()     : looks up a vehicle ID and returns the vehicle type.
 *  - resolveCarPlate()        : looks up a vehicle ID and returns the car plate.
 *
 * METHOD MOVED FROM PaymentService.java (unchanged):
 *  - findServiceHistoryId()   : searches serviceHistory.txt for the SH row that
 *                               matches a given appointment ID and returns its SH ID.
 */
public class ServiceHistoryService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "serviceHistory.txt";

    private static final int EXPECTED_COLUMNS = 8;

    // ─────────────────────────────────────────────────────────────
    // Dependent services — used by the ID-resolution helpers below.
    // Declared here so ServiceHistoryPage no longer needs to import
    // or instantiate AccountService / VehicleService itself.
    // ─────────────────────────────────────────────────────────────
    private final AccountService accountService = new AccountService();
    private final VehicleService vehicleService = new VehicleService();

    // ═══════════════════════════════════════════════════════════════
    // Inner class: SummaryStats
    //
    // A plain data-holder returned by getSummaryStats().
    // ServiceHistoryPage reads the fields directly to update its
    // stat-card labels — no business logic lives in the view.
    // ═══════════════════════════════════════════════════════════════
    public static class SummaryStats {
        /** Total number of service records for this customer. */
        public final int    totalServices;

        /** Date string of the most recent service (e.g. "2026-03-20"). */
        public final String latestServiceDate;

        /** "<VehicleType> — <Status>" for the most recent service. */
        public final String latestServiceSubText;

        /** Resolved full name of the most-visited technician. */
        public final String favTechName;

        /** e.g. "3 services handled" for the favourite technician. */
        public final String favTechSubText;

        public SummaryStats(int totalServices,
                            String latestServiceDate,
                            String latestServiceSubText,
                            String favTechName,
                            String favTechSubText) {
            this.totalServices       = totalServices;
            this.latestServiceDate   = latestServiceDate;
            this.latestServiceSubText = latestServiceSubText;
            this.favTechName         = favTechName;
            this.favTechSubText      = favTechSubText;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getAllRecords() — reads every row from serviceHistory.txt
    //
    // Each returned String[] has 8 elements:
    //   [0] serviceHistoryID  [1] customerID    [2] appointmentID
    //   [3] vehicleID         [4] paymentID     [5] technicianID
    //   [6] serviceDate       [7] status
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

    // ═══════════════════════════════════════════════════════════════
    // getRecordsForCustomer()
    // (moved from ServiceHistoryPage.refresh())
    //
    // Filters the full record list down to rows that belong to
    // the given customer ID and returns them.
    //
    // Called by:
    //   ServiceHistoryPage.refresh() — replaces the inline filter loop
    //
    // @param customerId  e.g. "C1"
    // @return list of String[8] rows for that customer (may be empty)
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getRecordsForCustomer(String customerId) {
        List<String[]> myRecords = new ArrayList<>();
        for (String[] row : getAllRecords()) {
            if (row[1].trim().equalsIgnoreCase(customerId)) {
                myRecords.add(row);
            }
        }
        return myRecords;
    }

    // ═══════════════════════════════════════════════════════════════
    // getSummaryStats()
    // (moved from ServiceHistoryPage.updateStatsCards())
    //
    // Derives the three stat-card values from a non-empty record list
    // and returns them wrapped in a SummaryStats object.
    //
    // Called by:
    //   ServiceHistoryPage.refresh() — replaces the inline updateStatsCards() call
    //
    // @param records  non-empty list of String[8] rows (caller must check)
    // @return SummaryStats with all five display strings populated
    // ═══════════════════════════════════════════════════════════════
    public SummaryStats getSummaryStats(List<String[]> records) {
        // Card 1: total count
        int total = records.size();

        // Card 2: latest service — last row in the list
        String[] latest       = records.get(records.size() - 1);
        String   latestDate   = latest[6].trim();
        String   latestSub    = resolveVehicleType(latest[3].trim())
                                + " — " + latest[7].trim();

        // Card 3: technician seen most often
        Map<String, Integer> techCount = new HashMap<>();
        for (String[] row : records) {
            String id = row[5].trim();
            techCount.put(id, techCount.getOrDefault(id, 0) + 1);
        }

        String favId    = "";
        int    favCount = 0;
        for (Map.Entry<String, Integer> e : techCount.entrySet()) {
            if (e.getValue() > favCount) {
                favId    = e.getKey();
                favCount = e.getValue();
            }
        }

        String favTechName = resolveTechnicianName(favId);
        String favTechSub  = favCount + " service" + (favCount > 1 ? "s" : "") + " handled";

        return new SummaryStats(total, latestDate, latestSub, favTechName, favTechSub);
    }

    // ═══════════════════════════════════════════════════════════════
    // buildTableRows()
    // (moved from ServiceHistoryPage.fillTable())
    //
    // Converts raw service-history records into display-ready Object[]
    // rows, resolving all IDs to human-readable values so the view
    // only needs to call tableModel.addRow().
    //
    // Called by:
    //   ServiceHistoryPage.refresh() — replaces the inline fillTable() call
    //
    // Returned Object[] column order matches the JTable definition:
    //   [0] History ID   [1] Appointment ID  [2] Vehicle (type)
    //   [3] Car Plate    [4] Payment ID      [5] Technician
    //   [6] Date         [7] Status
    //
    // @param records  list of String[8] rows to convert
    // @return list of Object[8] rows ready to be fed to DefaultTableModel
    // ═══════════════════════════════════════════════════════════════
    public List<Object[]> buildTableRows(List<String[]> records) {
        List<Object[]> rows = new ArrayList<>();

        for (String[] row : records) {
            String historyId     = row[0].trim();
            String appointmentId = row[2].trim();
            String vehicleId     = row[3].trim();
            String paymentId     = row[4].trim();
            String techId        = row[5].trim();
            String date          = row[6].trim();
            String status        = row[7].trim();

            String techName       = resolveTechnicianName(techId);
            String vehicleType    = resolveVehicleType(vehicleId);
            String carPlate       = resolveCarPlate(vehicleId);
            String paymentDisplay = paymentId.equalsIgnoreCase("NULL") ? "—" : paymentId;

            rows.add(new Object[]{
                historyId, appointmentId, vehicleType, carPlate,
                paymentDisplay, techName, date, status
            });
        }

        return rows;
    }

    // ═══════════════════════════════════════════════════════════════
    // resolveTechnicianName()
    // (moved from ServiceHistoryPage)
    //
    // Looks up a technician ID in the accounts list and returns the
    // user's full name. Falls back to the raw ID if not found.
    //
    // Called by:
    //   getSummaryStats()  — for the favourite-technician stat card
    //   buildTableRows()   — for the Technician column in the table
    //
    // @param techId  e.g. "T1"
    // @return full name e.g. "Alice Smith", or techId as fallback
    // ═══════════════════════════════════════════════════════════════
    public String resolveTechnicianName(String techId) {
        for (model.User user : accountService.getAllUsers()) {
            if (user.getUserId().equalsIgnoreCase(techId)) {
                return user.getName();
            }
        }
        return techId; // fallback: show the raw ID if name not found
    }

    // ═══════════════════════════════════════════════════════════════
    // resolveVehicleType()
    // (moved from ServiceHistoryPage)
    //
    // Looks up a vehicle ID and returns the vehicle type portion of
    // the label string (the part before " · ").
    //
    // Called by:
    //   getSummaryStats()  — for the latest-service sub-label
    //   buildTableRows()   — for the Vehicle column in the table
    //
    // @param vehicleId  e.g. "V1"
    // @return vehicle type e.g. "Toyota Vios", or vehicleId as fallback
    // ═══════════════════════════════════════════════════════════════
    public String resolveVehicleType(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[0].trim() : vehicleId;
    }

    // ═══════════════════════════════════════════════════════════════
    // resolveCarPlate()
    // (moved from ServiceHistoryPage)
    //
    // Looks up a vehicle ID and returns the car-plate portion of the
    // label string (the part after " · ").
    //
    // Called by:
    //   buildTableRows()  — for the Car Plate column in the table
    //
    // @param vehicleId  e.g. "V1"
    // @return car plate e.g. "WXY 1234", or vehicleId as fallback
    // ═══════════════════════════════════════════════════════════════
    public String resolveCarPlate(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[1].trim() : vehicleId;
    }

    // ─────────────────────────────────────────────────────────────
    // appointmentAlreadyRecorded() — checks if an appointment ID
    // already has a record in serviceHistory.txt.
    // ─────────────────────────────────────────────────────────────
    public boolean appointmentAlreadyRecorded(String appointmentId) {
        for (String[] row : getAllRecords()) {
            if (row[2].trim().equalsIgnoreCase(appointmentId)) {
                return true;
            }
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────
    // generateNextId() — returns the next SH number.
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
                writer.write(newId        + ","
                           + customerId   + ","
                           + appointmentId+ ","
                           + vehicleId    + ","
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

    // ═══════════════════════════════════════════════════════════════
    // findServiceHistoryId()
    // (moved from PaymentService.java — was a private helper there)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Searches serviceHistory.txt for the row whose AppointmentID matches
     * the given appointmentId, and returns its ServiceHistoryID (e.g. "SH4").
     *
     * MOVED FROM: private PaymentService.findServiceHistoryId()
     * Reason: this method reads serviceHistory.txt — that file is owned by
     *         this service. PaymentService now calls this method instead of
     *         having its own private copy that reads the same file.
     *
     * Called by:
     *   PaymentService.savePayment() — needs the SH ID to write into payments.txt
     *
     * serviceHistory.txt column layout (8 columns):
     *   [0] SH_ID  [1] customerID  [2] appointmentID  [3] vehicleID
     *   [4] paymentID  [5] technicianID  [6] serviceDate  [7] status
     *
     * @param appointmentId  e.g. "AP4"
     * @return the matching SH ID e.g. "SH4", or "NULL" if not found
     */
    public String findServiceHistoryId(String appointmentId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return "NULL";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length >= 3) {
                    // Column [2] = AppointmentID
                    if (cols[2].trim().equalsIgnoreCase(appointmentId)) {
                        return cols[0].trim(); // Column [0] = ServiceHistoryID
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "NULL";
    }

    // ─────────────────────────────────────────────────────────────
    // updatePaymentId() — updates the PaymentID column from "NULL"
    //                     to the real PY ID once a customer pays.
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
                    // Rebuild line with updated PaymentID at column index [4]
                    String updatedLine = cols[0].trim() + ","   // SH ID
                                      + cols[1].trim() + ","   // customerID
                                      + cols[2].trim() + ","   // appointmentID
                                      + cols[3].trim() + ","   // vehicleID
                                      + newPaymentId   + ","   // paymentID (was NULL)
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