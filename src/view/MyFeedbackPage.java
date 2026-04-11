package view;

import model.MyFeedbackService;
import model.MyFeedbackService.MyFeedback;
import model.MyFeedbackService.AppointmentRow;
import model.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MyFeedbackPage
 *
 * Shows two tabs:
 *   1. "Pending Feedback"  — completed appointments that have no feedback yet
 *   2. "Feedback History"  — all feedback the customer has already submitted
 *
 * FIXES in this version:
 *   FIX 1 — "Feedback" button in the table now shows a hand cursor on hover.
 *            Root cause: the JPanel wrapper was intercepting the cursor, so the
 *            JButton cursor setting was never visible.
 *            Solution: set HAND_CURSOR on the wrapper panel as well.
 *
 *   FIX 2 — Popup gaps reduced.
 *            The large empty space between the star row and "Your feedback" label
 *            was caused by a 24 px vertical strut. Reduced to 10 px.
 *
 *   FIX 3 — Placeholder text now disappears correctly when typing.
 *            Root cause: the JScrollPane around the JTextArea was receiving
 *            the mouse click first, so focusGained() on the JTextArea was
 *            never fired.
 *            Solution: removed the JScrollPane and used a plain JTextArea with
 *            its own border + a MouseListener that also clears the placeholder
 *            when the user first clicks inside.
 */
public class MyFeedbackPage extends JPanel {

