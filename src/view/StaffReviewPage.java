package view;

import model.StaffReviewService;
import model.StaffReviewService.StaffReview;
import model.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class StaffReviewPage extends JPanel {

    // ── Colours ───────────────────────────────────────────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);
    private static final Color BLUE_ACCENT  = new Color(80, 110, 230);
    private static final Color YELLOW_STAR  = new Color(255, 193, 7);

    // Left bar colours for the two summary cards
    private static final Color BAR_COLOR_AVERAGE   = YELLOW_STAR;   // soft blue
    private static final Color BAR_COLOR_BREAKDOWN = BLUE_ACCENT;   // calm teal

    // ── Service + logged-in user ───────────────────────────────────
    private final StaffReviewService service = new StaffReviewService();
    private User loggedInUser;

    // ── All reviews loaded by refresh() ──────────────────────────
    private List<StaffReview> allReviews = new ArrayList<>();

    // ── Average card labels — updated by refresh() ────────────────
    private JLabel avgNumberLabel;
    private JLabel avgStarsLabel;
    private JLabel avgCountLabel;

    // ── Breakdown bars panel — rebuilt by fillBreakdownBars() ─────
    private JPanel barsPanel;

    // ── Table ─────────────────────────────────────────────────────
    private DefaultTableModel tableModel;
    private JTable            table;

    // ── Search + filter controls ──────────────────────────────────
    private JTextField        searchField;
    private JComboBox<String> ratingFilter;

    // ── Sort state ────────────────────────────────────────────────
    private int     sortColumnIndex = -1;  // -1 = no sort
    private boolean sortAscending   = true;

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public StaffReviewPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildSubtitleRow(), BorderLayout.NORTH);

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
    // setUser() — update the user before calling refresh()
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh() — reloads data and updates all UI components
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;

        // ── Data loading — delegated to StaffReviewService ────────
        if (customerId != null) {
            allReviews = service.getReviewsByCustomer(customerId);
        } else {
            allReviews = new ArrayList<>();
        }

        // Always update the summary cards
        updateAverageCard(allReviews);
        fillBreakdownBars(allReviews);

        // Reset search / filter / sort back to defaults
        if (searchField  != null) searchField.setText("");
        if (ratingFilter != null) ratingFilter.setSelectedIndex(0);
        sortColumnIndex = -1;
        sortAscending   = true;

        // Fill table — will be empty if allReviews is empty
        fillTable(allReviews);
    }

    // ─────────────────────────────────────────────────────────────
    // buildSubtitleRow() — muted description under the page title
    // ─────────────────────────────────────────────────────────────
    private JPanel buildSubtitleRow() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel subtitle = new JLabel(
                "Reviews and comments submitted by the service technicians.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);

        return header;
    }

    // ─────────────────────────────────────────────────────────────
    // buildDataPanel()
    //   NORTH  — two summary cards side by side
    //   CENTER — table card (search/filter + table)
    // ─────────────────────────────────────────────────────────────
    private JPanel buildDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        JPanel topRow = buildTopCardsRow();
        topRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        panel.add(topRow, BorderLayout.NORTH);

        panel.add(buildTableCard(), BorderLayout.CENTER);

        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTopCardsRow() — average card (left) + breakdown (right)
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTopCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(buildAverageCard());
        row.add(buildBreakdownCard());
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildAverageCard() — shows big rating number + yellow stars
    // ─────────────────────────────────────────────────────────────
    private JPanel buildAverageCard() {
        JPanel card = makeRoundedCardWithLeftBar(BAR_COLOR_AVERAGE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        // Big blue number — starts at "0.0" until refresh() loads real data
        avgNumberLabel = new JLabel("0.0");
        avgNumberLabel.setFont(new Font("SansSerif", Font.BOLD, 52));
        avgNumberLabel.setForeground(BLUE_ACCENT);
        avgNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Star row — starts as 5 empty stars
        avgStarsLabel = new JLabel("\u2606\u2606\u2606\u2606\u2606");
        avgStarsLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        avgStarsLabel.setForeground(YELLOW_STAR);
        avgStarsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Count text
        avgCountLabel = new JLabel("average rating from 0 reviews");
        avgCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        avgCountLabel.setForeground(COLOR_MUTED);
        avgCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        inner.add(avgNumberLabel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(avgStarsLabel);
        inner.add(Box.createVerticalStrut(6));
        inner.add(avgCountLabel);

        content.add(inner);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // updateAverageCard() — fills labels with real or zero values.
    //
    // Calls service.calculateAverageRating() and
    // service.buildStarString() — no calculation logic here.
    // ─────────────────────────────────────────────────────────────
    private void updateAverageCard(List<StaffReview> reviews) {
        if (reviews.isEmpty()) {
            // No reviews — show 0.0 and empty stars
            avgNumberLabel.setText("0.0");
            avgStarsLabel.setText("\u2606\u2606\u2606\u2606\u2606");
            avgCountLabel.setText("average rating from 0 reviews");
        } else {
            // ── Delegated to StaffReviewService ──────────────────
            double avg   = service.calculateAverageRating(reviews);
            int    total = reviews.size();
            avgNumberLabel.setText(String.format("%.1f", avg));
            avgStarsLabel.setText(service.buildStarString(avg)); // <-- moved to service
            String word = (total == 1) ? "review" : "reviews";
            avgCountLabel.setText("average rating from " + total + " " + word);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // buildBreakdownCard() — 5-star bar chart card
    // ─────────────────────────────────────────────────────────────
    private JPanel buildBreakdownCard() {
        JPanel card = makeRoundedCardWithLeftBar(BAR_COLOR_BREAKDOWN);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);

        JLabel heading = new JLabel("Rating breakdown");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(COLOR_TEXT);
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        content.add(heading, BorderLayout.NORTH);

        barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setOpaque(false);

        // Placeholder bars — all zero until refresh() runs
        for (int star = 5; star >= 1; star--) {
            barsPanel.add(buildOneBarRow(star, 0, 1));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        content.add(barsPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // fillBreakdownBars() — rebuilds the 5 bar rows with real counts.
    //
    // Calls service.countByStarLevel() — no counting logic here.
    // ─────────────────────────────────────────────────────────────
    private void fillBreakdownBars(List<StaffReview> reviews) {
        if (barsPanel == null) return;

        barsPanel.removeAll();

        int total = reviews.size(); // 0 when no data

        for (int star = 5; star >= 1; star--) {
            // ── Delegated to StaffReviewService ──────────────────
            int count = reviews.isEmpty() ? 0 : service.countByStarLevel(reviews, star);
            barsPanel.add(buildOneBarRow(star, count, Math.max(total, 1)));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        barsPanel.revalidate();
        barsPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // buildOneBarRow() — "5★  [===bar===]  1"
    // ─────────────────────────────────────────────────────────────
    private JPanel buildOneBarRow(int star, int count, int max) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel starLabel = new JLabel(star + "\u2605");
        starLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        starLabel.setForeground(COLOR_TEXT);
        starLabel.setPreferredSize(new Dimension(28, 16));
        row.add(starLabel, BorderLayout.WEST);

        JPanel track = new JPanel(new BorderLayout());
        track.setBackground(new Color(230, 230, 235));
        track.setBorder(new EmptyBorder(4, 0, 4, 0));

        JPanel fill = new JPanel();
        fill.setBackground(BLUE_ACCENT);
        double ratio = (max == 0) ? 0.0 : (double) count / max;
        fill.setPreferredSize(new Dimension((int) (200 * ratio), 8));
        track.add(fill, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);

        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(COLOR_MUTED);
        countLabel.setPreferredSize(new Dimension(20, 16));
        row.add(countLabel, BorderLayout.EAST);

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTableCard() — contains inline controls + the review table
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        String[] columns = {
            "Comment ID", "Service History ID", "Technician Name",
            "Appointment ID", "Vehicle Type", "Car Plate",
            "Rating", "Feedback", "Date"
        };

        // Add search + filter controls above the table
        card.add(buildInlineControlsRow(), BorderLayout.NORTH);

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = TableHelper.buildTable(tableModel);
        table.setRowHeight(40);

        // Click column header to sort
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedColumn = table.columnAtPoint(e.getPoint());
                if (clickedColumn < 0) return;

                if (sortColumnIndex == clickedColumn) {
                    sortAscending = !sortAscending; // flip direction
                } else {
                    sortColumnIndex = clickedColumn;
                    sortAscending   = true;
                }

                applyFilters();
                updateColumnHeaderArrows(columns);
            }
        });

        // Centre renderer for all columns except Feedback (index 7)
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
            if (i != 7) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Feedback column (index 7) — wrapping multi-line text area
        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, isSelected, hasFocus, row, col) -> {
                JTextArea ta = new JTextArea();
                ta.setText(value != null ? value.toString() : "");
                ta.setLineWrap(true);
                ta.setWrapStyleWord(true);
                ta.setOpaque(true);
                ta.setFont(new Font("SansSerif", Font.PLAIN, 13));
                ta.setBorder(new EmptyBorder(8, 14, 8, 14));

                if (isSelected) {
                    ta.setBackground(new Color(80, 110, 230, 60));
                    ta.setForeground(COLOR_TEXT);
                } else {
                    ta.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                    ta.setForeground(COLOR_TEXT);
                }

                // Auto-resize the row height to fit wrapped text
                int colWidth = t.getColumnModel().getColumn(col).getWidth();
                ta.setSize(new Dimension(colWidth, Short.MAX_VALUE));
                int newHeight = Math.max(40, ta.getPreferredSize().height);
                if (t.getRowHeight(row) != newHeight) {
                    final int fh = newHeight;
                    SwingUtilities.invokeLater(() -> t.setRowHeight(row, fh));
                }
                return ta;
            }
        );

        // Column widths
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(90);
        colModel.getColumn(1).setPreferredWidth(110);
        colModel.getColumn(2).setPreferredWidth(120);
        colModel.getColumn(3).setPreferredWidth(110);
        colModel.getColumn(4).setPreferredWidth(90);
        colModel.getColumn(5).setPreferredWidth(90);
        colModel.getColumn(6).setPreferredWidth(70);
        colModel.getColumn(7).setPreferredWidth(260);
        colModel.getColumn(8).setPreferredWidth(100);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.setPreferredSize(new Dimension(0, 420));
        tableScroll.setMinimumSize(new Dimension(0, 200));

        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildInlineControlsRow() — search field + rating filter dropdown
    // Right-aligned, transparent background
    // ─────────────────────────────────────────────────────────────
    private JPanel buildInlineControlsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 8, 0));

        // Search text field
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        searchField.setToolTipText("Search by any column");

        // Fire applyFilters() on every keystroke
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilters(); }
                @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilters(); }
                @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            }
        );

        // Rating filter dropdown
        String[] ratingOptions = {
            "All Ratings",
            "5 Stars (4.5 - 5.0)",
            "4 Stars (3.5 - 4.4)",
            "3 Stars (2.5 - 3.4)",
            "2 Stars (1.5 - 2.4)",
            "1 Star  (1.0 - 1.4)"
        };
        ratingFilter = new JComboBox<>(ratingOptions);
        ratingFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ratingFilter.setPreferredSize(new Dimension(190, 28));
        ratingFilter.setBackground(Color.WHITE);
        ratingFilter.setToolTipText("Filter by star rating");
        ratingFilter.addActionListener(e -> applyFilters());

        row.add(searchField);
        row.add(ratingFilter);

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // applyFilters()
    // ─────────────────────────────────────────────────────────────
    private void applyFilters() {
        // Read current UI state
        String keyword = (searchField  != null) ? searchField.getText() : "";
        int ratingIndex = (ratingFilter != null) ? ratingFilter.getSelectedIndex() : 0;

        // ── Filtering delegated to StaffReviewService ─────────────
        List<StaffReview> filtered = service.filterReviews(allReviews, keyword, ratingIndex);

        // ── Sorting delegated to StaffReviewService ───────────────
        service.sortReviews(filtered, sortColumnIndex, sortAscending);

        // Update the table
        fillTableFromList(filtered);
    }

    // ─────────────────────────────────────────────────────────────
    // updateColumnHeaderArrows() — shows ▲ or ▼ in sorted column
    // ─────────────────────────────────────────────────────────────
    private void updateColumnHeaderArrows(String[] baseColumnNames) {
        if (table == null) return;

        TableColumnModel colModel = table.getColumnModel();
        for (int i = 0; i < baseColumnNames.length; i++) {
            String text;
            if (i == sortColumnIndex) {
                // ▲ = ascending, ▼ = descending
                text = baseColumnNames[i] + (sortAscending ? " \u25B2" : " \u25BC");
            } else {
                text = baseColumnNames[i];
            }
            colModel.getColumn(i).setHeaderValue(text);
        }
        table.getTableHeader().repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // fillTable() — fills the table AND rebuilds the breakdown bars.
    // Called by refresh().
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<StaffReview> reviews) {
        fillTableFromList(reviews);
        fillBreakdownBars(reviews);
    }

    // ─────────────────────────────────────────────────────────────
    // fillTableFromList() — puts any list of reviews into the table.
    // Used by both fillTable() and applyFilters().
    // ─────────────────────────────────────────────────────────────
    private void fillTableFromList(List<StaffReview> reviews) {
        tableModel.setRowCount(0); // clear existing rows

        for (StaffReview review : reviews) {
            String ratingDisplay = String.format("%.1f", review.rating);

            tableModel.addRow(new Object[]{
                review.commentId,
                review.staffName,
                review.technicianName,
                review.appointmentId,
                review.vehicleType,
                review.carPlate,
                ratingDisplay,
                review.feedbackText,
                review.date
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // UI HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    // White rounded card with a coloured left accent bar
    private JPanel makeRoundedCardWithLeftBar(Color barColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                // White background
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                // Grey border
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                // Coloured rounded rect on left edge
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, 10, getHeight(), 14, 14);
                // White cover to hide the right half of the coloured area
                g2.setColor(COLOR_CARD);
                g2.fillRect(5, 0, 10, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 20));
        return card;
    }

    // Plain white rounded card — no left bar
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