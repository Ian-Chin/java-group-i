package view;

import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.User;
 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class TechAppointment extends JPanel {
	private final AppFrame app;
    private final AppointmentService appointmentService = new AppointmentService();
 
    // The scrollable list — rebuilt by refresh()
    private JPanel listPanel;
 
    private static final Color GREEN  = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY   = new Color(108, 117, 125);

//constructor
    public TechAppointment(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));
 
        // Page title
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(UIConstants.BG_CONTENT);
 
        JLabel title = new JLabel("My Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(UIConstants.TEXT_PRIMARY);
 
        JLabel subtitle = new JLabel("View appointment details and mark them as completed.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 20, 0));
 
        titlePanel.add(title);
        titlePanel.add(subtitle);
        add(titlePanel, BorderLayout.NORTH);
 
        // Scrollable list panel
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(UIConstants.BG_CONTENT);
 
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }
 
    /**
     * Reloads appointments from appointments.txt and rebuilds the card list.
     * Called by TechnicianDashboard when navigating to this page.
     */
    public void refresh() {
        listPanel.removeAll();
 
        User user = app.getLoggedInUserObj();
        if (user == null) { listPanel.revalidate(); listPanel.repaint(); return; }
 
        // Filter appointments assigned to this technician
        List<Appointment> mine = new ArrayList<>();
        for (Appointment a : appointmentService.getAll()) {
            if (a.getTechnicianEmail().equalsIgnoreCase(user.getUserId()))
                mine.add(a);
        }
 
        if (mine.isEmpty()) {
            JLabel empty = new JLabel("No appointments assigned to you yet.");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.setBorder(new EmptyBorder(60, 0, 0, 0));
            listPanel.add(empty);
        } else {
            for (Appointment appt : mine) {
                listPanel.add(buildCard(appt));
                listPanel.add(Box.createVerticalStrut(14));
            }
        }
 
        listPanel.revalidate();
        listPanel.repaint();
    }
 
    // ══════════════════════════════════════════════════════════
    // CARD BUILDER
    // ══════════════════════════════════════════════════════════
 
    /**
     * Builds one appointment card showing:
     * - ID, service, date/time, duration, status badge
     * - Customer name and ID
     * - Customer comment (read-only, if any exists)
     * - "Mark Completed" button (hidden if already Completed)
     */
    private JPanel buildCard(Appointment appt) {
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
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        // ── Top row: details + status badge ───────────────────
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
 
        JLabel idLabel = new JLabel(
                appt.getId() + "  ·  " + appt.getServiceType()
                + "  ·  " + appt.getDateTime()
                + "  ·  " + appt.getDurationHours() + "h");
        idLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        idLabel.setForeground(UIConstants.TEXT_MUTED);
 
        JLabel statusBadge = new JLabel(appt.getStatus());
        statusBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        switch (appt.getStatus()) {
            case "Completed":   statusBadge.setForeground(GREEN);  break;
            case "In Progress": statusBadge.setForeground(ORANGE); break;
            default:            statusBadge.setForeground(GREY);   break;
        }
        topRow.add(idLabel,     BorderLayout.WEST);
        topRow.add(statusBadge, BorderLayout.EAST);
        card.add(topRow);
        card.add(Box.createVerticalStrut(10));
 
        // ── Customer info ──────────────────────────────────────
        JLabel custLabel = new JLabel(
                "Customer: " + resolveName(appt.getCustomerEmail())
                + "  (" + appt.getCustomerEmail() + ")");
        custLabel.setFont(UIConstants.FONT_BODY);
        custLabel.setForeground(UIConstants.TEXT_PRIMARY);
        custLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(custLabel);
        card.add(Box.createVerticalStrut(10));
 
        // ── Customer comment (read-only) ───────────────────────
        String comment = appointmentService.getComment(appt.getId());
        if (comment != null && !comment.isBlank()) {
            JLabel commentTitle = new JLabel("Customer Comment:");
            commentTitle.setFont(UIConstants.FONT_SMALL_BOLD);
            commentTitle.setForeground(UIConstants.TEXT_MUTED);
            commentTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(commentTitle);
            card.add(Box.createVerticalStrut(4));
 
            JTextArea commentArea = new JTextArea(comment);
            commentArea.setFont(UIConstants.FONT_BODY);
            commentArea.setForeground(UIConstants.TEXT_SECONDARY);
            commentArea.setBackground(new Color(245, 246, 250));
            commentArea.setEditable(false);
            commentArea.setLineWrap(true);
            commentArea.setWrapStyleWord(true);
            commentArea.setBorder(new EmptyBorder(8, 10, 8, 10));
            commentArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
            commentArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(commentArea);
            card.add(Box.createVerticalStrut(10));
        }
 
        // ── Divider ────────────────────────────────────────────
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(12));
 
        // ── Action buttons ─────────────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        if (!appt.getStatus().equals("Completed")) {
            JButton completeBtn = actionButton("Mark Completed", GREEN, Color.WHITE);
            completeBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(app,
                        "Mark appointment " + appt.getId() + " as Completed?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (appointmentService.updateStatus(appt.getId(), "Completed")) {
                        JOptionPane.showMessageDialog(app,
                                "Appointment " + appt.getId() + " marked as Completed.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                        refresh(); // rebuild list
                    } else {
                        JOptionPane.showMessageDialog(app,
                                "Failed to update status.", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnRow.add(completeBtn);
        } else {
            JLabel doneLabel = new JLabel("\u2714 Completed");
            doneLabel.setFont(UIConstants.FONT_SMALL_BOLD);
            doneLabel.setForeground(GREEN);
            btnRow.add(doneLabel);
        }
 
        card.add(btnRow);
        return card;
    }
 
    // ══════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════
 
    private String resolveName(String id) {
        for (User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id))
                return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }
 
    private JButton actionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
    
}