    // ── Colours ───────────────────────────────────────────────────
    // All colours are defined here so they are easy to change later.
    private static final Color COLOR_BG     = new Color(245, 246, 250); // light grey background
    private static final Color COLOR_CARD   = Color.WHITE;              // white card
    private static final Color COLOR_BORDER = new Color(225, 228, 235); // card border
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);  // dark text
    private static final Color COLOR_MUTED  = new Color(110, 118, 140); // grey helper text
    private static final Color BLUE_ACCENT  = new Color(80, 110, 230);  // primary blue
    private static final Color YELLOW_STAR  = new Color(255, 193, 7);   // star colour

    // ── Service ───────────────────────────────────────────────────
    private final MyFeedbackService service = new MyFeedbackService();

    // ── Logged-in customer ────────────────────────────────────────
    private User loggedInUser;

    // ── CardLayout switches between the two tab panels ────────────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Tab buttons ───────────────────────────────────────────────
    private JButton pendingTabBtn;
    private JButton historyTabBtn;
    private String  activeTab = "PENDING";

    // ── Table models ──────────────────────────────────────────────
    private DefaultTableModel pendingTableModel;
    private DefaultTableModel historyTableModel;

    // ── Table references ──────────────────────────────────────────
    private JTable pendingTable;
    private JTable historyTable;

    // ── Full data lists (kept so search/filter can re-apply to all) ─
    private List<AppointmentRow> currentPending = new ArrayList<>();
    private List<MyFeedback>     currentHistory = new ArrayList<>();

    // ── Inner CardLayouts for each tab (data vs empty state) ──────
    private final CardLayout pendingCardLayout = new CardLayout();
    private final JPanel     pendingSwitch     = new JPanel(pendingCardLayout);

    private final CardLayout historyCardLayout = new CardLayout();
    private final JPanel     historySwitch     = new JPanel(historyCardLayout);

    // ── Search / Sort controls ────────────────────────────────────
    private JTextField        pendingSearchField;
    private JComboBox<String> pendingSortCombo;
    private JTextField        historySearchField;
    private JComboBox<String> historySortCombo;

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────
    public MyFeedbackPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildTopRow(), BorderLayout.NORTH);

        switchPanel.setOpaque(false);
        switchPanel.add(buildPendingPanel(), "PENDING");
        switchPanel.add(buildHistoryPanel(), "HISTORY");
        pageContent.add(switchPanel, BorderLayout.CENTER);

        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);

        cardLayout.show(switchPanel, "PENDING");
    }

    // ─────────────────────────────────────────────────────────────
    // setUser() — update the logged-in user before calling refresh()
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh() — reloads all data from files, re-applies search/sort
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;
        System.out.println("[MyFeedbackPage] refresh() — customer: " + customerId);

        if (customerId != null) {
            currentPending = service.getCompletedAppointmentsWithoutFeedback(customerId);
            currentHistory = service.getFeedbackByCustomer(customerId);
        } else {
            currentPending = new ArrayList<>();
            currentHistory = new ArrayList<>();
        }

        // Re-apply whatever search/sort is currently active
        applyPendingSearchAndSort();
        applyHistorySearchAndSort();

        pendingCardLayout.show(pendingSwitch, currentPending.isEmpty() ? "EMPTY" : "DATA");
        historyCardLayout.show(historySwitch, currentHistory.isEmpty() ? "EMPTY" : "DATA");

        cardLayout.show(switchPanel, activeTab);
    }

    // ═════════════════════════════════════════════════════════════
    // TOP ROW — subtitle + tab toggle buttons
    // ═════════════════════════════════════════════════════════════
    private JPanel buildTopRow() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel subtitle = new JLabel(
                "Completed appointments are shown below. Click \"Feedback\" to rate your experience.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        top.add(subtitle, BorderLayout.WEST);

        // 6 px gap between the two buttons
        JPanel tabRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        tabRow.setOpaque(false);

        pendingTabBtn = buildTabButton("Pending Feedback", true);
        historyTabBtn = buildTabButton("Feedback History", false);

        pendingTabBtn.addActionListener(e -> switchTab("PENDING"));
        historyTabBtn.addActionListener(e -> switchTab("HISTORY"));

        tabRow.add(pendingTabBtn);
        tabRow.add(historyTabBtn);
        top.add(tabRow, BorderLayout.EAST);

        return top;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTabButton() — pill-shaped toggle button
    // ─────────────────────────────────────────────────────────────
    private JButton buildTabButton(String text, boolean isActive) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getClientProperty("active") == Boolean.TRUE
                        ? BLUE_ACCENT : COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BLUE_ACCENT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(isActive ? Color.WHITE : BLUE_ACCENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(148, 32));
        btn.putClientProperty("active", isActive);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // switchTab() — highlights the correct button and flips the card
    // ─────────────────────────────────────────────────────────────
    private void switchTab(String tab) {
        activeTab = tab;
        boolean pendingActive = tab.equals("PENDING");

        pendingTabBtn.putClientProperty("active", pendingActive);
        pendingTabBtn.setForeground(pendingActive ? Color.WHITE : BLUE_ACCENT);
        pendingTabBtn.repaint();

        historyTabBtn.putClientProperty("active", !pendingActive);
        historyTabBtn.setForeground(!pendingActive ? Color.WHITE : BLUE_ACCENT);
        historyTabBtn.repaint();

        cardLayout.show(switchPanel, tab);
    }

    // ═════════════════════════════════════════════════════════════
    // SEARCH / SORT BAR — shared helper used by both tabs
    // ═════════════════════════════════════════════════════════════

    /**
     * Builds a row with a search field and a sort/filter combo box.
     * It is placed NORTH of each table card so it sits above the table.
     *
     * @param searchField  the JTextField already created by the caller
     * @param sortCombo    the JComboBox already created by the caller
     * @param sortOptions  items to add to the combo box
     * @param onChanged    called every time the user types or picks an option
     */
    private JPanel buildSearchSortBar(JTextField searchField,
                                      JComboBox<String> sortCombo,
                                      String[] sortOptions,
                                      Runnable onChanged) {

        for (String option : sortOptions) {
            sortCombo.addItem(option);
        }

        // Style the search field
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        searchField.setToolTipText("Type to search...");

        // Placeholder: grey "Search..." that disappears on focus
        searchField.setForeground(COLOR_MUTED);
        searchField.setText("Search...");
        searchField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (searchField.getText().equals("Search...")) {
                    searchField.setText("");
                    searchField.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search...");
                    searchField.setForeground(COLOR_MUTED);
                }
            }
        });

        // Filter rows every time the user types a character
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                onChanged.run();
            }
        });

        // Style the combo box
        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sortCombo.setPreferredSize(new Dimension(180, 32));
        sortCombo.setBackground(COLOR_CARD);
        sortCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Filter rows every time the user picks a different option
        sortCombo.addActionListener(e -> onChanged.run());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        bar.add(searchField);
        bar.add(sortCombo);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════
    // PENDING TAB
    // ═════════════════════════════════════════════════════════════
    private JPanel buildPendingPanel() {
        pendingSwitch.setOpaque(false);
        pendingSwitch.add(buildPendingDataCard(),   "DATA");
        pendingSwitch.add(buildPendingEmptyPanel(), "EMPTY");
        pendingCardLayout.show(pendingSwitch, "EMPTY");
        return pendingSwitch;
    }

    // ─────────────────────────────────────────────────────────────
    // buildPendingDataCard()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPendingDataCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        // Build and attach the search + sort bar
        pendingSearchField = new JTextField();
        pendingSortCombo   = new JComboBox<>();
        String[] pendingSortOptions = {
            "Default Order",
            "Sort by Date (Newest First)",
            "Sort by Date (Oldest First)",
            "Sort by Service Type (A-Z)",
            "Sort by Duration (Shortest First)",
            "Sort by Duration (Longest First)"
        };
        card.add(
            buildSearchSortBar(pendingSearchField, pendingSortCombo,
                               pendingSortOptions, this::applyPendingSearchAndSort),
            BorderLayout.NORTH
        );

        // Table columns
        String[] columns = { "Appointment ID", "Service Type", "Date / Time", "Duration", "" };

        pendingTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                // Only the last column (Feedback button) is interactive
                return col == 4;
            }
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 4 ? JButton.class : String.class;
            }
        };

        pendingTable = TableHelper.buildTable(pendingTableModel);
        pendingTable.setRowHeight(44);

        // Centre renderer for text columns 0-3
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };
        for (int i = 0; i < 4; i++) {
            pendingTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // ── FIX 1 — RENDERER ─────────────────────────────────────
        // The JPanel wrapper sits on top of the JButton inside the table cell.
        // Before this fix, only the JButton had HAND_CURSOR, but the wrapper
        // panel was absorbing the mouse events, so the cursor never changed.
        // Fix: also set HAND_CURSOR on the WRAPPER PANEL.
        // ─────────────────────────────────────────────────────────
        pendingTable.getColumnModel().getColumn(4).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JPanel wrapper = new JPanel(new GridBagLayout()); // centres button in cell
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                // FIX 1: set cursor on the wrapper so the hand appears over the whole cell
                wrapper.setCursor(new Cursor(Cursor.HAND_CURSOR));
                wrapper.add(makeFeedbackButton("Feedback"));
                return wrapper;
            }
        );

        // ── FIX 1 — EDITOR ───────────────────────────────────────
        // Same fix applied to the editor component (handles real clicks).
        // ─────────────────────────────────────────────────────────
        pendingTable.getColumnModel().getColumn(4).setCellEditor(
            new DefaultCellEditor(new JCheckBox()) {
                @Override
                public Component getTableCellEditorComponent(JTable t, Object value,
                        boolean isSelected, int row, int col) {

                    JButton btn = makeFeedbackButton("Feedback");
                    btn.addActionListener(e -> {
                        fireEditingStopped(); // exit edit mode first
                        // Look up the original row by appointmentId in case
                        // the list was filtered (row index may not match)
                        String apptId = (String) pendingTableModel.getValueAt(row, 0);
                        AppointmentRow matched = currentPending.stream()
                                .filter(a -> a.appointmentId.equals(apptId))
                                .findFirst().orElse(null);
                        if (matched != null) {
                            showFeedbackPopup(matched);
                        }
                    });

                    JPanel wrapper = new JPanel(new GridBagLayout());
                    wrapper.setOpaque(true);
                    wrapper.setBackground(Color.WHITE);
                    // FIX 1: cursor on the editor wrapper too
                    wrapper.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    wrapper.add(btn);
                    return wrapper;
                }
                @Override public Object getCellEditorValue() { return "Feedback"; }
            }
        );

        // Column widths
        TableColumnModel cols = pendingTable.getColumnModel();
        cols.getColumn(0).setPreferredWidth(130);
        cols.getColumn(1).setPreferredWidth(150);
        cols.getColumn(2).setPreferredWidth(150);
        cols.getColumn(3).setPreferredWidth(90);
        cols.getColumn(4).setPreferredWidth(120);
        cols.getColumn(4).setMinWidth(110);
        cols.getColumn(4).setMaxWidth(130);
        pendingTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(pendingTable);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_CARD);
        scroll.getViewport().setBackground(COLOR_CARD);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 420));
        scroll.setMinimumSize(new Dimension(0, 200));
        card.add(scroll, BorderLayout.CENTER);

        JLabel hint = new JLabel(
                "  \uD83D\uDCA1  Click \"Feedback\" on any row to rate your experience.");
        hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setBorder(new EmptyBorder(10, 16, 12, 16));
        card.add(hint, BorderLayout.SOUTH);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // applyPendingSearchAndSort()
    // Reads the search field + combo, filters currentPending, fills table.
    // ─────────────────────────────────────────────────────────────
    private void applyPendingSearchAndSort() {
        if (pendingSearchField == null || pendingSortCombo == null) return;

        String raw   = pendingSearchField.getText().trim();
        String query = (raw.equals("Search...") || raw.isEmpty()) ? "" : raw.toLowerCase();

        List<AppointmentRow> filtered = currentPending.stream()
            .filter(a -> {
                if (query.isEmpty()) return true;
                return a.appointmentId.toLowerCase().contains(query)
                    || a.serviceType.toLowerCase().contains(query)
                    || a.dateTime.toLowerCase().contains(query)
                    || a.duration.toLowerCase().contains(query);
            })
            .collect(Collectors.toList());

        String sortOption = (String) pendingSortCombo.getSelectedItem();
        if (sortOption != null) {
            switch (sortOption) {
                case "Sort by Date (Newest First)":
                    filtered.sort(Comparator.comparing(
                            (AppointmentRow a) -> a.dateTime).reversed());
                    break;
                case "Sort by Date (Oldest First)":
                    filtered.sort(Comparator.comparing(a -> a.dateTime));
                    break;
                case "Sort by Service Type (A-Z)":
                    filtered.sort(Comparator.comparing(a -> a.serviceType.toLowerCase()));
                    break;
                case "Sort by Duration (Shortest First)":
                    filtered.sort(Comparator.comparingDouble(
                            a -> parseDuration(a.duration)));
                    break;
                case "Sort by Duration (Longest First)":
                    filtered.sort(Comparator.comparingDouble(
                            (AppointmentRow a) -> parseDuration(a.duration)).reversed());
                    break;
            }
        }

        fillPendingTable(filtered);
        pendingCardLayout.show(pendingSwitch,
                currentPending.isEmpty() ? "EMPTY" : "DATA");
    }

    /** Converts "1.5" or "2 hr(s)" to a double for duration sorting. */
    private double parseDuration(String duration) {
        try {
            return Double.parseDouble(duration.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // buildPendingEmptyPanel()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPendingEmptyPanel() {
        return ServiceHistoryPage.buildNoDataPanel(
                "\uD83D\uDCAC",
                "No pending feedback found.",
                "Completed appointments that need feedback will appear here."
        );
    }

    // ─────────────────────────────────────────────────────────────
    // fillPendingTable()
    // ─────────────────────────────────────────────────────────────
    private void fillPendingTable(List<AppointmentRow> pending) {
        pendingTableModel.setRowCount(0); // clear old rows
        for (AppointmentRow appt : pending) {
            pendingTableModel.addRow(new Object[]{
                appt.appointmentId,
                appt.serviceType,
                appt.dateTime,
                appt.duration + " hr(s)",
                "Feedback"
            });
        }
    }

    // ═════════════════════════════════════════════════════════════
    // HISTORY TAB
    // ═════════════════════════════════════════════════════════════
    private JPanel buildHistoryPanel() {
        historySwitch.setOpaque(false);
        historySwitch.add(buildHistoryDataCard(),   "DATA");
        historySwitch.add(buildHistoryEmptyPanel(), "EMPTY");
        historyCardLayout.show(historySwitch, "EMPTY");
        return historySwitch;
    }

    // ─────────────────────────────────────────────────────────────
    // buildHistoryDataCard()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildHistoryDataCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        // Build and attach the search + sort bar
        historySearchField = new JTextField();
        historySortCombo   = new JComboBox<>();
        String[] historySortOptions = {
            "Default Order",
            "Sort by Date (Newest First)",
            "Sort by Date (Oldest First)",
            "Filter: Excellent Only",
            "Filter: Good Only",
            "Filter: Average Only",
            "Sort by Technician (A-Z)"
        };
        card.add(
            buildSearchSortBar(historySearchField, historySortCombo,
                               historySortOptions, this::applyHistorySearchAndSort),
            BorderLayout.NORTH
        );

        // Table columns
        String[] columns = {
            "Feedback ID", "Appointment ID", "Vehicle Type",
            "Car Plate", "Technician", "Condition", "Feedback", "Date"
        };

        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        historyTable = TableHelper.buildTable(historyTableModel);
        historyTable.setRowHeight(40);

        // Centre renderer for all columns except Condition (5) and Feedback (6)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(
                        t, value, isSelected, hasFocus, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };
        for (int i = 0; i < columns.length; i++) {
            if (i != 5 && i != 6) {
                historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Condition column (5) — coloured badge centred by GridBagLayout
        historyTable.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                String condition = value != null ? value.toString() : "";

                JLabel badge = new JLabel(condition);
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setBorder(new EmptyBorder(3, 10, 3, 10));

                switch (condition.toLowerCase()) {
                    case "excellent":
                        badge.setBackground(new Color(220, 248, 232));
                        badge.setForeground(new Color(34, 139, 80));
                        break;
                    case "good":
                        badge.setBackground(new Color(220, 235, 255));
                        badge.setForeground(new Color(40, 80, 200));
                        break;
                    case "average":
                        badge.setBackground(new Color(255, 243, 220));
                        badge.setForeground(new Color(180, 110, 20));
                        break;
                    default:
                        badge.setBackground(new Color(235, 236, 240));
                        badge.setForeground(COLOR_MUTED);
                }

                // GridBagLayout with no extra constraints = perfectly centred
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setOpaque(true);
                wrapper.setBackground(isSelected
                        ? new Color(80, 110, 230, 60)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253)));
                wrapper.add(badge);
                return wrapper;
            }
        );

        // Feedback column (6) — wrapping text area that grows with content
        historyTable.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JTextArea textArea = new JTextArea();
                textArea.setText(value != null ? value.toString() : "");
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);
                textArea.setOpaque(true);
                textArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
                textArea.setBorder(new EmptyBorder(8, 14, 8, 14));

                if (isSelected) {
                    textArea.setBackground(new Color(80, 110, 230, 60));
                    textArea.setForeground(COLOR_TEXT);
                } else {
                    textArea.setBackground(
                            row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                    textArea.setForeground(COLOR_TEXT);
                }

                // Auto-grow row height to fit wrapped text
                int colWidth = t.getColumnModel().getColumn(col).getWidth();
                textArea.setSize(new Dimension(colWidth, Short.MAX_VALUE));
                int preferredHeight = textArea.getPreferredSize().height;
                int newHeight       = Math.max(40, preferredHeight);
                if (t.getRowHeight(row) != newHeight) {
                    final int fh = newHeight;
                    SwingUtilities.invokeLater(() -> t.setRowHeight(row, fh));
                }
                return textArea;
            }
        );

        // Column widths
        TableColumnModel colModel = historyTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(90);
        colModel.getColumn(1).setPreferredWidth(110);
        colModel.getColumn(2).setPreferredWidth(90);
        colModel.getColumn(3).setPreferredWidth(90);
        colModel.getColumn(4).setPreferredWidth(110);
        colModel.getColumn(5).setPreferredWidth(90);
        colModel.getColumn(6).setPreferredWidth(270);
        colModel.getColumn(7).setPreferredWidth(100);
        historyTable.getTableHeader().setReorderingAllowed(false);

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(null);
        scroll.setBackground(COLOR_CARD);
        scroll.getViewport().setBackground(COLOR_CARD);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 420));
        scroll.setMinimumSize(new Dimension(0, 200));
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // applyHistorySearchAndSort()
    // ─────────────────────────────────────────────────────────────
    private void applyHistorySearchAndSort() {
        if (historySearchField == null || historySortCombo == null) return;

        String raw   = historySearchField.getText().trim();
        String query = (raw.equals("Search...") || raw.isEmpty()) ? "" : raw.toLowerCase();

        List<MyFeedback> filtered = currentHistory.stream()
            .filter(fb -> {
                if (query.isEmpty()) return true;
                return fb.feedbackId.toLowerCase().contains(query)
                    || fb.appointmentId.toLowerCase().contains(query)
                    || fb.vehicleType.toLowerCase().contains(query)
                    || fb.carPlate.toLowerCase().contains(query)
                    || fb.technicianName.toLowerCase().contains(query)
                    || fb.condition.toLowerCase().contains(query)
                    || fb.feedbackText.toLowerCase().contains(query)
                    || fb.date.toLowerCase().contains(query);
            })
            .collect(Collectors.toList());

        String sortOption = (String) historySortCombo.getSelectedItem();
        if (sortOption != null) {
            switch (sortOption) {
                case "Sort by Date (Newest First)":
                    filtered.sort(Comparator.comparing(
                            (MyFeedback fb) -> fb.date).reversed());
                    break;
                case "Sort by Date (Oldest First)":
                    filtered.sort(Comparator.comparing(fb -> fb.date));
                    break;
                case "Filter: Excellent Only":
                    filtered = filtered.stream()
                            .filter(fb -> fb.condition.equalsIgnoreCase("Excellent"))
                            .collect(Collectors.toList());
                    break;
                case "Filter: Good Only":
                    filtered = filtered.stream()
                            .filter(fb -> fb.condition.equalsIgnoreCase("Good"))
                            .collect(Collectors.toList());
                    break;
                case "Filter: Average Only":
                    filtered = filtered.stream()
                            .filter(fb -> fb.condition.equalsIgnoreCase("Average"))
                            .collect(Collectors.toList());
                    break;
                case "Sort by Technician (A-Z)":
                    filtered.sort(Comparator.comparing(
                            fb -> fb.technicianName.toLowerCase()));
                    break;
            }
        }

        fillHistoryTable(filtered);
        historyCardLayout.show(historySwitch,
                currentHistory.isEmpty() ? "EMPTY" : "DATA");
    }

    // ─────────────────────────────────────────────────────────────
    // buildHistoryEmptyPanel()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildHistoryEmptyPanel() {
        return ServiceHistoryPage.buildNoDataPanel(
                "\uD83D\uDCAC",
                "No feedback history found.",
                "Your submitted feedback will appear here."
        );
    }

    // ─────────────────────────────────────────────────────────────
    // fillHistoryTable()
    // ─────────────────────────────────────────────────────────────
    private void fillHistoryTable(List<MyFeedback> feedbackList) {
        historyTableModel.setRowCount(0);
        for (MyFeedback fb : feedbackList) {
            historyTableModel.addRow(new Object[]{
                fb.feedbackId,
                fb.appointmentId,
                fb.vehicleType,
                fb.carPlate,
                fb.technicianName,
                fb.condition,
                fb.feedbackText,
                fb.date
            });
        }
    }

    // ═════════════════════════════════════════════════════════════
    // FEEDBACK POPUP
    // ═════════════════════════════════════════════════════════════
    private void showFeedbackPopup(AppointmentRow appt) {

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Submit Feedback",
                true  // modal — blocks the main window while open
        );
        dialog.setSize(600, 520);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // All form content stacks vertically inside this panel
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 32, 20, 32));

        // ── Info bar ──────────────────────────────────────────────
        JLabel infoBar = new JLabel(
                "Appointment: " + appt.appointmentId
                + "  |  Service: " + appt.serviceType
                + "  |  Date: " + appt.dateTime);
        infoBar.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoBar.setForeground(COLOR_MUTED);
        infoBar.setOpaque(true);
        infoBar.setBackground(new Color(245, 246, 250));
        infoBar.setBorder(new EmptyBorder(8, 14, 8, 14));
        infoBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        form.add(infoBar);
        // FIX 2 — was 22 px, now 12 px (less empty space after info bar)
        form.add(Box.createVerticalStrut(12));

        // ── Star rating ───────────────────────────────────────────
        JLabel ratingQuestion = new JLabel("How would you rate this service?");
        ratingQuestion.setFont(new Font("SansSerif", Font.BOLD, 14));
        ratingQuestion.setForeground(COLOR_TEXT);
        ratingQuestion.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(ratingQuestion);
        form.add(Box.createVerticalStrut(8));

        // selectedRating[0] tracks which star number was last clicked
        int[] selectedRating = { 0 };

        JPanel starsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        starsRow.setOpaque(false);
        starsRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel[] starLabels = new JLabel[5];
        JLabel   ratingText = new JLabel("");
        ratingText.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ratingText.setForeground(COLOR_MUTED);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            JLabel star = new JLabel("\u2606");             // ☆ hollow star
            star.setFont(new Font("SansSerif", Font.PLAIN, 30));
            star.setForeground(new Color(200, 200, 210));
            star.setCursor(new Cursor(Cursor.HAND_CURSOR)); // hand cursor on stars
            starLabels[i] = star;

            star.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    // Preview: highlight up to the hovered star
                    updateStarDisplay(starLabels, starIndex, selectedRating[0]);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    // Revert to only the locked-in stars
                    updateStarDisplay(starLabels, selectedRating[0], selectedRating[0]);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    // Lock in this star rating
                    selectedRating[0] = starIndex;
                    updateStarDisplay(starLabels, starIndex, starIndex);
                    ratingText.setText(starIndex + " out of 5");
                }
            });

            starsRow.add(star);
        }
        starsRow.add(Box.createHorizontalStrut(8));
        starsRow.add(ratingText);
        form.add(starsRow);
        // FIX 2 — was 24 px, now 10 px (this was the main gap between stars and label)
        form.add(Box.createVerticalStrut(10));

        // ── "Your feedback" label ─────────────────────────────────
        JPanel feedbackLabelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        feedbackLabelRow.setOpaque(false);
        feedbackLabelRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel feedbackLabel = new JLabel("Your feedback");
        feedbackLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        feedbackLabel.setForeground(COLOR_TEXT);

        JLabel optionalLabel = new JLabel(" (optional)");
        optionalLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        optionalLabel.setForeground(COLOR_MUTED);

        feedbackLabelRow.add(feedbackLabel);
        feedbackLabelRow.add(optionalLabel);
        form.add(feedbackLabelRow);
        form.add(Box.createVerticalStrut(8));

        // ── FIX 3 — PLACEHOLDER TEXT ──────────────────────────────
        //
        // WHAT WAS WRONG:
        //   The JTextArea was wrapped inside a JScrollPane.
        //   When the customer clicked, the JScrollPane got the mouse event
        //   first.  By the time focus reached the JTextArea, the click had
        //   already been consumed, so focusGained() was often not triggered
        //   — meaning the grey placeholder text stayed on screen even while
        //   the customer was typing.
        //
        // HOW IT IS FIXED:
        //   1. We NO LONGER wrap the JTextArea in a JScrollPane.
        //      Instead, the JTextArea is added to the form directly.
        //   2. We add a MouseListener to the JTextArea itself.
        //      When mouseClicked() fires, we immediately:
        //        a. Request keyboard focus for the text area.
        //        b. Clear the placeholder text right away.
        //      This makes the placeholder disappear the instant the customer
        //      clicks, before they even start typing.
        //   3. The FocusListener is kept as a safety net for keyboard
        //      navigation (e.g. Tab key to reach the text area).
        // ─────────────────────────────────────────────────────────

        final String PLACEHOLDER = "Share your experience about this service...";

        // Create the text area — 8 visible rows gives it a taller look
        JTextArea feedbackArea = new JTextArea(8, 10);
        feedbackArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);

        // Show the grey placeholder text to start
        feedbackArea.setForeground(COLOR_MUTED);
        feedbackArea.setText(PLACEHOLDER);

        // Give the text area a visible border (replacing the JScrollPane border)
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)));

        // FocusListener: clears the placeholder when the area gains focus
        // (covers keyboard Tab navigation)
        feedbackArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                // Clear the placeholder if it has not been cleared yet
                if (feedbackArea.getText().equals(PLACEHOLDER)) {
                    feedbackArea.setText("");
                    feedbackArea.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                // Restore the placeholder if the customer left the box empty
                if (feedbackArea.getText().trim().isEmpty()) {
                    feedbackArea.setText(PLACEHOLDER);
                    feedbackArea.setForeground(COLOR_MUTED);
                }
            }
        });

        // MouseListener: clears the placeholder immediately on a mouse click.
        // This is the key fix — without this, the JScrollPane used to absorb
        // the click and the placeholder would stay visible.
        feedbackArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // 1. Give the text area keyboard focus
                feedbackArea.requestFocusInWindow();
                // 2. Wipe the placeholder right away (don't wait for focusGained)
                if (feedbackArea.getText().equals(PLACEHOLDER)) {
                    feedbackArea.setText("");
                    feedbackArea.setForeground(COLOR_TEXT);
                }
            }
        });

        // Make the text area fill the full width of the form
        feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        form.add(feedbackArea); // added directly — no JScrollPane wrapper

        // ── Character counter ─────────────────────────────────────
        JLabel counter = new JLabel("0 / 1500");
        counter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        counter.setForeground(COLOR_MUTED);
        counter.setHorizontalAlignment(SwingConstants.RIGHT);
        counter.setAlignmentX(Component.LEFT_ALIGNMENT);
        counter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        form.add(Box.createVerticalStrut(4));
        form.add(counter);

        // Update the counter each time the text changes
        feedbackArea.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                void update() {
                    String text = feedbackArea.getText();
                    // While the placeholder is showing, count stays 0
                    if (text.equals(PLACEHOLDER)) {
                        counter.setText("0 / 1500");
                        return;
                    }
                    int len = text.length();
                    counter.setText(len + " / 1500");
                    // Turn red when the limit is exceeded
                    counter.setForeground(len > 1500 ? new Color(220, 60, 60) : COLOR_MUTED);
                }
                @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { update(); }
                @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { update(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            }
        );

        form.add(Box.createVerticalStrut(14));

        // ── Cancel + Submit buttons ───────────────────────────────
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton cancelBtn = makeDialogButton("Cancel",          COLOR_CARD,  COLOR_TEXT,  COLOR_BORDER);
        JButton submitBtn = makeDialogButton("Submit feedback", BLUE_ACCENT, Color.WHITE, BLUE_ACCENT);

        cancelBtn.addActionListener(e -> dialog.dispose());

        submitBtn.addActionListener(e -> {
            // Must select at least one star
            if (selectedRating[0] == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a star rating before submitting.",
                        "Rating Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Get the typed text, treating the placeholder as empty
            String text = feedbackArea.getText().trim();
            if (text.equals(PLACEHOLDER)) text = "";

            // Must not exceed 1500 characters
            if (text.length() > 1500) {
                JOptionPane.showMessageDialog(dialog,
                        "Feedback exceeds 1500 characters. Please shorten it.",
                        "Too Long", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Map star count to a condition label
            String condition;
            if      (selectedRating[0] == 5) condition = "Excellent";
            else if (selectedRating[0] >= 3) condition = "Good";
            else                             condition = "Average";

            String today = LocalDate.now().toString();

            // Ask the service to append a line to feedback.txt
            boolean saved = service.saveFeedback(
                    loggedInUser.getUserId(),
                    appt.appointmentId,
                    appt.vehicleId,
                    appt.technicianId,
                    condition,
                    text.isEmpty() ? "No feedback provided." : text,
                    today
            );

            if (saved) {
                dialog.dispose();
                JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(MyFeedbackPage.this),
                        "Your feedback has been submitted successfully!",
                        "Feedback Submitted", JOptionPane.INFORMATION_MESSAGE);
                refresh(); // move the row from Pending → History
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Failed to save feedback. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(submitBtn);
        form.add(btnRow);

        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // updateStarDisplay() — fills stars yellow up to highlightCount
    // ─────────────────────────────────────────────────────────────
    private void updateStarDisplay(JLabel[] starLabels, int highlightCount, int selectedCount) {
        for (int i = 0; i < 5; i++) {
            if (i < highlightCount || i < selectedCount) {
                starLabels[i].setText("\u2605"); // ★ filled star
                starLabels[i].setForeground(YELLOW_STAR);
            } else {
                starLabels[i].setText("\u2606"); // ☆ hollow star
                starLabels[i].setForeground(new Color(200, 200, 210));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // makeFeedbackButton() — outlined blue button used in table rows
    // ─────────────────────────────────────────────────────────────
    private JButton makeFeedbackButton(String text) {
        JButton btn = new JButton(text) {
            // hov = true while the mouse is hovering over this button
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // Blue fill on hover, white fill when not hovering
                g2.setColor(hov ? BLUE_ACCENT : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BLUE_ACCENT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(BLUE_ACCENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); // hand cursor on the button
        btn.setPreferredSize(new Dimension(90, 30));
        // Also change text colour on hover
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.WHITE); }
            public void mouseExited (MouseEvent e) { btn.setForeground(BLUE_ACCENT); }
        });
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // makeDialogButton() — styled button for the popup footer
    // ─────────────────────────────────────────────────────────────
    private JButton makeDialogButton(String text, Color bg, Color fg, Color borderColor) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(text.length() > 8 ? 160 : 90, 36));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // makeRoundedCard() — white panel with rounded corners + border
    // ─────────────────────────────────────────────────────────────
    private JPanel makeRoundedCard() {
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
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }
}