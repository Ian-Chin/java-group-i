package view;

import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PaymentHistoryPage
 * ──────────────────
 * Shows the logged-in customer's payment history.
 *
 * This page reads payments.txt DIRECTLY — no PaymentService needed.
 * This fixes the "model.PaymentService cannot be resolved" error.
 *
 * payments.txt format (9 columns, comma-separated):
 *   [0] paymentID      [1] customerID      [2] serviceHistoryID
 *   [3] appointmentID  [4] vehicleID       [5] amount
 *   [6] paymentDate    [7] method          [8] status
 *
 * Example line:
 *   PY1,C1,SH1,AP1,V1,120.00,2026-03-05,Cash,Paid
 *
 * Table columns shown:
 *   Payment ID | SH ID | Appointment ID | Vehicle Type |
 *   Car Plate  | Amount (RM) | Date | Method | Status
 *
 * Features:
 *   - 3 stat cards: Total Paid, Payments Made, Preferred Method
 *   - ALL table columns are centre-aligned
 *   - Vertical scrollbar appears when rows overflow
 *   - Clickable rows → invoice popup
 *   - Empty-state card when no records found
 */
public class PaymentHistoryPage extends JPanel {

    // ── Path to the payments data file ────────────────────────────
    private static final String PAYMENTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "payments.txt";

    // ── Number of columns expected per line in payments.txt ───────
    private static final int EXPECTED_COLUMNS = 9;

    // ── VehicleService: resolves vehicle type and car plate ───────
    private final VehicleService vehicleService = new VehicleService();

    // ── CardLayout switches between "TABLE" and "EMPTY" views ─────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Stat card value labels — updated in refresh() ─────────────
    private JLabel totalPaidValueLabel;
    private JLabel totalPaidSubLabel;
    private JLabel paymentCountValueLabel;
    private JLabel paymentCountSubLabel;
    private JLabel prefMethodValueLabel;
    private JLabel prefMethodSubLabel;

    // ── Table model — rows are added in refresh() ─────────────────
    private DefaultTableModel tableModel;

    // ── Raw payment rows for the current user ─────────────────────
    // Stored so the row-click listener can look up the full data.
    private List<String[]> currentRows = new ArrayList<>();

    // ── Design colours (match ServiceHistoryPage exactly) ─────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    // ══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // Builds the page skeleton once. refresh() fills the data.
    // ══════════════════════════════════════════════════════════════
    public PaymentHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // Main content panel with padding around all edges
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Page subtitle at the top
        pageContent.add(buildPageHeader(), BorderLayout.NORTH);

        // Switch between data view and empty-state view
        switchPanel.setOpaque(false);
        switchPanel.add(buildDataPanel(),  "TABLE");
        switchPanel.add(buildEmptyPanel(), "EMPTY");
        pageContent.add(switchPanel, BorderLayout.CENTER);

