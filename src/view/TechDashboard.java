package view;
 
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.User;
 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
 
/**
 * TechnicianDashboardPanel — the Dashboard page for technicians.
 *
 * Shows a monthly calendar grid with the technician's appointments.
 * Each day cell shows coloured chips for appointments.
 * Clicking a day opens a popup with full appointment details.
 *
 * OOP concepts:
 *   Inheritance  : extends JPanel
 *   Encapsulation: all fields private, public refresh() only
 *   Abstraction  : calendar building split into helper methods
 */
public class TechDashboard extends JPanel {
 
    private final AppFrame app;
    private final AppointmentService appointmentService = new AppointmentService();
 
    // Tracks which month/year the calendar is showing
    private int calendarYear;
    private int calendarMonth;
 
    // Container that holds the calendar — rebuilt on month navigation
    private JPanel calendarContainer;
 
    private static final Color GREEN  = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY   = new Color(108, 117, 125);
 
    // ══════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ══════════════════════════════════════════════════════════
 
    public TechDashboard (AppFrame app) {
        this.app = app;
 
        // Start on current month
        LocalDate today = LocalDate.now();
        calendarYear  = today.getYear();
        calendarMonth = today.getMonthValue();
 
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));
 
        // Welcome text at top
        JLabel welcome = new JLabel("Welcome back, Technician");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 26));
        welcome.setForeground(UIConstants.TEXT_PRIMARY);
 
        JLabel sub = new JLabel("Your appointment schedule for the month.");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(4, 0, 20, 0));
 
        JPanel topText = new JPanel();
        topText.setLayout(new BoxLayout(topText, BoxLayout.Y_AXIS));
        topText.setBackground(UIConstants.BG_CONTENT);
        topText.add(welcome);
        topText.add(sub);
        add(topText, BorderLayout.NORTH);
 
        // Calendar container — rebuilt whenever month changes
        calendarContainer = new JPanel(new BorderLayout());
        calendarContainer.setBackground(UIConstants.BG_CONTENT);
        calendarContainer.add(buildCalendar(), BorderLayout.CENTER);
        add(calendarContainer, BorderLayout.CENTER);
    }
 
    /**
     * Called by TechnicianDashboard when navigating to this page.
     * Rebuilds the calendar to show fresh data.
     */
    public void refresh() {
        calendarContainer.removeAll();
        calendarContainer.add(buildCalendar(), BorderLayout.CENTER);
        calendarContainer.revalidate();
        calendarContainer.repaint();
    }
 
    // ══════════════════════════════════════════════════════════
    // CALENDAR BUILDER
    // ══════════════════════════════════════════════════════════
 
    /**
     * Builds the full calendar card for the current calendarYear/calendarMonth.
     * Includes: month navigation arrows, day-of-week headers, day cells, legend.
     */
    private JPanel buildCalendar() {
        YearMonth yearMonth = YearMonth.of(calendarYear, calendarMonth);
        LocalDate firstDay  = yearMonth.atDay(1);
        int daysInMonth = yearMonth.lengthOfMonth();
        // Sunday=0, Monday=1 ... Saturday=6
        int startDow = firstDay.getDayOfWeek().getValue() % 7;
 
        // Load this technician's appointments for this month
        User user = app.getLoggedInUserObj();
        Map<Integer, List<Appointment>> apptsByDay = new HashMap<>();
        if (user != null) {
            for (Appointment a : appointmentService.getAll()) {
                if (!a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) continue;
                try {
                    String[] parts = a.getDateTime().split(" ");
                    LocalDate date = LocalDate.parse(parts[0]);
                    if (date.getYear() == calendarYear
                            && date.getMonthValue() == calendarMonth) {
                        int day = date.getDayOfMonth();
                        apptsByDay.computeIfAbsent(day, k -> new ArrayList<>()).add(a);
                    }
                } catch (Exception ignored) {}
            }
        }
 
        // ── Outer rounded card ─────────────────────────────────
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
 
        // ── Navigation row: ◀ Month Year ▶ ───────────────────
        String monthName = Month.of(calendarMonth)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        JLabel monthYearLabel = new JLabel(
                monthName + " " + calendarYear, SwingConstants.CENTER);
        monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        monthYearLabel.setForeground(UIConstants.TEXT_PRIMARY);
 
        JButton prevBtn = navArrowBtn("\u25C0");
        prevBtn.addActionListener(e -> {
            calendarMonth--;
            if (calendarMonth < 1) { calendarMonth = 12; calendarYear--; }
            refresh();
        });
 
        JButton nextBtn = navArrowBtn("\u25B6");
        nextBtn.addActionListener(e -> {
            calendarMonth++;
            if (calendarMonth > 12) { calendarMonth = 1; calendarYear++; }
            refresh();
        });
 
        JPanel navRow = new JPanel(new BorderLayout());
        navRow.setOpaque(false);
        navRow.setBorder(new EmptyBorder(0, 0, 16, 0));
        navRow.add(prevBtn,        BorderLayout.WEST);
        navRow.add(monthYearLabel, BorderLayout.CENTER);
        navRow.add(nextBtn,        BorderLayout.EAST);
        card.add(navRow, BorderLayout.NORTH);
 
        // ── Day-of-week header ─────────────────────────────────
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        JPanel dowRow = new JPanel(new GridLayout(1, 7, 4, 0));
        dowRow.setOpaque(false);
        dowRow.setBorder(new EmptyBorder(0, 0, 8, 0));
        for (String d : dayNames) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setForeground(UIConstants.TEXT_MUTED);
            dowRow.add(lbl);
        }
 
        // ── Day cells grid ─────────────────────────────────────
        int totalCells = startDow + daysInMonth;
        int numRows    = (int) Math.ceil(totalCells / 7.0);
        JPanel grid = new JPanel(new GridLayout(numRows, 7, 4, 4));
        grid.setOpaque(false);
 
        LocalDate todayDate = LocalDate.now();
 
        // Empty cells before day 1
        for (int i = 0; i < startDow; i++) grid.add(emptyCell());
 
        // Day cells
        for (int day = 1; day <= daysInMonth; day++) {
            final int d = day;
            List<Appointment> dayAppts =
                    apptsByDay.getOrDefault(day, new ArrayList<>());
            boolean isToday = (todayDate.getYear() == calendarYear
                    && todayDate.getMonthValue() == calendarMonth
                    && todayDate.getDayOfMonth() == day);
            grid.add(buildDayCell(d, dayAppts, isToday));
        }
 
        // Remaining empty cells
        int remaining = (numRows * 7) - startDow - daysInMonth;
        for (int i = 0; i < remaining; i++) grid.add(emptyCell());
 
        // Wrap header + grid
        JPanel calBody = new JPanel(new BorderLayout(0, 0));
        calBody.setOpaque(false);
        calBody.add(dowRow, BorderLayout.NORTH);
        calBody.add(grid,   BorderLayout.CENTER);
 
        JScrollPane scroll = new JScrollPane(calBody);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        card.add(scroll, BorderLayout.CENTER);
 
        // ── Legend ─────────────────────────────────────────────
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(12, 0, 0, 0));
        legend.add(legendItem(GREEN,  "Completed"));
        legend.add(legendItem(ORANGE, "In Progress"));
        legend.add(legendItem(GREY,   "Pending"));
        card.add(legend, BorderLayout.SOUTH);
 
        return card;
    }
 
    // ══════════════════════════════════════════════════════════
    // DAY CELL
    // ══════════════════════════════════════════════════════════
 
    /**
     * Builds one day cell in the calendar grid.
     * Shows the day number + up to 2 appointment chips.
     * Clicking opens a detail popup.
     */
    private JPanel buildDayCell(int day,
            List<Appointment> appts, boolean isToday) {
 
        JPanel cell = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isToday
                        ? new Color(235, 240, 255)
                        : new Color(250, 250, 253));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(isToday ? UIConstants.PRIMARY : new Color(225, 225, 232));
                g2.setStroke(new BasicStroke(isToday ? 2f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        cell.setOpaque(false);
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setBorder(new EmptyBorder(6, 6, 6, 6));
 
        // Day number
        JLabel dayLabel = new JLabel(String.valueOf(day));
        dayLabel.setFont(new Font("SansSerif",
                isToday ? Font.BOLD : Font.PLAIN, 13));
        dayLabel.setForeground(isToday
                ? UIConstants.PRIMARY : UIConstants.TEXT_DARK);
        dayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(dayLabel);
 
        if (!appts.isEmpty()) cell.add(Box.createVerticalStrut(4));
 
        // Show up to 2 chips, then "+N more"
        int shown = Math.min(appts.size(), 2);
        for (int i = 0; i < shown; i++) {
            cell.add(buildChip(appts.get(i)));
            cell.add(Box.createVerticalStrut(2));
        }
        if (appts.size() > 2) {
            JLabel more = new JLabel("+" + (appts.size() - 2) + " more");
            more.setFont(new Font("SansSerif", Font.PLAIN, 10));
            more.setForeground(UIConstants.TEXT_MUTED);
            more.setAlignmentX(Component.LEFT_ALIGNMENT);
            cell.add(more);
        }
 
        // Click to show detail popup
        if (!appts.isEmpty()) {
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    showDetailPopup(day, appts);
                }
            });
        }
        return cell;
    }
 
    /**
     * Coloured appointment chip shown inside a day cell.
     * Shows: time + service type (abbreviated).
     * Color reflects appointment status.
     */
    private JLabel buildChip(Appointment a) {
        String time = a.getDateTime().contains(" ")
                ? a.getDateTime().split(" ")[1] : "";
        String service = a.getServiceType().replace(" Service", "");
 
        JLabel chip = new JLabel(time + " " + service) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                switch (a.getStatus()) {
                    case "Completed":   bg = new Color(220, 245, 225); break;
                    case "In Progress": bg = new Color(255, 243, 220); break;
                    default:            bg = new Color(230, 232, 240); break;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        Color fg;
        switch (a.getStatus()) {
            case "Completed":   fg = GREEN;  break;
            case "In Progress": fg = ORANGE; break;
            default:            fg = GREY;   break;
        }
        chip.setForeground(fg);
        chip.setFont(new Font("SansSerif", Font.BOLD, 10));
        chip.setBorder(new EmptyBorder(2, 4, 2, 4));
        chip.setOpaque(false);
        chip.setAlignmentX(Component.LEFT_ALIGNMENT);
        chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        return chip;
    }
 
    /**
     * Popup dialog showing full details for all appointments on a clicked day.
     * Shows: status, ID, service, time, duration, customer name.
     */
    private void showDetailPopup(int day, List<Appointment> appts) {
        String monthName = Month.of(calendarMonth)
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
 
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                day + " " + monthName + " " + calendarYear, true);
        dialog.setSize(420, Math.min(80 + appts.size() * 140, 520));
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
 
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIConstants.BG_CONTENT);
        content.setBorder(new EmptyBorder(16, 16, 16, 16));
 
        for (Appointment a : appts) {
            JPanel row = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(UIConstants.BG_CARD);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                }
            };
            row.setOpaque(false);
            row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
            row.setBorder(new EmptyBorder(12, 14, 12, 14));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
 
            // Status
            JLabel statusLbl = new JLabel(a.getStatus());
            statusLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
            switch (a.getStatus()) {
                case "Completed":   statusLbl.setForeground(GREEN);  break;
                case "In Progress": statusLbl.setForeground(ORANGE); break;
                default:            statusLbl.setForeground(GREY);   break;
            }
            statusLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(statusLbl);
            row.add(Box.createVerticalStrut(4));
 
            // ID + Service
            JLabel idLbl = new JLabel(a.getId() + "  ·  " + a.getServiceType());
            idLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            idLbl.setForeground(UIConstants.TEXT_PRIMARY);
            idLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(idLbl);
            row.add(Box.createVerticalStrut(4));
 
            // Time + duration
            String time = a.getDateTime().contains(" ")
                    ? a.getDateTime().split(" ")[1] : a.getDateTime();
            JLabel timeLbl = new JLabel(
                    "\u23F0 " + time + "  ·  " + a.getDurationHours() + " hour(s)");
            timeLbl.setFont(UIConstants.FONT_SMALL);
            timeLbl.setForeground(UIConstants.TEXT_MUTED);
            timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(timeLbl);
            row.add(Box.createVerticalStrut(4));
 
            // Customer
            JLabel custLbl = new JLabel(
                    "Customer: " + resolveName(a.getCustomerEmail())
                    + "  (" + a.getCustomerEmail() + ")");
            custLbl.setFont(UIConstants.FONT_SMALL);
            custLbl.setForeground(UIConstants.TEXT_SECONDARY);
            custLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(custLbl);
 
            content.add(row);
            content.add(Box.createVerticalStrut(10));
        }
 
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        dialog.add(scroll, BorderLayout.CENTER);
 
        JButton closeBtn = UIFactory.createPrimaryButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        btnPanel.setBackground(UIConstants.BG_CONTENT);
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
 
        dialog.setVisible(true);
    }
 
    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════
 
    private String resolveName(String id) {
        for (model.User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id))
                return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }
 
    private JPanel emptyCell() {
        JPanel cell = new JPanel();
        cell.setOpaque(false);
        return cell;
    }
 
    private JButton navArrowBtn(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover()
                        ? new Color(235, 240, 255) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setPreferredSize(new Dimension(36, 30));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setRolloverEnabled(true);
        return btn;
    }
 
    private JPanel legendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel dot = new JLabel("\u25CF");
        dot.setFont(new Font("SansSerif", Font.PLAIN, 14));
        dot.setForeground(color);
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        item.add(dot); item.add(lbl);
        return item;
    }
}
