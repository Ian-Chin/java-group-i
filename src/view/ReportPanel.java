package view;

import model.AccountService;
import model.PriceConfig;
import model.User;
import view.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows summary stat cards and a staff-count breakdown for the admin.
 * Reads appointment data from appointments.txt if it exists.
 *
 * OOP highlights:
 *  - Encapsulation : each stat is computed by a private method
 *  - Inheritance   : extends JPanel; StatCard is an inner class extending JPanel
 *  - Abstraction   : buildStatCard() hides layout boilerplate
 *  - Polymorphism  : countByRole() works for any role string
 */
public class ReportPanel extends JPanel {

    private static final String APPT_FILE     = "src" + File.separator + "TxtFile"
            + File.separator + "appointments.txt";
    private static final String COMMENTS_FILE = "src" + File.separator + "TxtFile"
            + File.separator + "customer_comments.txt";

    private final AccountService accountService;
    private final PriceConfig    priceConfig;

    public ReportPanel(AccountService accountService) {
        this.accountService = accountService;
        this.priceConfig    = new PriceConfig();

        setLayout(new BorderLayout(0, 24));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        add(buildStatCards(),    BorderLayout.NORTH);
        add(buildDetailsPanel(), BorderLayout.CENTER);
    }

    // ─── Stat cards row ──────────────────────────────────────────

    private JPanel buildStatCards() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setBackground(UIConstants.BG_CONTENT);

        int totalStaff  = countByRole("staff") + countByRole("technician") + countByRole("admin");
        int totalCustomers = countByRole("customer");
        List<String[]> appts = loadCsv(APPT_FILE);
        int totalAppts  = appts.size();
        double avgRating = computeAvgRating();

        row.add(buildStatCard("Total Staff",     String.valueOf(totalStaff),
                new Color(80, 110, 230),  "\u2663"));
        row.add(buildStatCard("Customers",        String.valueOf(totalCustomers),
                new Color(40, 180, 200),  "\u263A"));
        row.add(buildStatCard("Appointments",     String.valueOf(totalAppts),
                new Color(80, 190, 110),  "\u2714"));
        row.add(buildStatCard("Avg Rating",       avgRating == 0 ? "N/A" : String.format("%.1f / 5", avgRating),
                new Color(230, 160, 40),  "\u2605"));

        return row;
    }

    private JPanel buildStatCard(String label, String value, Color accent, String icon) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent stripe
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, 6, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 16));

        JLabel iconLbl = new JLabel(icon);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 26));
        iconLbl.setForeground(accent);
        iconLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLbl.setForeground(UIConstants.TEXT_PRIMARY);
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(UIConstants.FONT_SMALL);
        labelLbl.setForeground(UIConstants.TEXT_MUTED);
        labelLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLbl);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(labelLbl);
        return card;
    }

    // ─── Details section ─────────────────────────────────────────

    private JPanel buildDetailsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 20, 0));
        panel.setBackground(UIConstants.BG_CONTENT);
        panel.add(buildStaffBreakdown());
        panel.add(buildPriceSummary());
        return panel;
    }

    private JPanel buildStaffBreakdown() {
        JPanel card = roundCard();

        JLabel title = new JLabel("Staff Breakdown");
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(18));

        String[][] breakdown = {
            {"Admins",       String.valueOf(countByRole("admin"))},
            {"Counter Staff", String.valueOf(countByRole("staff"))},
            {"Technicians",  String.valueOf(countByRole("technician"))},
            {"Customers",    String.valueOf(countByRole("customer"))},
        };
        Color[] colours = {
            new Color(80, 110, 230),
            new Color(40, 180, 200),
            new Color(80, 190, 110),
            new Color(230, 160, 40),
        };

        for (int i = 0; i < breakdown.length; i++) {
            card.add(buildBreakdownRow(breakdown[i][0], breakdown[i][1], colours[i]));
            if (i < breakdown.length - 1) card.add(Box.createVerticalStrut(12));
        }
        return card;
    }

    private JPanel buildBreakdownRow(String label, String count, Color colour) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(UIConstants.FONT_SMALL);
        nameLbl.setForeground(UIConstants.TEXT_DARK);

        JLabel countBadge = new JLabel(count, SwingConstants.CENTER);
        countBadge.setFont(UIConstants.FONT_SMALL_BOLD);
        countBadge.setForeground(Color.WHITE);
        countBadge.setBackground(colour);
        countBadge.setOpaque(true);
        countBadge.setPreferredSize(new Dimension(36, 24));
        countBadge.setBorder(new EmptyBorder(0, 6, 0, 6));

        row.add(nameLbl,     BorderLayout.WEST);
        row.add(countBadge,  BorderLayout.EAST);
        return row;
    }

    private JPanel buildPriceSummary() {
        JPanel card = roundCard();

        JLabel title = new JLabel("Current Service Prices");
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(title);
        card.add(Box.createVerticalStrut(18));

        card.add(priceRow("Normal Service (1 hr)",  String.format("RM %.2f", priceConfig.getNormalPrice())));
        card.add(Box.createVerticalStrut(12));
        card.add(priceRow("Major Service  (3 hrs)", String.format("RM %.2f", priceConfig.getMajorPrice())));
        card.add(Box.createVerticalStrut(24));

        // Appointment status breakdown
        List<String[]> appts = loadCsv(APPT_FILE);
        long completed = appts.stream().filter(a -> a.length > 4 && a[4].trim().equalsIgnoreCase("Completed")).count();
        long pending   = appts.size() - completed;

        JLabel apptTitle = new JLabel("Appointment Status");
        apptTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        apptTitle.setForeground(UIConstants.TEXT_DARK);
        card.add(apptTitle);
        card.add(Box.createVerticalStrut(10));
        card.add(buildBreakdownRow("Completed", String.valueOf(completed), new Color(80, 190, 110)));
        card.add(Box.createVerticalStrut(8));
        card.add(buildBreakdownRow("Pending / In Progress", String.valueOf(pending), new Color(230, 160, 40)));

        return card;
    }

    private JPanel priceRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_DARK);

        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_SMALL_BOLD);
        val.setForeground(UIConstants.PRIMARY);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    // ─── Computation helpers ─────────────────────────────────────

    private int countByRole(String role) {
        return accountService.getUsersByRole(role).size();
    }

    private double computeAvgRating() {
        List<String[]> rows = loadCsv(COMMENTS_FILE);
        if (rows.isEmpty()) return 0;
        double sum = 0; int count = 0;
        for (String[] row : rows) {
            try {
                // rating is column index 2
                sum += Double.parseDouble(row[2].trim());
                count++;
            } catch (Exception ignored) {}
        }
        return count == 0 ? 0 : sum / count;
    }

    private List<String[]> loadCsv(String path) {
        List<String[]> rows = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) return rows;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) rows.add(line.split(",", -1));
            }
        } catch (IOException e) { e.printStackTrace(); }
        return rows;
    }

    // ─── UI helper ───────────────────────────────────────────────

    private JPanel roundCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        return card;
    }
}