package model;

import java.util.List;

/**
 * NotificationService provides role-specific notification types.
 *
 * Each user role in the system has different responsibilities,
 * so they receive different categories of notifications.
 *
 * Java OOP principles used:
 *  - Encapsulation : notification data is stored privately and accessed
 *                    only through the public getNotifications() method
 *  - Abstraction   : callers simply pass a role string and get back
 *                    the relevant notifications without knowing the
 *                    internal storage details
 */
public class NotificationService {

    private static final List<String> ADMIN_NOTIFICATIONS = List.of(
            "\uD83D\uDC65  New staff account registered",
            "\u2B50  New customer feedback received",
            "\uD83D\uDCC8  Monthly report ready for review",
            "\uD83D\uDEE0\uFE0F  Service price update pending",
            "\u26A0\uFE0F  Staff performance alert"
    );

    private static final List<String> COUNTER_STAFF_NOTIFICATIONS = List.of(
            "\uD83D\uDCC5  New appointment booked",
            "\uD83D\uDCB3  Payment received from customer",
            "\uD83D\uDC64  New customer registered",
            "\uD83D\uDD27  Service request pending approval",
            "\u26A0\uFE0F  Appointment cancellation request"
    );

    private static final List<String> TECHNICIAN_NOTIFICATIONS = List.of(
            "\uD83D\uDD27  New appointment assigned to you",
            "\uD83D\uDCC5  Appointment schedule updated",
            "\uD83D\uDCAC  New customer feedback on your service",
            "\u26A0\uFE0F  Urgent service request",
            "\u2705  Service completion confirmed"
    );

    private static final List<String> CUSTOMER_NOTIFICATIONS = List.of(
            "\uD83D\uDCC5  Appointment status updated",
            "\u2705  Service completed for your vehicle",
            "\uD83D\uDCB3  Payment due reminder",
            "\uD83D\uDD14  Upcoming appointment reminder",
            "\uD83D\uDCAC  Response to your feedback"
    );

    /**
     * Returns the list of notification types relevant to the given role.
     *
     * @param role the user's role (e.g. "admin", "counterstaff", "technician", "customer")
     * @return list of notification messages for that role
     */
    public List<String> getNotifications(String role) {
        if (role == null) return List.of();
        switch (role.toLowerCase()) {
            case "admin":        return ADMIN_NOTIFICATIONS;
            case "staff":        return COUNTER_STAFF_NOTIFICATIONS;
            case "technician":   return TECHNICIAN_NOTIFICATIONS;
            case "customer":     return CUSTOMER_NOTIFICATIONS;
            default:             return List.of();
        }
    }
}
