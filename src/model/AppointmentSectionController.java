package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

public class AppointmentSectionController {

    // ── Services used to read/write data files ────────────────────
    private final AppointmentService    appointmentService;
    private final PaymentService        paymentService;
    private final ServiceHistoryService serviceHistoryService;
    private final VehicleService        vehicleService;

    // ── Callback so this controller can access the logged-in user ─
    private final SessionView sessionView;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * SessionView is a small interface so this controller can get the
     * logged-in user without importing CustomerDashboard directly.
     */
    public interface SessionView {
        User getLoggedInUserObj();
    }

    // ── Constructor ───────────────────────────────────────────────
    public AppointmentSectionController(
            SessionView sessionView,
            AppointmentService appointmentService,
            PaymentService paymentService,
            ServiceHistoryService serviceHistoryService,
            VehicleService vehicleService) {

        this.sessionView            = sessionView;
        this.appointmentService     = appointmentService;
        this.paymentService         = paymentService;
        this.serviceHistoryService  = serviceHistoryService;
        this.vehicleService         = vehicleService;
    }

    // ═══════════════════════════════════════════════════════════════
    // AUTO-COMPLETE EXPIRED APPOINTMENTS — DISABLED
    // ═══════════════════════════════════════════════════════════════

    /**
     * Expired appointments are hidden by getPendingAppointments() instead.
     */
    public void autoCompleteExpired() {
        // Intentionally left empty.
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns upcoming (Pending or In Progress) appointments for the currently logged-in customer.
     *
     * Row layout: [0] appointmentID  [1] vehicleID  [2] technicianID
     *             [3] serviceType    [4] status     [5] dateTime     [6] duration
     */
    public List<String[]> getPendingAppointments() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        return appointmentService.getPendingAppointments(user.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING PAYMENT
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getUnpaidAppointments() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        Set<String> paidIds = paymentService.getPaidAppointmentIds(user.getUserId());
        return appointmentService.getUnpaidAppointments(user.getUserId(), paidIds);
    }

    /**
     * Returns the set of appointment IDs the logged-in customer has already paid.
     */
    public Set<String> getPaidAppointmentIds() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.HashSet<>();
        return paymentService.getPaidAppointmentIds(user.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════
    // ALL APPOINTMENTS — for dashboard stats / charts
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getAllAppointmentsForUser() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        return appointmentService.getAllAppointmentsForUser(user.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════
    // AMOUNT CALCULATION  ← FIXED
    // ═══════════════════════════════════════════════════════════════
    public String calculateAmount(String serviceType, String durationStr) {
        // Normal Service = RM 50.00
        // Major Service  = RM 200.00
        double price = serviceType.equalsIgnoreCase("Major Service") ? 200.00 : 50.00;
        return String.format("%.2f", price);
    }

    /**
     * Saves a payment record to payments.txt and updates serviceHistory.txt.
     * Called when the customer clicks "Confirm & Pay" in the invoice dialog.
     *
     * Delegates to PaymentService.savePayment() because it writes to payments.txt.
     */
    public boolean savePayment(String appointmentId, String vehicleId,
                               String amount, String method) {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return false;
        return paymentService.savePayment(
                user.getUserId(), appointmentId, vehicleId, amount, method);
    }

    // ═══════════════════════════════════════════════════════════════
    // USER NAME LOOKUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolves a user ID like "T3" to a display name like "Mike Tan".
     * Used to show the technician's name in appointment rows.
     */
    public String resolveUserName(List<User> allUsers, String userId) {
        for (User u : allUsers) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(userId)) {
                return u.getName();
            }
        }
        return userId;
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFILE STATS (used by ViewProfile.java)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns the TOTAL number of appointments for the logged-in customer.
     * Used by ViewProfile to display the "Total appointments" stat.
     */
    public int getTotalAppointments() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return 0;
        return appointmentService.getAllAppointmentsForUser(user.getUserId()).size();
    }

    /**
     * Returns the TOTAL amount (in RM) the logged-in customer has spent.
     * Used by ViewProfile to display the "Total spent" stat.
     * Reads from payments.txt via PaymentService.
     */
    public String getTotalSpent() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return "0.00";

        double total = 0.0;
        for (String[] row : paymentService.getAllPayments()) {
            if (row.length < 9) continue;
            if (row[1].trim().equalsIgnoreCase(user.getUserId())
                    && row[8].trim().equalsIgnoreCase("Paid")) {
                try {
                    total += Double.parseDouble(row[5].trim());
                } catch (NumberFormatException e) {
                    System.out.println("[getTotalSpent] Skipping bad amount: " + row[5].trim());
                }
            }
        }
        return String.format("%.2f", total);
    }

