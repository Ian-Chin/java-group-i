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
    // AUTO-COMPLETE EXPIRED APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Checks all appointments and auto-completes any that have passed
     * their end time. Called once when the customer logs in.
     *
     * Delegates to AppointmentService which contains the actual logic.
     */
    public void autoCompleteExpired() {
        appointmentService.autoCompleteExpiredAppointments(serviceHistoryService);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Returns upcoming (Pending or In Progress) appointments for the
     * currently logged-in customer.
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
    // PAYMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Calculates the payment amount for a given service type and duration.
     *
     * Pricing rules:
     *   Major Service = RM 350.00 per hour
     *   Normal Service (or anything else) = RM 150.00 per hour
     *
     * @param serviceType  e.g. "Major Service" or "Normal Service"
     * @param durationStr  duration as a string, e.g. "3"
     * @return total amount as a formatted string e.g. "1050.00"
     */
    public String calculateAmount(String serviceType, String durationStr) {
        double pricePerHour = serviceType.equalsIgnoreCase("Major Service") ? 350.00 : 150.00;
        int hours = 1;
        try { hours = Integer.parseInt(durationStr.trim()); }
        catch (NumberFormatException ignored) {}
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
        return userId; // safe fallback if user not found
    }
}