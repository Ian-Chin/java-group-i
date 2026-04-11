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
import java.util.Comparator;
import java.util.List;

/**
 * StaffReviewPage
 *
 * Shows the logged-in customer's staff reviews.
 *
 * FEATURES:
 *   1. Left colour bar on summary cards
 *        - Average rating card   -> BLUE bar
 *        - Rating breakdown card -> YELLOW bar (same as star colour)
 *
 *   2. Search box + Rating filter dropdown
 *        - Right-aligned directly above the table (no separate card/panel)
 *        - Transparent background, no title label
 *        - Matches screenshot: [text field]  [All Ratings dropdown]
 *
 *   3. Sort by column header
 *        - Click any column header to sort ascending
 *        - Click again to reverse to descending
 *        - A triangle arrow (up/down) appears in the sorted header
 *
 * Layout:
 *   - Two summary cards side by side (top)
 *   - [search field] [All Ratings dropdown] right-aligned above table
 *   - Review table below
 *   - Empty-state panel when no reviews exist
 */
public class StaffReviewPage extends JPanel {

    // ── Colours ───────────────────────────────────────────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    // Blue — logo accent colour, used for rating number + bar fills
    private static final Color BLUE_ACCENT = new Color(80, 110, 230);

    // Yellow — star icons AND the breakdown card's left bar
    private static final Color YELLOW_STAR = new Color(255, 193, 7);

    // Left bar colours for the two summary cards
    private static final Color BAR_COLOR_AVERAGE   = BLUE_ACCENT;  // blue
    private static final Color BAR_COLOR_BREAKDOWN = YELLOW_STAR;  // yellow (was teal)

    // ── Service + user ────────────────────────────────────────────
    private final StaffReviewService service = new StaffReviewService();
    private User loggedInUser;

    // ── CardLayout: "DATA" or "EMPTY" view ───────────────────────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Full unfiltered list — kept in memory so search/filter work
    //    without re-reading the file each time ─────────────────────
    private DefaultTableModel tableModel;
    private List<StaffReview> allReviews = new ArrayList<>();

    // ── Average card labels — updated by updateAverageCard() ──────
    private JLabel avgNumberLabel;   // e.g. "4.4"
    private JLabel avgStarsLabel;    // e.g. "★★★★½"
    private JLabel avgCountLabel;    // e.g. "average rating from 2 reviews"

    // ── Breakdown bars panel — rebuilt by fillBreakdownBars() ─────
    private JPanel barsPanel;

    // ── Search + filter controls ──────────────────────────────────
    private JTextField        searchField;   // keyword search
    private JComboBox<String> ratingFilter;  // star-level filter

    // ── Sort state ────────────────────────────────────────────────
    private int     sortColumnIndex = -1;   // -1 means no sort active
    private boolean sortAscending   = true;

    // ── Table reference — needed for column header click listener ──
    private JTable table;

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────
    public StaffReviewPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        pageContent.add(buildSubtitleRow(), BorderLayout.NORTH);

