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

public class TechFeedback extends JPanel {
	private final AppFrame app;
	private final AppointmentService appointmentService = new AppointmentService();
	private JPanel listPanel; 
    private static final Color GREEN  = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY   = new Color(108, 117, 125);
    
//    Constructor
    public TechFeedback (AppFrame app) {
    	this.app = app;
    	setLayout(new BorderLayout());
    	setBorder (new EmptyBorder (30, 36, 30,36));
    	JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setBackground(UIConstants.BG_CONTENT);
        JLabel title = new JLabel("My Feedbacks");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel subtitle = new JLabel("Write or update your feedback for each assigned appointment.");
        subtitle.setFont(UIConstants.FONT_BODY);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setBorder(new EmptyBorder(4, 0, 20, 0));
        titlePanel.add(title);
        titlePanel.add(subtitle);
        add(titlePanel, BorderLayout.NORTH);

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
            JLabel empty = new JLabel("No appointments to provide feedback for.");
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
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
 
        JLabel idLabel = new JLabel(
                appt.getId() + "  ·  " + appt.getServiceType()
                + "  ·  " + appt.getDateTime());
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
        card.add(Box.createVerticalStrut(8));
 
        JLabel custLabel = new JLabel(
        "Customer: " + resolveName(appt.getCustomerEmail()));
        custLabel.setFont(UIConstants.FONT_BODY);
        custLabel.setForeground(UIConstants.TEXT_PRIMARY);
        custLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(custLabel);
        card.add(Box.createVerticalStrut(12));

        JLabel feedbackTitle = new JLabel("My Feedback:");
        feedbackTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        feedbackTitle.setForeground(UIConstants.TEXT_MUTED);
        feedbackTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(feedbackTitle);
        card.add(Box.createVerticalStrut(6));

        String existing = appointmentService.getFeedback(appt.getId());
        JTextArea feedbackArea = new JTextArea(existing != null ? existing : "");
        feedbackArea.setFont(UIConstants.FONT_BODY);
        feedbackArea.setForeground(UIConstants.TEXT_DARK);
        feedbackArea.setBackground(Color.WHITE);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
        new EmptyBorder(8, 10, 8, 10)));
        feedbackArea.setRows(3);
        feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(feedbackArea);
        card.add(Box.createVerticalStrut(12));
        
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        JButton saveBtn = actionButton("Save Feedback",
                new Color(80, 110, 230), Color.WHITE);
        saveBtn.addActionListener(e -> {
            String fb = feedbackArea.getText().trim();
            if (fb.isEmpty()) {
                JOptionPane.showMessageDialog(app,
                        "Please enter feedback before saving.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (appointmentService.saveFeedback(appt.getId(), fb)) {
                JOptionPane.showMessageDialog(app,
                        "Feedback saved successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app,
                        "Failed to save feedback.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        });
 
        btnRow.add(saveBtn);
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
        btn.setPreferredSize(new Dimension(130, 34));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    }
