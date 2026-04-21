package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * AppointmentSectionController — handles all logic for the Upcoming Appointments
 * section in the Customer Dashboard.
 *
 * This controller separates business logic from the UI (CustomerDashboard.java).
 * The dashboard only calls methods here — it never reads files or calculates
 * amounts itself.
 *
 * Java OOP principles used:
 *  - Encapsulation  : all appointment logic is hidden inside this class
 *  - Abstraction    : the dashboard calls simple methods like getPendingAppointments()
 *                     without knowing how data is read from files
 *  - Separation of Concerns : appointment logic here, payment logic in PaymentService
 *
 * METHODS MOVED FROM CustomerDashboard.java (appointment-related only):
 *  - countThisMonth()        : counts appointments in the current month (stat card)
 *  - buildMonthlyCounts()    : builds month → count map for the bar chart
 *  - buildServiceBreakdown() : builds service type → count map for the donut chart
 *  - getNextAppointmentDate(): formats the next upcoming appointment date for display
 *  - getDaysUntilLabel()     : returns "Today", "Tomorrow", or "In N days" label
 *
 * Methods in other files:
 *  - getVehicleLabel()      → VehicleService   (vehicle data lookup)
 *  - buildVehicleSubtitle() → VehicleService   (reads vehicle type data)
 *  - calcPendingAmount()    → PaymentService   (payment total)
 *  - calcTotalSpent()       → PaymentService   (payment total)
 */
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
     * Kept so existing call sites compile — intentionally does nothing.
     * Expired appointments are hidden by getPendingAppointments() instead.
     */
    public void autoCompleteExpired() {
        // Intentionally left empty.
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns upcoming (Pending or In Progress) appointments for the
     * currently logged-in customer.
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

    /**
     * Returns completed but unpaid appointments for the currently logged-in customer.
     *
     * Row layout: [0] appointmentID  [1] vehicleID  [2] technicianID
     *             [3] serviceType    [4] status     [5] dateTime     [6] duration
     */
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

    /**
     * Returns ALL appointments (any status) for the currently logged-in customer.
     * Used by the bar chart, donut chart, and total count stat card.
     *
     * Row layout: [0] appointmentID  [1] vehicleID  [2] technicianID
     *             [3] serviceType    [4] status     [5] dateTime     [6] duration
     */
    public List<String[]> getAllAppointmentsForUser() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        return appointmentService.getAllAppointmentsForUser(user.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════
    // AMOUNT CALCULATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calculates the payment amount for a given service type and duration.
     *
     * Pricing rules:
     *   Major Service  = RM 350.00 per hour
     *   Normal Service = RM 150.00 per hour
     *
     * NOTE: This lives here (not PaymentService) because the pricing rule
     * depends on the service type and duration stored in appointments.txt.
     *
     * @param serviceType  e.g. "Major Service" or "Normal Service"
     * @param durationStr  duration as a string, e.g. "3"
     * @return total amount as a formatted string e.g. "1050.00"
     */
    public String calculateAmount(String serviceType, String durationStr) {
        double pricePerHour = serviceType.equalsIgnoreCase("Major Service") ? 350.00 : 150.00;
        int hours = 1;
        try {
            hours = Integer.parseInt(durationStr.trim());
        } catch (NumberFormatException ignored) {}
        return String.format("%.2f", pricePerHour * hours);
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
    // (moved from CustomerDashboard.java — appointment-related only)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Counts how many appointments fall in the current calendar month.
     * Used by the "Total Appointments" stat card subtitle (e.g. "▲ 3 this month").
     *
     * Data source: appointments.txt (dateTime column) → belongs here.
     *
     * @param appts  list of appointment rows (any status)
     * @return count of appointments whose dateTime falls in the current month
     */
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
     * Data source: appointments.txt (dateTime column) → belongs here.
     *
     * Example result for months=6 (current month = April):
     *   { "Nov"→0, "Dec"→1, "Jan"→2, "Feb"→0, "Mar"→3, "Apr"→1 }
     *
     * @param appts   list of all appointment rows
     * @param months  how many months back to include
     * @return ordered map of month abbreviation → count
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
     *
     * Data source: appointments.txt (serviceType column) → belongs here.
     *
     * @param appts  list of all appointment rows
     * @return ordered map of service type → count (max 4 entries)
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
     *
     * Data source: appointments.txt (dateTime column) → belongs here.
     *
     * @param upcoming  sorted list of upcoming appointment rows (nearest first)
     * @return formatted date string, or empty string if none
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
     *
     * Data source: appointments.txt (dateTime column) → belongs here.
     *
     * Examples: same day → "Today", next day → "Tomorrow", later → "In 5 days"
     *
     * @param dateTimeStr  the appointment's dateTime e.g. "2026-04-25 09:00"
     * @return label string, or empty string if the date cannot be parsed
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