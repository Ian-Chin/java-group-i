package model;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * ServiceHistoryService handles reading and writing records in serviceHistory.txt.
 *
 * File format (8 columns):
 *   ServiceHistoryID , CustomerID , AppointmentID , VehicleID ,
 *   PaymentID , TechnicianID , ServiceDate , Status
 */
public class ServiceHistoryService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "serviceHistory.txt";

    private static final int EXPECTED_COLUMNS = 8;

    // ─────────────────────────────────────────────────────────────
    // Dependent services — used by the ID-resolution helpers below.
    // ─────────────────────────────────────────────────────────────
    private final AccountService accountService = new AccountService();
    private final VehicleService vehicleService = new VehicleService();

    // ═══════════════════════════════════════════════════════════════
    // Inner class: SummaryStats
    //
    // A plain data-holder returned by getSummaryStats().
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
            this.totalServices        = totalServices;
            this.latestServiceDate    = latestServiceDate;
            this.latestServiceSubText = latestServiceSubText;
            this.favTechName          = favTechName;
            this.favTechSubText       = favTechSubText;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getAllRecords()
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
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getRecordsForCustomer(String customerId) {
        List<String[]> myRecords = new ArrayList<>();
        for (String[] row : getAllRecords()) {
            // Column [1] = customerID — keep the row if it matches
            if (row[1].trim().equalsIgnoreCase(customerId)) {
                myRecords.add(row);
            }
        }
        return myRecords;
    }

    // ═══════════════════════════════════════════════════════════════
    // getSummaryStats()
    // ═══════════════════════════════════════════════════════════════
    public SummaryStats getSummaryStats(List<String[]> records) {

        // ── Card 1: total count ───────────────────────────────────
        int total = records.size();

        // ── Card 2: latest service ────────────────────────────────
        // The last row in the list is the most recent record.
        String[] latest     = records.get(records.size() - 1);
        String   latestDate = latest[6].trim();
        // Sub-label shows vehicle type and the service status
        String   latestSub  = resolveVehicleType(latest[3].trim())
                              + " — " + latest[7].trim();

        // ── Card 3: most-visited technician ──────────────────────
        // Count how many times each technician ID appears
        Map<String, Integer> techCount = new HashMap<>();
        for (String[] row : records) {
            String id = row[5].trim(); // column [5] = technicianID
            techCount.put(id, techCount.getOrDefault(id, 0) + 1);
        }

        // Find the technician ID with the highest count
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

            // Resolve IDs to human-readable display values
            String techName       = resolveTechnicianName(techId);
            String vehicleType    = resolveVehicleType(vehicleId);
            String carPlate       = resolveCarPlate(vehicleId);

            // Show a dash instead of "NULL" for unpaid records
            String paymentDisplay = paymentId.equalsIgnoreCase("NULL") ? "—" : paymentId;

            rows.add(new Object[]{
                historyId, appointmentId, vehicleType, carPlate,
                paymentDisplay, techName, date, status
            });
        }

        return rows;
    }

    // ═══════════════════════════════════════════════════════════════
    // filterRecords()
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> filterRecords(List<String[]> records, String keyword) {

        // If the keyword is empty or null, return everything unchanged
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>(records);
        }

        // Lowercase the keyword once here so we do not repeat it in the loop
        String lowerKeyword = keyword.trim().toLowerCase();

        List<String[]> filtered = new ArrayList<>();

        for (String[] row : records) {
            // Join all columns into one searchable string
            // String.join(" ", row) produces: "SH1 C1 AP1 V1 PY1 T1 2026-03-05 Completed"
            String combined = String.join(" ", row).toLowerCase();

            // Keep this row if the keyword appears anywhere in the combined string
            if (combined.contains(lowerKeyword)) {
                filtered.add(row);
            }
        }

        return filtered;
    }

    // ═══════════════════════════════════════════════════════════════
    // sortRecords()
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> sortRecords(List<String[]> records, String sortOption) {

        // Make a copy so we never modify the caller's original list
        List<String[]> sorted = new ArrayList<>(records);

        // No option chosen — return the unsorted copy as-is
        if (sortOption == null || sortOption.equals("Sort by...")) {
            return sorted;
        }

        switch (sortOption) {

            case "Date (Newest)":
                // Column [6] = serviceDate  e.g. "2026-03-20"
                // We compare b vs a (reversed) so the newest date comes first.
                sorted.sort((a, b) -> b[6].trim().compareTo(a[6].trim()));
                break;

            case "Date (Oldest)":
                // We compare a vs b so the oldest date comes first.
                sorted.sort((a, b) -> a[6].trim().compareTo(b[6].trim()));
                break;

            case "Technician A\u2192Z":
                // Column [5] = technicianID  e.g. "T1", "T2"
                // Simple alphabetical sort on the raw technician ID.
                // \u2192 is the → arrow character.
                sorted.sort((a, b) -> a[5].trim().compareTo(b[5].trim()));
                break;

            default:
                // Unknown option — return the unsorted copy
                break;
        }

        return sorted;
    }

    // ═══════════════════════════════════════════════════════════════
    // getAvailableSortOptions()
    // ═══════════════════════════════════════════════════════════════
    public String[] getAvailableSortOptions() {
        return new String[] {
            "Sort by...",          // index 0 — default placeholder (no sort applied)
            "Date (Newest)",       // index 1 — most recent service date first
            "Date (Oldest)",       // index 2 — oldest service date first
            "Technician A\u2192Z"  // index 3 — technician ID A to Z (\u2192 = →)
        };
    }

    // ═══════════════════════════════════════════════════════════════
    // applyFilter()
    // ═══════════════════════════════════════════════════════════════
    public void applyFilter(TableRowSorter<DefaultTableModel> rowSorter, String searchText) {
        if (rowSorter == null) return;

        String text = (searchText == null) ? "" : searchText.trim();

        // Treat the placeholder as "no filter" — show all rows
        if (text.equals("Search...") || text.isEmpty()) {
            rowSorter.setRowFilter(null);
            return;
        }

        try {
            // (?i) = case-insensitive match
            // Pattern.quote() = treat the text as a literal string,
            //                   not a regex (safe for special characters)
            rowSorter.setRowFilter(RowFilter.regexFilter(
                    "(?i)" + java.util.regex.Pattern.quote(text)));
        } catch (java.util.regex.PatternSyntaxException ignored) {
            // Ignore if the user types characters that break the pattern
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // applySort()
    // ═══════════════════════════════════════════════════════════════
    public void applySort(TableRowSorter<DefaultTableModel> rowSorter, String selectedOption) {
        if (rowSorter == null) return;

        // Build the list of sort keys to hand to the TableRowSorter
        List<RowSorter.SortKey> keys = new ArrayList<>();

        if (selectedOption == null || selectedOption.equals("Sort by...")) {
            // "Sort by..." = no sort — remove all sort keys
            rowSorter.setSortKeys(null);
            return;
        }

        switch (selectedOption) {

            case "Date (Newest)":
                // Column 6 = Date ("YYYY-MM-DD"), DESCENDING = newest first.
                // String comparison works correctly for this date format:
                //   "2026-03-20" > "2026-03-05"  →  2026-03-20 comes first.
                keys.add(new RowSorter.SortKey(6, SortOrder.DESCENDING));
                break;

            case "Date (Oldest)":
                // ASCENDING = oldest date first
                keys.add(new RowSorter.SortKey(6, SortOrder.ASCENDING));
                break;

            case "Technician A\u2192Z":
                // Column 5 = Technician name, ASCENDING = A to Z
                // \u2192 is the → arrow character used in the dropdown label
                keys.add(new RowSorter.SortKey(5, SortOrder.ASCENDING));
                break;

            default:
                // Unknown option — clear any existing sort
                rowSorter.setSortKeys(null);
                return;
        }

        // Apply the chosen sort key to the table
        rowSorter.setSortKeys(keys);
    }

    // ═══════════════════════════════════════════════════════════════
    // resolveTechnicianName()
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
    // ═══════════════════════════════════════════════════════════════
    public String resolveVehicleType(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[0].trim() : vehicleId;
    }

    // ═══════════════════════════════════════════════════════════════
    // resolveCarPlate()
    // ═══════════════════════════════════════════════════════════════
    public String resolveCarPlate(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[1].trim() : vehicleId;
    }

    // ─────────────────────────────────────────────────────────────
    // appointmentAlreadyRecorded()
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
    // generateNextId()
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
    // addRecord()
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
                writer.write(newId         + ","
                           + customerId    + ","
                           + appointmentId + ","
                           + vehicleId     + ","
                           + paymentId     + ","
                           + technicianId  + ","
                           + serviceDate   + ","
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
    // ═══════════════════════════════════════════════════════════════
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
    // updatePaymentId()
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
                    // Rebuild the line with the updated PaymentID at index [4]
                    String updatedLine = cols[0].trim() + ","  // SH ID
                                      + cols[1].trim() + ","  // customerID
                                      + cols[2].trim() + ","  // appointmentID
                                      + cols[3].trim() + ","  // vehicleID
                                      + newPaymentId   + ","  // paymentID (was NULL)
                                      + cols[5].trim() + ","  // technicianID
                                      + cols[6].trim() + ","  // serviceDate
                                      + cols[7].trim();       // status
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