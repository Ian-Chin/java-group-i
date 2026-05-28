package view;

import java.awt.Color;

/**
 * Single source of truth for the colours used to render an appointment status
 * (badge foreground, badge background, bar/dot tint).
 *
 * Each constant pairs a canonical status label (as written to appointments.txt
 * and emitted by AppointmentService) with its display colours. Use
 * {@link #from(String)} to resolve a style from the stored status string.
 */
public enum AppointmentStatusStyle {

    COMPLETED("Completed",                     new Color( 40, 167,  69), new Color(220, 245, 225)),
    IN_PROGRESS("In Progress",                 new Color(202, 138,   4), new Color(254, 243, 199)),
    PENDING("Pending",                         new Color(202, 138,   4), new Color(254, 243, 199)),
    CANCELLED("Cancelled",                     new Color(220,  38,  38), new Color(254, 226, 226)),
    WAITING("Waiting for Confirmation",        new Color(150, 100, 200), new Color(240, 230, 250)),
    UNKNOWN("",                                new Color(108, 117, 125), new Color(235, 235, 240));

    private final String label;
    private final Color foreground;
    private final Color background;

    AppointmentStatusStyle(String label, Color foreground, Color background) {
        this.label = label;
        this.foreground = foreground;
        this.background = background;
    }

    public String label()      { return label; }
    public Color foreground()  { return foreground; }
    public Color background()  { return background; }

    public static AppointmentStatusStyle from(String status) {
        if (status == null) return UNKNOWN;
        for (AppointmentStatusStyle s : values()) {
            if (s.label.equalsIgnoreCase(status.trim())) return s;
        }
        return UNKNOWN;
    }

    public static Color fg(String status) { return from(status).foreground(); }
    public static Color bg(String status) { return from(status).background(); }
}
