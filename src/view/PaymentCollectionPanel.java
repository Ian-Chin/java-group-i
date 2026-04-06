package view;

import model.AccountService;
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.PaymentService;
import model.User;
import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;


/**
 * PaymentCollectionPanel — Counter Staff page for collecting payments
 * and generating receipts.
 *
 * Features:
 *   - 3 stat cards: Total Collected, Unpaid Appointments, Today's Collections
 *   - Two views: "All Payments" table and "Unpaid Appointments" table
 *   - Collect Payment: select an unpaid completed appointment, choose method, confirm
 *   - Generate Receipt: click any paid row to view a printable invoice popup
 */
public class PaymentCollectionPanel extends JPanel {

    private final AccountService accountService;
    private final PaymentService paymentService = new PaymentService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final VehicleService vehicleService = new VehicleService();

    // Stat labels
    private JLabel totalCollectedValue;
    private JLabel totalCollectedSub;
    private JLabel unpaidCountValue;
    private JLabel unpaidCountSub;
    private JLabel todayCollectedValue;
    private JLabel todayCollectedSub;

    // Tables
    private DefaultTableModel paymentsTableModel;
    private DefaultTableModel unpaidTableModel;
    private JTable paymentsTable;
    private JTable unpaidTable;

    // Data
    private List<String[]> allPayments = new ArrayList<>();
    private List<Appointment> unpaidAppointments = new ArrayList<>();

    // Tab buttons
    private JButton allPaymentsTabBtn;
    private JButton unpaidTabBtn;
    private CardLayout tableCardLayout;
    private JPanel tableCardPanel;

