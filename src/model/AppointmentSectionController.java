package model;

import java.util.List;
import java.util.Set;

/**
 * AppointmentSectionController — handles all logic for the Upcoming Appointments
 * and Pending Payment sections in the Customer Dashboard.
 *
 * This controller separates business logic from the UI (CustomerDashboard.java).
 * The dashboard only calls methods here — it never reads files or calculates
 * amounts itself.
 *
 * Java OOP principles used:
 *  - Encapsulation  : all appointment and payment logic is hidden inside this class
 *  - Abstraction    : the dashboard calls simple methods like getPendingAppointments()
 *                     without knowing how data is read from files
 *  - Separation of  : business logic (this class) is separate from UI (CustomerDashboard)
 *    Concerns
 *
 * CHANGE: autoCompleteExpired() has been emptied out.
 *         Expired appointments are now simply hidden from the Upcoming section
 *         by checking the end time inside getPendingAppointments().
 *         The file is never modified just because time has passed.
 */
public class AppointmentSectionController {

    // ── Services used to read/write data files ────────────────────
    private final AppointmentService    appointmentService;
    private final PaymentService        paymentService;
    private final ServiceHistoryService serviceHistoryService;
    private final VehicleService        vehicleService;

    // ── Callback so this controller can access the logged-in user ─
    private final SessionView sessionView;

    /**
     * SessionView is a small interface so this controller can get the
     * logged-in user without importing CustomerDashboard directly.
     * CustomerDashboard implements this interface.
     */
    public interface SessionView {
        /** Returns the full logged-in User object. */
        User getLoggedInUserObj();
    }

    // ── Constructor ───────────────────────────────────────────────

    /**
     * Creates the controller and connects it to all required services.
     *
     * @param sessionView           provides the logged-in user (from CustomerDashboard)
     * @param appointmentService    reads/writes appointments.txt
     * @param paymentService        reads/writes payments.txt
     * @param serviceHistoryService reads/writes serviceHistory.txt
     * @param vehicleService        reads vehicles.txt (to look up vehicle labels)
     */
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
     * Previously this method changed expired appointment statuses to
     * "Completed" in the file. This behaviour has been removed.
     *
     * Expired appointments are now simply hidden from the Upcoming
     * Appointments card by checking the end time inside
     * getPendingAppointments(). The file is never modified here.
     *
     * This method is kept so that any existing calls to it (e.g. in
     * CustomerDashboard.refreshUser) compile without errors — it just
     * does nothing now.
     */
    public void autoCompleteExpired() {
        // Intentionally left empty.
        // No longer auto-completing expired appointments.
        // getPendingAppointments() in AppointmentService already hides
        // any appointment whose end time has passed.
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns upcoming (Pending or In Progress) appointments for the
     * currently logged-in customer.
     *
     * HOW IT WORKS (beginner-friendly):
     *   - Calls AppointmentService.getPendingAppointments()
     *   - That method checks whether each appointment's end time has
     *     already passed. If it has, the appointment is hidden.
     *   - The appointments.txt file is NEVER changed here.
     *
     * Each String[] in the returned list has 7 elements:
     *   [0] appointmentID  [1] vehicleID  [2] technicianID
     *   [3] serviceType    [4] status     [5] dateTime     [6] duration
     *
     * @return list of upcoming appointment rows, or empty list if not logged in
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
     * Returns completed but unpaid appointments for the currently
     * logged-in customer.
     *
     * Each String[] in the returned list has 7 elements:
     *   [0] appointmentID  [1] vehicleID  [2] technicianID
     *   [3] serviceType    [4] status     [5] dateTime     [6] duration
     *
     * @return list of unpaid appointment rows, or empty list if not logged in
     */
    public List<String[]> getUnpaidAppointments() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        Set<String> paidIds = paymentService.getPaidAppointmentIds(user.getUserId());
        return appointmentService.getUnpaidAppointments(user.getUserId(), paidIds);
    }

    /**
     * Returns the set of appointment IDs the logged-in customer has already paid.
     * Used to filter the Pending Payment card.
     *
     * @return set of paid appointment ID strings, or empty set if not logged in
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
     * Used by the Service Activity bar chart and Service Breakdown donut chart
     * to calculate total services, total spent, and average per visit.
     *
     * Each String[] has 7 elements:
     *   [0] appointmentID  [1] vehicleID  [2] technicianID
     *   [3] serviceType    [4] status     [5] dateTime     [6] duration
     *
     * @return list of all appointment rows, or empty list if not logged in
     */
    public List<String[]> getAllAppointmentsForUser() {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return new java.util.ArrayList<>();
        return appointmentService.getAllAppointmentsForUser(user.getUserId());
    }

