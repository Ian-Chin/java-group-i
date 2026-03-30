package view;

import model.StaffReviewService;
import model.StaffReviewService.StaffReview;
import model.User;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;

/**
 * StaffReviewPage
 *
 * Shows the logged-in customer's staff reviews.
 *
 * Layout:
 *   - Average rating card (LEFT) + Rating breakdown card (RIGHT) side by side
 *   - Review table below with columns:
 *       Comment ID | Staff Name | Technician Name | Appointment ID |
 *       Vehicle Type | Car Plate | Rating | Feedback | Date
 *   - Empty-state panel exactly matches PaymentHistoryPage size and style
 *   - Stars in the average card are YELLOW
 *   - Rating column in the table shows only the number e.g. "4.0" (no stars)
 */
public class StaffReviewPage extends JPanel {

    // ── Colours — exactly matching PaymentHistoryPage ─────────────
    private static final Color COLOR_BG     = new Color(245, 246, 250);
    private static final Color COLOR_CARD   = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);

    // Blue accent — same colour as the APU ASC logo
    private static final Color BLUE_ACCENT   = new Color(80, 110, 230);

    // Yellow — used for the star icons in the average rating card
    private static final Color YELLOW_STAR   = new Color(255, 193, 7);

    // ── The service that reads comments.txt, accounts.txt, vehicles.txt
    private final StaffReviewService service = new StaffReviewService();

    // ── The currently logged-in customer ─────────────────────────
    private User loggedInUser;

    // ── CardLayout switches between "DATA" and "EMPTY" views ─────
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Table model — rows filled in fillTable() ──────────────────
    private DefaultTableModel tableModel;

    // ── Summary card labels — updated in updateAverageCard() ──────
    private JLabel avgNumberLabel;  // big blue "4.4" number
    private JLabel avgStarsLabel;   // yellow star icons e.g. "★★★★½"
    private JLabel avgCountLabel;   // "average rating from N reviews"

    // ── Breakdown bars panel — rebuilt in fillBreakdownBars() ─────
    private JPanel barsPanel;

    // ─────────────────────────────────────────────────────────────
    // Constructor
    // ─────────────────────────────────────────────────────────────
    public StaffReviewPage(User loggedInUser) {
        this.loggedInUser = loggedInUser;

        setLayout(new BorderLayout());
        setBackground(COLOR_BG);

        // Main content panel with the same padding as PaymentHistoryPage
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));

        // Subtitle row at the top (no big heading — heading is in the sidebar/header)
        pageContent.add(buildSubtitleRow(), BorderLayout.NORTH);

        // The switchPanel holds TWO cards — only one is visible at a time
        switchPanel.setOpaque(false);
        switchPanel.add(buildDataPanel(),  "DATA");   // shown when reviews exist
        switchPanel.add(buildEmptyPanel(), "EMPTY");  // shown when no reviews
        pageContent.add(switchPanel, BorderLayout.CENTER);

        // Outer scroll pane so the whole page scrolls on small windows
        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);

        // Default view is the empty state until refresh() is called
        cardLayout.show(switchPanel, "EMPTY");
    }

    // ─────────────────────────────────────────────────────────────
    // setUser()
    // Call this in CustomerDashboard.refreshUser() BEFORE calling refresh()
    // so the page knows which customer is logged in.
    // ─────────────────────────────────────────────────────────────
    public void setUser(User user) {
        this.loggedInUser = user;
    }

    // ─────────────────────────────────────────────────────────────
    // refresh()
    // Called every time the user clicks "Staff Review" in the sidebar.
    // Reads comments.txt and updates the whole UI.
    // ─────────────────────────────────────────────────────────────
    public void refresh() {
        String customerId = (loggedInUser != null) ? loggedInUser.getUserId() : null;
        System.out.println("[StaffReviewPage] refresh() — customer: " + customerId);

        // Load reviews for this customer (empty list if not logged in)
        List<StaffReview> reviews;
        if (customerId != null) {
            reviews = service.getReviewsByCustomer(customerId);
        } else {
            reviews = new java.util.ArrayList<>();
        }

        System.out.println("[StaffReviewPage] Reviews found: " + reviews.size());

        if (reviews.isEmpty()) {
            // No data — show the empty state card
            cardLayout.show(switchPanel, "EMPTY");
        } else {
            // Update the average card and fill the table, then show data view
            updateAverageCard(reviews);
            fillTable(reviews);
            cardLayout.show(switchPanel, "DATA");
        }
    }

    // ═════════════════════════════════════════════════════════════
    // PAGE STRUCTURE BUILDERS
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // buildSubtitleRow()
    // Small muted subtitle text shown below the page heading.
    // Same style and spacing as PaymentHistoryPage.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildSubtitleRow() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        // Same bottom gap as PaymentHistoryPage uses
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
    // The "DATA" card:
    //   - Top row: average rating card (left) + rating breakdown card (right)
    //   - Below: review table
    // ─────────────────────────────────────────────────────────────
    private JPanel buildDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        // Top row of two side-by-side cards with a gap below
        JPanel topRow = buildTopCardsRow();
        topRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        panel.add(topRow, BorderLayout.NORTH);

        // Review table below the top cards
        panel.add(buildTableCard(), BorderLayout.CENTER);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTopCardsRow()
    // Two cards placed side by side using a GridLayout with a gap.
    // Left card = average rating   Right card = rating breakdown
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTopCardsRow() {
        // GridLayout(1 row, 2 columns, 16px horizontal gap, 0 vertical gap)
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);

        row.add(buildAverageCard());
        row.add(buildBreakdownCard());

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildAverageCard()
    // Left card: big blue number + YELLOW star icons + review count text.
    //
    // Stars are now YELLOW (not blue) as requested.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildAverageCard() {
        JPanel card = makeRoundedCard();

        // GridBagLayout centres everything inside the card
        card.setLayout(new GridBagLayout());

        // Inner panel stacks items vertically and is centred by GridBagLayout
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(COLOR_CARD);

        // Big blue rating number — placeholder until updateAverageCard() is called
        avgNumberLabel = new JLabel("—");
        avgNumberLabel.setFont(new Font("SansSerif", Font.BOLD, 52));
        avgNumberLabel.setForeground(BLUE_ACCENT);  // blue number
        avgNumberLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Star icons row — now YELLOW colour
        avgStarsLabel = new JLabel("☆☆☆☆☆");
        avgStarsLabel.setFont(new Font("SansSerif", Font.PLAIN, 22));
        avgStarsLabel.setForeground(YELLOW_STAR);   // ← YELLOW stars
        avgStarsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // "average rating from N reviews" text
        avgCountLabel = new JLabel("average rating from 0 reviews");
        avgCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        avgCountLabel.setForeground(COLOR_MUTED);
        avgCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Stack items with small gaps between them
        inner.add(avgNumberLabel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(avgStarsLabel);
        inner.add(Box.createVerticalStrut(6));
        inner.add(avgCountLabel);

        card.add(inner);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // updateAverageCard()
    // Fills the left card with real data after reviews are loaded.
    // Called inside refresh() when reviews are found.
    // ─────────────────────────────────────────────────────────────
    private void updateAverageCard(List<StaffReview> reviews) {
        // Calculate average rating using the service
        double avg   = service.calculateAverageRating(reviews);
        int    total = reviews.size();

        // Update the big number e.g. "4.4"
        avgNumberLabel.setText(String.format("%.1f", avg));

        // Update star icons using the helper below
        avgStarsLabel.setText(buildStarString(avg));

        // Update the count text e.g. "average rating from 2 reviews"
        String word = (total == 1) ? "review" : "reviews";
        avgCountLabel.setText("average rating from " + total + " " + word);
    }

    // ─────────────────────────────────────────────────────────────
    // buildBreakdownCard()
    // Right card: "Rating breakdown" heading + 5 horizontal bar rows.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildBreakdownCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        // Heading at the top of the card
        JLabel heading = new JLabel("Rating breakdown");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(COLOR_TEXT);
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(heading, BorderLayout.NORTH);

        // Bars panel — filled with placeholder bars now, real data in fillBreakdownBars()
        barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setBackground(COLOR_CARD);

        // Draw 5 empty bars (count=0) so the card looks correct before refresh()
        for (int star = 5; star >= 1; star--) {
            barsPanel.add(buildOneBarRow(star, 0, 1));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        card.add(barsPanel, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // fillBreakdownBars()
    // Rebuilds the five bar rows with real data from the reviews list.
    // Called from fillTable() after reviews are loaded.
    // ─────────────────────────────────────────────────────────────
    private void fillBreakdownBars(List<StaffReview> reviews) {
        if (barsPanel == null) return;

        // Remove old bars
        barsPanel.removeAll();

        int total = reviews.size();

        // Add one bar row per star level (5 down to 1)
        for (int star = 5; star >= 1; star--) {
            int count = service.countByStarLevel(reviews, star);
            barsPanel.add(buildOneBarRow(star, count, total));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        // Tell Swing to redraw the panel
        barsPanel.revalidate();
        barsPanel.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // buildOneBarRow()
    // One horizontal bar row:  "5★  [===blue bar===]  1"
    // ─────────────────────────────────────────────────────────────
    private JPanel buildOneBarRow(int star, int count, int max) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(COLOR_CARD);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        // Left side: star label e.g. "5★"
        JLabel starLabel = new JLabel(star + "\u2605");
        starLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        starLabel.setForeground(COLOR_TEXT);
        starLabel.setPreferredSize(new Dimension(28, 16));
        row.add(starLabel, BorderLayout.WEST);

        // Middle: light grey track with a blue fill bar inside
        JPanel track = new JPanel(new BorderLayout());
        track.setBackground(new Color(230, 230, 235)); // light grey track background
        track.setBorder(new EmptyBorder(4, 0, 4, 0));

        JPanel fill = new JPanel();
        fill.setBackground(BLUE_ACCENT); // blue fill bar

        // Calculate how wide the fill bar should be (0 to 200 pixels)
        double ratio = (max == 0) ? 0.0 : (double) count / max;
        fill.setPreferredSize(new Dimension((int) (200 * ratio), 8));
        track.add(fill, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);

        // Right side: count number e.g. "1"
        JLabel countLabel = new JLabel(String.valueOf(count));
        countLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLabel.setForeground(COLOR_MUTED);
        countLabel.setPreferredSize(new Dimension(20, 16));
        row.add(countLabel, BorderLayout.EAST);

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildTableCard()
    // Rounded white card holding the review table.
    //
    // Columns:
    //   Comment ID | Staff Name | Technician Name | Appointment ID |
    //   Vehicle Type | Car Plate | Rating | Feedback | Date
    //
    // Rating column shows only the number e.g. "4.0" (no stars).
    // Feedback column wraps long text automatically.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());

        // ── Column headers ────────────────────────────────────────
        String[] columns = {
                "Comment ID", "Staff Name", "Technician Name",
                "Appointment ID", "Vehicle Type", "Car Plate",
                "Rating", "Feedback", "Date"
        };

        // Non-editable table model — user cannot type inside cells
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        // Build using the shared TableHelper for a consistent header style
        JTable table = TableHelper.buildTable(tableModel);
        table.setRowHeight(40); // default row height; auto-grows for long feedback

        // ── Centre renderer — applied to all columns EXCEPT Feedback (index 7) ──
        // This renderer centres text and applies alternating row background colours.
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
                    // Even rows = white, odd rows = very light grey
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }
                return this;
            }
        };

        // Apply the centre renderer to every column except Feedback (index 7)
        for (int i = 0; i < columns.length; i++) {
            if (i != 7) { // index 7 = Feedback
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // ── Feedback column (index 7) — wrapping multi-line text area ────────
        // Long feedback text wraps inside a JTextArea, and the row grows taller.
        table.getColumnModel().getColumn(7).setCellRenderer(
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

                    // Automatically resize the row height to fit wrapped text
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

        // ── Column widths — Feedback gets the most space ──────────
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(90);   // Comment ID
        colModel.getColumn(1).setPreferredWidth(110);  // Staff Name
        colModel.getColumn(2).setPreferredWidth(120);  // Technician Name
        colModel.getColumn(3).setPreferredWidth(110);  // Appointment ID
        colModel.getColumn(4).setPreferredWidth(90);   // Vehicle Type
        colModel.getColumn(5).setPreferredWidth(90);   // Car Plate
        colModel.getColumn(6).setPreferredWidth(70);   // Rating
        colModel.getColumn(7).setPreferredWidth(260);  // Feedback — widest
        colModel.getColumn(8).setPreferredWidth(100);  // Date

        // Prevent the user from reordering columns by dragging
        table.getTableHeader().setReorderingAllowed(false);

        // ── Scroll pane — same fixed height as PaymentHistoryPage ─
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(null);
        tableScroll.setBackground(COLOR_CARD);
        tableScroll.getViewport().setBackground(COLOR_CARD);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        tableScroll.getVerticalScrollBar().setUnitIncrement(16);
        tableScroll.setPreferredSize(new Dimension(0, 420)); // same as PaymentHistoryPage
        tableScroll.setMinimumSize(new Dimension(0, 200));

        card.add(tableScroll, BorderLayout.CENTER);
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildEmptyPanel()
    //
    // Shown when no review records are found.
    //
    // This now uses ServiceHistoryPage.buildNoDataPanel() in EXACTLY
    // the same way that PaymentHistoryPage does, so the size, position,
    // and style are identical.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildEmptyPanel() {
        // Use the same shared helper as PaymentHistoryPage
        return ServiceHistoryPage.buildNoDataPanel(
                "\u2605",                                        // ★ star icon
                "No staff review records found.",
                "Reviews from technicians will appear here."
        );
    }

    // ═════════════════════════════════════════════════════════════
    // DATA HELPERS
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // fillTable()
    // Clears the table, adds one row per review, and rebuilds bars.
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<StaffReview> reviews) {
        tableModel.setRowCount(0); // clear old rows first

        for (StaffReview review : reviews) {

            // Rating: show only the number e.g. "4.0" — NO stars in the table
            String ratingDisplay = String.format("%.1f", review.rating);

            tableModel.addRow(new Object[]{
                    review.commentId,       // Comment ID
                    review.staffName,       // Staff Name
                    review.technicianName,  // Technician Name
                    review.appointmentId,   // Appointment ID
                    review.vehicleType,     // Vehicle Type  e.g. "Car"
                    review.carPlate,        // Car Plate     e.g. "WXY1234"
                    ratingDisplay,          // Rating        e.g. "4.0"
                    review.feedbackText,    // Feedback      (wraps automatically)
                    review.date             // Date
            });
        }

        // Rebuild the breakdown bars with real data
        fillBreakdownBars(reviews);
    }

    // ─────────────────────────────────────────────────────────────
    // buildStarString()
    // Converts a numeric rating into star symbols.
    // e.g. 4.4 → "★★★★½"
    // Used for the average rating card only (NOT the table).
    // ─────────────────────────────────────────────────────────────
    private String buildStarString(double rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if      (rating >= i)       sb.append("\u2605"); // ★ full star
            else if (rating >= i - 0.5) sb.append("\u00BD"); // ½ half star
            else                        sb.append("\u2606"); // ☆ empty star
        }
        return sb.toString();
    }

    // ═════════════════════════════════════════════════════════════
    // UI HELPER
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // makeRoundedCard()
    // Creates a white rounded card with a light border.
    // Identical to the card style used in PaymentHistoryPage.
    // ─────────────────────────────────────────────────────────────
    private JPanel makeRoundedCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Use Graphics2D so we can turn on anti-aliasing for smooth corners
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                // Fill the rounded rectangle with white
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);

                // Draw the border (one pixel smaller so the border is not clipped)
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);

                g2.dispose();
            }
        };
        card.setOpaque(false); // let paintComponent handle drawing
        card.setBorder(new EmptyBorder(18, 20, 18, 20)); // padding inside the card
        return card;
    }
}