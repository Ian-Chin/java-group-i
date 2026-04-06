package view;

import model.AccountService;
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.List;

public class AppointmentPanel extends JPanel {

    private final AccountService accountService;
    private final AppointmentService appointmentService;

    // Form fields
    private JComboBox<String> customerCombo;
    private JComboBox<String> technicianCombo;
    private JComboBox<String> serviceTypeCombo;
    private JButton datePickerBtn;
    private LocalDate selectedDate = LocalDate.now();
    private JComboBox<String> timeCombo;

    // Table
    private DefaultTableModel tableModel;
    private JTable table;
    private JLabel countLabel;
    private JTextField searchField;
    private JComboBox<String> filterCombo;

    private static final String[] TABLE_COLUMNS = {"ID", "Customer", "Technician", "Service", "Date & Time", "Duration", "Status", ""};

    private static final Color SUCCESS_GREEN  = new Color(40, 167, 69);
    private static final Color WARNING_ORANGE = new Color(255, 165, 0);
    private static final Color STATUS_PENDING = new Color(108, 117, 125);

    public AppointmentPanel(AccountService accountService) {
        this.accountService = accountService;
        this.appointmentService = new AppointmentService();

        setLayout(new BorderLayout(16, 0));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        // Left: fixed-width form panel
        JPanel formPanel = buildFormPanel();
        formPanel.setPreferredSize(new Dimension(370, 0));
        formPanel.setMinimumSize(new Dimension(370, 0));

        // Right: flexible table panel
        JPanel tablePanel = buildTablePanel();

        add(formPanel, BorderLayout.WEST);
        add(tablePanel, BorderLayout.CENTER);
    }