    // ═══════════════════════════════════════════════════════════════
    // PAYMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calculates the payment amount for a given service type and duration.
     *
     * Pricing rules:
     *   Major Service  = RM 350.00 per hour
     *   Normal Service = RM 150.00 per hour
     *
     * @param serviceType  e.g. "Major Service" or "Normal Service"
     * @param durationStr  duration as a string, e.g. "3"
     * @return total amount as a formatted string e.g. "1050.00"
     */
    public String calculateAmount(String serviceType, String durationStr) {
        // Choose price per hour based on service type
        double pricePerHour = serviceType.equalsIgnoreCase("Major Service") ? 350.00 : 150.00;

        // Parse the duration; default to 1 hour if the value is not a valid number
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
     * @param appointmentId  the appointment being paid e.g. "AP4"
     * @param vehicleId      the vehicle ID e.g. "V4"
     * @param amount         amount string e.g. "1050.00"
     * @param method         payment method: "Cash", "Card", or "Online"
     * @return true if saved successfully, false otherwise
     */
    public boolean savePayment(String appointmentId, String vehicleId,
                               String amount, String method) {
        User user = sessionView.getLoggedInUserObj();
        if (user == null) return false;
        return paymentService.savePayment(
                user.getUserId(), appointmentId, vehicleId, amount, method);
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE LABEL LOOKUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Looks up a vehicle by its ID and returns a short display label.
     * Example: "Car · LIN110" or "Motor · AJH1312"
     *
     * @param vehicleId  the vehicle ID e.g. "V4"
     * @return display label string
     */
    public String getVehicleLabel(String vehicleId) {
        return vehicleService.getVehiclePlate(vehicleId);
    }

    // ═══════════════════════════════════════════════════════════════
    // USER NAME LOOKUP
    // ═══════════════════════════════════════════════════════════════

    /**
     * Resolves a user ID like "T3" to a display name like "Mike Tan".
     * Used to show the technician's name in appointment rows.
     *
     * @param allUsers  the full list of users from AccountService
     * @param userId    the user ID to look up
     * @return the user's name, or the userId itself if not found
     */
    public String resolveUserName(List<User> allUsers, String userId) {
        for (User u : allUsers) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(userId)) {
                return u.getName();
            }
        }
        // Safe fallback: return the raw ID if the user is not found
        return userId;
    }

    // ═══════════════════════════════════════════════════════════════
    // ★ NEW — PROFILE STATS (added for ViewProfile.java)
    // ═══════════════════════════════════════════════════════════════

    /**
     * ★ NEW METHOD — Returns the TOTAL number of appointments for the
     * logged-in customer. Used by ViewProfile to display the
     * "Total appointments" stat.
     *
     * Counts ALL appointments regardless of status:
     *   Pending + In Progress + Completed are all included.
     *
     * HOW IT WORKS (beginner-friendly):
     *   1. Get the logged-in user from the session.
     *   2. If no user is logged in, return 0 safely.
     *   3. Ask AppointmentService for every appointment for that user.
     *   4. Return how many there are (.size()).
     *
     * Example for Zhi Lin (C3):
     *   Appointments: AP3, AP4, AP5, AP16, AP21, AP22, AP23, AP24, AP25
     *   → returns 9
     *
     * @return total appointment count, or 0 if no user is logged in
     */
    public int getTotalAppointments() {
        // Step 1: Get the logged-in user
        User user = sessionView.getLoggedInUserObj();

        // Step 2: Nobody logged in → return 0 safely
        if (user == null) return 0;

        // Step 3: Get ALL appointments for this user (any status)
        // getAllAppointmentsForUser() reads appointments.txt and filters by userId
        List<String[]> allAppointments = appointmentService.getAllAppointmentsForUser(user.getUserId());

        // Step 4: Return the count
        return allAppointments.size();
    }

    /**
     * ★ NEW METHOD — Returns the TOTAL amount (in RM) the logged-in
     * customer has spent. Used by ViewProfile to display the
     * "Total spent" stat.
     *
     * Only "Paid" payment records are counted — unpaid ones are excluded.
     *
     * HOW IT WORKS (beginner-friendly):
     *   1. Get the logged-in user from the session.
     *   2. If no user is logged in, return "0.00" safely.
     *   3. Ask PaymentService for every row in payments.txt.
     *   4. For each row:
     *        - Column [1] = CustomerID — must match the logged-in user.
     *        - Column [8] = Status     — must be "Paid".
     *        - Column [5] = Amount     — add this to the running total.
     *   5. Return the total as a string with 2 decimal places e.g. "150.00".
     *
     * payments.txt column layout (9 columns):
     *   [0] paymentID     [1] customerID    [2] serviceHistoryID
     *   [3] appointmentID [4] vehicleID     [5] amount
     *   [6] paymentDate   [7] method        [8] status
     *
     * Example for Zhi Lin (C3):
     *   PY8,C3,SH3,AP3,V3,150.00,2026-03-30,Online,Paid
     *   → returns "150.00"
     *
     * @return total spent formatted to 2 decimal places, or "0.00" if nothing paid
     */
    public String getTotalSpent() {
        // Step 1: Get the logged-in user
        User user = sessionView.getLoggedInUserObj();

        // Step 2: Nobody logged in → return "0.00" safely
        if (user == null) return "0.00";

        // Running total — add each matching paid amount to this
        double total = 0.0;

        // Step 3: Get every payment row from payments.txt
        // getAllPayments() returns each line as a String[] with 9 elements
        List<String[]> allPayments = paymentService.getAllPayments();

        // Step 4: Loop through every payment row
        for (String[] row : allPayments) {

            // Safety check: skip any row that does not have all 9 columns
            // (protects against blank lines or malformed data in payments.txt)
            if (row.length < 9) continue;

            String customerIdInFile = row[1].trim(); // [1] CustomerID  e.g. "C3"
            String amountStr        = row[5].trim(); // [5] Amount      e.g. "150.00"
            String status           = row[8].trim(); // [8] Status      e.g. "Paid"

            // Does this payment belong to the logged-in user?
            boolean isThisCustomer = customerIdInFile.equalsIgnoreCase(user.getUserId());

            // Has the payment actually been completed?
            boolean isPaid = status.equalsIgnoreCase("Paid");

            // Only add to the total if BOTH conditions are true
            if (isThisCustomer && isPaid) {
                try {
                    total += Double.parseDouble(amountStr); // add the amount
                } catch (NumberFormatException e) {
                    // Amount was not a valid number — skip this row safely
                    System.out.println("[getTotalSpent] Skipping bad amount: " + amountStr);
                }
            }
        }

        // Step 5: Format to 2 decimal places before returning
        // e.g. 150.0 → "150.00"  |  0.0 → "0.00"
        return String.format("%.2f", total);
    }
}