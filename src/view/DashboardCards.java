package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.Map;

/**
 * Shared utility class — builds reusable dashboard card components.
 *
 * Both AdminDashboard and CounterStaffDashboard call these methods.
 * Neither needs to know HOW the cards are drawn — only what data to pass in.
 *
 * OOP concepts demonstrated:
 *  Abstraction  : callers pass plain data (strings, ints, maps); drawing is hidden here
 *  Encapsulation: all painting logic is private to this class via anonymous JPanel subclasses
 *  Polymorphism : buildKpiCard / buildStatusBar accept any label+color combination
 *  Inheritance  : every returned JPanel is a subclass with overridden paintComponent
 */
public final class DashboardCards {

    private DashboardCards() {} // utility class — no instances

    // ── Shared accent colours ─────────────────────────────────────────────────

    public static final Color BLUE   = new Color(80,  110, 230);
    public static final Color GREEN  = new Color(40,  167,  69);
    public static final Color AMBER  = new Color(255, 165,   0);
    public static final Color TEAL   = new Color(40,  180, 200);
    public static final Color PURPLE = new Color(160,  80, 230);
    public static final Color GRAY   = new Color(108, 117, 125);

    // ── KPI card ──────────────────────────────────────────────────────────────

    /**
     * White card with a coloured left stripe, large value, and label below.
     * Used in both admin and staff dashboards for top-row statistics.
     *
     * @param title      e.g. "Total Appointments"
     * @param value      e.g. "25"
     * @param sub        e.g. "all time" — small muted line below value
     * @param icon       Unicode character shown above value
     * @param accent     accent stripe + icon colour
     */
    public static JPanel buildKpiCard(String title, String value,
                                      String sub, String icon, Color accent) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16));
                g2.setColor(accent);
                g2.fill(new RoundRectangle2D.Float(0, 0, 5, getHeight(), 4, 4));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 14));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 22));
        iconLbl.setForeground(accent);
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 26));
        valueLbl.setForeground(UIConstants.TEXT_PRIMARY);
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        titleLbl.setForeground(UIConstants.TEXT_DARK);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(sub);
        subLbl.setFont(UIConstants.FONT_SMALL);
        subLbl.setForeground(UIConstants.TEXT_MUTED);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(valueLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(subLbl);
        return card;
    }

    // ── Status breakdown card (horizontal progress bars) ─────────────────────

    /**
     * Shows a horizontal progress bar per status label.
     *
     * @param entries  ordered map of label → count  (e.g. "Completed" → 12)
     * @param colors   one color per entry, in same order as entries
     * @param total    denominator for percentage calculation
     * @param subtitle small grey line under the card title
     */
    public static JPanel buildStatusBreakdownCard(String cardTitle, String subtitle,
                                                   Map<String, Integer> entries,
                                                   Color[] colors, int total) {
        JPanel card = roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel(cardTitle);
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel(subtitle);
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(20));

        int ci = 0;
        for (Map.Entry<String, Integer> e : entries.entrySet()) {
            Color c = ci < colors.length ? colors[ci] : GRAY;
            card.add(buildStatusBar(e.getKey(), e.getValue(), total, c));
            card.add(Box.createVerticalStrut(14));
            ci++;
        }
        card.add(Box.createVerticalGlue());
        return card;
    }

    private static JPanel buildStatusBar(String label, int count, int total, Color barColor) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Label row: name on left, "N (XX%)" on right
        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLbl.setForeground(UIConstants.TEXT_DARK);

        int pct = total > 0 ? Math.round(count * 100f / total) : 0;
        JLabel countLbl = new JLabel(count + " (" + pct + "%)");
        countLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        countLbl.setForeground(barColor);

        labelRow.add(nameLbl,  BorderLayout.WEST);
        labelRow.add(countLbl, BorderLayout.EAST);
        row.add(labelRow);
        row.add(Box.createVerticalStrut(5));

        float fraction = total > 0 ? (float) count / total : 0f;
        Color bgColor = new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 35);
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                int fw = Math.max(0, (int)(getWidth() * fraction));
                if (fw > 0) { g2.setColor(barColor); g2.fillRoundRect(0, 0, fw, getHeight(), 6, 6); }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        row.add(bar);
        return row;
    }

    // ── Payment summary card ──────────────────────────────────────────────────

    /**
     * Shows total collected, paid count vs unpaid count with coloured tiles.
     *
     * @param paidCount    number of paid payments
     * @param unpaidCount  number of unpaid payments
     * @param totalPaid    sum of paid amounts
     * @param totalUnpaid  sum of unpaid amounts
     */
    public static JPanel buildPaymentSummaryCard(int paidCount, int unpaidCount,
                                                  double totalPaid, double totalUnpaid) {
        JPanel card = roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Payment Summary");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel sub = new JLabel("Collection status overview");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sub);
        card.add(Box.createVerticalStrut(16));

        JLabel totalLbl = new JLabel(String.format("RM %.2f", totalPaid));
        totalLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        totalLbl.setForeground(GREEN);
        totalLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(totalLbl);
        card.add(Box.createVerticalStrut(2));

        JLabel noteLbl = new JLabel("Collected from " + paidCount + " payment" + (paidCount != 1 ? "s" : ""));
        noteLbl.setFont(UIConstants.FONT_SMALL);
        noteLbl.setForeground(UIConstants.TEXT_MUTED);
        noteLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(noteLbl);
        card.add(Box.createVerticalStrut(16));

        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(235, 235, 240));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(14));

        JPanel tiles = new JPanel(new GridLayout(1, 2, 12, 0));
        tiles.setOpaque(false);
        tiles.setAlignmentX(Component.LEFT_ALIGNMENT);
        tiles.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        tiles.add(payTile(paidCount   + " Paid",   String.format("RM %.2f", totalPaid),   GREEN,
                new Color(220, 245, 225)));
        tiles.add(payTile(unpaidCount + " Unpaid",  String.format("RM %.2f", totalUnpaid), AMBER,
                new Color(255, 243, 220)));
        card.add(tiles);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private static JPanel payTile(String label, String amount, Color fg, Color bg) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        lbl.setForeground(fg);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amt = new JLabel(amount);
        amt.setFont(UIConstants.FONT_SMALL);
        amt.setForeground(fg);
        amt.setAlignmentX(Component.LEFT_ALIGNMENT);

        tile.add(lbl);
        tile.add(amt);
        return tile;
    }

    // ── Stat tile grid ────────────────────────────────────────────────────────

    /**
     * Small coloured tile with icon, large number, and label.
     * Compose several of these in a GridLayout for a service overview.
     */
    public static JPanel buildStatTile(String label, String value,
                                        String icon, Color accent, Color bg) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 16));
        iconLbl.setForeground(accent);
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(iconLbl);
        tile.add(Box.createVerticalStrut(6));

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        valLbl.setForeground(UIConstants.TEXT_PRIMARY);
        valLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(valLbl);
        tile.add(Box.createVerticalStrut(2));

        JLabel lblLbl = new JLabel(label);
        lblLbl.setFont(UIConstants.FONT_SMALL);
        lblLbl.setForeground(UIConstants.TEXT_MUTED);
        lblLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(lblLbl);
        return tile;
    }

    // ── Upcoming appointment row ──────────────────────────────────────────────

    /**
     * Single appointment row card — coloured left bar, names, date/time, status badge.
     * Used inside a scrollable list in the upcoming appointments card.
     */
    public static JPanel buildAppointmentRow(String apptId, String custName, String techName,
                                              String serviceType, String date, String time,
                                              String status) {
        Color barColor = statusColor(status);

        JPanel row = new JPanel(new BorderLayout(10, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 249, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(8, 12, 8, 12));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 54));

        // Left colour bar
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(4, 0));
        row.add(bar, BorderLayout.WEST);

        // Centre: names + detail line
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel nameLine = new JLabel(custName + "  \u2022  " + serviceType);
        nameLine.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLine.setForeground(UIConstants.TEXT_DARK);
        nameLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLine = new JLabel(date + (time.isEmpty() ? "" : " at " + time) + "  |  Tech: " + techName);
        detailLine.setFont(new Font("SansSerif", Font.PLAIN, 11));
        detailLine.setForeground(UIConstants.TEXT_MUTED);
        detailLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLine);
        info.add(Box.createVerticalStrut(2));
        info.add(detailLine);
        row.add(info, BorderLayout.CENTER);

        // Right: status badge
        JLabel statusLbl = new JLabel(status) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(barColor.getRed(), barColor.getGreen(), barColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        statusLbl.setForeground(barColor);
        statusLbl.setHorizontalAlignment(SwingConstants.CENTER);
        statusLbl.setPreferredSize(new Dimension(78, 22));
        statusLbl.setMaximumSize(new Dimension(78, 22));
        row.add(statusLbl, BorderLayout.EAST);

        return row;
    }

    // ── Shared card shell ─────────────────────────────────────────────────────

    /**
     * White rounded rectangle card — used as the outer container for most cards.
     * Returns a JPanel with a subtle border and rounded corners.
     */
    public static JPanel roundedCard() {
        return new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Maps appointment status string to a display colour. */
    public static Color statusColor(String status) {
        if (status == null) return GRAY;
        switch (status) {
            case "Completed":   return GREEN;
            case "In Progress": return AMBER;
            case "Pending":     return GRAY;
            default:            return GRAY;
        }
    }
}