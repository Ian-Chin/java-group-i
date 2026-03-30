package view;

import model.AccountService;
import model.ServiceHistoryService;
import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
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
 * This class is responsible ONLY for building and displaying the
 * Service History page. It does NOT read files or do business logic.
 *
 * All data is fetched by calling methods on:
 *   - ServiceHistoryService  (reads serviceHistory.txt)
 *   - AccountService         (reads accounts.txt — for technician names)
 *   - VehicleService         (reads vehicles.txt — for vehicle type & plate)
 *
 * If no records exist → shows a friendly empty-state card instead.
 */
public class ServiceHistoryPage extends JPanel {

    // ── Service classes (data layer — NOT UI) ─────────────────────
    private final ServiceHistoryService serviceHistoryService = new ServiceHistoryService();
    private final AccountService        accountService        = new AccountService();
    private final VehicleService        vehicleService        = new VehicleService();

    // ── CardLayout switcher ───────────────────────────────────────
    // "TABLE" = records exist,  "EMPTY" = no records
    private final CardLayout cardLayout  = new CardLayout();
    private final JPanel     switchPanel = new JPanel(cardLayout);

    // ── Stat card value labels (updated by refresh()) ─────────────
    private JLabel totalServicesValueLabel;
    private JLabel latestServiceValueLabel;
    private JLabel latestServiceSubLabel;
    private JLabel favTechValueLabel;
    private JLabel favTechSubLabel;

    // ── Table model ───────────────────────────────────────────────
    private DefaultTableModel tableModel;

    // ── Design constants ──────────────────────────────────────────
    private static final Color COLOR_BG       = new Color(245, 246, 250);
    private static final Color COLOR_CARD     = Color.WHITE;
    private static final Color COLOR_BORDER   = new Color(225, 228, 235);
    private static final Color COLOR_TEXT     = new Color(30,  35,  50);
    private static final Color COLOR_MUTED    = new Color(110, 118, 140);
    private static final Color COLOR_EMPTY_BG = new Color(248, 249, 253);

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
    //
    // Shows the subtitle line under the header title.
    // Font size bumped to 14 (was 13) so it reads more clearly.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPageHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));

        // Subtitle — font size increased from 13 → 14 for better readability
        JLabel subtitle = new JLabel("Your completed service records are shown below.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14)); // ← was 13
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(subtitle);
        return header;
    }

