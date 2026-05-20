package view;

import model.ServiceHistoryService;
import model.ServiceHistoryService.SummaryStats;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class ServiceHistoryPage extends JPanel {

    // ── Data-layer service ────────────────────────────────────────
    private final ServiceHistoryService serviceHistoryService = new ServiceHistoryService();

    // ── Stat card value labels — updated by refresh() ─────────────
    private JLabel totalServicesValueLabel;
    private JLabel latestServiceValueLabel;
    private JLabel latestServiceSubLabel;
    private JLabel favTechValueLabel;
    private JLabel favTechSubLabel;

    // ── Table model and row sorter ────────────────────────────────
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter;

    // ── Search field and sort dropdown ────────────────────────────
    // Both stored as fields so refresh() can reset them to defaults.
    private JTextField        searchField;
    private JComboBox<String> sortCombo;

    // ── Design colours ────────────────────────────────────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    // Left accent bar colours for the three stat cards
    private static final Color ACCENT_BLUE   = new Color(80,  110, 230);
    private static final Color ACCENT_GREEN  = new Color(34,  165,  90);
    private static final Color ACCENT_PURPLE = new Color(139,  92, 246);

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public ServiceHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // Entire page sits inside a scroll pane so small windows can scroll
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildPageHeader(), BorderLayout.NORTH);
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
    // buildPageHeader() — subtitle label below the page title
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel subtitle = new JLabel("Your completed service records are shown below.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);

        return header;
    }

    // ─────────────────────────────────────────────────────────────
    // buildDataPanel()
    //
    // Holds the three stat cards (NORTH) and the table card (CENTER).
    // Always visible regardless of whether data exists.
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
    //
    // Labels stored in instance fields so refresh() can update them.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        // Card 1: Total services — blue left bar
        totalServicesValueLabel = makeBigValueLabel("0");
        row.add(buildStatCard(
                "Total services",
                totalServicesValueLabel,
                makeMutedLabel("since joining"),
                ACCENT_BLUE
        ));

        // Card 2: Latest service — green left bar
        latestServiceValueLabel = makeBigValueLabel("—");
        latestServiceSubLabel   = makeMutedLabel("no records yet");
        row.add(buildStatCard(
                "Latest service",
                latestServiceValueLabel,
                latestServiceSubLabel,
                ACCENT_GREEN
        ));

        // Card 3: Favourite technician — purple left bar
        favTechValueLabel = makeBigValueLabel("—");
        favTechSubLabel   = makeMutedLabel("no records yet");
        row.add(buildStatCard(
                "Favourite technician",
                favTechValueLabel,
                favTechSubLabel,
                ACCENT_PURPLE
        ));

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatCard()
    //
    // Creates one white rounded card with a coloured left accent bar.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatCard(String topText, JLabel valueLabel,
                                  JLabel subLabel, Color accentColor) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. White rounded background
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // 2. Grey border
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // 3. Coloured left accent bar — clipped to rounded corners
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
    // buildTableCard()
    //
    // White rounded card containing:
    //   NORTH  — search + sort bar
    //   CENTER — the data table (empty when there is no data)
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

        // Search + sort bar at the top of the card
        card.add(buildSearchSortBar(), BorderLayout.NORTH);

        // Table column headers
        String[] columns = {
            "History ID", "Appointment ID", "Vehicle",
            "Car Plate", "Payment ID", "Technician", "Date", "Status"
        };

        // Non-editable table model — rows are added by refresh()
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = TableHelper.buildTable(tableModel);

        // Row sorter enables both the search filter and the column sort
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);

        // Centre-align columns 0 to 6
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    // Alternate row colours: white / very light blue-grey
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };
        for (int i = 0; i <= 6; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Status column (index 7) — coloured badge renderer
        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JLabel badge = new JLabel(value != null ? value.toString() : "");
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));

                String status = value != null ? value.toString() : "";
                if (status.equalsIgnoreCase("Completed")) {
                    badge.setBackground(new Color(220, 248, 232));
                    badge.setForeground(new Color(34, 139, 80));
                } else if (status.equalsIgnoreCase("Pending")) {
                    badge.setBackground(new Color(255, 243, 220));
                    badge.setForeground(new Color(180, 110, 20));
                } else {
                    badge.setBackground(new Color(235, 236, 240));
                    badge.setForeground(COLOR_MUTED);
                }

                // Wrapper centres the badge inside the cell
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

        // Set preferred column widths
        int[] widths = { 90, 120, 80, 90, 100, 140, 110, 120 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.setPreferredSize(new Dimension(0, 400));
        tableScroll.setMinimumSize(new Dimension(0, 200));

        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildSearchSortBar()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildSearchSortBar() {

        // RIGHT-aligned flow with 8 px horizontal gap between controls
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        bar.setOpaque(false);

        // ── Search text field ─────────────────────────────────────
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBackground(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(4, 10, 4, 10)
        ));
        searchField.setPreferredSize(new Dimension(180, 30));

        // Grey placeholder text — cleared when the field receives focus
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

        // applyFilter() is called on every single keystroke
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
            }
        );

        // ── Sort dropdown (JComboBox) ─────────────────────────────
        // Labels come from the service layer — view stays logic-free
        String[] sortOptions = serviceHistoryService.getAvailableSortOptions();

        sortCombo = new JComboBox<>(sortOptions);
        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sortCombo.setBackground(Color.WHITE);
        sortCombo.setForeground(COLOR_TEXT);
        sortCombo.setPreferredSize(new Dimension(160, 30));
        sortCombo.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1, true));

        // applySort() is called whenever the user picks a different option
        sortCombo.addActionListener(e -> applySort());

        // Add both controls to the bar (search first, then sort)
        bar.add(searchField);
        bar.add(sortCombo);

        // Bottom border divides the bar from the table rows below it
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(10, 16, 10, 16)
        ));

        return bar;
    }

    // ─────────────────────────────────────────────────────────────
    // applyFilter()
    // ─────────────────────────────────────────────────────────────
    private void applyFilter() {
        serviceHistoryService.applyFilter(rowSorter,
                searchField != null ? searchField.getText() : "");
    }

    // ─────────────────────────────────────────────────────────────
    // applySort()
    // ─────────────────────────────────────────────────────────────
    private void applySort() {
        serviceHistoryService.applySort(rowSorter,
                sortCombo != null ? (String) sortCombo.getSelectedItem() : null);
    }

    // ═══════════════════════════════════════════════════════════════
    // refresh()
    // ═══════════════════════════════════════════════════════════════
    public void refresh() {
        model.User loggedInUser = getLoggedInUser();

        if (loggedInUser == null) {
            // No user logged in — reset everything to zero / dash
            resetStatsToZero();
            tableModel.setRowCount(0);
            return;
        }

        // ── 1. Load this customer's records via the service layer ──
        String customerId = loggedInUser.getUserId();
        List<String[]> myRecords = serviceHistoryService.getRecordsForCustomer(customerId);

        if (myRecords.isEmpty()) {
            // Customer has no service records yet — show zero values
            resetStatsToZero();
            tableModel.setRowCount(0);
        } else {
            // ── 2. Update stat cards ───────────────────────────────
            SummaryStats stats = serviceHistoryService.getSummaryStats(myRecords);
            totalServicesValueLabel.setText(String.valueOf(stats.totalServices));
            latestServiceValueLabel.setText(stats.latestServiceDate);
            latestServiceSubLabel.setText(stats.latestServiceSubText);
            favTechValueLabel.setText(stats.favTechName);
            favTechSubLabel.setText(stats.favTechSubText);

            // ── 3. Fill the table ──────────────────────────────────
            tableModel.setRowCount(0); // clear old rows first
            for (Object[] row : serviceHistoryService.buildTableRows(myRecords)) {
                tableModel.addRow(row);
            }
        }

        // ── Reset ALL controls to their default state ─────────────
        // Runs whether or not there is data, so controls are always
        // clean when the user returns to this page.

        if (searchField != null) {
            searchField.setForeground(COLOR_MUTED);
            searchField.setText("Search...");
        }

        // Reset the sort dropdown back to the placeholder
        if (sortCombo != null) {
            sortCombo.setSelectedIndex(0);
        }

        if (rowSorter != null) {
            rowSorter.setRowFilter(null); // clear any active text filter
            rowSorter.setSortKeys(null);  // clear any active column sort
        }
    }

    // ─────────────────────────────────────────────────────────────
    // resetStatsToZero()
    //
    // Sets all stat card labels to their "no data" default values.
    // Called when the customer has no service history records.
    // ─────────────────────────────────────────────────────────────
    private void resetStatsToZero() {
        totalServicesValueLabel.setText("0");
        latestServiceValueLabel.setText("—");
        latestServiceSubLabel.setText("no records yet");
        favTechValueLabel.setText("—");
        favTechSubLabel.setText("no records yet");
    }

    // ─────────────────────────────────────────────────────────────
    // getLoggedInUser()
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

    // ═══════════════════════════════════════════════════════════════
    // buildNoDataPanel()
    //
    // KEPT for backward compatibility — other pages (StaffReviewPage,
    // MyFeedbackPage) still call this static method.
    // Not used by ServiceHistoryPage itself.
    // ═══════════════════════════════════════════════════════════════
    public static JPanel buildNoDataPanel(String iconUnicode, String title, String subtitle) {
        Color bgColor     = new Color(248, 249, 253);
        Color borderColor = new Color(225, 228, 235);
        Color textColor   = new Color(30,  35,  50);
        Color mutedColor  = new Color(110, 118, 140);

        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(20, 0, 40, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(440, 220));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel iconLabel;
        if (iconUnicode != null && !iconUnicode.isEmpty()) {
            iconLabel = new JLabel(iconUnicode);
            iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 38));
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        } else {
            iconLabel = new JLabel("—");
            iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 38));
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(textColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLabel.setForeground(mutedColor);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(subLabel);
        card.add(Box.createVerticalGlue());

        outer.add(card);
        return outer;
    }
}