    // ═══════════════════════════════════════════════════════════════
    // LEFT: Create & Assign Form
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildFormPanel() {
        JPanel card = new JPanel() {
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
        card.setOpaque(false);
        card.setLayout(new BorderLayout());

        JPanel formContent = new JPanel();
        formContent.setOpaque(false);
        formContent.setLayout(new BoxLayout(formContent, BoxLayout.Y_AXIS));
        formContent.setBorder(new EmptyBorder(28, 28, 28, 28));

        // Header section (centered content wrapped in a left-aligned panel)
        JPanel headerPanel = new JPanel();
        headerPanel.setOpaque(false);
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        JLabel icon = new JLabel("\u2637") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 240, 255));
                g2.fillOval(0, 0, 48, 48);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        icon.setForeground(UIConstants.PRIMARY);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setVerticalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setMaximumSize(new Dimension(48, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(icon);
        headerPanel.add(Box.createVerticalStrut(12));

        JLabel title = new JLabel("Create Appointment");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(title);
        headerPanel.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Assign a service to a technician");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerPanel.add(subtitle);

        formContent.add(headerPanel);
        formContent.add(Box.createVerticalStrut(22));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(235, 235, 240));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        formContent.add(sep);
        formContent.add(Box.createVerticalStrut(20));

        // Form fields
        formContent.add(formRow("Customer", customerCombo = styledCombo()));
        loadCustomers();
        formContent.add(Box.createVerticalStrut(14));

        formContent.add(formRow("Assign Technician", technicianCombo = styledCombo()));
        loadTechnicians();
        formContent.add(Box.createVerticalStrut(14));

        serviceTypeCombo = styledCombo();
        serviceTypeCombo.addItem("Normal Service (1 Hour)");
        serviceTypeCombo.addItem("Major Service (3 Hours)");
        formContent.add(formRow("Service Type", serviceTypeCombo));
        formContent.add(Box.createVerticalStrut(14));

        datePickerBtn = new JButton(selectedDate.toString()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        datePickerBtn.setFont(UIConstants.FONT_BODY);
        datePickerBtn.setForeground(UIConstants.TEXT_DARK);
        datePickerBtn.setHorizontalAlignment(SwingConstants.LEFT);
        datePickerBtn.setContentAreaFilled(false);
        datePickerBtn.setBorderPainted(false);
        datePickerBtn.setFocusPainted(false);
        datePickerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        datePickerBtn.setBorder(new EmptyBorder(0, 12, 0, 12));
        datePickerBtn.addActionListener(e -> showCalendarPopup());
        formContent.add(dateFormRow("Date", datePickerBtn));
        formContent.add(Box.createVerticalStrut(14));

        timeCombo = styledCombo();
        loadTimeSlots();
        formContent.add(formRow("Time Slot", timeCombo));
        formContent.add(Box.createVerticalStrut(24));

        // Create button
        JButton createBtn = new JButton("Create Appointment") {
            private boolean hovering = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? UIConstants.PRIMARY_HOVER : UIConstants.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        createBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        createBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        createBtn.setPreferredSize(new Dimension(300, 44));
        createBtn.setFont(UIConstants.FONT_BODY_BOLD);
        createBtn.setForeground(Color.WHITE);
        createBtn.setContentAreaFilled(false);
        createBtn.setBorderPainted(false);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> handleCreate());
        formContent.add(createBtn);

        // Add glue to push form content to top
        formContent.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(formContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel formRow(String labelText, JComboBox<String> combo) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_DARK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        row.add(Box.createVerticalStrut(6));

        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        combo.setPreferredSize(new Dimension(300, 38));
        row.add(combo);

        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    // RIGHT: Appointments Table
    // ═══════════════════════════════════════════════════════════════

    private JPanel buildTablePanel() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(UIConstants.BG_CONTENT);

        // Header bar
        JPanel headerBar = new JPanel(new BorderLayout());
        headerBar.setOpaque(false);
        headerBar.setBorder(new EmptyBorder(0, 4, 12, 0));

        JLabel listTitle = new JLabel("Appointments");
        listTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        listTitle.setForeground(UIConstants.TEXT_PRIMARY);

        // Search bar and filter
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controlsPanel.setOpaque(false);

        searchField = new JTextField(15) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setForeground(UIConstants.TEXT_DARK);
        searchField.setOpaque(false);
        searchField.setBorder(new EmptyBorder(6, 10, 6, 10));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setToolTipText("Search appointments...");
        searchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override public void keyReleased(java.awt.event.KeyEvent e) {
                refreshTable();
            }
        });

        filterCombo = new JComboBox<>(new String[]{"All Status", "Pending", "In Progress", "Completed"});
        filterCombo.setFont(UIConstants.FONT_SMALL_BOLD);
        filterCombo.setBackground(Color.WHITE);
        filterCombo.setPreferredSize(new Dimension(120, 32));
        filterCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        filterCombo.addActionListener(e -> refreshTable());

        controlsPanel.add(searchField);
        controlsPanel.add(filterCombo);

        headerBar.add(listTitle, BorderLayout.WEST);
        headerBar.add(controlsPanel, BorderLayout.EAST);
        wrapper.add(headerBar, BorderLayout.NORTH);

        // Table card
        tableModel = new DefaultTableModel(TABLE_COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);
        cm.getColumn(0).setMaxWidth(60);
        cm.getColumn(1).setPreferredWidth(110);
        cm.getColumn(2).setPreferredWidth(110);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(130);
        cm.getColumn(5).setPreferredWidth(60);
        cm.getColumn(5).setMaxWidth(70);
        cm.getColumn(6).setPreferredWidth(85);
        cm.getColumn(6).setMaxWidth(95);
        cm.getColumn(7).setPreferredWidth(70);
        cm.getColumn(7).setMaxWidth(80);

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && col == 7) {
                    String id = (String) tableModel.getValueAt(row, 0);
                    int confirm = JOptionPane.showConfirmDialog(AppointmentPanel.this,
                            "Delete appointment " + id + "?", "Confirm", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        appointmentService.delete(id);
                        refreshTable();
                    }
                }
            }
        });

        JPanel tableCard = new JPanel(new BorderLayout()) {
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
        tableCard.setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 240)),
                new EmptyBorder(10, 18, 10, 18)));
        countLabel = new JLabel("0 appointments");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_MUTED);
        footer.add(countLabel, BorderLayout.WEST);
        tableCard.add(footer, BorderLayout.SOUTH);

        wrapper.add(tableCard, BorderLayout.CENTER);
        refreshTable();
        return wrapper;
    }

    private JButton outlineBtn(String text) {
        JButton btn = new JButton(text) {
            private boolean hovering = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? new Color(245, 245, 250) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(UIConstants.BORDER_OUTLINE);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 32));
        return btn;
    }

    // ─── Table styling ───────────────────────────────────────────

    private void styleTable(JTable t) {
        t.setRowHeight(48);
        t.setFont(UIConstants.FONT_BODY);
        t.setForeground(UIConstants.TEXT_DARK);
        t.setGridColor(new Color(240, 240, 245));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setSelectionBackground(new Color(235, 240, 255));
        t.setSelectionForeground(UIConstants.TEXT_DARK);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 0));

        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(new Color(250, 250, 253));
        t.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        t.getTableHeader().setPreferredSize(new Dimension(0, 44));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 238)));

        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        t.getColumnModel().getColumn(0).setCellRenderer(centre);

        DefaultTableCellRenderer durRenderer = new DefaultTableCellRenderer();
        durRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        t.getColumnModel().getColumn(5).setCellRenderer(durRenderer);

        // Status column
        t.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                String status = val != null ? val.toString() : "";
                switch (status) {
                    case "Completed":   lbl.setForeground(SUCCESS_GREEN); break;
                    case "In Progress": lbl.setForeground(WARNING_ORANGE); break;
                    default:            lbl.setForeground(STATUS_PENDING); break;
                }
                return lbl;
            }
        });

        // Delete column
        t.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
                cell.setOpaque(true);
                cell.setBackground(sel ? tbl.getSelectionBackground() : Color.WHITE);
                JLabel del = new JLabel("\u2715 Delete") {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(255, 235, 235));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                del.setFont(new Font("SansSerif", Font.BOLD, 11));
                del.setForeground(UIConstants.TEXT_DANGER);
                del.setHorizontalAlignment(SwingConstants.CENTER);
                del.setPreferredSize(new Dimension(62, 28));
                del.setOpaque(false);
                del.setCursor(new Cursor(Cursor.HAND_CURSOR));
                cell.add(del);
                return cell;
            }
        });
    }

    // ─── Data loading helpers ────────────────────────────────────

    private void loadCustomers() {
        if (customerCombo == null) return;
        customerCombo.removeAllItems();
        List<User> customers = accountService.getUsersByRole("customer");
        if (customers.isEmpty()) {
            customerCombo.addItem("-- No customers --");
        } else {
            for (User u : customers) {
                customerCombo.addItem(u.getName() + " (" + u.getEmail() + ")");
            }
        }
    }

    private void loadTechnicians() {
        if (technicianCombo == null) return;
        technicianCombo.removeAllItems();
        List<User> techs = accountService.getUsersByRole("technician");
        if (techs.isEmpty()) {
            technicianCombo.addItem("-- No technicians --");
        } else {
            for (User u : techs) {
                technicianCombo.addItem(u.getName() + " (" + u.getEmail() + ")");
            }
        }
    }

    private void loadTimeSlots() {
        timeCombo.removeAllItems();
        for (int h = 8; h <= 17; h++) {
            timeCombo.addItem(LocalTime.of(h, 0).toString());
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Appointment> all = appointmentService.getAll();

        String searchText = (searchField != null) ? searchField.getText().trim().toLowerCase() : "";
        String statusFilter = (filterCombo != null && filterCombo.getSelectedItem() != null)
                ? filterCombo.getSelectedItem().toString() : "All Status";

        int count = 0;
        for (Appointment a : all) {
            // Status filter
            if (!"All Status".equals(statusFilter) && !a.getStatus().equalsIgnoreCase(statusFilter)) {
                continue;
            }

            String custName = resolveName(a.getCustomerEmail());
            String techName = resolveName(a.getTechnicianEmail());

            // Search filter — matches against ID, customer, technician, service, date/time
            if (!searchText.isEmpty()) {
                boolean match = a.getId().toLowerCase().contains(searchText)
                        || custName.toLowerCase().contains(searchText)
                        || techName.toLowerCase().contains(searchText)
                        || a.getServiceType().toLowerCase().contains(searchText)
                        || a.getDateTime().toLowerCase().contains(searchText);
                if (!match) continue;
            }

            tableModel.addRow(new Object[]{
                    a.getId(),
                    custName,
                    techName,
                    a.getServiceType(),
                    a.getDateTime(),
                    a.getDurationHours() + "h",
                    a.getStatus(),
                    ""
            });
            count++;
        }
        countLabel.setText(count + " appointment" + (count != 1 ? "s" : ""));
    }

    private String resolveName(String idOrEmail) {
        List<User> all = accountService.getAllUsers();
        for (User u : all) {
            if (u.getEmail().equalsIgnoreCase(idOrEmail)) return u.getName();
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(idOrEmail)) return u.getName();
        }
        return idOrEmail;
    }

    // ─── Create handler ──────────────────────────────────────────

    private void handleCreate() {
        if (customerCombo.getItemCount() == 0 || customerCombo.getSelectedItem().toString().startsWith("--")) {
            error("Please select a customer."); return;
        }
        if (technicianCombo.getItemCount() == 0 || technicianCombo.getSelectedItem().toString().startsWith("--")) {
            error("Please select a technician."); return;
        }

        String custEntry = customerCombo.getSelectedItem().toString();
        String techEntry = technicianCombo.getSelectedItem().toString();
        String custEmail = extractEmail(custEntry);
        String techEmail = extractEmail(techEntry);

        String serviceSelection = serviceTypeCombo.getSelectedItem().toString();
        String serviceType = serviceSelection.contains("Normal") ? "Normal Service" : "Major Service";
        int duration = serviceType.equals("Normal Service") ? 1 : 3;

        String date = selectedDate.toString();
        String time = timeCombo.getSelectedItem().toString();
        String dateTime = date + " " + time;

        String id = appointmentService.nextId();
        Appointment appt = new Appointment(id, custEmail, "", techEmail, serviceType, "Pending", dateTime, duration);

        if (appointmentService.add(appt)) {
            refreshTable();
            JOptionPane.showMessageDialog(this, "Appointment " + id + " created successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            error("Failed to create appointment.");
        }
    }

    private String extractEmail(String entry) {
        int start = entry.lastIndexOf('(');
        int end = entry.lastIndexOf(')');
        if (start >= 0 && end > start) {
            return entry.substring(start + 1, end);
        }
        return entry;
    }

    // ─── Date picker ─────────────────────────────────────────────

    private JPanel dateFormRow(String labelText, JButton btn) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_DARK);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(lbl);
        row.add(Box.createVerticalStrut(6));

        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        btn.setPreferredSize(new Dimension(300, 38));
        row.add(btn);

        return row;
    }

    private void showCalendarPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 8, 8, 8)));
        popup.setBackground(Color.WHITE);

        JPanel calPanel = new JPanel(new BorderLayout(0, 6));
        calPanel.setBackground(Color.WHITE);
        calPanel.setPreferredSize(new Dimension(280, 260));

        final YearMonth[] currentMonth = { YearMonth.from(selectedDate) };

        // Nav row
        JPanel navRow = new JPanel(new BorderLayout());
        navRow.setBackground(Color.WHITE);

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        monthLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JButton prev = miniNavBtn("<");
        JButton next = miniNavBtn(">");
        navRow.add(prev, BorderLayout.WEST);
        navRow.add(monthLabel, BorderLayout.CENTER);
        navRow.add(next, BorderLayout.EAST);
        calPanel.add(navRow, BorderLayout.NORTH);

        // Day-of-week header + grid
        JPanel body = new JPanel(new BorderLayout(0, 4));
        body.setBackground(Color.WHITE);

        JPanel dowRow = new JPanel(new GridLayout(1, 7, 2, 0));
        dowRow.setBackground(Color.WHITE);
        for (String d : new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"}) {
            JLabel dl = new JLabel(d, SwingConstants.CENTER);
            dl.setFont(new Font("SansSerif", Font.BOLD, 11));
            dl.setForeground(UIConstants.TEXT_MUTED);
            dowRow.add(dl);
        }
        body.add(dowRow, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 7, 2, 2));
        grid.setBackground(Color.WHITE);
        body.add(grid, BorderLayout.CENTER);
        calPanel.add(body, BorderLayout.CENTER);

        Runnable[] refresh = new Runnable[1];
        refresh[0] = () -> {
            monthLabel.setText(currentMonth[0].getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentMonth[0].getYear());
            grid.removeAll();

            LocalDate first = currentMonth[0].atDay(1);
            int startDow = first.getDayOfWeek().getValue() % 7;
            int daysInMonth = currentMonth[0].lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int i = 0; i < startDow; i++) grid.add(new JLabel(""));

            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = currentMonth[0].atDay(d);
                boolean isToday = date.equals(today);
                boolean isSel = date.equals(selectedDate);
                boolean isPast = date.isBefore(today);
                final LocalDate clickDate = date;

                JLabel cell = new JLabel(String.valueOf(d), SwingConstants.CENTER) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (isSel) {
                            g2.setColor(UIConstants.PRIMARY);
                            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                        } else if (isToday) {
                            g2.setColor(new Color(235, 240, 255));
                            g2.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                        }
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                cell.setFont(new Font("SansSerif", isToday || isSel ? Font.BOLD : Font.PLAIN, 12));
                cell.setForeground(isSel ? Color.WHITE : isPast ? UIConstants.TEXT_MUTED : (isToday ? UIConstants.PRIMARY : UIConstants.TEXT_DARK));
                cell.setPreferredSize(new Dimension(34, 30));
                cell.setOpaque(false);
                if (!isPast) {
                    cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    cell.addMouseListener(new MouseAdapter() {
                        @Override public void mouseClicked(MouseEvent e) {
                            selectedDate = clickDate;
                            datePickerBtn.setText(selectedDate.toString());
                            popup.setVisible(false);
                        }
                    });
                }
                grid.add(cell);
            }
            grid.revalidate();
            grid.repaint();
        };

        prev.addActionListener(e -> { currentMonth[0] = currentMonth[0].minusMonths(1); refresh[0].run(); });
        next.addActionListener(e -> { currentMonth[0] = currentMonth[0].plusMonths(1); refresh[0].run(); });
        refresh[0].run();

        popup.add(calPanel);
        popup.show(datePickerBtn, 0, datePickerBtn.getHeight());
    }

    private JButton miniNavBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setPreferredSize(new Dimension(30, 24));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── UI helpers ──────────────────────────────────────────────

    private JComboBox<String> styledCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(UIConstants.FONT_BODY);
        combo.setBackground(Color.WHITE);
        combo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return combo;
    }aaaaaaaa

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
}