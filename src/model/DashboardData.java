package model;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Loads and aggregates all data needed by the Report dashboard.
 * All file paths and parsing logic are encapsulated here — the view
 * just calls getters and receives ready-to-use numbers and maps.
 *
 * OOP: Encapsulation — all file I/O and computation is private.
 */
public class DashboardData {

    private static final String SEP   = File.separator;
    private static final String BASE  = "src" + SEP + "TxtFile" + SEP;

    private static final String APPT_FILE     = BASE + "appointments.txt";
    private static final String COMMENTS_FILE = BASE + "customer_comments.txt";
    private static final String FEEDBACK_FILE = BASE + "technician_feedback.txt";
    private static final String ACCOUNTS_FILE = BASE + "accounts.txt";

    // ── Raw rows ────────────────────────────────────────────────────
    // appointments: [apptID, custID, techID, serviceType, status, dateTime, durationHrs]
    private final List<String[]> appointments = new ArrayList<>();
    // comments: [commentID, apptID, rating, comment, staffID, techID]
    private final List<String[]> comments     = new ArrayList<>();

    public DashboardData() {
        load(APPT_FILE,     appointments);
        load(COMMENTS_FILE, comments);
    }

    // ── KPI numbers ─────────────────────────────────────────────────

    public int totalAppointments()  { return appointments.size(); }

    public int completedCount() {
        return (int) appointments.stream()
                .filter(r -> r.length > 4 && "Completed".equalsIgnoreCase(r[4].trim()))
                .count();
    }

    public int inProgressCount() {
        return (int) appointments.stream()
                .filter(r -> r.length > 4 && "In Progress".equalsIgnoreCase(r[4].trim()))
                .count();
    }

    public int pendingCount() {
        return (int) appointments.stream()
                .filter(r -> r.length > 4 && "Pending".equalsIgnoreCase(r[4].trim()))
                .count();
    }

    public double completionRate() {
        int total = totalAppointments();
        return total == 0 ? 0.0 : (completedCount() * 100.0 / total);
    }

    public double totalRevenue(double normalPrice, double majorPrice) {
        double sum = 0;
        for (String[] r : appointments) {
            if (r.length < 5) continue;
            // Only count completed appointments as revenue
            if (!"Completed".equalsIgnoreCase(r[4].trim())) continue;
            sum += "Major Service".equalsIgnoreCase(r[3].trim()) ? majorPrice : normalPrice;
        }
        return sum;
    }

    public double averageRating() {
        if (comments.isEmpty()) return 0;
        double sum = 0; int count = 0;
        for (String[] r : comments) {
            try { sum += Double.parseDouble(r[2].trim()); count++; }
            catch (Exception ignored) {}
        }
        return count == 0 ? 0 : sum / count;
    }

    // ── Pie chart data ───────────────────────────────────────────────

    /** { "Completed", "In Progress", "Pending" } → counts */
    public Map<String, Integer> statusBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Completed",   completedCount());
        m.put("In Progress", inProgressCount());
        m.put("Pending",     pendingCount());
        return m;
    }

    /** { "Normal Service", "Major Service" } → counts */
    public Map<String, Integer> serviceTypeBreakdown() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Normal Service", 0);
        m.put("Major Service",  0);
        for (String[] r : appointments) {
            if (r.length < 4) continue;
            String t = r[3].trim();
            if ("Normal Service".equalsIgnoreCase(t)) m.merge("Normal Service", 1, Integer::sum);
            else if ("Major Service".equalsIgnoreCase(t)) m.merge("Major Service",  1, Integer::sum);
        }
        return m;
    }

    // ── Bar chart data ───────────────────────────────────────────────

    /**
     * Appointments grouped by week label (most recent 8 weeks).
     * Returns ordered map: "Mar W1" → count.
     */
    public Map<String, Integer> appointmentsByWeek() {
        Map<String, Integer> raw = new TreeMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter wkFmt = DateTimeFormatter.ofPattern("MMM dd");

        for (String[] r : appointments) {
            if (r.length < 6) continue;
            try {
                LocalDate d = LocalDate.parse(r[5].trim(), fmt);
                // Week start (Monday)
                LocalDate monday = d.minusDays(d.getDayOfWeek().getValue() - 1);
                String key = monday.format(wkFmt);
                raw.merge(key, 1, Integer::sum);
            } catch (Exception ignored) {}
        }
        // Keep last 8 weeks
        Map<String, Integer> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(raw.keySet());
        int start = Math.max(0, keys.size() - 8);
        for (int i = start; i < keys.size(); i++) result.put(keys.get(i), raw.get(keys.get(i)));
        if (result.isEmpty()) result.put("No data", 0);
        return result;
    }

    /**
     * Appointments per technician ID, sorted descending by count.
     * Returns: techID → count (top 6 techs).
     */
    public Map<String, Integer> appointmentsByTechnician() {
        Map<String, Integer> m = new HashMap<>();
        for (String[] r : appointments) {
            if (r.length < 3) continue;
            m.merge(r[2].trim(), 1, Integer::sum);
        }
        // Sort descending, limit 6
        Map<String, Integer> sorted = new LinkedHashMap<>();
        m.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(6)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    /**
     * Average rating per technician (from customer_comments col[5]).
     * Returns techID → avgRating (only techs with ≥1 comment).
     */
    public Map<String, Double> avgRatingByTechnician() {
        Map<String, List<Double>> ratings = new HashMap<>();
        for (String[] r : comments) {
            if (r.length < 6) continue;
            try {
                double rating = Double.parseDouble(r[2].trim());
                String techID = r[5].trim();
                ratings.computeIfAbsent(techID, k -> new ArrayList<>()).add(rating);
            } catch (Exception ignored) {}
        }
        Map<String, Double> result = new LinkedHashMap<>();
        ratings.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    double avg = e.getValue().stream().mapToDouble(d -> d).average().orElse(0);
                    result.put(e.getKey(), avg);
                });
        return result;
    }

    /**
     * Monthly revenue for last 6 months.
     * Returns "Mar 2026" → revenue (completed appts only).
     */
    public Map<String, Double> revenueByMonth(double normalPrice, double majorPrice) {
        Map<String, Double> raw = new TreeMap<>();
        DateTimeFormatter fmt    = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        DateTimeFormatter monFmt = DateTimeFormatter.ofPattern("MMM yyyy");

        for (String[] r : appointments) {
            if (r.length < 6) continue;
            if (!"Completed".equalsIgnoreCase(r[4].trim())) continue;
            try {
                LocalDate d   = LocalDate.parse(r[5].trim(), fmt);
                String key    = d.format(monFmt);
                double price  = "Major Service".equalsIgnoreCase(r[3].trim()) ? majorPrice : normalPrice;
                raw.merge(key, price, Double::sum);
            } catch (Exception ignored) {}
        }
        Map<String, Double> result = new LinkedHashMap<>();
        List<String> keys = new ArrayList<>(raw.keySet());
        int start = Math.max(0, keys.size() - 6);
        for (int i = start; i < keys.size(); i++) result.put(keys.get(i), raw.get(keys.get(i)));
        if (result.isEmpty()) result.put("No data", 0.0);
        return result;
    }

    // ── File loading ─────────────────────────────────────────────────

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