package view;

import model.AccountService;
import model.DashboardData;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.RoundRectangle2D;
import java.util.LinkedHashMap;
import java.util.Map;
import util.PdfUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ReportPanel extends JPanel {

    private static final Color C_BLUE   = new Color(80,  110, 230);
    private static final Color C_TEAL   = new Color(40,  180, 200);
    private static final Color C_GREEN  = new Color(80,  190, 110);
    private static final Color C_AMBER  = new Color(230, 160, 40);
    private static final Color C_CORAL  = new Color(230, 100, 80);
    private static final Color C_PURPLE = new Color(160, 80,  230);

    private static final Color[] STATUS_COLORS   = {C_GREEN, C_AMBER, C_CORAL};
    private static final Color[] SERVICE_COLORS  = {C_BLUE,  C_PURPLE};
    private static final Color[] WORKLOAD_COLORS = {C_BLUE, C_TEAL, C_GREEN, C_AMBER, C_CORAL, C_PURPLE};
    private static final Color[] WEEKLY_COLORS   = {C_BLUE};
    private static final Color[] REVENUE_COLORS  = {C_TEAL};
    private static final Color[] RATING_COLORS   = {C_AMBER};
    private static final Color[] ROLE_COLORS     = {C_PURPLE, C_BLUE, C_GREEN, C_AMBER};
    private static final Color[] METHOD_COLORS   = {C_GREEN, C_BLUE, C_TEAL};
    private static final Color[] VEHICLE_COLORS  = {C_BLUE, C_CORAL};

    private final DashboardData  data;
    private final AccountService accountService;

    public ReportPanel(AccountService accountService) {
        this.accountService = accountService;
        this.data   = new DashboardData();

        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);

        JScrollPane scroll = new JScrollPane(buildDashboard());
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildDashboard() {
        JPanel dash = new JPanel();
        dash.setLayout(new BoxLayout(dash, BoxLayout.Y_AXIS));
        dash.setBackground(UIConstants.BG_CONTENT);
        dash.setBorder(new EmptyBorder(28, 36, 36, 36));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        JPanel titleBox = new JPanel();
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.setOpaque(false);

        JLabel title = new JLabel("Analytics Report");
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        titleBox.add(title);

        JLabel sub = new JLabel("Service performance  \u00B7  Staff workload  \u00B7  Revenue  \u00B7  Vehicles");
        sub.setFont(UIConstants.FONT_SMALL);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(new EmptyBorder(4, 0, 24, 0));
        titleBox.add(sub);

        header.add(titleBox, BorderLayout.WEST);

        JButton exportBtn = new JButton("Export PDF");
        exportBtn.setFocusPainted(false);
        exportBtn.setFont(UIConstants.FONT_BODY_BOLD);
        exportBtn.setBackground(C_BLUE);
        exportBtn.setForeground(Color.WHITE);
        exportBtn.addActionListener(e -> exportToPDF(dash, exportBtn));
        
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(exportBtn);
        header.add(btnPanel, BorderLayout.EAST);

        dash.add(header);

        // Section 1 — Appointment overview
        dash.add(sectionLabel("Appointment Overview"));
        dash.add(vgap(10));
        dash.add(row3(
            buildPieChart("By Status",       data.statusBreakdown(),      STATUS_COLORS),
            buildPieChart("By Service Type", data.serviceTypeBreakdown(), SERVICE_COLORS),
            buildBarChart("Technician Workload",
                toNumberMap(data.appointmentsByTechnician()), WORKLOAD_COLORS, false)
        ));
        dash.add(vgap(22));

        // Section 2 — Revenue & ratings (revenueByMonth now reads real payment amounts)
        dash.add(sectionLabel("Trends & Revenue"));
        dash.add(vgap(10));
        dash.add(row3(
            buildBarChart("Weekly Appointment Volume",
                toNumberMap(data.appointmentsByWeek()), WEEKLY_COLORS, false),
            buildBarChart("Monthly Revenue (RM)",
                data.revenueByMonth(), REVENUE_COLORS, true),
            buildBarChart("Avg Rating by Technician",
                data.avgRatingByTechnician(), RATING_COLORS, true)
        ));
        dash.add(vgap(22));

        // Section 3 — Payments & vehicles
        dash.add(sectionLabel("Payments & Fleet"));
        dash.add(vgap(10));
        dash.add(row3(
            buildPieChart("Payment Method",   data.paymentMethodBreakdown(), METHOD_COLORS),
            buildPieChart("Vehicle Type",     data.vehicleTypeBreakdown(),   VEHICLE_COLORS),
            buildBarChart("Top Vehicle Brands",
                toNumberMap(data.topVehicleBrands()), WORKLOAD_COLORS, false)
        ));
        dash.add(vgap(22));

        // Section 4 — Staff & status summary
        dash.add(sectionLabel("Staff & Status Summary"));
        dash.add(vgap(10));
        dash.add(row2(
            buildHBarChart("Staff by Role",      buildRoleMap(),                   ROLE_COLORS),
            buildHBarChart("Appointment Status", toLinked(data.statusBreakdown()), STATUS_COLORS)
        ));

        return dash;
    }

    // ── Pie (donut) chart ──────────────────────────────────────────

    private JPanel buildPieChart(String title, Map<String, Integer> slices, Color[] colors) {
        JPanel card = chartCard(title);

        JPanel draw = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int total = slices.values().stream().mapToInt(Integer::intValue).sum();
                if (total == 0) { drawEmpty(g2, getWidth(), getHeight()); return; }

                int dim = Math.min(getWidth() - 20, getHeight() - 10);
                int x   = (getWidth()  - dim) / 2;
                int y   = (getHeight() - dim) / 2;
                int hole = (int)(dim * 0.44);
                int hx  = x + (dim - hole) / 2;
                int hy  = y + (dim - hole) / 2;

                double angle = 90; int ci = 0;
                for (Map.Entry<String, Integer> e : slices.entrySet()) {
                    double sweep = e.getValue() * 360.0 / total;
                    g2.setColor(colors[ci % colors.length]);
                    g2.fill(new Arc2D.Double(x, y, dim, dim, angle, sweep, Arc2D.PIE));
                    angle -= sweep; ci++;
                }
                g2.setColor(UIConstants.BG_CARD);
                g2.fillOval(hx, hy, hole, hole);

                g2.setColor(UIConstants.TEXT_PRIMARY);
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                String s = String.valueOf(total);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(s, hx + (hole - fm.stringWidth(s)) / 2,
                        hy + hole / 2 + fm.getAscent() / 2 - 2);
                g2.dispose();
            }
        };
        draw.setOpaque(false);
        draw.setPreferredSize(new Dimension(0, 155));

        JPanel legend = legendPanel();
        int total = slices.values().stream().mapToInt(Integer::intValue).sum();
        int ci = 0;
        for (Map.Entry<String, Integer> e : slices.entrySet()) {
            int pct = total == 0 ? 0 : (int)(e.getValue() * 100.0 / total);
            legend.add(legendRow(e.getKey(), e.getValue() + "  (" + pct + "%)", colors[ci % colors.length]));
            legend.add(Box.createVerticalStrut(4));
            ci++;
        }
        card.add(draw);
        card.add(legend);
        return card;
    }

    // ── Vertical bar chart ─────────────────────────────────────────

    private JPanel buildBarChart(String title, Map<String, ? extends Number> bars,
                                  Color[] colors, boolean isDecimal) {
        JPanel card = chartCard(title);

        JPanel draw = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bars.isEmpty()) { drawEmpty((Graphics2D) g.create(), getWidth(), getHeight()); return; }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int pad = 38, bpad = 30;
                int w   = getWidth()  - pad - 8;
                int h   = getHeight() - bpad - 8;
                int n   = bars.size();
                if (w <= 0 || h <= 0) { g2.dispose(); return; }

                double maxVal = bars.values().stream().mapToDouble(Number::doubleValue).max().orElse(1);
                if (maxVal == 0) maxVal = 1;

                for (int i = 1; i <= 4; i++) {
                    int gy = 8 + (int)(h * (1.0 - i / 4.0));
                    g2.setColor(new Color(230, 230, 235));
                    g2.setStroke(new BasicStroke(0.5f));
                    g2.drawLine(pad, gy, pad + w, gy);
                    g2.setColor(UIConstants.TEXT_MUTED);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    double gv = maxVal * i / 4.0;
                    g2.drawString(isDecimal ? String.format("%.1f", gv) : String.valueOf((int)gv), 1, gy + 4);
                }

                int barW = Math.max(8, w / n - 8);
                int gap  = Math.max(2, (w - n * barW) / (n + 1));
                String[] keys = bars.keySet().toArray(new String[0]);
                for (int i = 0; i < n; i++) {
                    double val  = bars.get(keys[i]).doubleValue();
                    int    barH = (int)(h * val / maxVal);
                    int    bx   = pad + gap + i * (barW + gap);
                    int    by   = 8 + h - barH;

                    g2.setColor(colors[i % colors.length]);
                    g2.fill(new RoundRectangle2D.Float(bx, by, barW, barH, 5, 5));

                    String vs = isDecimal ? String.format("%.1f", val) : String.valueOf((int)val);
                    g2.setColor(UIConstants.TEXT_DARK);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 9));
                    FontMetrics fm = g2.getFontMetrics();
                    int lx = bx + (barW - fm.stringWidth(vs)) / 2;
                    g2.drawString(vs, lx, barH > 14 ? by + 12 : by - 3);

                    g2.setColor(UIConstants.TEXT_MUTED);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    String xl = keys[i].length() > 8 ? keys[i].substring(0, 7) + "\u2026" : keys[i];
                    int xlx = bx + (barW - g2.getFontMetrics().stringWidth(xl)) / 2;
                    g2.drawString(xl, xlx, getHeight() - 6);
                }
                g2.dispose();
            }
        };
        draw.setOpaque(false);
        draw.setPreferredSize(new Dimension(0, 185));
        card.add(draw);
        return card;
    }

    // ── Horizontal bar chart ───────────────────────────────────────

    private JPanel buildHBarChart(String title, Map<String, Integer> bars, Color[] colors) {
        JPanel card = chartCard(title);

        JPanel draw = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bars.isEmpty()) { drawEmpty((Graphics2D) g.create(), getWidth(), getHeight()); return; }
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int labelW  = 100, rightPad = 40;
                int usableW = getWidth() - labelW - rightPad;
                int n = bars.size();
                int rowH = (getHeight() - 12) / Math.max(n, 1);
                int barH = Math.min(22, rowH - 8);
                int maxVal = bars.values().stream().mapToInt(Integer::intValue).max().orElse(1);
                if (maxVal == 0) maxVal = 1;

                String[] keys = bars.keySet().toArray(new String[0]);
                for (int i = 0; i < n; i++) {
                    int val  = bars.get(keys[i]);
                    int barW = (int)((double) val / maxVal * usableW);
                    int by   = 8 + i * rowH + (rowH - barH) / 2;

                    g2.setColor(UIConstants.TEXT_DARK);
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(keys[i], labelW - fm.stringWidth(keys[i]) - 8,
                            by + barH / 2 + fm.getAscent() / 2 - 2);

                    g2.setColor(colors[i % colors.length]);
                    g2.fill(new RoundRectangle2D.Float(labelW, by, Math.max(barW, 4), barH, 5, 5));

                    g2.setColor(UIConstants.TEXT_DARK);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.drawString(String.valueOf(val), labelW + barW + 6,
                            by + barH / 2 + g2.getFontMetrics().getAscent() / 2 - 1);
                }
                g2.dispose();
            }
        };
        draw.setOpaque(false);
        draw.setPreferredSize(new Dimension(0, 150));
        card.add(draw);
        return card;
    }

    // ── Layout helpers ─────────────────────────────────────────────

    private JPanel row3(JPanel a, JPanel b, JPanel c) {
        JPanel row = new JPanel(new GridLayout(1, 3, 16, 0));
        row.setBackground(UIConstants.BG_CONTENT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));
        row.add(a); row.add(b); row.add(c);
        return row;
    }

    private JPanel row2(JPanel a, JPanel b) {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setBackground(UIConstants.BG_CONTENT);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        row.add(a); row.add(b);
        return row;
    }

    private JPanel chartCard(String title) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel lbl = new JLabel(title);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(lbl);
        card.add(Box.createVerticalStrut(8));

        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(8));
        return card;
    }

    private JPanel legendPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 0, 0, 0));
        return p;
    }

    private JPanel legendRow(String key, String value, Color color) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dot = new JLabel("\u25CF");
        dot.setFont(new Font("SansSerif", Font.PLAIN, 11));
        dot.setForeground(color);

        JLabel name = new JLabel(key);
        name.setFont(UIConstants.FONT_SMALL);
        name.setForeground(UIConstants.TEXT_DARK);

        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_SMALL);
        val.setForeground(UIConstants.TEXT_MUTED);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.setOpaque(false);
        left.add(dot); left.add(name);
        row.add(left, BorderLayout.WEST);
        row.add(val,  BorderLayout.EAST);
        return row;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private Component vgap(int h) { return Box.createVerticalStrut(h); }

    private void drawEmpty(Graphics2D g2, int w, int h) {
        g2.setColor(UIConstants.TEXT_MUTED);
        g2.setFont(UIConstants.FONT_SMALL);
        g2.drawString("No data yet", w / 2 - 30, h / 2);
        g2.dispose();
    }

    // ── Data helpers ───────────────────────────────────────────────

    private Map<String, Integer> buildRoleMap() {
        Map<String, Integer> m = new LinkedHashMap<>();
        m.put("Admin",      accountService.getUsersByRole("admin").size());
        m.put("Staff",      accountService.getUsersByRole("staff").size());
        m.put("Technician", accountService.getUsersByRole("technician").size());
        m.put("Customer",   accountService.getUsersByRole("customer").size());
        return m;
    }

    private Map<String, Number> toNumberMap(Map<String, Integer> src) {
        Map<String, Number> m = new LinkedHashMap<>();
        src.forEach(m::put);
        return m;
    }

    private Map<String, Integer> toLinked(Map<String, Integer> src) {
        return new LinkedHashMap<>(src);
    }
    
    private void exportToPDF(JPanel panelToExport, JButton btnToHide) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Report as PDF");
        fileChooser.setSelectedFile(new File("Analytics_Report.pdf"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = fileChooser.getSelectedFile().getAbsolutePath();
            if (!path.toLowerCase().endsWith(".pdf")) {
                path += ".pdf";
            }
            
            try {
                if (btnToHide != null) {
                    btnToHide.setVisible(false);
                }
                
                int w = panelToExport.getWidth();
                int h = panelToExport.getHeight();
                if (w == 0 || h == 0) {
                    w = panelToExport.getPreferredSize().width;
                    h = panelToExport.getPreferredSize().height;
                    panelToExport.setSize(w, h);
                    panelToExport.doLayout();
                }
                
                BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = image.createGraphics();
                panelToExport.paint(g2);
                g2.dispose();
                
                if (btnToHide != null) {
                    btnToHide.setVisible(true);
                }
                
                // Use PdfUtil to write the image into a one-page PDF
                try {
                    PdfUtil.writeImageAsPdf(image, new File(path));
                } catch (IOException ioe) {
                    throw new RuntimeException(ioe);
                }
                
                JOptionPane.showMessageDialog(this, "Success exported to PDF!\n" + path, "Success", JOptionPane.INFORMATION_MESSAGE);
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(path));
                }
            } catch (Exception ex) {
                if (btnToHide != null) {
                    btnToHide.setVisible(true);
                }
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error exporting PDF: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}