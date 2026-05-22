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
 
    private final AppFrame           app;
    private final AppointmentService appointmentService = new AppointmentService();
 
    private final YearMonth[] currentMonth = { YearMonth.now() };
    private final LocalDate[] selectedDate = { LocalDate.now() };
 
    private JPanel   calGrid;
    private JLabel   monthLabel;

    private JPanel   summaryContent;
    private JLabel   summaryTitle;
 
    private static final Color GREEN  = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY   = new Color(108, 117, 125);
 

    public TechDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        JPanel topText = new JPanel();
        topText.setLayout(new BoxLayout(topText, BoxLayout.Y_AXIS));
        topText.setBackground(UIConstants.BG_CONTENT);
        topText.setBorder(new EmptyBorder(0, 0, 20, 0));
 
        JLabel welcome = new JLabel("Welcome back, Technician");
        welcome.setFont(new Font("SansSerif", Font.BOLD, 22));
        welcome.setForeground(UIConstants.TEXT_PRIMARY);
 
        JLabel sub = new JLabel("Your appointment schedule for the month.");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));
 
        topText.add(welcome);
        topText.add(sub);
        add(topText, BorderLayout.NORTH);
 
        add(buildCalendarContent(), BorderLayout.CENTER);
    }
 

    public void refresh() {
        rebuildCalGrid();
        refreshSummary();
    }
 
    private JPanel buildCalendarContent() {
        JPanel page = new JPanel(new BorderLayout(16, 0));
        page.setBackground(UIConstants.BG_CONTENT);
 
        JPanel calCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        calCard.setOpaque(false);
        calCard.setBorder(new EmptyBorder(28, 32, 24, 32));
 
        JPanel navRow = new JPanel(new BorderLayout());
        navRow.setOpaque(false);
        navRow.setBorder(new EmptyBorder(0, 0, 16, 0));
 
        monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        monthLabel.setForeground(UIConstants.TEXT_PRIMARY);
 
        JButton prevBtn = calNavBtn("<");
        JButton nextBtn = calNavBtn(">");
 
        prevBtn.addActionListener(e -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            rebuildCalGrid();
        });
        nextBtn.addActionListener(e -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            rebuildCalGrid();
        });
 
        navRow.add(prevBtn,    BorderLayout.WEST);
        navRow.add(monthLabel, BorderLayout.CENTER);
        navRow.add(nextBtn,    BorderLayout.EAST);
        calCard.add(navRow, BorderLayout.NORTH);
 
        JPanel calBody = new JPanel(new BorderLayout(0, 8));
        calBody.setOpaque(false);
 
        JPanel dowRow = new JPanel(new GridLayout(1, 7, 4, 0));
        dowRow.setOpaque(false);
        dowRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        for (String d : new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}) {
            JLabel dl = new JLabel(d, SwingConstants.CENTER);
            dl.setFont(new Font("SansSerif", Font.BOLD, 13));
            dl.setForeground(UIConstants.TEXT_MUTED);
            dowRow.add(dl);
        }
        calBody.add(dowRow, BorderLayout.NORTH);
 
        calGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calGrid.setOpaque(false);
        calBody.add(calGrid, BorderLayout.CENTER);
        calCard.add(calBody, BorderLayout.CENTER);
 
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        legend.setOpaque(false);
        legend.setBorder(new EmptyBorder(12, 0, 0, 0));
        legend.add(legendDot(GREEN,  "Completed"));
        legend.add(legendDot(ORANGE, "In Progress"));
        legend.add(legendDot(GREY,   "Pending"));
        calCard.add(legend, BorderLayout.SOUTH);

        JPanel summaryCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        summaryCard.setOpaque(false);
        summaryCard.setBorder(new EmptyBorder(24, 24, 24, 24));
        summaryCard.setPreferredSize(new Dimension(360, 0));
        summaryCard.setMinimumSize(new Dimension(360, 0));
 
        summaryTitle = new JLabel("Appointments for today");
        summaryTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        summaryTitle.setForeground(UIConstants.TEXT_PRIMARY);
        summaryTitle.setBorder(new EmptyBorder(0, 0, 14, 0));
        summaryCard.add(summaryTitle, BorderLayout.NORTH);
 
        summaryContent = new JPanel();
        summaryContent.setLayout(new BoxLayout(summaryContent, BoxLayout.Y_AXIS));
        summaryContent.setOpaque(false);
 
        JScrollPane summaryScroll = new JScrollPane(summaryContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        summaryScroll.setBorder(null);
        summaryScroll.setOpaque(false);
        summaryScroll.getViewport().setOpaque(false);
        summaryScroll.getVerticalScrollBar().setUnitIncrement(12);
        summaryCard.add(summaryScroll, BorderLayout.CENTER);
 
        page.add(calCard,     BorderLayout.CENTER);
        page.add(summaryCard, BorderLayout.EAST);

        rebuildCalGrid();
        refreshSummary();
 
        return page;
    }
 
    private void rebuildCalGrid() {
        if (calGrid == null || monthLabel == null) return;
 
        monthLabel.setText(currentMonth[0].getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + currentMonth[0].getYear());
        calGrid.removeAll();
 
        User user = app.getLoggedInUserObj();

        Map<String, List<String>> apptsByDate = new HashMap<>();
        if (user != null) {
            for (Appointment a : appointmentService.getAll()) {
                if (!a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) continue;
                String dt       = a.getDateTime();
                String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
                apptsByDate.computeIfAbsent(apptDate, k -> new ArrayList<>()).add(a.getStatus());
            }
        }
 
        LocalDate first      = currentMonth[0].atDay(1);
        int       startDow   = first.getDayOfWeek().getValue() % 7;
        int       daysInMonth = currentMonth[0].lengthOfMonth();
        LocalDate today       = LocalDate.now();

        for (int i = 0; i < startDow; i++) calGrid.add(new JLabel(""));
 
        for (int d = 1; d <= daysInMonth; d++) {
            LocalDate    date     = currentMonth[0].atDay(d);
            List<String> statuses = apptsByDate.getOrDefault(
                    date.toString(), Collections.emptyList());
            boolean isToday = date.equals(today);
            boolean isSel   = date.equals(selectedDate[0]);
            final int day   = d;
 
            JPanel cell = new JPanel(new BorderLayout(0, 1)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (isSel) {
                        g2.setColor(UIConstants.PRIMARY);
                        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                    } else if (isToday) {
                        g2.setColor(new Color(235, 240, 255));
                        g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                    }
                    g2.setColor(isSel ? UIConstants.PRIMARY : new Color(220, 222, 230));
                    g2.setStroke(new BasicStroke(1f));
                    g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
                    g2.dispose();
                }
            };
            cell.setOpaque(false);
            cell.setPreferredSize(new Dimension(44, 56));
 
            JLabel dayLabel = new JLabel(String.valueOf(d), SwingConstants.CENTER);
            dayLabel.setFont(new Font("SansSerif",
                    (isToday || isSel) ? Font.BOLD : Font.PLAIN, 14));
            dayLabel.setForeground(isSel ? Color.WHITE
                    : (isToday ? UIConstants.PRIMARY : UIConstants.TEXT_DARK));
            cell.add(dayLabel, BorderLayout.CENTER);

            if (!statuses.isEmpty()) {
                JPanel barsPanel = new JPanel();
                barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
                barsPanel.setOpaque(false);
                barsPanel.setBorder(new EmptyBorder(0, 6, 3, 6));
 
                int maxBars = Math.min(statuses.size(), 3);
                for (int si = 0; si < maxBars; si++) {
                    String status = statuses.get(si);
                    Color barColor;
                    switch (status) {
                        case "Completed":   barColor = GREEN;  break;
                        case "In Progress": barColor = ORANGE; break;
                        default:            barColor = GREY;   break;
                    }
                    final Color fc = isSel ? new Color(255, 255, 255, 200) : barColor;
                    JPanel bar = new JPanel() {
                        @Override protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                            g2.setColor(fc);
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                            g2.dispose();
                        }
                    };
                    bar.setOpaque(false);
                    bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 4));
                    bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
                    bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                    barsPanel.add(bar);
                    if (si < maxBars - 1) barsPanel.add(Box.createVerticalStrut(1));
                }
                if (statuses.size() > 3) {
                    JLabel more = new JLabel("+" + (statuses.size() - 3), SwingConstants.CENTER);
                    more.setFont(new Font("SansSerif", Font.BOLD, 8));
                    more.setForeground(isSel ? Color.WHITE : UIConstants.TEXT_MUTED);
                    more.setAlignmentX(Component.LEFT_ALIGNMENT);
                    barsPanel.add(more);
                }
                cell.add(barsPanel, BorderLayout.SOUTH);
            }
 
            cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
            cell.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    LocalDate clicked = currentMonth[0].atDay(day);
                    if (clicked.equals(selectedDate[0])) {
                        // Second click → open detail popup (original feature)
                        showDetailPopup(selectedDate[0]);
                    } else {
                        // First click → select and show summary
                        selectedDate[0] = clicked;
                        rebuildCalGrid();
                        refreshSummary();
                    }
                }
            });
            calGrid.add(cell);
        }
 
        calGrid.revalidate();
        calGrid.repaint();
    }
 
    private void refreshSummary() {
        if (summaryContent == null || summaryTitle == null) return;
 
        summaryContent.removeAll();
        String dateStr = selectedDate[0].toString();
        summaryTitle.setText("Appointments for " + dateStr);
 
        User user = app.getLoggedInUserObj();
        int count = 0;
 
        for (Appointment a : appointmentService.getAll()) {
            if (user != null && !a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) continue;
            String dt       = a.getDateTime();
            String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
            if (!apptDate.equals(dateStr)) continue;
            count++;
            String time     = dt.contains(" ") ? dt.split(" ")[1] : "";
            String custName = resolveName(a.getCustomerEmail());
            summaryContent.add(buildAppointmentCard(a, custName, time));
            summaryContent.add(Box.createVerticalStrut(8));
        }
 
        if (count == 0) {
            JLabel empty = new JLabel("No appointments on this date.");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(new EmptyBorder(12, 0, 0, 0));
            summaryContent.add(empty);
        }
 
        summaryContent.revalidate();
        summaryContent.repaint();
    }
 
    private JPanel buildAppointmentCard(Appointment a, String custName, String time) {
        Color barColor;
        switch (a.getStatus()) {
            case "Completed":   barColor = GREEN;  break;
            case "In Progress": barColor = ORANGE; break;
            default:            barColor = GREY;   break;
        }
 
        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 249, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(230, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setMinimumSize(new Dimension(0, 72));
        card.setBorder(new EmptyBorder(8, 8, 8, 10));
 
        // Coloured left bar
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(4, 0));
        card.add(bar, BorderLayout.WEST);
 
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);
 
        JLabel line1 = new JLabel(a.getServiceType() + "  \u2022  "
                + a.getDurationHours() + "h  \u2022  " + time);
        line1.setFont(UIConstants.FONT_SMALL_BOLD);
        line1.setForeground(UIConstants.TEXT_PRIMARY);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        JLabel line2 = new JLabel("Customer: " + custName
                + "  (" + a.getCustomerEmail() + ")");
        line2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        line2.setForeground(UIConstants.TEXT_SECONDARY);
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        JLabel statusLabel = new JLabel(a.getStatus()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                switch (a.getStatus()) {
                    case "Completed":   bg = new Color(220, 245, 225); break;
                    case "In Progress": bg = new Color(255, 243, 220); break;
                    default:            bg = new Color(235, 235, 240); break;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        statusLabel.setForeground(barColor);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(2, 8, 2, 8));
        statusLabel.setMaximumSize(new Dimension(90, 18));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        details.add(line1);
        details.add(Box.createVerticalStrut(2));
        details.add(line2);
        details.add(Box.createVerticalStrut(4));
        details.add(statusLabel);
        card.add(details, BorderLayout.CENTER);
 
        return card;
    }
 

    private void showDetailPopup(LocalDate date) {
        User user = app.getLoggedInUserObj();
        String dateStr = date.toString();
 
        List<Appointment> appts = new ArrayList<>();
        for (Appointment a : appointmentService.getAll()) {
            if (user != null && !a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) continue;
            String dt       = a.getDateTime();
            String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
            if (apptDate.equals(dateStr)) appts.add(a);
        }
        if (appts.isEmpty()) return;
 
        String monthName = date.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
 
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                date.getDayOfMonth() + " " + monthName + " " + date.getYear(), true);
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
 
            JLabel idLbl = new JLabel(a.getId() + "  \u00B7  " + a.getServiceType());
            idLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            idLbl.setForeground(UIConstants.TEXT_PRIMARY);
            idLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(idLbl);
            row.add(Box.createVerticalStrut(4));
 
            String time = a.getDateTime().contains(" ")
                    ? a.getDateTime().split(" ")[1] : a.getDateTime();
            JLabel timeLbl = new JLabel(
                    "\u23F0 " + time + "  \u00B7  " + a.getDurationHours() + " hour(s)");
            timeLbl.setFont(UIConstants.FONT_SMALL);
            timeLbl.setForeground(UIConstants.TEXT_MUTED);
            timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(timeLbl);
            row.add(Box.createVerticalStrut(4));
 
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
 
    private String resolveName(String id) {
        for (User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id)) return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }
 
    private JButton calNavBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setPreferredSize(new Dimension(40, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
 
    private JPanel legendDot(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        item.setOpaque(false);
        JLabel circle = new JLabel("\u25CF");
        circle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        circle.setForeground(color);
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(UIConstants.TEXT_DARK);
        item.add(circle);
        item.add(label);
        return item;
    }
}