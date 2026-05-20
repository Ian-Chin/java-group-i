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
import java.util.List;

public class MyFeedbackPage extends JPanel {

    // ── Colours ───────────────────────────────────────────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);
    private static final Color BLUE_ACCENT  = new Color(80, 110, 230);
    private static final Color YELLOW_STAR  = new Color(255, 193, 7);

    // ── Star label descriptions — shown beside the rating text in the popup ──
    // Index 0 = 1 star, index 4 = 5 stars
    private static final String[] STAR_LABELS = {
        "Unsatisfactory — Did not meet expectations",   // 1 star
        "Poor — Below expectations",                    // 2 stars
        "Average — Met basic expectations",             // 3 stars
        "Good — Satisfactory overall",                  // 4 stars
        "Excellent — Exceeds all expectations"          // 5 stars
    };

    // ── Service ───────────────────────────────────────────────────
    // All file I/O, searching, sorting, filtering, and row-building
    // logic lives in MyFeedbackService — this page only calls it.
    private final MyFeedbackService service = new MyFeedbackService();

    // ── Logged-in customer ────────────────────────────────────────
    private User loggedInUser;

    // ── Tab state ─────────────────────────────────────────────────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);
    private JButton pendingTabBtn;
    private JButton historyTabBtn;
    private String  activeTab = "PENDING";

    // ── Table models ──────────────────────────────────────────────
    private DefaultTableModel pendingTableModel;
    private DefaultTableModel historyTableModel;

    // ── Table references ──────────────────────────────────────────
    private JTable pendingTable;
    private JTable historyTable;

    // ── Full data lists (kept so search/sort can re-apply) ────────
    // These are loaded once per refresh(); the service's
    // filterAndSortPending() / filterAndSortHistory() then derive
    // the displayed subset from them on every search/sort change.
    private List<AppointmentRow> currentPending = new ArrayList<>();
    private List<MyFeedback>     currentHistory = new ArrayList<>();

