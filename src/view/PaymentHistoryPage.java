package view;

import model.AccountService;
import model.PaymentService;
import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PaymentHistoryPage extends JPanel {

    // ── Data-layer services ───────────────────────────────────────
    private final PaymentService  paymentService  = new PaymentService();
    private final VehicleService  vehicleService  = new VehicleService();
    private final AccountService  accountService  = new AccountService();

    // ── Stat card value labels — updated by refresh() ─────────────
    private JLabel totalPaidValueLabel;
    private JLabel totalPaidSubLabel;
    private JLabel paymentCountValueLabel;
    private JLabel paymentCountSubLabel;
    private JLabel prefMethodValueLabel;
    private JLabel prefMethodSubLabel;

    // ── Table model, sorter, search and sort controls ─────────────
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField        searchField;
    private JComboBox<String> sortCombo;

    // ── Raw payment rows kept in memory for the invoice popup ─────
    private List<String[]> currentRows = new ArrayList<>();

    // ── Design colours ────────────────────────────────────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    // Left accent bar colours for each stat card
    private static final Color ACCENT_GREEN  = new Color(34,  165,  90);
    private static final Color ACCENT_BLUE   = new Color(80,  110, 230);
    private static final Color ACCENT_PURPLE = new Color(139,  92, 246);

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public PaymentHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildPageHeader(), BorderLayout.NORTH);

        // The data panel is always shown — no switching between panels
        pageContent.add(buildDataPanel(), BorderLayout.CENTER);

        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);
    }

    // ─────────────────────────────────────────────────────────────
    // buildPageHeader() — subtitle label
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
    // buildDataPanel() — stat cards on top + table below
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
    // buildStatsRow() — three summary stat cards side by side
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        // Card 1: Total amount paid — green bar
        totalPaidValueLabel = makeBigValueLabel("RM 0.00");
        totalPaidSubLabel   = makeMutedLabel("across 0 payments");
        row.add(buildStatCard("Total paid", totalPaidValueLabel,
                totalPaidSubLabel, ACCENT_GREEN));

        // Card 2: Number of payment records — blue bar
        paymentCountValueLabel = makeBigValueLabel("0");
        paymentCountSubLabel   = makeMutedLabel("no records yet");
        row.add(buildStatCard("Payments made", paymentCountValueLabel,
                paymentCountSubLabel, ACCENT_BLUE));

        // Card 3: Most-used payment method — purple bar
        prefMethodValueLabel = makeBigValueLabel("—");
        prefMethodSubLabel   = makeMutedLabel("used most frequently");
        row.add(buildStatCard("Preferred method", prefMethodValueLabel,
                prefMethodSubLabel, ACCENT_PURPLE));

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatCard() — one white rounded card with coloured left bar
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatCard(String topText, JLabel valueLabel,
                                  JLabel subLabel, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // White rounded background
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Grey border
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // Coloured left bar clipped to rounded corners
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 4, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 16, 18, 20));

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
    // buildTableCard() — white card with search/sort bar + table
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

        // Search + sort bar above the table
        card.add(buildSearchSortBar(), BorderLayout.NORTH);

        // Table column headers
        String[] columns = {
            "Payment ID", "SH ID", "Appointment ID",
            "Vehicle Type", "Car Plate",
            "Amount (RM)", "Date", "Method", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        JTable table = TableHelper.buildTable(tableModel);

        // Row sorter enables search filtering and column sorting
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        // Centre-align columns 0 to 7
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
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
        for (int i = 0; i <= 7; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Status column (index 8) — coloured badge
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

        // Column widths
        int[] widths = { 90, 70, 120, 110, 100, 100, 100, 90, 90 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // Row click → show receipt popup
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int viewRow = table.getSelectedRow();
                if (viewRow < 0) return;
                // Convert visible row index to model row index
                // (needed because sorting may have reordered rows)
                int modelRow = table.convertRowIndexToModel(viewRow);
                if (modelRow >= 0 && modelRow < currentRows.size()) {
                    ReceiptUtil.showReceipt(
                        SwingUtilities.getWindowAncestor(PaymentHistoryPage.this),
                        currentRows.get(modelRow),
                        vehicleService,
                        accountService
                    );
                }
            }
        });

        // Show hand cursor when hovering over a row
        table.addMouseMotionListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                table.setCursor(row >= 0
                        ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                        : Cursor.getDefaultCursor());
            }
        });

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.setPreferredSize(new Dimension(0, 420));
        tableScroll.setMinimumSize(new Dimension(0, 200));

        // Small hint at the bottom of the card
        JLabel hint = new JLabel("  \uD83D\uDCA1  Click any row to view the full invoice.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setBorder(new EmptyBorder(10, 16, 12, 16));

        card.add(tableScroll, BorderLayout.CENTER);
        card.add(hint,        BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildSearchSortBar() — search field + sort dropdown above table
    // ─────────────────────────────────────────────────────────────
    private JPanel buildSearchSortBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);

        // Search field
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBackground(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchField.setPreferredSize(new Dimension(180, 30));

        // Grey placeholder
        searchField.setForeground(COLOR_MUTED);
        searchField.setText("Search...");
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search...")) {
                    searchField.setText("");
                    searchField.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setForeground(COLOR_MUTED);
                    searchField.setText("Search...");
                }
            }
        });

        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            }
        );

        // Sort dropdown
        String[] sortOptions = {
            "Sort by...",
            "Date (Newest)",
            "Date (Oldest)",
            "Amount (High\u2192Low)",
            "Amount (Low\u2192High)",
            "Method",
            "Payment ID"
        };
        sortCombo = new JComboBox<>(sortOptions);
        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sortCombo.setBackground(Color.WHITE);
        sortCombo.setForeground(COLOR_TEXT);
        sortCombo.setPreferredSize(new Dimension(170, 30));
        sortCombo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));
        sortCombo.addActionListener(e -> applySort());

        bar.add(searchField);
        bar.add(sortCombo);

        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(10, 16, 10, 16)
        ));

        return bar;
    }

    // ─────────────────────────────────────────────────────────────
    // applyFilter() — delegates keyword matching to PaymentService.
    // ─────────────────────────────────────────────────────────────
    private void applyFilter() {
        if (rowSorter == null || searchField == null) return;

        String text = searchField.getText().trim();

        // Clear filter when the field is empty or shows the placeholder
        if (paymentService.filterByKeyword(new ArrayList<>(), text).equals(new ArrayList<>())) {
            // Blank / placeholder — remove filter
        }

        if (text.equals("Search...") || text.isEmpty()) {
            rowSorter.setRowFilter(null);
            return;
        }

        // Delegate the per-row match check to PaymentService.matchesKeyword()
        rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                // Build a temporary String[] from the visible table row so we
                // can pass it to the service without coupling the service to Swing
                int colCount = entry.getValueCount();
                String[] rowData = new String[colCount];
                for (int i = 0; i < colCount; i++) {
                    Object val = entry.getValue(i);
                    rowData[i] = val != null ? val.toString() : "";
                }
                return paymentService.matchesKeyword(rowData, text);
            }
        });
    }

    // ─────────────────────────────────────────────────────────────
    // applySort() — delegates comparator construction to PaymentService.
    // ─────────────────────────────────────────────────────────────
    private void applySort() {
        if (rowSorter == null || sortCombo == null) return;

        int selectedIndex = sortCombo.getSelectedIndex();

        // Ask PaymentService for the appropriate Comparator
        Comparator<String[]> comparator = paymentService.getSortComparator(selectedIndex);

        if (comparator == null) {
            // Index 0 "Sort by..." — clear all sort keys
            rowSorter.setSortKeys(null);
            return;
        }

        // Determine which column index the service is sorting on so we can
        int sortCol;
        switch (selectedIndex) {
        case 1:  sortCol = 6; break;
        case 2:  sortCol = 6; break;
        case 3:  sortCol = 5; break;
        case 4:  sortCol = 5; break;
        case 5:  sortCol = 7; break;
        case 6:  sortCol = 0; break;
        default: rowSorter.setSortKeys(null); return;
        }

        // Wrap the service's Comparator<String[]> into a column-level
        // Comparator<Object> that TableRowSorter expects
        rowSorter.setSortKeys(null);
        
        rowSorter.setComparator(sortCol, (Comparator<Object>) (a, b) -> {
            // Reconstruct minimal String[] stubs containing only the
            // relevant column so the service comparator can do its work
            String[] rowA = new String[9];
            String[] rowB = new String[9];
            rowA[sortCol] = a != null ? a.toString() : "";
            rowB[sortCol] = b != null ? b.toString() : "";
            return comparator.compare(rowA, rowB);
        });

        List<RowSorter.SortKey> keys = new ArrayList<>();
        keys.add(new RowSorter.SortKey(sortCol, SortOrder.ASCENDING));
        rowSorter.setSortKeys(keys);
    }

    // ═══════════════════════════════════════════════════════════════
    // refresh()
    // ═══════════════════════════════════════════════════════════════
    public void refresh() {
        model.User loggedInUser = getLoggedInUser();

        if (loggedInUser == null) {
            resetStatsToZero();
            tableModel.setRowCount(0);
            currentRows = new ArrayList<>();
            return;
        }

        // Delegate data loading and filtering to PaymentService
        String customerId = loggedInUser.getUserId();
        currentRows = paymentService.getPaymentsForCustomer(customerId);

        if (currentRows.isEmpty()) {
            // No payments yet — show zero values, empty table
            resetStatsToZero();
            tableModel.setRowCount(0);
        } else {
            updateStatsCards(currentRows);
            fillTable(currentRows);
        }

        // Reset search and sort controls
        if (searchField != null) {
            searchField.setForeground(COLOR_MUTED);
            searchField.setText("Search...");
        }
        if (sortCombo  != null) sortCombo.setSelectedIndex(0);
        if (rowSorter  != null) {
            rowSorter.setRowFilter(null);
            rowSorter.setSortKeys(null);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // resetStatsToZero() — default "no data" values for stat cards
    // ─────────────────────────────────────────────────────────────
    private void resetStatsToZero() {
        totalPaidValueLabel.setText("RM 0.00");
        totalPaidSubLabel.setText("across 0 payments");
        paymentCountValueLabel.setText("0");
        paymentCountSubLabel.setText("no records yet");
        prefMethodValueLabel.setText("—");
        prefMethodSubLabel.setText("used most frequently");
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatsCards() — fills stat card labels with real data.
    // ─────────────────────────────────────────────────────────────
    private void updateStatsCards(List<String[]> rows) {
        // Card 1: total paid — via PaymentService
        double totalPaid = paymentService.calcTotalPaidAmount(rows);
        int    paidCount = paymentService.countPaidRows(rows);
        totalPaidValueLabel.setText(String.format("RM %.2f", totalPaid));
        totalPaidSubLabel.setText(
                "across " + paidCount + " payment" + (paidCount != 1 ? "s" : ""));

        // Card 2: record count + pending count — via PaymentService
        int total   = rows.size();
        int pending = paymentService.countPendingRows(rows);
        paymentCountValueLabel.setText(String.valueOf(total));
        paymentCountSubLabel.setText(pending > 0 ? pending + " pending" : "all completed");

        // Card 3: most-used payment method — via PaymentService
        String favMethod = paymentService.getPreferredMethod(rows);
        prefMethodValueLabel.setText(favMethod);
        prefMethodSubLabel.setText("used most frequently");
    }

    // ─────────────────────────────────────────────────────────────
    // fillTable() — adds one row per payment record to the table.
    // Vehicle type and plate are resolved via VehicleService.
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<String[]> rows) {
        tableModel.setRowCount(0);

        for (String[] row : rows) {
            String paymentId     = row[0].trim();
            String shId          = row[2].trim();
            String appointmentId = row[3].trim();
            String vehicleId     = row[4].trim();
            String amount        = row[5].trim();
            String date          = row[6].trim();
            String method        = row[7].trim();
            String status        = row[8].trim();

            String vehicleType = resolveVehicleType(vehicleId);
            String carPlate    = resolveCarPlate(vehicleId);

            String amountDisplay;
            try {
                amountDisplay = String.format("%.2f", Double.parseDouble(amount));
            } catch (NumberFormatException e) {
                amountDisplay = amount;
            }

            tableModel.addRow(new Object[]{
                paymentId, shId, appointmentId,
                vehicleType, carPlate,
                amountDisplay, date, method, status
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Vehicle helpers — resolve vehicleId to type and plate label via VehicleService
    // ─────────────────────────────────────────────────────────────
    private String resolveVehicleType(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        if (label != null && label.contains(" · ")) {
            return label.split(" · ", 2)[0].trim();
        }
        return vehicleId;
    }

    private String resolveCarPlate(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        if (label != null && label.contains(" · ")) {
            return label.split(" · ", 2)[1].trim();
        }
        return vehicleId;
    }

    // ─────────────────────────────────────────────────────────────
    // getLoggedInUser() — walks up the Swing parent chain to find AppFrame
    // ─────────────────────────────────────────────────────────────
    private model.User getLoggedInUser() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof AppFrame) {
                return ((AppFrame) parent).getLoggedInUserObj();
            }
            parent = parent.getParent();
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // Label factory helpers
    // ─────────────────────────────────────────────────────────────
    private JLabel makeBigValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 24));
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