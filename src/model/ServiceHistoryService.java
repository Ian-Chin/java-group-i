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
 *
 * Example:
 *   SH1,C1,AP1,V1,PY1,T1,2026-03-05,Completed
 *   SH3,C3,AP3,V3,NULL,T3,2026-03-20,Completed
 *
 * METHODS ADDED (moved from ServiceHistoryPage.java):
 *  - getRecordsForCustomer()  : filters all records down to one customer's rows.
 *  - buildTableRows()         : converts raw records into display-ready Object[] rows.
 *  - getSummaryStats()        : derives the three stat-card values from a record list.
 *  - resolveTechnicianName()  : looks up a technician ID and returns the user's name.
 *  - resolveVehicleType()     : looks up a vehicle ID and returns the vehicle type.
 *  - resolveCarPlate()        : looks up a vehicle ID and returns the car plate.
 *
 * METHODS ADDED (new — filter/sort feature):
 *  - filterRecords()          : keyword search across all columns of a record list.
 *  - sortRecords()            : sorts a record list by date or technician.
 *  - getAvailableSortOptions(): returns the dropdown labels used by the view.
 *  - applyFilter()            : applies a live keyword RowFilter to a TableRowSorter.
 *  - applySort()              : applies a sort key to a TableRowSorter by option label.
 *
 * METHOD MOVED FROM PaymentService.java:
 *  - findServiceHistoryId()   : searches serviceHistory.txt for the SH row that
 *                               matches a given appointment ID.
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
            this.totalServices        = totalServices;
            this.latestServiceDate    = latestServiceDate;
            this.latestServiceSubText = latestServiceSubText;
            this.favTechName          = favTechName;
            this.favTechSubText       = favTechSubText;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // getAllRecords()
    //
    // Reads every row from serviceHistory.txt and returns them as a
    // list of String arrays. Each array has 8 elements:
    //   [0] serviceHistoryID  [1] customerID    [2] appointmentID
    //   [3] vehicleID         [4] paymentID     [5] technicianID
    //   [6] serviceDate       [7] status
    //
    // Blank lines and comment lines (starting with #) are skipped.
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
    //
    // Filters the full record list down to only the rows that belong
    // to the given customer ID and returns them as a new list.
    //
    // How it works:
    //   We loop through every row returned by getAllRecords().
    //   Column [1] of each row is the customerID.
    //   If it matches the given customerId (ignoring case), we keep it.
    //
    // Called by:
    //   ServiceHistoryPage.refresh()
    //
    // @param customerId  e.g. "C1"
    // @return list of String[8] rows for that customer (may be empty)
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
    //
    // Derives the three stat-card values from a non-empty record list
    // and returns them wrapped in a SummaryStats object.
    //
    // What each stat card shows:
    //   Card 1 — Total services   : count of all records in the list
    //   Card 2 — Latest service   : date + vehicle type of the last record
    //   Card 3 — Favourite tech   : technician that appears most often
    //
    // Called by:
    //   ServiceHistoryPage.refresh()
    //
    // @param records  non-empty list of String[8] rows (caller must check)
    // @return SummaryStats with all five display strings populated
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
    //
    // Converts raw service-history records into display-ready Object[]
    // rows, resolving all IDs to human-readable values so the view
    // only needs to call tableModel.addRow().
    //
    // Returned Object[] column order matches the JTable definition:
    //   [0] History ID   [1] Appointment ID  [2] Vehicle (type)
    //   [3] Car Plate    [4] Payment ID      [5] Technician
    //   [6] Date         [7] Status
    //
    // Called by:
    //   ServiceHistoryPage.refresh()
    //
    // @param records  list of String[8] rows to convert
    // @return list of Object[8] rows ready to feed into DefaultTableModel
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
    //
    // Searches through a list of service history records and keeps only
    // the rows where ANY column contains the search keyword.
    //
    // How it works:
    //   For each row, we join all 8 column values into one big string,
    //   then check whether the keyword appears anywhere in it.
    //   Both the keyword and the combined string are lowercased first,
    //   so the search is case-insensitive ("toyota" matches "Toyota").
    //
    // Example:
    //   keyword = "completed"
    //   row     = ["SH1","C1","AP1","V1","PY1","T1","2026-03-05","Completed"]
    //   joined  = "SH1 C1 AP1 V1 PY1 T1 2026-03-05 Completed"
    //   → "completed" is found → row is kept
    //
    // Note:
    //   In ServiceHistoryPage the live table search is handled by
    //   TableRowSorter (which is faster for an already-loaded table).
    //   This method is the model-layer equivalent — use it if you ever
    //   need to filter records before they are loaded into the table.
    //
    // Called by:
    //   ServiceHistoryPage (available for future use)
    //
    // @param records  the list of String[8] rows to search through
    // @param keyword  the text the user typed in the search box
    // @return a new list containing only the rows that match
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
    //
    // Sorts a list of service history records by the chosen option
    // and returns a NEW sorted list (the original list is not changed).
    //
    // Sort options (must exactly match the labels in getAvailableSortOptions()):
    //   "Date (Newest)"   — most recent service date first  (descending)
    //   "Date (Oldest)"   — oldest service date first       (ascending)
    //   "Technician A→Z"  — technician ID alphabetically    (ascending)
    //
    // How date sorting works:
    //   Dates are stored as "YYYY-MM-DD" strings (e.g. "2026-03-05").
    //   Because the year comes first, a plain String.compareTo() gives
    //   the correct chronological order — no date parsing is needed.
    //     "2026-03-20".compareTo("2026-03-05") > 0  → 2026-03-20 is later
    //
    // Note:
    //   In ServiceHistoryPage the live table sort is handled by
    //   TableRowSorter (which works on already-loaded rows).
    //   This method is the model-layer equivalent — use it if you ever
    //   need to sort records before they are loaded into the table.
    //
    // Called by:
    //   ServiceHistoryPage (available for future use)
    //
    // @param records     the list of String[8] rows to sort
    // @param sortOption  one of the strings from getAvailableSortOptions()
    // @return a NEW sorted list (original list is never modified)
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
    //
    // Returns the array of sort-option labels that should appear in
    // the dropdown (JComboBox) inside ServiceHistoryPage.
    //
    // Rules:
    //   • The FIRST item must always be "Sort by..." — this is the
    //     default "nothing selected" placeholder (index 0).
    //   • The remaining strings must EXACTLY match the case and spelling
    //     used inside sortRecords() so the switch statement works.
    //   • The strings must also EXACTLY match the cases inside
    //     ServiceHistoryService.applySort() for the same reason.
    //
    // Why this method exists:
    //   By putting the labels here in the service layer, the view file
    //   (ServiceHistoryPage) never hard-codes them. If you want to add
    //   or rename a sort option, you only edit this one method and the
    //   switch in sortRecords() / applySort() — nothing else changes.
    //
    // Called by:
    //   ServiceHistoryPage.buildSearchSortBar() — to populate the JComboBox
    //
    // @return String array of option labels, index 0 is always "Sort by..."
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
    //
    // Applies a live keyword RowFilter to the given TableRowSorter.
    // Called by ServiceHistoryPage on every keystroke in the search field.
    //
    // How it works:
    //   • If the text is empty or the "Search..." placeholder, the filter
    //     is cleared so all rows are visible.
    //   • Otherwise a case-insensitive regex filter is set that matches any
    //     row whose combined column text contains the search term.
    //   • Pattern.quote() wraps the text so special characters (e.g. ".")
    //     are treated as literals rather than regex metacharacters.
    //
    // Works alongside applySort() — both can be active at the same time.
    //
    // Column index reference for this table:
    //   0 = History ID     1 = Appointment ID   2 = Vehicle
    //   3 = Car Plate      4 = Payment ID        5 = Technician
    //   6 = Date           7 = Status
    //
    // Called by:
    //   ServiceHistoryPage.applyFilter()
    //
    // @param rowSorter   the sorter attached to the service-history JTable
    // @param searchText  the raw text currently in the search field
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
    //
    // Applies a sort key to the given TableRowSorter based on the
    // option label selected in the sort dropdown.
    // Called by ServiceHistoryPage when the user changes the dropdown.
    //
    // How it works:
    //   Reads the selected option label and maps it to a column index
    //   and sort direction, then hands a SortKey to the TableRowSorter.
    //   "Sort by..." clears all sort keys (returns to insertion order).
    //
    // Works alongside applyFilter() — both can be active at the same time.
    //
    // Column index reference for this table:
    //   0 = History ID     1 = Appointment ID   2 = Vehicle
    //   3 = Car Plate      4 = Payment ID        5 = Technician
    //   6 = Date           7 = Status
    //
    // A RowSorter.SortKey pairs a column index with a direction:
    //   SortOrder.ASCENDING  = A → Z  /  oldest → newest
    //   SortOrder.DESCENDING = Z → A  /  newest → oldest
    //
    // Called by:
    //   ServiceHistoryPage.applySort()
    //
    // @param rowSorter      the sorter attached to the service-history JTable
    // @param selectedOption the label currently selected in the sort JComboBox
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
    //
    // Looks up a vehicle ID and returns the vehicle type portion of
    // the label string (the part before " · ").
    //
    // Example:
    //   vehicleService.getVehiclePlate("V1") returns "Toyota Vios · WXY 1234"
    //   resolveVehicleType("V1")             returns "Toyota Vios"
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
    //
    // Looks up a vehicle ID and returns the car-plate portion of the
    // label string (the part after " · ").
    //
    // Example:
    //   vehicleService.getVehiclePlate("V1") returns "Toyota Vios · WXY 1234"
    //   resolveCarPlate("V1")                returns "WXY 1234"
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
    // appointmentAlreadyRecorded()
    //
    // Returns true if the given appointment ID already has a row
    // in serviceHistory.txt. Used to prevent duplicate records.
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
    //
    // Scans all existing SH IDs, finds the highest number,
    // and returns the next one (e.g. if "SH5" is the highest,
    // this returns "SH6").
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
    //
    // Appends one new completed service record to serviceHistory.txt.
    // The new SH ID is auto-generated by generateNextId().
    // Returns true on success, false if an IOException occurs.
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
    //
    // Searches serviceHistory.txt for the row whose AppointmentID
    // matches the given appointmentId, and returns its SH ID.
    //
    // serviceHistory.txt column layout (8 columns):
    //   [0] SH_ID  [1] customerID  [2] appointmentID  [3] vehicleID
    //   [4] paymentID  [5] technicianID  [6] serviceDate  [7] status
    //
    // Called by:
    //   PaymentService.savePayment()
    //
    // @param appointmentId  e.g. "AP4"
    // @return the matching SH ID e.g. "SH4", or "NULL" if not found
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
    //
    // Updates the PaymentID column (index [4]) in serviceHistory.txt
    // from "NULL" to the real PY ID once a customer pays.
    // Rewrites the entire file with the one updated line.
    // Returns true on success, false if the record was not found
    // or an IOException occurs.
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