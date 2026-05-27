package model;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Loads and aggregates all data for the Report dashboard.
 * All file I/O and computation is encapsulated here — the view only calls getters.
 *
 * ── Column layouts (current) ──────────────────────────────────────────────────
 *
 * appointments.txt
 *   [0]apptID  [1]custID  [2]vehicleID  [3]techID  [4]serviceType
 *   [5]status  [6]dateTime(yyyy-MM-dd HH:mm)  [7]durationHrs
 *
 * feedback.txt
 *   [0]FBID  [1]custID  [2]apptID  [3]vehicleID  [4]techID  [5]ratingWords  [6]comment  [7]date
 *
 * accounts.txt
 *   [0]userID  [1]name  [2]email  [3]password  [4]role  [5]profilePic
 *
 * payments.txt
 *   [0]payID  [1]custID  [2]shID  [3]apptID  [4]vehicleID
 *   [5]amount  [6]date  [7]method  [8]status
 *
 * vehicles.txt
 *   [0]vehicleID  [1]custID  [2]type  [3]plate  [4]brandModel  [5]year  [6]colour
 *
 * OOP: Encapsulation — all file paths, parsing, and aggregation are private.
 */
public class DashboardData {

    private static final String SEP  = File.separator;
    private static final String BASE = "src" + SEP + "TxtFile" + SEP;

    private static final String APPT_FILE     = BASE + "appointments.txt";
    private static final String COMMENTS_FILE = BASE + "comments.txt";
    private static final String ACCOUNTS_FILE = BASE + "accounts.txt";
    private static final String PAYMENTS_FILE = BASE + "payments.txt";
    private static final String VEHICLES_FILE = BASE + "vehicles.txt";

    // ── Raw rows ──────────────────────────────────────────────────────────────
    private final List<String[]> appointments = new ArrayList<>();
    private final List<String[]> comments     = new ArrayList<>();
    private final List<String[]> payments     = new ArrayList<>();
    private final List<String[]> vehicles     = new ArrayList<>();

    // ── Lookup: userID → name  (built from accounts.txt) ─────────────────────
    private final Map<String, String> idToName = new HashMap<>();

    public DashboardData() {
        load(APPT_FILE,     appointments);
        load(COMMENTS_FILE, comments);
        load(PAYMENTS_FILE, payments);
        load(VEHICLES_FILE, vehicles);
        buildNameLookup();
    }

    // ─── KPI numbers ──────────────────────────────────────────────────────────

    public int totalAppointments() { return appointments.size(); }

    public int completedCount() {
        return count(appointments, 5, "Completed");
    }

    public int inProgressCount() {
        return count(appointments, 5, "In Progress");
    }

    public int pendingCount() {
        return count(appointments, 5, "Pending");
    }

    public double completionRate() {
        int t = totalAppointments();
        return t == 0 ? 0.0 : completedCount() * 100.0 / t;
    }

    /** Actual total revenue — sum of paid payment amounts from payments.txt. */
    public double totalRevenue() {
        return getTotalPaid();
    }

    public int getPaidCount() {
        return countPayments("Paid");
    }

    public int getUnpaidCount() {
        return payments.size() - getPaidCount(); // assuming all others are unpaid, or explicitly count "Unpaid"/"Pending"
    }

    private int countPayments(String status) {
        int count = 0;
        for (String[] r : payments) {
            if (r.length >= 9 && status.equalsIgnoreCase(r[8].trim())) count++;
        }
        return count;
    }