    // ═══════════════════════════════════════════════════════════════
    // DASHBOARD APPOINTMENT STAT HELPERS
    // ═══════════════════════════════════════════════════════════════
    public int countThisMonth(List<String[]> appts) {
        LocalDate today = LocalDate.now();
        int count = 0;
        for (String[] row : appts) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(row[5], DATE_TIME_FORMAT);
                if (ldt.getYear() == today.getYear() && ldt.getMonth() == today.getMonth())
                    count++;
            } catch (DateTimeParseException ignored) {}
        }
        return count;
    }

    /**
     * Builds a month-label → appointment-count map for the last N months.
     * Used by the Service Activity bar chart in the dashboard.
     *
     * Example result for months=6 (current month = April):
     *   { "Nov"→0, "Dec"→1, "Jan"→2, "Feb"→0, "Mar"→3, "Apr"→1 }
     */
    public Map<String, Integer> buildMonthlyCounts(List<String[]> appts, int months) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int i = months - 1; i >= 0; i--)
            counts.put(today.minusMonths(i).format(DateTimeFormatter.ofPattern("MMM")), 0);
        for (String[] row : appts) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(row[5], DATE_TIME_FORMAT);
                for (int i = months - 1; i >= 0; i--) {
                    LocalDate d = today.minusMonths(i);
                    if (ldt.getYear() == d.getYear() && ldt.getMonth() == d.getMonth()) {
                        String key = d.format(DateTimeFormatter.ofPattern("MMM"));
                        counts.put(key, counts.get(key) + 1);
                        break;
                    }
                }
            } catch (DateTimeParseException ignored) {}
        }
        return counts;
    }

    /**
     * Builds a service-type → count map for the donut chart.
     * Top 3 types listed individually; the rest grouped as "Other".
     */
    public Map<String, Integer> buildServiceBreakdown(List<String[]> appts) {
        Map<String, Integer> raw = new LinkedHashMap<>();
        for (String[] row : appts) raw.merge(row[3], 1, Integer::sum);
        Map<String, Integer> result = new LinkedHashMap<>();
        int otherCount = 0, rank = 0;
        for (Map.Entry<String, Integer> e : raw.entrySet()) {
            if (rank < 3) result.put(e.getKey(), e.getValue());
            else          otherCount += e.getValue();
            rank++;
        }
        if (otherCount > 0) result.put("Other", otherCount);
        return result;
    }

    /**
     * Formats the date of the next upcoming appointment for display.
     * Used by the "Next Appointment" stat card value (e.g. "26 Mar 2026").
     */
    public String getNextAppointmentDate(List<String[]> upcoming) {
        if (upcoming.isEmpty()) return "";
        try {
            return LocalDateTime.parse(upcoming.get(0)[5], DATE_TIME_FORMAT)
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy"));
        } catch (DateTimeParseException e) {
            String dt = upcoming.get(0)[5];
            return dt.length() > 10 ? dt.substring(0, 10) : dt;
        }
    }

    /**
     * Returns a human-friendly label for how far away an appointment is.
     * Used by the "Next Appointment" stat card subtitle.
     * Examples: same day → "Today", next day → "Tomorrow", later → "In 5 days"
     */
    public String getDaysUntilLabel(String dateTimeStr) {
        try {
            LocalDate appt = LocalDateTime.parse(dateTimeStr, DATE_TIME_FORMAT).toLocalDate();
            long days = LocalDate.now().until(appt, java.time.temporal.ChronoUnit.DAYS);
            if (days == 0) return "Today";
            if (days == 1) return "Tomorrow";
            if (days > 1)  return "In " + days + " days";
        } catch (DateTimeParseException ignored) {}
        return "";
    }

}