        // Outer scroll pane so the whole page can scroll on small windows
        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    // buildPageHeader()
    // Small muted subtitle shown below the page title.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel subtitle = new JLabel(
            "Your payment transactions are listed below. Click any row to view the invoice.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(subtitle);
        return header;
    }

    // ─────────────────────────────────────────────────────────────
    // buildDataPanel()
    // The "TABLE" card: 3 stat cards on top + table card below.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel statsRow = buildStatsRow();
        statsRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        panel.add(statsRow,         BorderLayout.NORTH);
        panel.add(buildTableCard(), BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatsRow()
    // Three summary cards placed side by side.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        // Card 1: Total amount paid (RM)
        totalPaidValueLabel = makeBigValueLabel("—");
        totalPaidSubLabel   = makeMutedLabel("across 0 payments");
        row.add(buildStatCard("Total paid", totalPaidValueLabel, totalPaidSubLabel));

        // Card 2: Number of payment records
        paymentCountValueLabel = makeBigValueLabel("—");
        paymentCountSubLabel   = makeMutedLabel("—");
        row.add(buildStatCard("Payments made", paymentCountValueLabel, paymentCountSubLabel));

        // Card 3: Most-used payment method
        prefMethodValueLabel = makeBigValueLabel("—");
        prefMethodSubLabel   = makeMutedLabel("used most frequently");
        row.add(buildStatCard("Preferred method", prefMethodValueLabel, prefMethodSubLabel));

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatCard()
    // One rounded white card: top label + big value + small sub-label.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatCard(String topText, JLabel valueLabel, JLabel subLabel) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel topLabel = new JLabel(topText);
        topLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        topLabel.setForeground(COLOR_MUTED);
        topLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(topLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subLabel);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTableCard()
    // Rounded white card holding the payments table.
    // Vertical scrollbar appears when rows overflow the fixed height.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());

        // Column headers
        String[] columns = {
            "Payment ID", "SH ID", "Appointment ID",
            "Vehicle Type", "Car Plate",
            "Amount (RM)", "Date", "Method", "Status"
        };

        // Non-editable table model
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // user cannot type inside cells
            }
        };

        // Base table style from the shared helper
        JTable table = TableHelper.buildTable(tableModel);

        // ── Centre-align columns 0 to 7 ───────────────────────────
        // This renderer centres text AND applies alternating row colours.
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER); // ← centre the text
                setBorder(new EmptyBorder(0, 10, 0, 10));      // left/right padding
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    // Alternate white and very-light-grey rows
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };

        // Apply centre renderer to every column except the last (Status)
        for (int i = 0; i <= 7; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // ── Column 8 — Status: coloured badge, centred ────────────
        table.getColumnModel().getColumn(8).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel badge = new JLabel(value != null ? value.toString() : "");
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));

                String status = value != null ? value.toString() : "";
                switch (status.toLowerCase()) {
                    case "paid":
                        badge.setBackground(new Color(220, 248, 232));
                        badge.setForeground(new Color(34, 139, 80));
                        break;
                    case "pending":
                        badge.setBackground(new Color(255, 243, 220));
                        badge.setForeground(new Color(180, 110, 20));
                        break;
                    default:
                        badge.setBackground(new Color(235, 236, 240));
                        badge.setForeground(COLOR_MUTED);
                }

                // Wrapper centres the badge vertically inside the cell
                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                if (isSelected) {
                    wrapper.setBackground(new Color(80, 110, 230, 60));
                }
                wrapper.add(badge);
                return wrapper;
            }
        );

        // ── Preferred column widths ────────────────────────────────
        int[] widths = { 90, 70, 120, 110, 100, 100, 100, 90, 90 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // ── Row click → open invoice popup ────────────────────────
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0 && selectedRow < currentRows.size()) {
                    showInvoicePopup(currentRows.get(selectedRow));
                }
            }
        });

        // Change cursor to hand pointer when hovering over rows
        table.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    table.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else {
                    table.setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        // ── Scroll pane — vertical scrollbar appears when rows overflow ──
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.setPreferredSize(new Dimension(0, 420)); // fixed height; scroll handles overflow
        tableScroll.setMinimumSize(new Dimension(0, 200));

        // Hint text at the bottom of the card
        JLabel hint = new JLabel("  \uD83D\uDCA1  Click any row to view the full invoice.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setBorder(new EmptyBorder(10, 16, 12, 16));

        card.add(tableScroll, BorderLayout.CENTER);
        card.add(hint,        BorderLayout.SOUTH);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildEmptyPanel()
    // Shown when no payment records are found for this customer.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildEmptyPanel() {
        return ServiceHistoryPage.buildNoDataPanel(
            "\uD83D\uDCB5",                       // 💵 money icon
            "No payment records found.",
            "Your completed payment transactions will appear here."
        );
    }

    // ══════════════════════════════════════════════════════════════
    // refresh()
    // Called every time the user clicks "Payment History" in the
    // sidebar. Reads payments.txt and updates the whole UI.
    // ══════════════════════════════════════════════════════════════
    public void refresh() {
        // Step 1: Find who is logged in
        model.User loggedInUser = getLoggedInUser();
        if (loggedInUser == null) {
            cardLayout.show(switchPanel, "EMPTY");
            return;
        }

        String customerId = loggedInUser.getUserId(); // e.g. "C1"

        // Step 2: Read ALL rows from payments.txt
        List<String[]> allPayments = readPaymentsFromFile();

        // Step 3: Keep only rows where column [1] matches this customer's ID
        currentRows = new ArrayList<>();
        for (String[] row : allPayments) {
            if (row[1].trim().equalsIgnoreCase(customerId)) {
                currentRows.add(row);
            }
        }

        // Step 4: Show empty state if no matching records
        if (currentRows.isEmpty()) {
            cardLayout.show(switchPanel, "EMPTY");
            return;
        }

        // Step 5: Populate stat cards and table, then show the TABLE card
        updateStatsCards(currentRows);
        fillTable(currentRows);
        cardLayout.show(switchPanel, "TABLE");
    }

    // ─────────────────────────────────────────────────────────────
    // readPaymentsFromFile()
    // Opens payments.txt and returns every valid data row.
    // Lines with the wrong number of columns are skipped.
    // ─────────────────────────────────────────────────────────────
    private List<String[]> readPaymentsFromFile() {
        List<String[]> list = new ArrayList<>();
        File file = new File(PAYMENTS_FILE);

        if (!file.exists()) return list; // file not created yet

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip blank lines and comment lines
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                // Split by comma; limit to EXPECTED_COLUMNS so the last value
                // is not accidentally split if it contains a comma
                String[] columns = line.split(",", EXPECTED_COLUMNS);

                if (columns.length == EXPECTED_COLUMNS) {
                    list.add(columns);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatsCards()
    // Calculates totals from the customer's rows and writes them
    // into the three stat card labels.
    // ─────────────────────────────────────────────────────────────
    private void updateStatsCards(List<String[]> rows) {
        // ── Card 1: Total amount (sum of "Paid" rows only) ────────
        double totalPaid = 0.0;
        int    paidCount = 0;
        for (String[] row : rows) {
            if (row[8].trim().equalsIgnoreCase("Paid")) {
                try {
                    totalPaid += Double.parseDouble(row[5].trim()); // column [5] = amount
                    paidCount++;
                } catch (NumberFormatException ignored) { }
            }
        }
        totalPaidValueLabel.setText(String.format("RM %.2f", totalPaid));
        totalPaidSubLabel.setText(
            "across " + paidCount + " payment" + (paidCount != 1 ? "s" : ""));

        // ── Card 2: Total records + how many are pending ──────────
        int total   = rows.size();
        int pending = 0;
        for (String[] row : rows) {
            if (row[8].trim().equalsIgnoreCase("Pending")) pending++;
        }
        paymentCountValueLabel.setText(String.valueOf(total));
        paymentCountSubLabel.setText(
            pending > 0 ? pending + " pending" : "all completed");

        // ── Card 3: Most-used payment method ──────────────────────
        Map<String, Integer> methodCount = new HashMap<>();
        for (String[] row : rows) {
            String method = row[7].trim(); // column [7] = method
            methodCount.put(method, methodCount.getOrDefault(method, 0) + 1);
        }
        String favMethod = "—";
        int    favCount  = 0;
        for (Map.Entry<String, Integer> entry : methodCount.entrySet()) {
            if (entry.getValue() > favCount) {
                favMethod = entry.getKey();
                favCount  = entry.getValue();
            }
        }
        prefMethodValueLabel.setText(favMethod);
        prefMethodSubLabel.setText("used most frequently");
    }

    // ─────────────────────────────────────────────────────────────
    // fillTable()
    // Clears the table and adds one row per payment record.
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<String[]> rows) {
        tableModel.setRowCount(0); // clear existing rows first

        for (String[] row : rows) {
            String paymentId     = row[0].trim(); // "PY1"
            String shId          = row[2].trim(); // "SH1"
            String appointmentId = row[3].trim(); // "AP1"
            String vehicleId     = row[4].trim(); // "V1"
            String amount        = row[5].trim(); // "120.00"
            String date          = row[6].trim(); // "2026-03-05"
            String method        = row[7].trim(); // "Cash"
            String status        = row[8].trim(); // "Paid"

            // Resolve vehicle type and car plate from VehicleService
            String vehicleType = resolveVehicleType(vehicleId);
            String carPlate    = resolveCarPlate(vehicleId);

            // Format amount to 2 decimal places
            String amountDisplay;
            try {
                amountDisplay = String.format("%.2f", Double.parseDouble(amount));
            } catch (NumberFormatException e) {
                amountDisplay = amount;
            }

            // Add the row to the table model
            tableModel.addRow(new Object[]{
                paymentId, shId, appointmentId,
                vehicleType, carPlate,
                amountDisplay, date, method, status
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // showInvoicePopup()
    // Opens an invoice dialog when a row is clicked.
    // rawRow = the original String[9] from payments.txt.
    // ─────────────────────────────────────────────────────────────
    private void showInvoicePopup(String[] rawRow) {
        String paymentId     = rawRow[0].trim();
        String shId          = rawRow[2].trim();
        String appointmentId = rawRow[3].trim();
        String vehicleId     = rawRow[4].trim();
        String amount        = rawRow[5].trim();
        String date          = rawRow[6].trim();
        String method        = rawRow[7].trim();
        String status        = rawRow[8].trim();

        String vehicleType = resolveVehicleType(vehicleId);
        String carPlate    = resolveCarPlate(vehicleId);

        String amountDisplay;
        try {
            amountDisplay = String.format("RM %.2f", Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            amountDisplay = "RM " + amount;
        }

        // ── Invoice content panel ──────────────────────────────────
        JPanel invoice = new JPanel();
        invoice.setLayout(new BoxLayout(invoice, BoxLayout.Y_AXIS));
        invoice.setBackground(Color.WHITE);
        invoice.setBorder(new EmptyBorder(28, 32, 28, 32));
        invoice.setPreferredSize(new Dimension(440, 490));

        // Shop name header
        JLabel shopName = new JLabel("APU Automotive Service Centre");
        shopName.setFont(new Font("SansSerif", Font.BOLD, 15));
        shopName.setForeground(new Color(80, 110, 230));
        shopName.setAlignmentX(Component.LEFT_ALIGNMENT);
        invoice.add(shopName);
        invoice.add(Box.createVerticalStrut(4));

        JLabel invoiceTitle = new JLabel("Official Payment Invoice");
        invoiceTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        invoiceTitle.setForeground(COLOR_MUTED);
        invoiceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        invoice.add(invoiceTitle);
        invoice.add(Box.createVerticalStrut(18));

        invoice.add(makeInvoiceDivider());
        invoice.add(Box.createVerticalStrut(16));

        // All detail rows
        invoice.add(makeInvoiceRow("Payment ID",         paymentId));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Service History ID", shId));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Appointment ID",     appointmentId));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Vehicle Type",       vehicleType));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Car Plate",          carPlate));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Payment Date",       date));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRow("Method",             method));
        invoice.add(Box.createVerticalStrut(10));
        invoice.add(makeInvoiceRowWithBadge("Status",    status));
        invoice.add(Box.createVerticalStrut(16));

        invoice.add(makeInvoiceDivider());
        invoice.add(Box.createVerticalStrut(16));

        // Total amount row — bold + green
        JPanel amountRow = new JPanel(new BorderLayout());
        amountRow.setOpaque(false);
        amountRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        amountRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountLabel = new JLabel("Total Amount");
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        amountLabel.setForeground(COLOR_TEXT);

        JLabel amountValue = new JLabel(amountDisplay);
        amountValue.setFont(new Font("SansSerif", Font.BOLD, 15));
        amountValue.setForeground(new Color(34, 139, 80)); // green

        amountRow.add(amountLabel, BorderLayout.WEST);
        amountRow.add(amountValue, BorderLayout.EAST);
        invoice.add(amountRow);

        // Show popup dialog
        JOptionPane.showMessageDialog(
            SwingUtilities.getWindowAncestor(this),
            invoice,
            "Invoice — " + paymentId,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    // ─────────────────────────────────────────────────────────────
    // makeInvoiceRow()
    // A "Label : Value" row inside the invoice.
    // ─────────────────────────────────────────────────────────────
    private JPanel makeInvoiceRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(160, 22)); // fixed width keeps values aligned

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(COLOR_TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // makeInvoiceRowWithBadge()
    // Like makeInvoiceRow() but shows a coloured badge for Status.
    // ─────────────────────────────────────────────────────────────
    private JPanel makeInvoiceRowWithBadge(String label, String status) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(160, 22));

        JLabel badge = new JLabel(status);
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));

        switch (status.toLowerCase()) {
            case "paid":
                badge.setBackground(new Color(220, 248, 232));
                badge.setForeground(new Color(34, 139, 80));
                break;
            case "pending":
                badge.setBackground(new Color(255, 243, 220));
                badge.setForeground(new Color(180, 110, 20));
                break;
            default:
                badge.setBackground(new Color(235, 236, 240));
                badge.setForeground(COLOR_MUTED);
        }

        row.add(lbl,   BorderLayout.WEST);
        row.add(badge, BorderLayout.CENTER);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // makeInvoiceDivider()
    // Thin horizontal divider line inside the invoice popup.
    // ─────────────────────────────────────────────────────────────
    private JSeparator makeInvoiceDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ─────────────────────────────────────────────────────────────
    // resolveVehicleType() / resolveCarPlate()
    // VehicleService.getVehiclePlate() returns "Type · Plate"
    // e.g. "Sedan · ABC1234". These helpers split that string.
    // ─────────────────────────────────────────────────────────────
    private String resolveVehicleType(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        if (label != null && label.contains(" · ")) {
            return label.split(" · ", 2)[0].trim();
        }
        return vehicleId; // fallback: show raw ID if not found
    }

    private String resolveCarPlate(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        if (label != null && label.contains(" · ")) {
            return label.split(" · ", 2)[1].trim();
        }
        return vehicleId; // fallback
    }

    // ─────────────────────────────────────────────────────────────
    // getLoggedInUser()
    // Walks up the Swing component tree to find the AppFrame,
    // then calls getLoggedInUserObj() — same approach as
    // ServiceHistoryPage so both pages work consistently.
    //
    // How it works:
    //   This panel → CustomerDashboard → AppFrame
    //   AppFrame stores the currently logged-in user.
    // ─────────────────────────────────────────────────────────────
    private model.User getLoggedInUser() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof AppFrame) {
                return ((AppFrame) parent).getLoggedInUserObj();
            }
            parent = parent.getParent();
        }
        return null; // not found
    }

    // ─────────────────────────────────────────────────────────────
    // Label factory helpers
    // ─────────────────────────────────────────────────────────────

    // Large bold number shown as the main value in a stat card
    private JLabel makeBigValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
        label.setForeground(COLOR_TEXT);
        return label;
    }

    // Small grey supporting text shown below the main value
    private JLabel makeMutedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(COLOR_MUTED);
        return label;
    }
}