    public double getTotalPaid() {
        double sum = 0;
        for (String[] r : payments) {
            if (r.length < 9) continue;
            if ("Paid".equalsIgnoreCase(r[8].trim())) {
                try { sum += Double.parseDouble(r[5].trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return sum;
    }

    public double getTotalUnpaid() {
        double sum = 0;
        for (String[] r : payments) {
            if (r.length < 9) continue;
            if (!"Paid".equalsIgnoreCase(r[8].trim())) {
                try { sum += Double.parseDouble(r[5].trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return sum;
    }

    public List<String[]> getUpcomingAppointments(String todayDate) {
        List<String[]> upcoming = new ArrayList<>();
        for (String[] r : appointments) {
            if (r.length < 7) continue;
            String dt = r[6].trim();
            String date = dt.contains(" ") ? dt.split(" ")[0] : dt;
            if (date.compareTo(todayDate) >= 0 && !"Completed".equalsIgnoreCase(r[5].trim())) {
                upcoming.add(r);
            }
        }
        upcoming.sort((a, b) -> a[6].trim().compareTo(b[6].trim()));
        return upcoming;
    }

    public Map<String, String> getNamesLookup() {
        return idToName;
    }

    /** Average customer rating from comments[6] */
    public double averageRating() {
        if (comments.isEmpty()) return 0;
        double sum = 0; int n = 0;
        for (String[] r : comments) {
            if (r.length < 7) continue;
            try {
                sum += Double.parseDouble(r[6].trim());
                n++; 
            }
            catch (Exception ignored) {}
        }
        return n == 0 ? 0 : sum / n;
    }

    // ─── Pie chart data ───────────────────────────────────────────────────────

    /** Appointment Status Breakdown. */
    public Map<String, Integer> statusBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String[] r : appointments) {
            if (r.length < 6) continue;
            String t = r[5].trim();
            if (!t.isEmpty()) m.merge(t, 1, Integer::sum);
        }
        return m;
    }

    /** Service type breakdown. */
    public Map<String, Integer> serviceTypeBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String[] r : appointments) {
            if (r.length < 5) continue;
            String t = r[4].trim();
            if (!t.isEmpty()) m.merge(t, 1, Integer::sum);
        }
        return m;
    }

    /** Payment method breakdown: Cash / Card / Online. */
    public Map<String, Integer> paymentMethodBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String[] r : payments) {
            if (r.length < 8) continue;
            String method = r[7].trim();
            if (!method.isEmpty()) m.merge(method, 1, Integer::sum);
        }
        // Sort by count descending
        Map<String, Integer> sorted = new LinkedHashMap<>();
        m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /** Vehicle type split: Car vs Motor (from vehicles.txt col[2]). */
    public Map<String, Integer> vehicleTypeBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String[] r : vehicles) {
            if (r.length < 3) continue;
            m.merge(r[2].trim(), 1, Integer::sum);
        }
        return m;
    }

    // ─── Bar chart data ───────────────────────────────────────────────────────

    /**
     * Weekly appointment volume — last 8 weeks.
     * Key: "Mar 24" (Monday of that week), Value: count.
     */
    public Map<String, Integer> appointmentsByWeek() {
        Map<String, Integer> raw = new TreeMap<>();
        DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM dd");
        for (String[] r : appointments) {
            if (r.length < 7) continue;
            try {
                LocalDate d      = LocalDate.parse(r[6].trim(), fmt);
                LocalDate monday = d.minusDays(d.getDayOfWeek().getValue() - 1);
                raw.merge(monday.format(label), 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        return tailInt(raw, 8);
    }

    /**
     * Appointments per technician — top 6, sorted descending.
     * Uses resolved names where available (e.g. "Alex Wong" not "T1").
     */
    public Map<String, Integer> appointmentsByTechnician() {
        Map<String, Integer> m = new HashMap<>();
        for (String[] r : appointments) {
            if (r.length < 4) continue;
            String id   = r[3].trim();
            String name = idToName.getOrDefault(id, id); // resolve to name
            m.merge(name, 1, Integer::sum);
        }
        Map<String, Integer> sorted = new LinkedHashMap<>();
        m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /**
     * Top rating by customer frequency (5.0, 4.9, 4.8, 4.7).
     * Uses comments[6] for rating.
     */
    public Map<String, Integer> topRatingsByCustomer() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("5.0", 0);
        result.put("4.9", 0);
        result.put("4.8", 0);
        result.put("4.7", 0);

        for (String[] r : comments) {
            if (r.length < 7) continue;
            String rating = r[6].trim();
            if (result.containsKey(rating)) {
                result.put(rating, result.get(rating) + 1);
            }
        }
        return result;
    }

    /**
     * Actual monthly revenue from payments.txt (last 6 months, Paid only).
     * Key: "Mar 2026", Value: RM amount.
     */
    public Map<String, Double> revenueByMonth() {
        Map<String, Double> raw = new TreeMap<>();
        DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter label = DateTimeFormatter.ofPattern("MMM yyyy");
        for (String[] r : payments) {
            if (r.length < 9) continue;
            if (!"Paid".equalsIgnoreCase(r[8].trim())) continue;
            try {
                LocalDate d = LocalDate.parse(r[6].trim(), fmt);
                double amt  = Double.parseDouble(r[5].trim());
                raw.merge(d.format(label), amt, Double::sum);
            } catch (Exception ignored) {}
        }
        return tailDbl(raw, 6);
    }

    /**
     * Top vehicle brands (from vehicles.txt col[4]) — top 6.
     * Splits "Toyota Vios" → "Toyota", "Honda City" → "Honda".
     */
    public Map<String, Integer> topVehicleBrands() {
        Map<String, Integer> m = new HashMap<>();
        for (String[] r : vehicles) {
            if (r.length < 5) continue;
            String brand = r[4].trim().split(" ")[0]; // first word = brand
            m.merge(brand, 1, Integer::sum);
        }
        Map<String, Integer> sorted = new LinkedHashMap<>();
        m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    /** Builds the userID→name lookup from accounts.txt. */
    private void buildNameLookup() {
        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", -1);
                // format: [0]userID [1]name [2]email [3]password [4]role [5]profilePic
                if (p.length >= 2) idToName.put(p[0].trim(), p[1].trim());
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    /** Counts rows where col[index] matches value (case-insensitive). */
    private int count(List<String[]> rows, int index, String value) {
        return (int) rows.stream()
                .filter(r -> r.length > index && value.equalsIgnoreCase(r[index].trim()))
                .count();
    }

    /** Keeps the last {@code n} entries of a sorted map (Integer values). */
    private Map<String, Integer> tailInt(Map<String, Integer> sorted, int n) {
        List<String> keys = new ArrayList<>(sorted.keySet());
        Map<String, Integer> result = new LinkedHashMap<>();
        int start = Math.max(0, keys.size() - n);
        for (int i = start; i < keys.size(); i++) result.put(keys.get(i), sorted.get(keys.get(i)));
        if (result.isEmpty()) result.put("No data", 0);
        return result;
    }

    /** Keeps the last {@code n} entries of a sorted map (Double values). */
    private Map<String, Double> tailDbl(Map<String, Double> sorted, int n) {
        List<String> keys = new ArrayList<>(sorted.keySet());
        Map<String, Double> result = new LinkedHashMap<>();
        int start = Math.max(0, keys.size() - n);
        for (int i = start; i < keys.size(); i++) result.put(keys.get(i), sorted.get(keys.get(i)));
        if (result.isEmpty()) result.put("No data", 0.0);
        return result;
    }

    /** Generic CSV loader — skips blank lines. */
    private void load(String path, List<String[]> target) {
        File file = new File(path);
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) target.add(line.split(",", -1));
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}