    // ─────────────────────────────────────────────────────────────
    // buildDataPanel()  — "TABLE" card
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
    // buildStatsRow()  — three summary cards side by side
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);

        totalServicesValueLabel = makeBigValueLabel("—");
        row.add(buildStatCard("Total services",
                totalServicesValueLabel, makeMutedLabel("since joining")));

        latestServiceValueLabel = makeBigValueLabel("—");
        latestServiceSubLabel   = makeMutedLabel("—");
        row.add(buildStatCard("Latest service",
                latestServiceValueLabel, latestServiceSubLabel));

        favTechValueLabel = makeBigValueLabel("—");
        favTechSubLabel   = makeMutedLabel("—");
        row.add(buildStatCard("Favourite technician",
                favTechValueLabel, favTechSubLabel));

        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStatCard()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildStatCard(String topText, JLabel valueLabel, JLabel subLabel) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

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
    // buildTableCard()  — white card containing the JTable
    // ─────────────────────────────────────────────────────────────
    private JPanel buildTableCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BorderLayout());

        String[] columns = {
            "History ID", "Appointment ID", "Vehicle",
            "Car Plate", "Payment ID", "Technician", "Date", "Status"
        };

        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        JTable table = TableHelper.buildTable(tableModel);

        // Center-align columns 0–6
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
        for (int i = 0; i <= 6; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Status column — coloured badge, centred
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
                } else if (status.equalsIgnoreCase("In Progress")) {
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
    // buildEmptyPanel()
    //
    // Shown when the customer has no service history.
    //
    // CHANGES vs previous version:
    //   1. Icon is now a proper circular-repeat / refresh symbol
    //      drawn with two smooth arcs and clean arrowheads using
    //      Java2D's Arc2D and GeneralPath — much cleaner than the
    //      old polygon approach.
    //   2. Bold title text is now shown again (was commented out).
    //   3. Subtitle font size bumped to 13 (was 12).
    // ─────────────────────────────────────────────────────────────
    private JPanel buildEmptyPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(new EmptyBorder(20, 0, 40, 0));

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // ── Proper circular-repeat icon ────────────────────────────
        // Drawn with Java2D:
        //   - Two 150° arcs forming a near-complete circle
        //   - Two filled arrowheads at the gap between the arcs
        //     pointing in opposite directions (clockwise repeat)
        JLabel iconLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() { return new Dimension(52, 52); }
            @Override
            public Dimension getMinimumSize()   { return new Dimension(52, 52); }
            @Override
            public Dimension getMaximumSize()   { return new Dimension(52, 52); }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                                    RenderingHints.VALUE_STROKE_PURE);

                Color iconColor = new Color(160, 165, 185); // soft blue-grey
                g2.setColor(iconColor);

                float cx = 26f, cy = 26f, r = 16f;
                float strokeW = 3.2f;
                g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND,
                                             BasicStroke.JOIN_ROUND));

                // ── Top arc: from ~30° sweeping 150° (clockwise) ──
                // Arc2D.OPEN draws just the curved line, no chords.
                Arc2D topArc = new Arc2D.Float(
                    cx - r, cy - r, r * 2, r * 2,
                    30,   // start angle (degrees, counter-clockwise from 3 o'clock)
                    150,  // sweep angle
                    Arc2D.OPEN
                );
                g2.draw(topArc);

                // ── Bottom arc: from ~210° sweeping 150° ──────────
                Arc2D bottomArc = new Arc2D.Float(
                    cx - r, cy - r, r * 2, r * 2,
                    210,
                    150,
                    Arc2D.OPEN
                );
                g2.draw(bottomArc);

                // ── Arrowhead at the END of the top arc (~180°) ───
                // The top arc ends at 30+150 = 180°, i.e. the left side.
                // Point on circle at 180° = (cx - r, cy).
                // Arrow points downward (tangent to circle going clockwise).
                drawArrowhead(g2, iconColor,
                    cx - r,      cy,          // tip of arrow (end of top arc)
                    cx - r,      cy - 7f      // tail direction (pointing up = arrow goes down)
                );

                // ── Arrowhead at the END of the bottom arc (~360°/0°) ──
                // The bottom arc ends at 210+150 = 360° = 0°, i.e. right side.
                // Point on circle at 0° = (cx + r, cy).
                // Arrow points upward (tangent to circle going clockwise).
                drawArrowhead(g2, iconColor,
                    cx + r,      cy,          // tip of arrow (end of bottom arc)
                    cx + r,      cy + 7f      // tail direction (pointing down = arrow goes up)
                );

                g2.dispose();
            }

            /**
             * Draws a small filled triangular arrowhead.
             *
             * @param g2       the Graphics2D context
             * @param color    fill colour
             * @param tipX     x of the arrowhead tip (the sharp point)
             * @param tipY     y of the arrowhead tip
             * @param tailX    x of the direction the arrow came FROM
             * @param tailY    y of the direction the arrow came FROM
             *
             * The arrowhead is sized to match the stroke width of the arcs.
             */
            private void drawArrowhead(Graphics2D g2, Color color,
                                       float tipX, float tipY,
                                       float tailX, float tailY) {
                // Direction vector from tail → tip
                float dx = tipX - tailX;
                float dy = tipY - tailY;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len == 0) return;

                // Normalise
                dx /= len;
                dy /= len;

                // Perpendicular vector
                float px = -dy;
                float py =  dx;

                // Arrow size
                float arrowLen  = 7f;  // how far back the base of the triangle is
                float arrowHalf = 4f;  // half-width of the base

                // Three vertices of the triangle
                float x1 = tipX;
                float y1 = tipY;
                float x2 = tipX - dx * arrowLen + px * arrowHalf;
                float y2 = tipY - dy * arrowLen + py * arrowHalf;
                float x3 = tipX - dx * arrowLen - px * arrowHalf;
                float y3 = tipY - dy * arrowLen - py * arrowHalf;

                GeneralPath arrow = new GeneralPath();
                arrow.moveTo(x1, y1);
                arrow.lineTo(x2, y2);
                arrow.lineTo(x3, y3);
                arrow.closePath();

                g2.setStroke(new BasicStroke(1f));
                g2.setColor(color);
                g2.fill(arrow);
            }
        };
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Bold title — font size 15 ──────────────────────────────
        // This was previously commented out; it is now restored and
        // made slightly larger for better readability.
        JLabel noDataLabel = new JLabel("No service history records found.");
        noDataLabel.setFont(new Font("SansSerif", Font.BOLD, 15)); // ← was commented, now 15
        noDataLabel.setForeground(COLOR_TEXT);
        noDataLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Subtitle — font size 13 ────────────────────────────────
        JLabel subLabel = new JLabel("Your completed service records will appear here.");
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13)); // ← was 12
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
    // refresh()
    // ═══════════════════════════════════════════════════════════════
    public void refresh() {
        model.User loggedInUser = getLoggedInUser();
        if (loggedInUser == null) {
            cardLayout.show(switchPanel, "EMPTY");
            return;
        }

        String customerId = loggedInUser.getUserId();
        List<String[]> allRecords = serviceHistoryService.getAllRecords();

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
            cardLayout.show(switchPanel, "TABLE");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatsCards()
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
    // fillTable()
    // ─────────────────────────────────────────────────────────────
    private void fillTable(List<String[]> records) {
        tableModel.setRowCount(0);

        for (String[] row : records) {
            String historyId     = row[0].trim();
            String appointmentId = row[2].trim();
            String vehicleId     = row[3].trim();
            String paymentId     = row[4].trim();
            String techId        = row[5].trim();
            String date          = row[6].trim();
            String status        = row[7].trim();

            String techName    = resolveTechnicianName(techId);
            String vehicleType = resolveVehicleType(vehicleId);
            String carPlate    = resolveCarPlate(vehicleId);
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
    // buildNoDataPanel()  — PUBLIC STATIC shared utility
    //
    // Called by PaymentHistoryPage, StaffReviewPage, MyFeedbackPage
    // to show a consistent empty-state card.
    //
    // CHANGES:
    //   - Bold title font increased to 15
    //   - Subtitle font increased to 13
    //   - Uses the same improved circular-repeat icon as above
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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

        // ── Icon ──────────────────────────────────────────────────
        JLabel iconLabel;
        if (iconUnicode != null && !iconUnicode.isEmpty()) {
            // Emoji icon path — font size 38 for visibility
            iconLabel = new JLabel(iconUnicode);
            iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 38)); // ← was 36
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        } else {
            // Fallback: same improved circular-repeat icon
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
                    g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2, 30,  150, Arc2D.OPEN));
                    g2.draw(new Arc2D.Float(cx-r, cy-r, r*2, r*2, 210, 150, Arc2D.OPEN));

                    // Arrowhead helpers
                    drawHead(g2, ic, cx-r, cy, cx-r, cy-7f);
                    drawHead(g2, ic, cx+r, cy, cx+r, cy+7f);
                    g2.dispose();
                }

                private void drawHead(Graphics2D g2, Color c,
                                      float tx, float ty, float fx, float fy) {
                    float dx = tx-fx, dy = ty-fy;
                    float len = (float) Math.sqrt(dx*dx+dy*dy);
                    if (len==0) return;
                    dx/=len; dy/=len;
                    float px=-dy, py=dx;
                    float al=7f, ah=4f;
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

        // Bold title — font size 15 (was commented out or 14)
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15)); // ← restored & bumped to 15
        titleLabel.setForeground(textColor);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Subtitle — font size 13 (was 12)
        JLabel subLabel = new JLabel(subtitle);
        subLabel.setFont(new Font("SansSerif", Font.PLAIN, 13)); // ← was 12
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