    // ── Search / Sort controls ────────────────────────────────────
    private JTextField        pendingSearchField;
    private JComboBox<String> pendingSortCombo;
    private JTextField        historySearchField;
    private JComboBox<String> historySortCombo;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public MyFeedbackPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildTopRow(), BorderLayout.NORTH);

        // Two tab panels — each always shows their full layout
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
    // setUser() — update the logged-in user before refresh()
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh()
    // Delegates the actual file reading to:
    //   service.getCompletedAppointmentsWithoutFeedback()
    //   service.getFeedbackByCustomer()
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;

        if (customerId != null) {
            // ── Data loading — handled entirely by MyFeedbackService ──
            currentPending = service.getCompletedAppointmentsWithoutFeedback(customerId);
            currentHistory = service.getFeedbackByCustomer(customerId);
        } else {
            currentPending = new ArrayList<>();
            currentHistory = new ArrayList<>();
        }

        // Re-apply search/sort — this fills both tables
        // (tables will just be empty if the lists are empty)
        applyPendingSearchAndSort();
        applyHistorySearchAndSort();

        // Stay on the currently active tab
        cardLayout.show(switchPanel, activeTab);
    }

    // ═════════════════════════════════════════════════════════════
    // TOP ROW — subtitle + tab buttons
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
                // Fill blue when active, white when inactive
                g2.setColor(getClientProperty("active") == Boolean.TRUE
                        ? BLUE_ACCENT : COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Always draw blue border
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
    // SHARED SEARCH / SORT BAR BUILDER
    // ═════════════════════════════════════════════════════════════
    private JPanel buildSearchSortBar(JTextField searchField,
                                      JComboBox<String> sortCombo,
                                      String[] sortOptions,
                                      Runnable onChanged) {
        for (String option : sortOptions) {
            sortCombo.addItem(option);
        }

        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(310, 32));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(4, 10, 4, 10)));
        searchField.setToolTipText("Type to search...");

        // Grey placeholder text
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

        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                onChanged.run();
            }
        });

        sortCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sortCombo.setPreferredSize(new Dimension(310, 32));
        sortCombo.setBackground(COLOR_CARD);
        sortCombo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        sortCombo.addActionListener(e -> onChanged.run());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 10, 0));
        bar.add(searchField);
        bar.add(sortCombo);
        return bar;
    }

    // ═════════════════════════════════════════════════════════════
    // PENDING TAB — table always shown, empty rows when no data
    // ═════════════════════════════════════════════════════════════
    private JPanel buildPendingPanel() {
        return buildPendingDataCard();
    }

    // ─────────────────────────────────────────────────────────────
    // buildPendingDataCard() — search bar + pending appointments table
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPendingDataCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

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

        String[] columns = { "Appointment ID", "Service Type", "Date / Time", "Duration", "" };

        pendingTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 4; // only the Feedback button column is "editable" (clickable)
            }
            @Override
            public Class<?> getColumnClass(int col) {
                return col == 4 ? JButton.class : String.class;
            }
        };

        pendingTable = TableHelper.buildTable(pendingTableModel);
        pendingTable.setRowHeight(44);

        // ── HAND CURSOR ON HOVER ──────────────────────────────────
        // Cell renderers are NOT real components — they never receive
        // mouse events. We must listen to the TABLE for mouse movement
        // and call setCursor() on the table directly.
        pendingTable.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int col = pendingTable.columnAtPoint(e.getPoint());
                pendingTable.setCursor(col == 4
                        ? new Cursor(Cursor.HAND_CURSOR)
                        : new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });
        pendingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                pendingTable.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        // Centre renderer for text columns 0-3
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
        for (int i = 0; i < 4; i++) {
            pendingTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Renderer for column 4 — paints the Feedback button visually
        pendingTable.getColumnModel().getColumn(4).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setOpaque(true);
                wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                wrapper.add(makeFeedbackButton("Feedback"));
                return wrapper;
            }
        );

        // Editor for column 4 — handles actual button clicks
        pendingTable.getColumnModel().getColumn(4).setCellEditor(
            new DefaultCellEditor(new JCheckBox()) {
                @Override
                public Component getTableCellEditorComponent(JTable t, Object value,
                        boolean isSelected, int row, int col) {
                    JButton btn = makeFeedbackButton("Feedback");
                    btn.addActionListener(e -> {
                        fireEditingStopped();
                        // Match the row's appointment ID back to the original data object
                        // (important when search/sort has changed row order)
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
                    wrapper.add(btn);
                    return wrapper;
                }
                @Override
                public Object getCellEditorValue() { return "Feedback"; }
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
    //
    // Reads the current search text and sort selection, delegates
    // the actual filtering/sorting to:
    //   service.filterAndSortPending()
    // then fills the table via fillPendingTable().
    // ─────────────────────────────────────────────────────────────
    private void applyPendingSearchAndSort() {
        if (pendingSearchField == null || pendingSortCombo == null) return;

        String raw   = pendingSearchField.getText().trim();
        String query = (raw.equals("Search...") || raw.isEmpty()) ? "" : raw.toLowerCase();
        String sortOption = (String) pendingSortCombo.getSelectedItem();

        // ── Search + Sort logic — handled by MyFeedbackService ────
        List<AppointmentRow> filtered =
                service.filterAndSortPending(currentPending, query, sortOption);

        fillPendingTable(filtered);
    }

    // ─────────────────────────────────────────────────────────────
    // fillPendingTable()
    //
    // Clears the pending table model and repopulates it.
    // Row data is built by service.buildPendingTableRows() so that
    // no model-to-display mapping logic lives in this file.
    // ─────────────────────────────────────────────────────────────
    private void fillPendingTable(List<AppointmentRow> pending) {
        pendingTableModel.setRowCount(0);
        // ── Row building — handled by MyFeedbackService ───────────
        Object[][] rows = service.buildPendingTableRows(pending);
        for (Object[] row : rows) {
            pendingTableModel.addRow(row);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // HISTORY TAB — table always shown, empty rows when no data
    // ═════════════════════════════════════════════════════════════
    private JPanel buildHistoryPanel() {
        return buildHistoryDataCard();
    }

    // ─────────────────────────────────────────────────────────────
    // buildHistoryDataCard() — search bar + feedback history table
    //
    // Filter options now include all five condition levels.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildHistoryDataCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        historySearchField = new JTextField();
        historySortCombo   = new JComboBox<>();
        String[] historySortOptions = {
            "Default Order",
            "Sort by Date (Newest First)",
            "Sort by Date (Oldest First)",
            "Filter: Excellent Only",
            "Filter: Good Only",
            "Filter: Average Only",
            "Filter: Poor Only",
            "Filter: Unsatisfactory Only",
            "Sort by Rating (Excellent → Unsatisfactory)",
            "Sort by Rating (Unsatisfactory → Excellent)",
            "Sort by Technician (A-Z)"
        };
        card.add(
            buildSearchSortBar(historySearchField, historySortCombo,
                               historySortOptions, this::applyHistorySearchAndSort),
            BorderLayout.NORTH
        );

        String[] columns = {
            "Feedback ID", "Appointment ID", "Vehicle Type",
            "Car Plate", "Technician", "Condition", "Feedback", "Date"
        };

        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        historyTable = TableHelper.buildTable(historyTableModel);
        historyTable.setRowHeight(40);

        // Centre renderer for all columns except Condition (5) and Feedback (6)
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
        for (int i = 0; i < columns.length; i++) {
            if (i != 5 && i != 6) {
                historyTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Condition column (5) — coloured badge
        // Five levels aligned with convertStarsToCondition():
        //   Excellent      → green
        //   Good           → blue
        //   Average        → amber / orange
        //   Poor           → orange-red
        //   Unsatisfactory → red
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
                    case "poor":
                        badge.setBackground(new Color(255, 230, 215));
                        badge.setForeground(new Color(200, 70, 20));
                        break;
                    case "unsatisfactory":
                        badge.setBackground(new Color(255, 218, 218));
                        badge.setForeground(new Color(180, 30, 30));
                        break;
                    default:
                        badge.setBackground(new Color(235, 236, 240));
                        badge.setForeground(COLOR_MUTED);
                }

                JPanel wrapper = new JPanel(new GridBagLayout());
                wrapper.setOpaque(true);
                wrapper.setBackground(isSelected
                        ? new Color(80, 110, 230, 60)
                        : (row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253)));
                wrapper.add(badge);
                return wrapper;
            }
        );

        // Feedback column (6) — wrapping text area
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

                // Auto-resize row height
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

        TableColumnModel colModel = historyTable.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(90);
        colModel.getColumn(1).setPreferredWidth(110);
        colModel.getColumn(2).setPreferredWidth(90);
        colModel.getColumn(3).setPreferredWidth(90);
        colModel.getColumn(4).setPreferredWidth(110);
        colModel.getColumn(5).setPreferredWidth(130); // wide enough for "Unsatisfactory"
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
    //
    // Reads the current search text and sort selection, delegates
    // the actual filtering/sorting to:
    //   service.filterAndSortHistory()
    // then fills the table via fillHistoryTable().
    // ─────────────────────────────────────────────────────────────
    private void applyHistorySearchAndSort() {
        if (historySearchField == null || historySortCombo == null) return;

        String raw   = historySearchField.getText().trim();
        String query = (raw.equals("Search...") || raw.isEmpty()) ? "" : raw.toLowerCase();
        String sortOption = (String) historySortCombo.getSelectedItem();

        // ── Search + Sort + Filter logic — handled by MyFeedbackService ──
        List<MyFeedback> filtered =
                service.filterAndSortHistory(currentHistory, query, sortOption);

        fillHistoryTable(filtered);
    }

    // ─────────────────────────────────────────────────────────────
    // fillHistoryTable()
    //
    // Clears the history table model and repopulates it.
    // Row data is built by service.buildHistoryTableRows() so that
    // no model-to-display mapping logic lives in this file.
    // ─────────────────────────────────────────────────────────────
    private void fillHistoryTable(List<MyFeedback> feedbackList) {
        historyTableModel.setRowCount(0);
        // ── Row building — handled by MyFeedbackService ───────────
        Object[][] rows = service.buildHistoryTableRows(feedbackList);
        for (Object[] row : rows) {
            historyTableModel.addRow(row);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // FEEDBACK POPUP — opened when the customer clicks "Feedback"
    // ═════════════════════════════════════════════════════════════
    private void showFeedbackPopup(AppointmentRow appt) {

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Submit Feedback", true);
        dialog.setSize(720, 520);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(20, 32, 20, 32));

        // Info bar at top of popup
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
        form.add(Box.createVerticalStrut(14));

        // Star rating question
        JLabel ratingQuestion = new JLabel("Service Rating");
        ratingQuestion.setFont(new Font("SansSerif", Font.BOLD, 14));
        ratingQuestion.setForeground(COLOR_TEXT);
        ratingQuestion.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(ratingQuestion);
        form.add(Box.createVerticalStrut(2));

        // Instruction line beneath the heading
        JLabel ratingInstruction = new JLabel(
                "Select a star rating that best reflects your experience.");
        ratingInstruction.setFont(new Font("SansSerif", Font.PLAIN, 12));
        ratingInstruction.setForeground(COLOR_MUTED);
        ratingInstruction.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(ratingInstruction);
        form.add(Box.createVerticalStrut(8));

        // Star rating row + formal label
        int[] selectedRating = { 0 };

        JPanel starsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        starsRow.setOpaque(false);
        starsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        starsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel[] starLabels = new JLabel[5];

        // Formal rating label shown beside the stars
        JLabel ratingText = new JLabel("No rating selected");
        ratingText.setFont(new Font("SansSerif", Font.ITALIC, 13));
        ratingText.setForeground(COLOR_MUTED);

        for (int i = 0; i < 5; i++) {
            final int starIndex = i + 1;
            JLabel star = new JLabel("\u2606"); // ☆ hollow star
            star.setFont(new Font("SansSerif", Font.PLAIN, 30));
            star.setForeground(new Color(200, 200, 210));
            star.setCursor(new Cursor(Cursor.HAND_CURSOR));
            starLabels[i] = star;

            star.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    updateStarDisplay(starLabels, starIndex, selectedRating[0]);
                    // Show preview label on hover
                    ratingText.setText(STAR_LABELS[starIndex - 1]);
                    ratingText.setForeground(COLOR_TEXT);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    updateStarDisplay(starLabels, selectedRating[0], selectedRating[0]);
                    // Restore the confirmed label (or placeholder) after hover
                    if (selectedRating[0] == 0) {
                        ratingText.setText("No rating selected");
                        ratingText.setForeground(COLOR_MUTED);
                    } else {
                        ratingText.setText(STAR_LABELS[selectedRating[0] - 1]);
                        ratingText.setForeground(COLOR_TEXT);
                    }
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedRating[0] = starIndex;
                    updateStarDisplay(starLabels, starIndex, starIndex);
                    ratingText.setText(STAR_LABELS[starIndex - 1]);
                    ratingText.setForeground(COLOR_TEXT);
                }
            });

            starsRow.add(star);
        }
        starsRow.add(Box.createHorizontalStrut(10));
        // Fixed preferred width large enough for the longest label
        // "Unsatisfactory — Did not meet expectations" so it is never clipped
        ratingText.setPreferredSize(new Dimension(340, 22));
        starsRow.add(ratingText);
        form.add(starsRow);
        form.add(Box.createVerticalStrut(14));

        // Feedback text area label
        JPanel feedbackLabelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        feedbackLabelRow.setOpaque(false);
        feedbackLabelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel feedbackLabel = new JLabel("Additional Comments");
        feedbackLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        feedbackLabel.setForeground(COLOR_TEXT);
        JLabel optionalLabel = new JLabel(" (optional)");
        optionalLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        optionalLabel.setForeground(COLOR_MUTED);
        feedbackLabelRow.add(feedbackLabel);
        feedbackLabelRow.add(optionalLabel);
        form.add(feedbackLabelRow);
        form.add(Box.createVerticalStrut(4));

        // Feedback text area
        final String PLACEHOLDER = "Please describe your experience in detail...";
        JTextArea feedbackArea = new JTextArea(8, 10);
        feedbackArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setForeground(COLOR_MUTED);
        feedbackArea.setText(PLACEHOLDER);
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(10, 12, 10, 12)));

        feedbackArea.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (feedbackArea.getText().equals(PLACEHOLDER)) {
                    feedbackArea.setText("");
                    feedbackArea.setForeground(COLOR_TEXT);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (feedbackArea.getText().trim().isEmpty()) {
                    feedbackArea.setText(PLACEHOLDER);
                    feedbackArea.setForeground(COLOR_MUTED);
                }
            }
        });

        feedbackArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                feedbackArea.requestFocusInWindow();
                if (feedbackArea.getText().equals(PLACEHOLDER)) {
                    feedbackArea.setText("");
                    feedbackArea.setForeground(COLOR_TEXT);
                }
            }
        });

        feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));
        form.add(feedbackArea);

        // Character counter
        final int MAX_CHARS = 500;
        JLabel counter = new JLabel("0 / " + MAX_CHARS);
        counter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        counter.setForeground(COLOR_MUTED);
        counter.setHorizontalAlignment(SwingConstants.RIGHT);
        counter.setAlignmentX(Component.LEFT_ALIGNMENT);
        counter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        form.add(Box.createVerticalStrut(3));
        form.add(counter);

        // Document filter — blocks input beyond MAX_CHARS
        ((javax.swing.text.AbstractDocument) feedbackArea.getDocument())
            .setDocumentFilter(new javax.swing.text.DocumentFilter() {
                @Override
                public void insertString(FilterBypass fb, int offset,
                                         String insertedText,
                                         javax.swing.text.AttributeSet attr)
                        throws javax.swing.text.BadLocationException {
                    String current = feedbackArea.getText();
                    int currentLen = current.equals(PLACEHOLDER) ? 0 : current.length();
                    if (currentLen + insertedText.length() <= MAX_CHARS) {
                        super.insertString(fb, offset, insertedText, attr);
                    }
                }

                @Override
                public void replace(FilterBypass fb, int offset, int length,
                                    String insertedText,
                                    javax.swing.text.AttributeSet attr)
                        throws javax.swing.text.BadLocationException {
                    String current = feedbackArea.getText();
                    int currentLen = current.equals(PLACEHOLDER) ? 0 : current.length();
                    int resultLen  = currentLen - length
                            + (insertedText == null ? 0 : insertedText.length());
                    if (resultLen <= MAX_CHARS) {
                        super.replace(fb, offset, length, insertedText, attr);
                    } else {
                        int available = MAX_CHARS - (currentLen - length);
                        if (available > 0 && insertedText != null) {
                            super.replace(fb, offset, length,
                                    insertedText.substring(0, available), attr);
                        }
                    }
                }

                @Override
                public void remove(FilterBypass fb, int offset, int length)
                        throws javax.swing.text.BadLocationException {
                    super.remove(fb, offset, length);
                }
            });

        // Update character counter label on every document change
        feedbackArea.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                void update() {
                    String text = feedbackArea.getText();
                    if (text.equals(PLACEHOLDER)) {
                        counter.setText("0 / " + MAX_CHARS);
                        counter.setForeground(COLOR_MUTED);
                        return;
                    }
                    int len = text.length();
                    counter.setText(len + " / " + MAX_CHARS);
                    if      (len >= MAX_CHARS)      counter.setForeground(new Color(220, 60, 60));
                    else if (len >= MAX_CHARS - 50) counter.setForeground(new Color(200, 100, 0));
                    else                            counter.setForeground(COLOR_MUTED);
                }
                @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { update(); }
                @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { update(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
            }
        );

        form.add(Box.createVerticalStrut(14));

        // Cancel + Submit buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JButton cancelBtn = makeDialogButton("Cancel",          COLOR_CARD,  COLOR_TEXT,  COLOR_BORDER);
        JButton submitBtn = makeDialogButton("Submit Feedback", BLUE_ACCENT, Color.WHITE, BLUE_ACCENT);

        cancelBtn.addActionListener(e -> dialog.dispose());

        submitBtn.addActionListener(e -> {
            if (selectedRating[0] == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a star rating before submitting.",
                        "Rating Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String text = feedbackArea.getText().trim();
            if (text.equals(PLACEHOLDER)) text = "";

            if (text.length() > MAX_CHARS) {
                JOptionPane.showMessageDialog(dialog,
                        "Feedback exceeds " + MAX_CHARS + " characters. Please shorten it.",
                        "Too Long", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // ── Convert star rating to condition label ─────────────
            String condition = service.convertStarsToCondition(selectedRating[0]);

            String today = LocalDate.now().toString();

            // ── Save feedback — handled by MyFeedbackService ───────
            boolean saved = service.saveFeedback(
                    loggedInUser.getUserId(),
                    appt.appointmentId,
                    appt.vehicleId,
                    appt.technicianId,
                    condition,
                    text.isEmpty() ? "No comments provided." : text,
                    today
            );

            if (saved) {
                dialog.dispose();
                JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(MyFeedbackPage.this),
                        "Your feedback has been submitted successfully. Thank you.",
                        "Feedback Submitted", JOptionPane.INFORMATION_MESSAGE);
                refresh(); // move the row from Pending → History
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "An error occurred while saving your feedback. Please try again.",
                        "Submission Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRow.add(cancelBtn);
        btnRow.add(submitBtn);
        form.add(btnRow);

        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // updateStarDisplay() — highlights stars yellow up to count
    // ─────────────────────────────────────────────────────────────
    private void updateStarDisplay(JLabel[] starLabels, int highlightCount,
                                    int selectedCount) {
        for (int i = 0; i < 5; i++) {
            if (i < highlightCount || i < selectedCount) {
                starLabels[i].setText("\u2605"); // ★ filled
                starLabels[i].setForeground(YELLOW_STAR);
            } else {
                starLabels[i].setText("\u2606"); // ☆ hollow
                starLabels[i].setForeground(new Color(200, 200, 210));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // makeFeedbackButton() — outlined blue button for the table
    // ─────────────────────────────────────────────────────────────
    private JButton makeFeedbackButton(String text) {
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(90, 30));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.WHITE); }
            public void mouseExited (MouseEvent e) { btn.setForeground(BLUE_ACCENT); }
        });
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // makeDialogButton() — styled button used in the feedback popup
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