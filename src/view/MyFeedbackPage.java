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
import java.util.List;

/**
 * MyFeedbackPage
 *
 * Layout:
 *   - Subtitle row on the left + two tab toggle buttons on the right
 *   - "Pending Feedback" tab: table with Appointment ID, Service Type,
 *     Date/Time, Duration, and a Feedback button per row
 *   - "Feedback History" tab: table with Feedback ID, Appointment ID,
 *     Vehicle Type, Car Plate, Technician, Condition (coloured badge),
 *     Feedback (wrapping text), Date
 *   - Both tabs show an empty-state card when there is no data
 *
 * Fixes applied vs previous version:
 *   1. Small gap (6 px) added between the two tab buttons
 *   2. Condition badge is vertically centred in its cell (GridBagLayout wrapper)
 *   3. Feedback History tab shows the shared empty-state panel when
 *      no history records are found (same design as PaymentHistoryPage)
 */
public class MyFeedbackPage extends JPanel {

    // ── Colours — identical to PaymentHistoryPage / StaffReviewPage ──
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);
    private static final Color BLUE_ACCENT  = new Color(80, 110, 230);
    private static final Color YELLOW_STAR  = new Color(255, 193, 7);

    // ── Service that reads / writes feedback.txt ──────────────────
    private final MyFeedbackService service = new MyFeedbackService();

    // ── Logged-in customer ────────────────────────────────────────
    private User loggedInUser;

    // ── CardLayout switches between tab content panels ────────────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Tab button references so we can restyle them ──────────────
    private JButton pendingTabBtn;
    private JButton historyTabBtn;
    private String  activeTab = "PENDING"; // "PENDING" or "HISTORY"

    // ── Table models — filled in refresh() ───────────────────────
    private DefaultTableModel pendingTableModel;
    private DefaultTableModel historyTableModel;

    // ── Pending table reference — needed for the button column ────
    private JTable pendingTable;

    // ── The currently loaded pending appointments ─────────────────
    // Kept as a field so the CellEditor can look up the right row.
    private List<AppointmentRow> currentPending = new java.util.ArrayList<>();

    // ── Inner CardLayouts for each tab (data vs empty state) ──────
    private final CardLayout pendingCardLayout = new CardLayout();
    private final JPanel     pendingSwitch     = new JPanel(pendingCardLayout);

    private final CardLayout historyCardLayout = new CardLayout();
    private final JPanel     historySwitch     = new JPanel(historyCardLayout);

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────
    public MyFeedbackPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // Outer content panel with padding matching PaymentHistoryPage
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Top row: subtitle on left, tab buttons on right
        pageContent.add(buildTopRow(), BorderLayout.NORTH);

        // Switch panel holds the two tab content panels
        switchPanel.setOpaque(false);
        switchPanel.add(buildPendingPanel(), "PENDING");
        switchPanel.add(buildHistoryPanel(), "HISTORY");
        pageContent.add(switchPanel, BorderLayout.CENTER);

        // Outer scroll so the whole page scrolls on small windows
        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);

        // Start on the Pending tab
        cardLayout.show(switchPanel, "PENDING");
    }

    // ─────────────────────────────────────────────────────────────
    // setUser() — call in CustomerDashboard.refreshUser() before refresh()
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh() — called every time the sidebar "My Feedback" is clicked
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;
        System.out.println("[MyFeedbackPage] refresh() — customer: " + customerId);

        // Load pending (completed appointments with no feedback yet)
        if (customerId != null) {
            currentPending = service.getCompletedAppointmentsWithoutFeedback(customerId);
        } else {
            currentPending = new java.util.ArrayList<>();
        }

        // Load submitted feedback history
        List<MyFeedback> history;
        if (customerId != null) {
            history = service.getFeedbackByCustomer(customerId);
        } else {
            history = new java.util.ArrayList<>();
        }

        // Fill both tables
        fillPendingTable(currentPending);
        fillHistoryTable(history);

        // Show the correct inner state for each tab
        pendingCardLayout.show(pendingSwitch, currentPending.isEmpty() ? "EMPTY" : "DATA");
        historyCardLayout.show(historySwitch, history.isEmpty()        ? "EMPTY" : "DATA");

        // Restore whichever tab was active before
        cardLayout.show(switchPanel, activeTab);
    }

    // ═════════════════════════════════════════════════════════════
    // TOP ROW — subtitle + tab toggle buttons
    // ═════════════════════════════════════════════════════════════
    private JPanel buildTopRow() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(0, 0, 18, 0));

        // Subtitle on the left
        JLabel subtitle = new JLabel(
                "Completed appointments are shown below. Click \"Feedback\" to rate your experience.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        top.add(subtitle, BorderLayout.WEST);

        // FIX 1: hgap=6 gives a small visible gap between the two buttons
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
                // Active = solid blue; inactive = white with blue border
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
    // switchTab() — highlights the correct button and shows the card
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
    // PENDING TAB
    // ═════════════════════════════════════════════════════════════

    private JPanel buildPendingPanel() {
        pendingSwitch.setOpaque(false);
        pendingSwitch.add(buildPendingDataCard(),  "DATA");
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

        String[] columns = {
                "Appointment ID", "Service Type", "Date / Time", "Duration", ""
        };

        pendingTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return col == 4; }
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

        // Button renderer — draws the Feedback button in every row
        pendingTable.getColumnModel().getColumn(4).setCellRenderer(
                (t, value, isSelected, hasFocus, row, col) -> {
                    // GridBagLayout centres the button both H and V inside the cell
                    JPanel wrapper = new JPanel(new GridBagLayout());
                    wrapper.setOpaque(true);
                    wrapper.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                    wrapper.add(makeFeedbackButton("Feedback"));
                    return wrapper;
                }
        );

        // Button editor — clicking the cell actually opens the popup
        pendingTable.getColumnModel().getColumn(4).setCellEditor(
                new DefaultCellEditor(new JCheckBox()) {
                    @Override
                    public Component getTableCellEditorComponent(JTable t, Object value,
                            boolean isSelected, int row, int col) {
                        JButton btn = makeFeedbackButton("Feedback");
                        btn.addActionListener(e -> {
                            fireEditingStopped();
                            if (row < currentPending.size()) {
                                showFeedbackPopup(currentPending.get(row));
                            }
                        });
                        JPanel wrapper = new JPanel(new GridBagLayout());
                        wrapper.setOpaque(true);
                        wrapper.setBackground(Color.WHITE);
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
    // buildPendingEmptyPanel() — no pending appointments state
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
        pendingTableModel.setRowCount(0);
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
        historySwitch.add(buildHistoryDataCard(),  "DATA");
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

        String[] columns = {
                "Feedback ID", "Appointment ID", "Vehicle Type",
                "Car Plate", "Technician", "Condition", "Feedback", "Date"
        };

        historyTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable historyTable = TableHelper.buildTable(historyTableModel);
        historyTable.setRowHeight(40);

        // Centre renderer — all columns except Condition (5) and Feedback (6)
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

        // FIX 2: Condition column (index 5) — GridBagLayout wrapper centres
        // the badge both horizontally AND vertically inside the cell.
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

                    // GridBagLayout with no constraints centres the badge
                    // perfectly both horizontally AND vertically — this is
                    // the fix for the "badge stuck to the top" problem.
                    JPanel wrapper = new JPanel(new GridBagLayout());
                    wrapper.setOpaque(true);
                    wrapper.setBackground(
                            isSelected
                                    ? new Color(80, 110, 230, 60)
                                    : (row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253)));
                    wrapper.add(badge); // no GridBagConstraints = auto-centred
                    return wrapper;
                }
        );

        // Feedback column (index 6) — wrapping multi-line text
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
    // buildHistoryEmptyPanel()
    // FIX 3: Uses the same shared helper as PaymentHistoryPage so the
    // empty state has exactly the same size, position, and style.
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
                true
        );
        dialog.setSize(520, 420);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(22, 28, 22, 28));

        // Grey info bar at the top — matches screenshot exactly
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
        form.add(Box.createVerticalStrut(20));

        // "How would you rate this service?" heading
        JLabel ratingQuestion = new JLabel("How would you rate this service?");
        ratingQuestion.setFont(new Font("SansSerif", Font.BOLD, 14));
        ratingQuestion.setForeground(COLOR_TEXT);
        ratingQuestion.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(ratingQuestion);
        form.add(Box.createVerticalStrut(10));

        // Star rating row
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
            JLabel star = new JLabel("\u2606"); // ☆
            star.setFont(new Font("SansSerif", Font.PLAIN, 28));
            star.setForeground(new Color(200, 200, 210));
            star.setCursor(new Cursor(Cursor.HAND_CURSOR));
            starLabels[i] = star;

            star.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    updateStarDisplay(starLabels, starIndex, selectedRating[0]);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    updateStarDisplay(starLabels, selectedRating[0], selectedRating[0]);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
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
        form.add(Box.createVerticalStrut(20));

        // "Your feedback (optional)" label row
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

        // Placeholder text constant
        final String PLACEHOLDER = "Share your experience about this service...";

        JTextArea feedbackArea = new JTextArea();
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
                if (feedbackArea.getText().isEmpty()) {
                    feedbackArea.setText(PLACEHOLDER);
                    feedbackArea.setForeground(COLOR_MUTED);
                }
            }
        });

        JScrollPane areaScroll = new JScrollPane(feedbackArea);
        areaScroll.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        areaScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 110));
        areaScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        areaScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        form.add(areaScroll);

        // Character counter
        JLabel counter = new JLabel("0 / 1500");
        counter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        counter.setForeground(COLOR_MUTED);
        counter.setHorizontalAlignment(SwingConstants.RIGHT);
        counter.setAlignmentX(Component.LEFT_ALIGNMENT);
        counter.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        form.add(Box.createVerticalStrut(4));
        form.add(counter);

        feedbackArea.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
                    void update() {
                        String text = feedbackArea.getText();
                        if (text.equals(PLACEHOLDER)) { counter.setText("0 / 1500"); return; }
                        int len = text.length();
                        counter.setText(len + " / 1500");
                        counter.setForeground(len > 1500 ? new Color(220, 60, 60) : COLOR_MUTED);
                    }
                    @Override public void insertUpdate (javax.swing.event.DocumentEvent e) { update(); }
                    @Override public void removeUpdate (javax.swing.event.DocumentEvent e) { update(); }
                    @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { update(); }
                }
        );

        form.add(Box.createVerticalStrut(16));

        // Cancel + Submit buttons
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JButton cancelBtn = makeDialogButton("Cancel",           COLOR_CARD,  COLOR_TEXT,  COLOR_BORDER);
        JButton submitBtn = makeDialogButton("Submit feedback",  BLUE_ACCENT, Color.WHITE, BLUE_ACCENT);

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

            if (text.length() > 1500) {
                JOptionPane.showMessageDialog(dialog,
                        "Feedback exceeds 1500 characters. Please shorten it.",
                        "Too Long", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String condition;
            if      (selectedRating[0] == 5) condition = "Excellent";
            else if (selectedRating[0] >= 3) condition = "Good";
            else                             condition = "Average";

            String today = LocalDate.now().toString();

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
                // Refresh so the row moves from Pending → History
                refresh();
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
    // updateStarDisplay()
    // ─────────────────────────────────────────────────────────────
    private void updateStarDisplay(JLabel[] starLabels, int highlightCount, int selectedCount) {
        for (int i = 0; i < 5; i++) {
            if (i < highlightCount || i < selectedCount) {
                starLabels[i].setText("\u2605"); // ★ filled + yellow
                starLabels[i].setForeground(YELLOW_STAR);
            } else {
                starLabels[i].setText("\u2606"); // ☆ empty + grey
                starLabels[i].setForeground(new Color(200, 200, 210));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // makeFeedbackButton() — white/blue outlined row button
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
    // makeDialogButton() — styled button for popup footer
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
    // makeRoundedCard() — white rounded card with a light border
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