package view;
 
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.User;
 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
 
/**
 * TechAppointment — "My Appointments" page for the Technician dashboard.
 *
 * Layout mirrors the admin/customer style:
 *   • Three KPI summary cards at the top (Total / Pending / Completed)
 *   • Rounded table card below with search + status filter
 *   • Clicking a non-Completed row shows a detail + "Mark Completed" dialog
 */
public class TechAppointment extends JPanel {
 

    private static final Color BG = new Color(245, 246, 250);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(225, 228, 235);
    private static final Color TEXT = new Color(30,  35,  50);
    private static final Color MUTED = new Color(110, 118, 140);
    private static final Color BLUE = new Color(80,  110, 230);
    private static final Color GREEN = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY = new Color(108, 117, 125);
    private static final Color PURPLE = new Color(130, 80,  220);
 
    private static final String[] COLUMNS = {
        "Appt ID", "Service Type", "Date / Time", "Duration",
        "Customer", "Vehicle", "Status", "Action"
    };
 
    private final AppFrame app;
    private final AppointmentService appointmentSvc = new AppointmentService();
 
    private JLabel lblTotal, lblPending, lblCompleted;
 
    private DefaultTableModel tableModel;
    private JTable table;
 
    private JTextField searchField;
    private JComboBox<String> statusFilter;
 
    private final List<Appointment> allMine = new ArrayList<>();
 
    public TechAppointment(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(BG);
 
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(24, 28, 28, 28));
        page.add(buildSubtitle(), BorderLayout.NORTH);
        page.add(buildBody(), BorderLayout.CENTER);
 