    // Prices: [0] = Normal Service, [1] = Major Service
    private double normalPrice = 50.0;
    private double majorPrice = 200.0;

    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30, 35, 50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    public PaymentCollectionPanel(AccountService accountService) {
        this.accountService = accountService;
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
        loadPrices();

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(COLOR_BG);
        content.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Header
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel subtitle = new JLabel("Manage payment collection for completed appointments. Click any paid row to generate a receipt.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);
        content.add(header, BorderLayout.NORTH);

        // Center: stats + tabs + tables
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        center.add(buildStatsRow());
        center.add(Box.createVerticalStrut(18));
        center.add(buildTabBar());
        center.add(Box.createVerticalStrut(12));
        center.add(buildTableCards());

        content.add(center, BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getViewport().setBackground(COLOR_BG);
        add(scroll, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refresh();
    }

    // ─── Load prices from prices.txt ────────────────────────────
    private void loadPrices() {
        File file = new File("src" + File.separator + "TxtFile" + File.separator + "prices.txt");
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !line.isBlank()) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    normalPrice = Double.parseDouble(parts[0].trim());
                    majorPrice = Double.parseDouble(parts[1].trim());
                }
            }
        } catch (Exception ignored) {}
    }

    // ─── Stats row ──────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        totalCollectedValue = makeBigValueLabel("—");
        totalCollectedSub = makeMutedLabel("from 0 payments");
        row.add(buildStatCard("Total Collected", totalCollectedValue, totalCollectedSub,
                new Color(40, 167, 69)));

        unpaidCountValue = makeBigValueLabel("—");
        unpaidCountSub = makeMutedLabel("awaiting collection");
        row.add(buildStatCard("Unpaid Appointments", unpaidCountValue, unpaidCountSub,
                new Color(255, 165, 0)));

        todayCollectedValue = makeBigValueLabel("—");
        todayCollectedSub = makeMutedLabel("collected today");
        row.add(buildStatCard("Today's Collections", todayCollectedValue, todayCollectedSub,
                new Color(80, 110, 230)));

        return row;
    }

    private JPanel buildStatCard(String topText, JLabel valueLabel, JLabel subLabel, Color accentColor) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // Left accent
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 5, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel topLabel = new JLabel(topText);
        topLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        topLabel.setForeground(COLOR_MUTED);
        topLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(topLabel);
        card.add(Box.createVerticalStrut(6));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(subLabel);
        return card;
    }

    // ─── Tab bar ────────────────────────────────────────────────
    private JPanel buildTabBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

        allPaymentsTabBtn = makeTabButton("All Payments", true);
        unpaidTabBtn = makeTabButton("Unpaid Appointments", false);

        allPaymentsTabBtn.addActionListener(e -> {
            styleTab(allPaymentsTabBtn, true);
            styleTab(unpaidTabBtn, false);
            tableCardLayout.show(tableCardPanel, "ALL");
        });
        unpaidTabBtn.addActionListener(e -> {
            styleTab(unpaidTabBtn, true);
            styleTab(allPaymentsTabBtn, false);
            tableCardLayout.show(tableCardPanel, "UNPAID");
        });

        bar.add(allPaymentsTabBtn);
        bar.add(unpaidTabBtn);
        return bar;
    }

    private JButton makeTabButton(String text, boolean active) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 36));
        styleTab(btn, active);
        return btn;
    }

    private void styleTab(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(new Color(80, 110, 230));
            btn.setForeground(Color.WHITE);
            btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        } else {
            btn.setBackground(new Color(235, 238, 248));
            btn.setForeground(COLOR_TEXT);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        }
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
    }

    // ─── Table cards ────────────────────────────────────────────
    private JPanel buildTableCards() {
        tableCardLayout = new CardLayout();
        tableCardPanel = new JPanel(tableCardLayout);
        tableCardPanel.setOpaque(false);
        tableCardPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        tableCardPanel.add(buildAllPaymentsCard(), "ALL");
        tableCardPanel.add(buildUnpaidCard(), "UNPAID");

        return tableCardPanel;
    }

    // ─── All Payments table ─────────────────────────────────────
    private JPanel buildAllPaymentsCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        String[] columns = {
            "Payment ID", "Customer", "Appointment ID",
            "Vehicle", "Amount (RM)", "Date", "Method", "Status"
        };

        paymentsTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        paymentsTable = TableHelper.buildTable(paymentsTableModel);

        // Centre-align all columns except Status
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };
        for (int i = 0; i <= 6; i++) {
            paymentsTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Status badge renderer
        paymentsTable.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel badge = new JLabel(value != null ? value.toString() : "");
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));
                badge.setBackground(new Color(220, 248, 232));
                badge.setForeground(new Color(34, 139, 80));
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                if (isSelected) wrapper.setBackground(new Color(80, 110, 230, 60));
                wrapper.add(badge);
                return wrapper;
            }
        );

        int[] widths = { 90, 130, 120, 140, 100, 110, 90, 80 };
        for (int i = 0; i < widths.length; i++) {
            paymentsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Click row to generate receipt
        paymentsTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = paymentsTable.getSelectedRow();
                if (row >= 0 && row < allPayments.size()) {
                    showReceipt(allPayments.get(row));
                }
            }
        });
        paymentsTable.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                paymentsTable.setCursor(paymentsTable.rowAtPoint(e.getPoint()) >= 0
                    ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    : Cursor.getDefaultCursor());
            }
        });

        JScrollPane scroll = new JScrollPane(paymentsTable);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_CARD);
        scroll.getViewport().setBackground(COLOR_CARD);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel hint = new JLabel("  Click any row to generate a receipt.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setBorder(new EmptyBorder(10, 16, 12, 16));

        card.add(scroll, BorderLayout.CENTER);
        card.add(hint, BorderLayout.SOUTH);
        return card;
    }

    // ─── Unpaid Appointments table ──────────────────────────────
    private JPanel buildUnpaidCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        String[] columns = {
            "Appointment ID", "Customer", "Vehicle",
            "Service Type", "Date", "Amount (RM)", "Action"
        };

        unpaidTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 6; }
        };

        unpaidTable = TableHelper.buildTable(unpaidTableModel);

        // Centre-align columns 0-5
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };
        for (int i = 0; i <= 5; i++) {
            unpaidTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Action column: "Collect" button renderer + editor
        unpaidTable.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JButton btn = new JButton("Collect");
                btn.setFont(new Font("SansSerif", Font.BOLD, 11));
                btn.setForeground(Color.WHITE);
                btn.setBackground(new Color(40, 167, 69));
                btn.setBorderPainted(false);
                btn.setFocusPainted(false);
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                if (isSelected) wrapper.setBackground(new Color(80, 110, 230, 60));
                wrapper.add(btn);
                return wrapper;
            }
        );

        // Button editor for the Action column
        unpaidTable.getColumnModel().getColumn(6).setCellEditor(new CollectButtonEditor());

        int[] widths = { 120, 130, 140, 120, 110, 100, 100 };
        for (int i = 0; i < widths.length; i++) {
            unpaidTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane scroll = new JScrollPane(unpaidTable);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_CARD);
        scroll.getViewport().setBackground(COLOR_CARD);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─── Collect Button Editor ──────────────────────────────────
    private class CollectButtonEditor extends DefaultCellEditor {
        private JButton button;
        private int currentRow;

        CollectButtonEditor() {
            super(new JCheckBox());
            button = new JButton("Collect");
            button.setFont(new Font("SansSerif", Font.BOLD, 11));
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(40, 167, 69));
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.addActionListener(e -> {
                fireEditingStopped();
                if (currentRow >= 0 && currentRow < unpaidAppointments.size()) {
                    showCollectPaymentDialog(unpaidAppointments.get(currentRow));
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            currentRow = row;
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 3));
            wrapper.setOpaque(true);
            wrapper.setBackground(Color.WHITE);
            wrapper.add(button);
            return wrapper;
        }

        @Override public Object getCellEditorValue() { return "Collect"; }
    }

    // ─── Refresh all data ───────────────────────────────────────
    public void refresh() {
        loadPrices();
        allPayments = paymentService.getAllPayments();

        // Find completed appointments with no payment
        List<Appointment> allAppts = appointmentService.getAll();
        Set<String> paidApptIds = new HashSet<>();
        for (String[] p : allPayments) {
            paidApptIds.add(p[3].trim()); // appointmentID
        }

        unpaidAppointments = new ArrayList<>();
        for (Appointment a : allAppts) {
            if ("Completed".equals(a.getStatus()) && !paidApptIds.contains(a.getId())) {
                unpaidAppointments.add(a);
            }
        }

        updateStats();
        fillPaymentsTable();
        fillUnpaidTable();
    }

    private void updateStats() {
        // Total collected
        double totalCollected = 0;
        int paidCount = 0;
        String today = LocalDate.now().toString();
        double todayTotal = 0;
        int todayCount = 0;

        for (String[] p : allPayments) {
            double amount = 0;
            try { amount = Double.parseDouble(p[5].trim()); } catch (NumberFormatException ignored) {}
            if ("Paid".equalsIgnoreCase(p[8].trim())) {
                totalCollected += amount;
                paidCount++;
                if (p[6].trim().equals(today)) {
                    todayTotal += amount;
                    todayCount++;
                }
            }
        }

        totalCollectedValue.setText(String.format("RM %.2f", totalCollected));
        totalCollectedSub.setText("from " + paidCount + " payment" + (paidCount != 1 ? "s" : ""));

        unpaidCountValue.setText(String.valueOf(unpaidAppointments.size()));
        unpaidCountSub.setText("awaiting collection");

        todayCollectedValue.setText(String.format("RM %.2f", todayTotal));
        todayCollectedSub.setText(todayCount + " collected today");
    }

    private void fillPaymentsTable() {
        paymentsTableModel.setRowCount(0);
        for (String[] p : allPayments) {
            String customerName = resolveCustomerName(p[1].trim());
            String vehicleLabel = vehicleService.getVehiclePlate(p[4].trim());
            String amountDisplay;
            try {
                amountDisplay = String.format("%.2f", Double.parseDouble(p[5].trim()));
            } catch (NumberFormatException e) {
                amountDisplay = p[5].trim();
            }

            paymentsTableModel.addRow(new Object[]{
                p[0].trim(),       // Payment ID
                customerName,      // Customer
                p[3].trim(),       // Appointment ID
                vehicleLabel,      // Vehicle
                amountDisplay,     // Amount
                p[6].trim(),       // Date
                p[7].trim(),       // Method
                p[8].trim()        // Status
            });
        }
    }

    private void fillUnpaidTable() {
        unpaidTableModel.setRowCount(0);
        for (Appointment a : unpaidAppointments) {
            String customerName = resolveCustomerName(a.getCustomerId());
            String vehicleLabel = vehicleService.getVehiclePlate(a.getVehicleId());
            double amount = getServicePrice(a.getServiceType());

            unpaidTableModel.addRow(new Object[]{
                a.getId(),                              // Appointment ID
                customerName,                           // Customer
                vehicleLabel,                           // Vehicle
                a.getServiceType(),                     // Service Type
                a.getDateTime().split(" ")[0],           // Date
                String.format("%.2f", amount),          // Amount
                "Collect"                               // Action
            });
        }
    }

    // ─── Collect Payment Dialog ─────────────────────────────────
    private void showCollectPaymentDialog(Appointment appt) {
        String customerName = resolveCustomerName(appt.getCustomerId());
        String vehicleLabel = vehicleService.getVehiclePlate(appt.getVehicleId());
        double amount = getServicePrice(appt.getServiceType());

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 28, 20, 28));
        panel.setPreferredSize(new Dimension(400, 360));

        // Title
        JLabel title = new JLabel("Collect Payment");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(COLOR_TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(16));

        // Details
        panel.add(makeDetailRow("Appointment", appt.getId()));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeDetailRow("Customer", customerName));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeDetailRow("Vehicle", vehicleLabel));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeDetailRow("Service", appt.getServiceType()));
        panel.add(Box.createVerticalStrut(8));
        panel.add(makeDetailRow("Date", appt.getDateTime()));
        panel.add(Box.createVerticalStrut(12));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(12));

        // Amount
        JPanel amountRow = new JPanel(new BorderLayout());
        amountRow.setOpaque(false);
        amountRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        amountRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel amountLabel = new JLabel("Amount");
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        amountLabel.setForeground(COLOR_TEXT);
        JLabel amountValue = new JLabel(String.format("RM %.2f", amount));
        amountValue.setFont(new Font("SansSerif", Font.BOLD, 18));
        amountValue.setForeground(new Color(40, 167, 69));
        amountRow.add(amountLabel, BorderLayout.WEST);
        amountRow.add(amountValue, BorderLayout.EAST);
        panel.add(amountRow);
        panel.add(Box.createVerticalStrut(16));

        // Payment method
        JLabel methodLabel = new JLabel("Payment Method");
        methodLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        methodLabel.setForeground(COLOR_TEXT);
        methodLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(methodLabel);
        panel.add(Box.createVerticalStrut(6));

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Cash", "Card", "Online"});
        methodCombo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        methodCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        methodCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(methodCombo);

        int result = JOptionPane.showConfirmDialog(
            SwingUtilities.getWindowAncestor(this),
            panel,
            "Collect Payment — " + appt.getId(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (result == JOptionPane.OK_OPTION) {
            String method = (String) methodCombo.getSelectedItem();
            String amountStr = String.format("%.2f", amount);

            boolean success = paymentService.savePayment(
                appt.getCustomerId(),
                appt.getId(),
                appt.getVehicleId(),
                amountStr,
                method
            );

            if (success) {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Payment collected successfully!\nPayment ID: " + paymentService.generateNextPaymentId().replace("PY", "PY" + ""),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE
                );
                refresh();
                // Switch to All Payments tab to show the new payment
                styleTab(allPaymentsTabBtn, true);
                styleTab(unpaidTabBtn, false);
                tableCardLayout.show(tableCardPanel, "ALL");
            } else {
                JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Failed to save payment. Please try again.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    // ─── Generate Receipt (Invoice popup) ───────────────────────
    private void showReceipt(String[] payment) {
        String paymentId     = payment[0].trim();
        String customerId    = payment[1].trim();
        String shId          = payment[2].trim();
        String appointmentId = payment[3].trim();
        String vehicleId     = payment[4].trim();
        String amount        = payment[5].trim();
        String date          = payment[6].trim();
        String method        = payment[7].trim();
        String status        = payment[8].trim();

        String customerName = resolveCustomerName(customerId);
        String vehicleLabel = vehicleService.getVehiclePlate(vehicleId);

        String amountDisplay;
        try {
            amountDisplay = String.format("RM %.2f", Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            amountDisplay = "RM " + amount;
        }

        // Receipt panel
        JPanel receipt = new JPanel();
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(new EmptyBorder(28, 32, 28, 32));
        receipt.setPreferredSize(new Dimension(460, 520));

        // Header
        JLabel shopName = new JLabel("APU Automotive Service Centre");
        shopName.setFont(new Font("SansSerif", Font.BOLD, 16));
        shopName.setForeground(new Color(80, 110, 230));
        shopName.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(shopName);
        receipt.add(Box.createVerticalStrut(4));

        JLabel receiptTitle = new JLabel("Official Payment Receipt");
        receiptTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        receiptTitle.setForeground(COLOR_MUTED);
        receiptTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(receiptTitle);
        receipt.add(Box.createVerticalStrut(18));

        // Divider
        receipt.add(makeDivider());
        receipt.add(Box.createVerticalStrut(16));

        // Details
        receipt.add(makeReceiptRow("Receipt No.", paymentId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Customer", customerName + " (" + customerId + ")"));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Service History ID", shId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Appointment ID", appointmentId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Vehicle", vehicleLabel));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Payment Date", date));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeReceiptRow("Payment Method", method));
        receipt.add(Box.createVerticalStrut(10));

        // Status with badge
        JPanel statusRow = new JPanel(new BorderLayout());
        statusRow.setOpaque(false);
        statusRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        statusRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel statusLabel = new JLabel("Status");
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setPreferredSize(new Dimension(160, 22));
        JLabel statusBadge = new JLabel(status);
        statusBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
        statusBadge.setOpaque(true);
        statusBadge.setBorder(new EmptyBorder(3, 10, 3, 10));
        statusBadge.setBackground(new Color(220, 248, 232));
        statusBadge.setForeground(new Color(34, 139, 80));
        statusRow.add(statusLabel, BorderLayout.WEST);
        statusRow.add(statusBadge, BorderLayout.CENTER);
        receipt.add(statusRow);
        receipt.add(Box.createVerticalStrut(16));

        // Divider
        receipt.add(makeDivider());
        receipt.add(Box.createVerticalStrut(16));

        // Total amount
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalLabel = new JLabel("Total Amount");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(COLOR_TEXT);
        JLabel totalValue = new JLabel(amountDisplay);
        totalValue.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalValue.setForeground(new Color(34, 139, 80));
        totalRow.add(totalLabel, BorderLayout.WEST);
        totalRow.add(totalValue, BorderLayout.EAST);
        receipt.add(totalRow);
        receipt.add(Box.createVerticalStrut(20));

        // Footer
        JLabel footer = new JLabel("Thank you for your patronage!");
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(COLOR_MUTED);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(footer);

        receipt.add(Box.createVerticalStrut(18));

        // Export PDF button
        JButton exportPdfBtn = new JButton("Export PDF");
        exportPdfBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        exportPdfBtn.setForeground(Color.WHITE);
        exportPdfBtn.setBackground(new Color(80, 110, 230));
        exportPdfBtn.setBorderPainted(false);
        exportPdfBtn.setFocusPainted(false);
        exportPdfBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        exportPdfBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        exportPdfBtn.setMaximumSize(new Dimension(140, 36));

        exportPdfBtn.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save Receipt as PDF");
            fileChooser.setSelectedFile(new java.io.File("Receipt_" + paymentId + ".pdf"));

            if (fileChooser.showSaveDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf")) {
                    path += ".pdf";
                }
                try {
                    exportReceiptToPdf(receipt, path);
                    JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Receipt exported successfully!\n" + path,
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Error exporting PDF: " + ex.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        });

        receipt.add(exportPdfBtn);

        JOptionPane.showMessageDialog(
            SwingUtilities.getWindowAncestor(this),
            receipt,
            "Receipt — " + paymentId,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    // ─── Helpers ────────────────────────────────────────────────

    private void exportReceiptToPdf(JPanel panelToExport, String path) throws Exception {
        int w = panelToExport.getWidth();
        int h = panelToExport.getHeight();
        if (w == 0 || h == 0) {
            w = panelToExport.getPreferredSize().width;
            h = panelToExport.getPreferredSize().height;
            panelToExport.setSize(w, h);
            panelToExport.doLayout();
        }

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        panelToExport.paint(g2);
        g2.dispose();

        Document doc = new Document(new com.itextpdf.text.Rectangle(w, h), 0, 0, 0, 0);
        PdfWriter.getInstance(doc, new FileOutputStream(path));
        doc.open();

        Image pdfImg = Image.getInstance(image, null);
        doc.add(pdfImg);
        doc.close();
    }

    private double getServicePrice(String serviceType) {
        if ("Major Service".equalsIgnoreCase(serviceType)) return majorPrice;
        return normalPrice;
    }

    private String resolveCustomerName(String customerId) {
        for (User u : accountService.getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(customerId)) {
                return u.getName();
            }
        }
        return customerId;
    }

    private JPanel makeDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(120, 20));

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(COLOR_TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JPanel makeReceiptRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(160, 22));

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(COLOR_TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JPanel makeRoundedCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JLabel makeBigValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setForeground(COLOR_TEXT);
        return label;
    }

    private JLabel makeMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(COLOR_MUTED);
        return label;
    }
}
