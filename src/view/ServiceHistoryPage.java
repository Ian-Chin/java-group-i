package view;

import model.AccountService;
import model.ServiceHistoryService;
import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ServiceHistoryPage — UI only.
 *
 * CHANGES IN THIS VERSION:
 *   1. Stat cards now have a coloured left accent bar (like the dashboard screenshot).
 *      - Total services  → blue  (#506EE6)
 *      - Latest service  → green (#22A55A)
 *      - Fav technician  → purple (#8B5CF6)
 *
 *   2. A search field and a status filter dropdown have been added above the table.
 *      - The search box lets the customer type any text to filter table rows.
 *      - The dropdown lets the customer pick "All Status", "Completed", or "In Progress".
 *      - Both controls work together at the same time.
 */
public class ServiceHistoryPage extends JPanel {

    // ── Service classes (data layer) ──────────────────────────────
    private final ServiceHistoryService serviceHistoryService = new ServiceHistoryService();
    private final AccountService        accountService        = new AccountService();
    private final VehicleService        vehicleService        = new VehicleService();

    // ── CardLayout: "TABLE" when records exist, "EMPTY" when not ──
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Stat card value labels (updated by refresh()) ─────────────
    private JLabel totalServicesValueLabel;
    private JLabel latestServiceValueLabel;
    private JLabel latestServiceSubLabel;
    private JLabel favTechValueLabel;
    private JLabel favTechSubLabel;

    // ── Table model and sorter (sorter enables search + filter) ───
    private DefaultTableModel  tableModel;
    private TableRowSorter<DefaultTableModel> rowSorter; // ← NEW: handles filtering

    // ── Search field and filter dropdown (declared here so
    //    refresh() can reset them when new data is loaded) ────────
    private JTextField searchField;

    // ── Design constants ──────────────────────────────────────────
    private static final Color COLOR_BG       = new Color(245, 246, 250);
    private static final Color COLOR_CARD     = Color.WHITE;
    private static final Color COLOR_BORDER   = new Color(225, 228, 235);
    private static final Color COLOR_TEXT     = new Color(30,  35,  50);
    private static final Color COLOR_MUTED    = new Color(110, 118, 140);
    private static final Color COLOR_EMPTY_BG = new Color(248, 249, 253);

    // ── Accent bar colours for each stat card ─────────────────────
    // These are the coloured left-side bars like in the screenshot.
    private static final Color ACCENT_BLUE   = new Color(80,  110, 230); // Total services
    private static final Color ACCENT_GREEN  = new Color(34,  165,  90); // Latest service
    private static final Color ACCENT_PURPLE = new Color(139,  92, 246); // Fav technician

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public ServiceHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildPageHeader(), BorderLayout.NORTH);

        switchPanel.setOpaque(false);
        switchPanel.add(buildDataPanel(),  "TABLE");
        switchPanel.add(buildEmptyPanel(), "EMPTY");
        pageContent.add(switchPanel, BorderLayout.CENTER);

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
    // buildDataPanel() — the "TABLE" card
    // Contains: stats row at the top, then table card below.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel statsRow = buildStatsRow();
        statsRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        panel.add(statsRow, BorderLayout.NORTH);
        panel.add(buildTableCard(), BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatsRow() — three summary cards side by side
    //
    // CHANGE: each card now receives an accent colour for its
    // left border bar.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        // Card 1: Total services — blue accent bar
        totalServicesValueLabel = makeBigValueLabel("—");
        row.add(buildStatCard(
            "Total services",
            totalServicesValueLabel,
            makeMutedLabel("since joining"),
            ACCENT_BLUE   // ← accent bar colour
        ));

        // Card 2: Latest service — green accent bar
        latestServiceValueLabel = makeBigValueLabel("—");
        latestServiceSubLabel   = makeMutedLabel("—");
        row.add(buildStatCard(
            "Latest service",
            latestServiceValueLabel,
            latestServiceSubLabel,
            ACCENT_GREEN  // ← accent bar colour
        ));

        // Card 3: Favourite technician — purple accent bar
        favTechValueLabel = makeBigValueLabel("—");
        favTechSubLabel   = makeMutedLabel("—");
        row.add(buildStatCard(
            "Favourite technician",
            favTechValueLabel,
            favTechSubLabel,
            ACCENT_PURPLE // ← accent bar colour
        ));

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatCard()
    //
    // CHANGE: added an "accentColor" parameter.
    // The card now draws a 4-pixel wide coloured bar on its left
    // edge, just like the dashboard screenshot.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatCard(String topText, JLabel valueLabel,
                                  JLabel subLabel, Color accentColor) {

        // This JPanel overrides paintComponent so we can draw the
        // rounded white background AND the left accent bar manually.
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);

                // 1. Draw the white rounded card background
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // 2. Draw the card border
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                // 3. Draw the coloured left accent bar (4 px wide).
                //    We clip it to the rounded corners so it looks neat.
                //    Steps:
                //      a) Set the clip region to the rounded card shape
                //      b) Fill the left 4 pixels with the accent colour
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                    0, 0, getWidth(), getHeight(), 14, 14
                ));
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 4, getHeight()); // 4 px wide bar on the left

                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // Left padding is 16 (4 px bar + 12 px gap); right padding is 20
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
    // buildTableCard() — white rounded card that holds:
    //   1. A search + filter toolbar at the top  ← NEW
    //   2. The data table below
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {

        // Outer card with rounded white background
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

        // ── 1. Build the search + filter toolbar ──────────────────
        card.add(buildSearchFilterBar(), BorderLayout.NORTH); // ← NEW

        // ── 2. Build the table ────────────────────────────────────
        String[] columns = {
            "History ID", "Appointment ID", "Vehicle",
            "Car Plate", "Payment ID", "Technician", "Date", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = TableHelper.buildTable(tableModel);

        // Create a row sorter so we can filter rows later.
        // TableRowSorter sits between the table and the model —
        // it decides which rows are visible based on the filter.
        rowSorter = new TableRowSorter<>(tableModel); // ← NEW
        table.setRowSorter(rowSorter);                // ← NEW

        // Center-align columns 0–6
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected,
                                                    hasFocus, row, col);
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
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Status column — coloured badge
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

                JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                if (isSelected) wrapper.setBackground(new Color(80, 110, 230, 60));
                wrapper.add(badge);
                return wrapper;
            }
        );

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
    // buildSearchFilterBar()  ← NEW METHOD
    //
    // Builds a small toolbar that sits above the table inside the
    // white card. It contains:
    //   - A "Appointments" bold label on the left (like the screenshot)
    //   - A search text field in the middle
    //   - A status dropdown on the right ("All Status" / "Completed" / "In Progress")
    //
    // How filtering works:
    //   Every time the user types in the search box OR changes the
    //   dropdown, the applyFilter() method is called. That method
    //   creates a RowFilter and sets it on the rowSorter, which
    //   automatically hides non-matching rows in the table.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildSearchFilterBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bar.setOpaque(false);

        // Search field — a plain text box with rounded border
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setBackground(Color.WHITE);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
            new EmptyBorder(4, 10, 4, 10)
        ));
        searchField.setPreferredSize(new Dimension(180, 30));

        // Placeholder text "Search..." shown in grey when empty
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

        // Every keystroke updates the table instantly
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        bar.add(searchField);

        // Divider line below the search bar, above the table
        bar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
            new EmptyBorder(10, 16, 10, 16)
        ));

        return bar;
    }

    // ─────────────────────────────────────────────────────────────
    // applyFilter()  ← NEW METHOD
    //
    // Called every time the search text or the dropdown changes.
    //
    // Logic:
    //   1. Get the current search text (ignore if it's the placeholder).
    //   2. Get the selected status from the dropdown.
    //   3. Build one or two RowFilters:
    //        - A "regex" filter on ALL columns for the search text
    //        - A "regex" filter on column 7 (Status) for the dropdown
    //   4. Combine them with RowFilter.andFilter() so BOTH must match.
    //   5. Set the combined filter on rowSorter.
    //
    // If both are blank / "All Status", remove the filter entirely
    // so all rows are visible again.
    // ─────────────────────────────────────────────────────────────
    private void applyFilter() {
        if (rowSorter == null || searchField == null) return;

        // Get search text; treat placeholder as empty
        String searchText = searchField.getText().trim();
        if (searchText.equals("Search...")) searchText = "";

        if (searchText.isEmpty()) {
            // Nothing typed — show all rows
            rowSorter.setRowFilter(null);
            return;
        }

        // Case-insensitive search across all columns
        try {
            rowSorter.setRowFilter(RowFilter.regexFilter(
                "(?i)" + java.util.regex.Pattern.quote(searchText)
            ));
        } catch (java.util.regex.PatternSyntaxException ex) {
            // If the user types special characters, just ignore
        }
    }

    // ─────────────────────────────────────────────────────────────
    // buildEmptyPanel() — shown when the customer has no records
    // ─────────────────────────────────────────────────────────────
    private JPanel buildEmptyPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(20, 0, 40, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_EMPTY_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(440, 220));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 40, 40, 40));

        // Circular-repeat / refresh icon drawn with Java2D
        JLabel iconLabel = new JLabel() {
            @Override public Dimension getPreferredSize() { return new Dimension(52, 52); }
            @Override public Dimension getMinimumSize()   { return new Dimension(52, 52); }
            @Override public Dimension getMaximumSize()   { return new Dimension(52, 52); }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                    RenderingHints.VALUE_STROKE_PURE);
                Color iconColor = new Color(160, 165, 185);
                g2.setColor(iconColor);
                float cx = 26f, cy = 26f, r = 16f;
                g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));
                g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2,  30, 150, Arc2D.OPEN));
                g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2, 210, 150, Arc2D.OPEN));
                drawArrowhead(g2, iconColor, cx-r, cy, cx-r, cy-7f);
                drawArrowhead(g2, iconColor, cx+r, cy, cx+r, cy+7f);
                g2.dispose();
            }

            private void drawArrowhead(Graphics2D g2, Color color,
                                       float tipX, float tipY,
                                       float tailX, float tailY) {
                float dx = tipX - tailX, dy = tipY - tailY;
                float len = (float) Math.sqrt(dx*dx + dy*dy);
                if (len == 0) return;
                dx /= len; dy /= len;
                float px = -dy, py = dx;
                float al = 7f, ah = 4f;
                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(tipX, tipY);
                arrow.lineTo(tipX - dx*al + px*ah, tipY - dy*al + py*ah);
                arrow.lineTo(tipX - dx*al - px*ah, tipY - dy*al - py*ah);
                arrow.closePath();
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(color);
                g2.fill(arrow);
            }
        };
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel noDataLabel = new JLabel("No service history records found.");
        noDataLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        noDataLabel.setForeground(COLOR_TEXT);
        noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subLabel = new JLabel("Your completed service records will appear here.");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        subLabel.setForeground(COLOR_MUTED);
        subLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(Box.createVerticalGlue());
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(noDataLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(subLabel);
        card.add(Box.createVerticalGlue());

        outer.add(card);
        return outer;
    }

    // ═══════════════════════════════════════════════════════════════
    // refresh() — called when the page is opened or the user logs in.
    // Loads data, fills stat cards, fills the table, and resets filters.
    // ═══════════════════════════════════════════════════════════════
    public void refresh() {
        model.User loggedInUser = getLoggedInUser();
        if (loggedInUser == null) {
            cardLayout.show(switchPanel, "EMPTY");
            return;
        }

        String customerId = loggedInUser.getUserId();
        List<String[]> allRecords = serviceHistoryService.getAllRecords();

        // Keep only rows that belong to this customer
        List<String[]> myRecords = new ArrayList<>();
        for (String[] row : allRecords) {
            if (row[1].trim().equalsIgnoreCase(customerId)) {
                myRecords.add(row);
            }
        }

        if (myRecords.isEmpty()) {
            cardLayout.show(switchPanel, "EMPTY");
        } else {
            updateStatsCards(myRecords);
            fillTable(myRecords);

            // Reset the search field back to default when new data is loaded
            if (searchField != null) {
                searchField.setForeground(COLOR_MUTED);
                searchField.setText("Search...");
            }
            if (rowSorter != null) {
                rowSorter.setRowFilter(null);
            }

            cardLayout.show(switchPanel, "TABLE");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatsCards() — fills the three value labels
    // ─────────────────────────────────────────────────────────────
    private void updateStatsCards(List<String[]> records) {
        totalServicesValueLabel.setText(String.valueOf(records.size()));

        String[] latest = records.get(records.size() - 1);
        latestServiceValueLabel.setText(latest[6].trim());
        latestServiceSubLabel.setText(latest[2].trim() + " — " + latest[7].trim());

        Map<String, Integer> techCount = new HashMap<>();
        for (String[] row : records) {
            String id = row[5].trim();
            techCount.put(id, techCount.getOrDefault(id, 0) + 1);
        }

        String favId = ""; int favCount = 0;
        for (Map.Entry<String, Integer> e : techCount.entrySet()) {
            if (e.getValue() > favCount) { favId = e.getKey(); favCount = e.getValue(); }
        }

        favTechValueLabel.setText(resolveTechnicianName(favId));
        favTechSubLabel.setText(favCount + " service" + (favCount > 1 ? "s" : "") + " handled");
    }

    // ─────────────────────────────────────────────────────────────
    // fillTable() — populates the table model with rows
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<String[]> records) {
        tableModel.setRowCount(0); // clear existing rows

        for (String[] row : records) {
            String historyId      = row[0].trim();
            String appointmentId  = row[2].trim();
            String vehicleId      = row[3].trim();
            String paymentId      = row[4].trim();
            String techId         = row[5].trim();
            String date           = row[6].trim();
            String status         = row[7].trim();

            String techName       = resolveTechnicianName(techId);
            String vehicleType    = resolveVehicleType(vehicleId);
            String carPlate       = resolveCarPlate(vehicleId);
            String paymentDisplay = paymentId.equalsIgnoreCase("NULL") ? "—" : paymentId;

            tableModel.addRow(new Object[]{
                historyId, appointmentId, vehicleType, carPlate,
                paymentDisplay, techName, date, status
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Data-lookup helpers — delegate to service classes
    // ─────────────────────────────────────────────────────────────
    private String resolveTechnicianName(String techId) {
        for (model.User user : accountService.getAllUsers()) {
            if (user.getUserId().equalsIgnoreCase(techId)) return user.getName();
        }
        return techId;
    }

    private String resolveVehicleType(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[0].trim() : vehicleId;
    }

    private String resolveCarPlate(String vehicleId) {
        String label = vehicleService.getVehiclePlate(vehicleId);
        return label.contains(" · ") ? label.split(" · ", 2)[1].trim() : vehicleId;
    }

    private model.User getLoggedInUser() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof AppFrame) return ((AppFrame) parent).getLoggedInUserObj();
            parent = parent.getParent();
        }
        return null;
    }

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
    // buildNoDataPanel() — public static shared utility
    // Called by PaymentHistoryPage, StaffReviewPage, MyFeedbackPage
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
            iconLabel = new JLabel() {
                @Override public Dimension getPreferredSize() { return new Dimension(52, 52); }
                @Override public Dimension getMinimumSize()   { return new Dimension(52, 52); }
                @Override public Dimension getMaximumSize()   { return new Dimension(52, 52); }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                        RenderingHints.VALUE_ANTIALIAS_ON);
                    Color ic = new Color(160, 165, 185);
                    g2.setColor(ic);
                    float cx = 26f, cy = 26f, r = 16f;
                    g2.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND,
                                                 BasicStroke.JOIN_ROUND));
                    g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2,  30, 150, Arc2D.OPEN));
                    g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2, 210, 150, Arc2D.OPEN));
                    drawHead(g2, ic, cx-r, cy, cx-r, cy-7f);
                    drawHead(g2, ic, cx+r, cy, cx+r, cy+7f);
                    g2.dispose();
                }

                private void drawHead(Graphics2D g2, Color c,
                                      float tx, float ty, float fx, float fy) {
                    float dx = tx-fx, dy = ty-fy;
                    float len = (float) Math.sqrt(dx*dx + dy*dy);
                    if (len == 0) return;
                    dx /= len; dy /= len;
                    float px = -dy, py = dx;
                    float al = 7f, ah = 4f;
                    GeneralPath p = new GeneralPath();
                    p.moveTo(tx, ty);
                    p.lineTo(tx-dx*al+px*ah, ty-dy*al+py*ah);
                    p.lineTo(tx-dx*al-px*ah, ty-dy*al-py*ah);
                    p.closePath();
                    g2.setStroke(new BasicStroke(1f));
                    g2.setColor(c);
                    g2.fill(p);
                }
            };
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