        JScrollPane outer = new JScrollPane(page);
        outer.setBorder(null);
        outer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        outer.getViewport().setBackground(BG);
        add(outer, BorderLayout.CENTER);
    }
 

    private JPanel buildSubtitle() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));
        JLabel lbl = new JLabel(
            "View your assigned appointments and mark them as completed when done.");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(MUTED);
        p.add(lbl);
        return p;
    }
 
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(buildKpiRow(), BorderLayout.NORTH);
        body.add(buildTableCard(), BorderLayout.CENTER);
        return body;
    }
 
    private JPanel buildKpiRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setOpaque(false);
 
        lblTotal = new JLabel("0");
        lblPending = new JLabel("0");
        lblCompleted = new JLabel("0");
 
        row.add(kpiCard("Total Assigned", lblTotal, "\u2756", BLUE, new Color(235, 240, 255)));
        row.add(kpiCard("Pending / In Progress", lblPending, "\u29D6", ORANGE, new Color(255, 245, 230)));
        row.add(kpiCard("Completed", lblCompleted, "\u2714", GREEN, new Color(220, 245, 225)));
        return row;
    }
 
    private JPanel kpiCard(String title, JLabel valueLabel, String icon,
        Color accent, Color iconBg) {
        JPanel card = roundedCard(true, accent);
        card.setLayout(new BorderLayout(14, 0));
 
  
        JLabel iconLbl = new JLabel(icon, SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(iconBg);
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 20));
        iconLbl.setForeground(accent);
        iconLbl.setPreferredSize(new Dimension(52, 52));
 
        JPanel iconWrap = new JPanel(new GridBagLayout());
        iconWrap.setOpaque(false);
        iconWrap.add(iconLbl);
        card.add(iconWrap, BorderLayout.WEST);
 
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
 
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLbl.setForeground(MUTED);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        valueLabel.setForeground(TEXT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        info.add(Box.createVerticalGlue());
        info.add(valueLabel);
        info.add(Box.createVerticalStrut(2));
        info.add(titleLbl);
        info.add(Box.createVerticalGlue());
        card.add(info, BorderLayout.CENTER);
        return card;
    }
 
    private JPanel buildTableCard() {
        JPanel card = roundedCard(false, null);
        card.setLayout(new BorderLayout());
        card.add(buildControls(), BorderLayout.NORTH);
 
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
 
        table = new JTable(tableModel);
        table.setRowHeight(46);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setGridColor(new Color(240, 240, 245));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(235, 240, 255));
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
 
        JTableHeader hdr = table.getTableHeader();
        hdr.setReorderingAllowed(false);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 12));
        hdr.setBackground(new Color(250, 250, 253));
        hdr.setForeground(MUTED);
        hdr.setPreferredSize(new Dimension(0, 44));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0,
                new Color(230, 230, 238)));
 
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                return this;
            }
        };
        for (int i = 0; i < COLUMNS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(centre);
 
        table.getColumnModel().getColumn(6).setCellRenderer((t, v, sel, foc, r, c) -> {
            String s = v != null ? v.toString() : "";
            JLabel badge = new JLabel(s, SwingConstants.CENTER);
            badge.setFont(new Font("SansSerif", Font.BOLD, 11));
            Color fc, bg2;
            switch (s) {
                case "Completed":
                    fc = GREEN; bg2 = new Color(220, 245, 225); break;
                case "In Progress":
                    fc = ORANGE; bg2 = new Color(255, 245, 225); break;
                default:
                    fc = GREY; bg2 = new Color(235, 235, 240); break;
            }
            badge.setForeground(fc);
            badge.setOpaque(true);
            badge.setBackground(bg2);
            badge.setBorder(new EmptyBorder(3, 10, 3, 10));
            JPanel wrap = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.dispose();
                }
            };
            wrap.setOpaque(false);

            JPanel pill = new JPanel(new GridBagLayout()) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(bg2);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                    g2.dispose();
                }
            };
            pill.setOpaque(false);
            pill.add(badge);
            badge.setOpaque(false);
            badge.setBackground(new Color(0, 0, 0, 0));
            wrap.add(pill);
            return wrap;
        });
 

        table.getColumnModel().getColumn(7).setCellRenderer((t, v, sel, foc, r, c) -> {
            String status = tableModel.getRowCount() > r
                    ? (String) tableModel.getValueAt(r, 6) : "";
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
            if ("Completed".equalsIgnoreCase(status)) {
                JLabel done = new JLabel("\u2714 Completed");
                done.setFont(new Font("SansSerif", Font.BOLD, 12));
                done.setForeground(GREEN);
                wrap.add(done);
            } else {
                wrap.add(outlineBtn("View Details", BLUE, 110));
            }
            return wrap;
        });
 
        int[] widths = {72, 110, 130, 70, 110, 80, 110, 120};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
 
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                String apptId = (String) tableModel.getValueAt(row, 0);
                String status = (String) tableModel.getValueAt(row, 6);
                Appointment appt = findAppt(apptId);
                if (appt != null) openDetailDialog(appt, row);
            }
        });
 
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 440));
        card.add(scroll, BorderLayout.CENTER);
 
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 240)),
            new EmptyBorder(10, 18, 10, 18)));
        JLabel hint = new JLabel("Click any row to view details or mark the appointment as completed.");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        footer.add(hint, BorderLayout.WEST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }
 
    private JPanel buildControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 10, 0));
 
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER, 1, true), new EmptyBorder(2, 8, 2, 8)));
        searchField.setToolTipText("Search appointments…");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
 
        statusFilter = new JComboBox<>(new String[]{
            "All Status", "Pending", "In Progress", "Completed"});
        statusFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusFilter.setPreferredSize(new Dimension(160, 30));
        statusFilter.setBackground(Color.WHITE);
        statusFilter.addActionListener(e -> applyFilters());
 
        row.add(searchField);
        row.add(statusFilter);
        return row;
    }
 
    private void openDetailDialog(Appointment appt, int tableRow) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Appointment — " + appt.getId(), true);
        dlg.setSize(520, 400);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
 
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(28, 36, 24, 36));

        JLabel title = new JLabel("Appointment Details");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(4));
 
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        form.add(sep);
        form.add(Box.createVerticalStrut(18));
 
        String custName = resolveName(appt.getCustomerId());
        String[][] fields = {
            {"Appointment ID", appt.getId()},
            {"Service Type", appt.getServiceType()},
            {"Date / Time", appt.getDateTime()},
            {"Duration", appt.getDurationHours() + " hour(s)"},
            {"Customer", custName + " (" + appt.getCustomerId() + ")"},
            {"Vehicle", appt.getVehicleId()},
            {"Status", appt.getStatus()},
        };
 
        for (String[] field : fields) {
            JPanel fieldRow = new JPanel(new BorderLayout(12, 0));
            fieldRow.setOpaque(false);
            fieldRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            fieldRow.setAlignmentX(Component.LEFT_ALIGNMENT);
 
            JLabel key = new JLabel(field[0]);
            key.setFont(new Font("SansSerif", Font.BOLD, 13));
            key.setForeground(MUTED);
            key.setPreferredSize(new Dimension(130, 20));
 
            JLabel val = new JLabel(field[1]);
            val.setFont(new Font("SansSerif", Font.PLAIN, 13));
            val.setForeground(TEXT);
 
            if (field[0].equals("Status")) {
                switch (field[1]) {
                    case "Completed": val.setForeground(GREEN);  break;
                    case "In Progress": val.setForeground(ORANGE); break;
                    default: val.setForeground(GREY);   break;
                }
                val.setFont(new Font("SansSerif", Font.BOLD, 13));
            }
 
            fieldRow.add(key, BorderLayout.WEST);
            fieldRow.add(val, BorderLayout.CENTER);
            form.add(fieldRow);
            form.add(Box.createVerticalStrut(10));
        }
 
        String comment = appointmentSvc.getComment(appt.getId());
        if (comment != null && !comment.isBlank()) {
            form.add(Box.createVerticalStrut(4));
            JLabel commentHdr = new JLabel("Customer Comment");
            commentHdr.setFont(new Font("SansSerif", Font.BOLD, 13));
            commentHdr.setForeground(MUTED);
            commentHdr.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(commentHdr);
            form.add(Box.createVerticalStrut(6));
 
            JTextArea commentArea = new JTextArea(comment);
            commentArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
            commentArea.setForeground(TEXT);
            commentArea.setBackground(new Color(245, 246, 250));
            commentArea.setEditable(false);
            commentArea.setLineWrap(true);
            commentArea.setWrapStyleWord(true);
            commentArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));
            commentArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            commentArea.setAlignmentX(Component.LEFT_ALIGNMENT);
            form.add(commentArea);
            form.add(Box.createVerticalStrut(10));
        }
 
        form.add(Box.createVerticalGlue());
 
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
 
        JButton closeBtn = dialogBtn("Close", Color.WHITE, TEXT, BORDER);
        closeBtn.addActionListener(e -> dlg.dispose());
        btnRow.add(closeBtn);
 
        if (!"Completed".equalsIgnoreCase(appt.getStatus())) {
            JButton markBtn = dialogBtn("Mark Completed", GREEN, Color.WHITE, GREEN);
            markBtn.addActionListener(e -> {
                int confirm = JOptionPane.showConfirmDialog(dlg,
                    "Mark appointment " + appt.getId() + " as Completed?",
                    "Confirm", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    if (appointmentSvc.updateStatus(appt.getId(), "Completed")) {
                        dlg.dispose();
                        refresh();  
                        JOptionPane.showMessageDialog(app,
                            appt.getId() + " marked as Completed.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(dlg,
                            "Failed to update status. Please try again.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            btnRow.add(markBtn);
        }
 
        form.add(btnRow);
        dlg.setContentPane(form);
        dlg.setVisible(true);
    }
 

    public void refresh() {
        allMine.clear();
        User user = app.getLoggedInUserObj();
        if (user == null) { tableModel.setRowCount(0); updateKpis(); return; }
 
        for (Appointment a : appointmentSvc.getAll()) {
            if (a.getTechnicianEmail().equalsIgnoreCase(user.getUserId()))
                allMine.add(a);
        }
 
        if (searchField != null) searchField.setText("");
        if (statusFilter != null) statusFilter.setSelectedIndex(0);
 
        applyFilters();
        updateKpis();
    }
 
    private void applyFilters() {
        String query  = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String selSt  = statusFilter != null
                ? (String) statusFilter.getSelectedItem() : "All Status";
 
        tableModel.setRowCount(0);
 
        for (Appointment a : allMine) {

            if (!"All Status".equals(selSt) && !selSt.equalsIgnoreCase(a.getStatus())) continue;
 
            String custName = resolveName(a.getCustomerId());

            if (!query.isEmpty()) {
                boolean hit = a.getId().toLowerCase().contains(query)
                        || a.getServiceType().toLowerCase().contains(query)
                        || a.getDateTime().toLowerCase().contains(query)
                        || custName.toLowerCase().contains(query)
                        || a.getVehicleId().toLowerCase().contains(query)
                        || a.getStatus().toLowerCase().contains(query);
                if (!hit) continue;
            }
 
            tableModel.addRow(new Object[]{
                a.getId(),
                a.getServiceType(),
                a.getDateTime(),
                a.getDurationHours() + "h",
                custName,
                a.getVehicleId(),
                a.getStatus(),
                "" 
            });
        }
    }
 
    private void updateKpis() {
        int total = allMine.size();
        int completed = 0, pending = 0;
        for (Appointment a : allMine) {
            if ("Completed".equalsIgnoreCase(a.getStatus()))       completed++;
            else                                                    pending++;
        }
        if (lblTotal != null) lblTotal.setText(String.valueOf(total));
        if (lblPending != null) lblPending.setText(String.valueOf(pending));
        if (lblCompleted != null) lblCompleted.setText(String.valueOf(completed));
    }
 

    private Appointment findAppt(String apptId) {
        for (Appointment a : allMine)
            if (a.getId().equalsIgnoreCase(apptId)) return a;
        return null;
    }
 
    private String resolveName(String id) {
        for (User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id)) return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }

    private JPanel roundedCard(boolean leftBar, Color barColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 14, 14);
                if (leftBar && barColor != null) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, 10, getHeight(), 14, 14);
                    g2.setColor(CARD);
                    g2.fillRect(5, 0, 8, getHeight());
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, leftBar ? 22 : 18, 18, 18));
        return card;
    }

    private JButton outlineBtn(String label, Color accent, int width) {
        JButton btn = new JButton(label) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? accent : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(accent);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(accent);
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.WHITE); }
            public void mouseExited (MouseEvent e) { btn.setForeground(accent); }
        });
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(width, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
 
    private JButton dialogBtn(String label, Color bg, Color fg, Color border) {
        JButton btn = new JButton(label) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(150, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}