        switchPanel.setOpaque(false);
        switchPanel.add(buildDataPanel(),  "DATA");
        switchPanel.add(buildEmptyPanel(), "EMPTY");
        pageContent.add(switchPanel, BorderLayout.CENTER);

        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);

        cardLayout.show(switchPanel, "EMPTY");
    }

    // ─────────────────────────────────────────────────────────────
    // setUser() — call this before refresh()
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh() — reload data and redraw the whole page
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;
        System.out.println("[StaffReviewPage] refresh() — customer: " + customerId);

        if (customerId != null) {
            allReviews = service.getReviewsByCustomer(customerId);
        } else {
            allReviews = new ArrayList<>();
        }

        System.out.println("[StaffReviewPage] Reviews found: " + allReviews.size());

        if (allReviews.isEmpty()) {
            cardLayout.show(switchPanel, "EMPTY");
        } else {
            updateAverageCard(allReviews);

            // Reset search and filter back to default
            if (searchField  != null) searchField.setText("");
            if (ratingFilter != null) ratingFilter.setSelectedIndex(0);
            sortColumnIndex = -1;
            sortAscending   = true;

            fillTable(allReviews);
            cardLayout.show(switchPanel, "DATA");
        }
    }

    // =========================================================
    // PAGE STRUCTURE
    // =========================================================

    // ─────────────────────────────────────────────────────────────
    // buildSubtitleRow() — muted description below the heading
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
    //   CENTER — table card (which has the inline controls + table)
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
    // buildTopCardsRow() — average card left, breakdown card right
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTopCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(buildAverageCard());
        row.add(buildBreakdownCard());
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildAverageCard() — white card with BLUE left bar
    //   Shows: big number, yellow stars, review count
    // ─────────────────────────────────────────────────────────────
    private JPanel buildAverageCard() {
        JPanel card = makeRoundedCardWithLeftBar(BAR_COLOR_AVERAGE);

        // GridBagLayout centres the inner panel inside the card
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);

        // Big blue number — placeholder until updateAverageCard() runs
        avgNumberLabel = new JLabel("--");
        avgNumberLabel.setFont(new Font("SansSerif", Font.BOLD, 52));
        avgNumberLabel.setForeground(BLUE_ACCENT);
        avgNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Yellow star row
        avgStarsLabel = new JLabel("\u2606\u2606\u2606\u2606\u2606"); // 5 empty stars
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
    // updateAverageCard() — fill the average card with real data
    // ─────────────────────────────────────────────────────────────
    private void updateAverageCard(List<StaffReview> reviews) {
        double avg   = service.calculateAverageRating(reviews);
        int    total = reviews.size();

        avgNumberLabel.setText(String.format("%.1f", avg));
        avgStarsLabel.setText(buildStarString(avg));

        String word = (total == 1) ? "review" : "reviews";
        avgCountLabel.setText("average rating from " + total + " " + word);
    }

    // ─────────────────────────────────────────────────────────────
    // buildBreakdownCard() — white card with YELLOW left bar
    //   Shows: "Rating breakdown" heading + 5 bar rows
    // ─────────────────────────────────────────────────────────────
    private JPanel buildBreakdownCard() {
        // YELLOW bar — changed from teal/green
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

        // Placeholder empty bars shown before refresh() is called
        for (int star = 5; star >= 1; star--) {
            barsPanel.add(buildOneBarRow(star, 0, 1));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        content.add(barsPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // fillBreakdownBars() — rebuild the 5 bar rows with real counts
    // ─────────────────────────────────────────────────────────────
    private void fillBreakdownBars(List<StaffReview> reviews) {
        if (barsPanel == null) return;

        barsPanel.removeAll();
        int total = reviews.size();

        for (int star = 5; star >= 1; star--) {
            int count = service.countByStarLevel(reviews, star);
            barsPanel.add(buildOneBarRow(star, count, total));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        barsPanel.revalidate();
        barsPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // buildOneBarRow() — "5★  [====blue fill====]  1"
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

    // =========================================================
    // TABLE CARD — contains inline controls + the review table
    // =========================================================

    // ─────────────────────────────────────────────────────────────
    // buildTableCard()
    //
    // A plain white rounded card.  Inside it:
    //
    //   NORTH  — buildInlineControlsRow()
    //            Right-aligned transparent row:
    //              (empty space)  [search text field]  [All Ratings v]
    //            No card background, no title label.
    //
    //   CENTER — the review table with clickable column headers for sort
    //
    // HOW SORT WORKS:
    //   Click a column header -> sort that column ascending (shows ▲)
    //   Click the same header -> reverse to descending          (shows ▼)
    //   Click a different header -> sort that column ascending
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        // Column names array — also used by updateColumnHeaderArrows()
        String[] columns = {
                "Comment ID", "Staff Name", "Technician Name",
                "Appointment ID", "Vehicle Type", "Car Plate",
                "Rating", "Feedback", "Date"
        };

        // Add the inline search + filter row at the top of the card
        // It is right-aligned and has a transparent background
        card.add(buildInlineControlsRow(), BorderLayout.NORTH);

        // Non-editable table model
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // customers cannot edit rows
            }
        };

        table = TableHelper.buildTable(tableModel);
        table.setRowHeight(40);

        // Attach click listener to column headers for sort
        table.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int clickedColumn = table.columnAtPoint(e.getPoint());
                if (clickedColumn < 0) return;

                if (sortColumnIndex == clickedColumn) {
                    // Same column clicked again — flip direction
                    sortAscending = !sortAscending;
                } else {
                    // New column — sort ascending
                    sortColumnIndex = clickedColumn;
                    sortAscending   = true;
                }

                applyFilters();                    // re-sort + re-filter
                updateColumnHeaderArrows(columns); // show arrow in header
            }
        });

        // Centre renderer for all columns except Feedback (index 7)
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
            if (i != 7) {
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Feedback column (index 7) — multi-line wrapping text area
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

                    // Auto-resize row height to fit wrapped text
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
    // buildInlineControlsRow()
    //
    // Builds the right-aligned search + filter row that sits directly
    // above the table — no background panel, no title, no border.
    //
    // Looks like this (right side of the row):
    //   (empty)  ... [___search field___]  [All Ratings  v]
    //
    // This matches the screenshot style exactly.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildInlineControlsRow() {

        // FlowLayout.RIGHT pushes everything to the right
        // The panel is opaque=false so it blends into the card background
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 8, 0)); // small gap above the table

        // Search text field — plain box, no label
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(2, 8, 2, 8)
        ));
        searchField.setToolTipText("Search by any column");

        // Fire applyFilters() on every keystroke (live search)
        searchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilters(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilters(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });

        // Rating filter dropdown
        // "All Ratings" is the default — selecting a band filters the table
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

        // Fire applyFilters() whenever the selection changes
        ratingFilter.addActionListener(e -> applyFilters());

        row.add(searchField);
        row.add(ratingFilter);

        return row;
    }

    // =========================================================
    // FILTER + SORT LOGIC
    // =========================================================

    // ─────────────────────────────────────────────────────────────
    // applyFilters()
    //
    // Called whenever the search text changes, the dropdown changes,
    // or a column header is clicked.
    //
    // Steps:
    //   1. Read current keyword and selected rating band
    //   2. Loop through allReviews, keep rows that match BOTH
    //   3. Sort the matching rows (if a column header was clicked)
    //   4. Put the result into the table
    // ─────────────────────────────────────────────────────────────
    private void applyFilters() {

        // Step 1: read keyword (lowercase for case-insensitive search)
        String keyword = (searchField != null)
                ? searchField.getText().trim().toLowerCase()
                : "";

        // Step 2: read rating band index
        //   0 = All Ratings (no filter)
        //   1 = 5 Stars  rating >= 4.5
        //   2 = 4 Stars  3.5 <= rating < 4.5
        //   3 = 3 Stars  2.5 <= rating < 3.5
        //   4 = 2 Stars  1.5 <= rating < 2.5
        //   5 = 1 Star   rating < 1.5
        int ratingIndex = (ratingFilter != null)
                ? ratingFilter.getSelectedIndex()
                : 0;

        // Step 3: build filtered list
        List<StaffReview> filtered = new ArrayList<>();

        for (StaffReview review : allReviews) {

            // Keyword match — true if the keyword appears in ANY column
            boolean keywordMatch = keyword.isEmpty()
                    || review.commentId      .toLowerCase().contains(keyword)
                    || review.staffName      .toLowerCase().contains(keyword)
                    || review.technicianName .toLowerCase().contains(keyword)
                    || review.appointmentId  .toLowerCase().contains(keyword)
                    || review.vehicleType    .toLowerCase().contains(keyword)
                    || review.carPlate       .toLowerCase().contains(keyword)
                    || review.feedbackText   .toLowerCase().contains(keyword)
                    || review.date           .toLowerCase().contains(keyword)
                    || String.valueOf(review.rating).contains(keyword);

            // Rating band match
            boolean ratingMatch;
            switch (ratingIndex) {
                case 1:  ratingMatch = (review.rating >= 4.5);                        break;
                case 2:  ratingMatch = (review.rating >= 3.5 && review.rating < 4.5); break;
                case 3:  ratingMatch = (review.rating >= 2.5 && review.rating < 3.5); break;
                case 4:  ratingMatch = (review.rating >= 1.5 && review.rating < 2.5); break;
                case 5:  ratingMatch = (review.rating  < 1.5);                        break;
                default: ratingMatch = true; break; // "All Ratings" — no filter
            }

            if (keywordMatch && ratingMatch) {
                filtered.add(review);
            }
        }

        // Step 4: sort the filtered list
        applySortToList(filtered);

        // Step 5: put the filtered + sorted rows into the table
        fillTableFromList(filtered);
    }

    // ─────────────────────────────────────────────────────────────
    // applySortToList()
    //
    // Sorts the list in place using sortColumnIndex and sortAscending.
    // Does nothing if no column is selected (sortColumnIndex == -1).
    //
    // Column-to-field mapping:
    //   0=Comment ID  1=Staff Name  2=Technician Name  3=Appointment ID
    //   4=Vehicle Type  5=Car Plate  6=Rating (number)  7=Feedback  8=Date
    // ─────────────────────────────────────────────────────────────
    private void applySortToList(List<StaffReview> list) {
        if (sortColumnIndex < 0) return; // no column selected

        Comparator<StaffReview> comparator;

        switch (sortColumnIndex) {
            case 0:  comparator = Comparator.comparing(r -> r.commentId);      break;
            case 1:  comparator = Comparator.comparing(r -> r.staffName);      break;
            case 2:  comparator = Comparator.comparing(r -> r.technicianName); break;
            case 3:  comparator = Comparator.comparing(r -> r.appointmentId);  break;
            case 4:  comparator = Comparator.comparing(r -> r.vehicleType);    break;
            case 5:  comparator = Comparator.comparing(r -> r.carPlate);       break;
            case 6:  comparator = Comparator.comparingDouble(r -> r.rating);   break;
            case 7:  comparator = Comparator.comparing(r -> r.feedbackText);   break;
            case 8:  comparator = Comparator.comparing(r -> r.date);           break;
            default: comparator = Comparator.comparing(r -> r.commentId);      break;
        }

        if (!sortAscending) {
            comparator = comparator.reversed(); // flip to descending
        }

        list.sort(comparator);
    }

    // ─────────────────────────────────────────────────────────────
    // updateColumnHeaderArrows()
    //
    // Shows a triangle arrow in the header of the sorted column:
    //   ascending  -> "Rating ▲"
    //   descending -> "Rating ▼"
    // All other columns show their plain name with no arrow.
    // ─────────────────────────────────────────────────────────────
    private void updateColumnHeaderArrows(String[] baseColumnNames) {
        if (table == null) return;

        TableColumnModel colModel = table.getColumnModel();
        for (int i = 0; i < baseColumnNames.length; i++) {
            String text;
            if (i == sortColumnIndex) {
                // Unicode 25B2 = ▲ (up arrow), 25BC = ▼ (down arrow)
                text = baseColumnNames[i] + (sortAscending ? " \u25B2" : " \u25BC");
            } else {
                text = baseColumnNames[i];
            }
            colModel.getColumn(i).setHeaderValue(text);
        }
        table.getTableHeader().repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // buildEmptyPanel() — shown when no reviews are found
    // ─────────────────────────────────────────────────────────────
    private JPanel buildEmptyPanel() {
        return ServiceHistoryPage.buildNoDataPanel(
                "\u2605",
                "No staff review records found.",
                "Reviews from technicians will appear here."
        );
    }

    // =========================================================
    // DATA HELPERS
    // =========================================================

    // ─────────────────────────────────────────────────────────────
    // fillTable() — called by refresh() with the full review list
    // Fills the table AND rebuilds the breakdown bars.
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<StaffReview> reviews) {
        fillTableFromList(reviews);
        fillBreakdownBars(reviews); // bars always reflect ALL reviews
    }

    // ─────────────────────────────────────────────────────────────
    // fillTableFromList()
    //
    // Puts any list of reviews into the table model.
    // Used by both fillTable() and applyFilters().
    // Does NOT rebuild the breakdown bars.
    // ─────────────────────────────────────────────────────────────
    private void fillTableFromList(List<StaffReview> reviews) {
        tableModel.setRowCount(0); // clear existing rows first

        for (StaffReview review : reviews) {
            String ratingDisplay = String.format("%.1f", review.rating);

            tableModel.addRow(new Object[]{
                    review.commentId,
                    review.staffName,
                    review.technicianName,
                    review.appointmentId,
                    review.vehicleType,
                    review.carPlate,
                    ratingDisplay,   // e.g. "4.0" — no star symbols in the table
                    review.feedbackText,
                    review.date
            });
        }
    }

    // ─────────────────────────────────────────────────────────────
    // buildStarString() — converts a number to star Unicode symbols
    // e.g. 4.4 -> "★★★★½"    used in the average card only
    // ─────────────────────────────────────────────────────────────
    private String buildStarString(double rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if      (rating >= i)       sb.append("\u2605"); // full star ★
            else if (rating >= i - 0.5) sb.append("\u00BD"); // half star ½
            else                        sb.append("\u2606"); // empty star ☆
        }
        return sb.toString();
    }

    // =========================================================
    // UI HELPER METHODS
    // =========================================================

    // ─────────────────────────────────────────────────────────────
    // makeRoundedCardWithLeftBar()
    //
    // White rounded card with a 5px coloured bar on the left edge.
    //
    // HOW IT WORKS (inside paintComponent):
    //   1. Draw a white filled rounded rectangle for the background
    //   2. Draw the grey border on top
    //   3. Paint a wider rounded rect using the bar colour on the left
    //   4. Cover the right portion of that coloured rect with white
    //      -> only the leftmost ~5px strip remains as the colour bar
    //
    // The card uses BorderLayout.
    // Callers must add their content panels to BorderLayout.CENTER.
    // ─────────────────────────────────────────────────────────────
    private JPanel makeRoundedCardWithLeftBar(Color barColor) {

        JPanel card = new JPanel(new BorderLayout()) {
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

                // 3. Coloured rounded rect on the left (10px wide)
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, 10, getHeight(), 14, 14);

                // 4. White cover on right half of that coloured area
                //    so only the leftmost 5px stays visible
                g2.setColor(COLOR_CARD);
                g2.fillRect(5, 0, 10, getHeight());

                g2.dispose();
            }
        };

        card.setOpaque(false);
        // Left padding = 22px (5px bar + 17px gap to content)
        card.setBorder(new EmptyBorder(18, 22, 18, 20));

        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // makeRoundedCard()
    //
    // Plain white rounded card with no left colour bar.
    // Used for the table card.
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