package view;
 
import model.StaffReviewService;
import model.StaffReviewService.StaffReview;
import model.User;
 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
 
public class TechReview extends JPanel {
 
    private static final Color BG = new Color(245, 246, 250);
    private static final Color CARD = Color.WHITE;
    private static final Color BORDER = new Color(225, 228, 235);
    private static final Color TEXT = new Color(30,  35,  50);
    private static final Color MUTED = new Color(110, 118, 140);
    private static final Color BLUE  = new Color(80,  110, 230);
    private static final Color YELLOW = new Color(255, 193,  7);
 
    private static final String[] COLUMNS = {
        "Comment ID", "Appointment ID", "Customer", "Rating", "Feedback", "Date"
    };
 
    private final AppFrame app;
    private final StaffReviewService reviewService = new StaffReviewService();
 
    private DefaultTableModel tableModel;
    private JTable table;
    private JTextField searchField;
    private JComboBox<String> ratingFilter;
 
    private JLabel avgRatingLabel;
    private JLabel totalReviewsLabel;
    private JPanel starsPanel;
 
    private List<StaffReview> allMyReviews;
 
    public TechReview(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        setBackground(BG);
 
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(24, 28, 28, 28));
        page.add(buildSubtitle(), BorderLayout.NORTH);
        page.add(buildBody(), BorderLayout.CENTER);
 
        JScrollPane outer = new JScrollPane(page);
        outer.setBorder(null);
        outer.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outer.getVerticalScrollBar().setUnitIncrement(16);
        outer.getViewport().setBackground(BG);
        add(outer, BorderLayout.CENTER);
    }
 
    public void refresh() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
 
        allMyReviews = reviewService.getAllReviews().stream()
                .filter(r -> r.technicianId.equalsIgnoreCase(user.getUserId()))
                .collect(java.util.stream.Collectors.toList());
 
        if (searchField  != null) searchField.setText("");
        if (ratingFilter != null) ratingFilter.setSelectedIndex(0);
 
        applyFilters();
        refreshSummary();
    }
 
    private JPanel buildSubtitle() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));
        JLabel lbl = new JLabel("View comments and ratings left by customers for your service.");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setForeground(MUTED);
        p.add(lbl);
        return p;
    }
 
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(buildSummaryRow(), BorderLayout.NORTH);
        body.add(buildTableCard(), BorderLayout.CENTER);
        return body;
    }
 
    private JPanel buildSummaryRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(buildAvgRatingCard());
        row.add(buildStarBreakdownCard());
        return row;
    }
 
    private JPanel buildAvgRatingCard() {
        JPanel card = roundedCardWithBar(YELLOW);
 
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
 
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
 
        avgRatingLabel = new JLabel("0.0");
        avgRatingLabel.setFont(new Font("SansSerif", Font.BOLD, 52));
        avgRatingLabel.setForeground(BLUE);
        avgRatingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        starsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        starsPanel.setOpaque(false);
 
        totalReviewsLabel = new JLabel("0 reviews");
        totalReviewsLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        totalReviewsLabel.setForeground(MUTED);
        totalReviewsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        inner.add(avgRatingLabel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(starsPanel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(totalReviewsLabel);
 
        content.add(inner);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
 
    private JPanel buildStarBreakdownCard() {
        JPanel card = roundedCardWithBar(BLUE);
        card.setLayout(new BorderLayout());
 
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
 
        JLabel heading = new JLabel("Rating Breakdown");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(TEXT);
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        content.add(heading, BorderLayout.NORTH);
 
        JPanel bars = new JPanel();
        bars.setName("barsPanel");
        bars.setLayout(new BoxLayout(bars, BoxLayout.Y_AXIS));
        bars.setOpaque(false);
 
        for (int i = 5; i >= 1; i--) {
            bars.add(buildBarRow(i + " \u2605", 0, 1));
            bars.add(Box.createVerticalStrut(6));
        }
 
        content.add(bars, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
 
    private JPanel buildBarRow(String label, int count, int max) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
 
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(TEXT);
        lbl.setPreferredSize(new Dimension(40, 16));
        row.add(lbl, BorderLayout.WEST);
 
        JPanel track = new JPanel(new BorderLayout());
        track.setBackground(new Color(230, 230, 235));
        track.setBorder(new EmptyBorder(4, 0, 4, 0));
        JPanel fill = new JPanel();
        fill.setBackground(YELLOW);
        double ratio = (max == 0) ? 0.0 : (double) count / max;
        fill.setPreferredSize(new Dimension((int)(180 * ratio), 8));
        track.add(fill, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);
 
        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLbl.setForeground(MUTED);
        countLbl.setPreferredSize(new Dimension(20, 16));
        row.add(countLbl, BorderLayout.EAST);
 
        return row;
    }
 
    private JPanel buildTableCard() {
        JPanel card = roundedCard();
        card.setLayout(new BorderLayout());
        card.add(buildControls(), BorderLayout.NORTH);
 
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
 
        table = new JTable(tableModel);
        table.setRowHeight(46);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setGridColor(new Color(240, 240, 245));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(235, 240, 255));
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setCursor(new Cursor(Cursor.HAND_CURSOR));
 
        JTableHeader hdr = table.getTableHeader();
        hdr.setReorderingAllowed(false);
        hdr.setFont(new Font("SansSerif", Font.BOLD, 12));
        hdr.setBackground(new Color(250, 250, 253));
        hdr.setForeground(MUTED);
        hdr.setPreferredSize(new Dimension(0, 44));
        hdr.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 238)));
 
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setHorizontalAlignment(CENTER);
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                return this;
            }
        };
 
        for (int i = 0; i < COLUMNS.length; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(centre);
 
        table.getColumnModel().getColumn(3).setCellRenderer((t, v, sel, foc, r, c) -> {
            String stars = v != null ? v.toString() : "";
            JLabel lbl = new JLabel(stars, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
            lbl.setForeground(YELLOW);
            JPanel wrap = new JPanel(new GridBagLayout());
            wrap.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
            wrap.add(lbl);
            return wrap;
        });
 
        table.getColumnModel().getColumn(4).setCellRenderer((t, v, sel, foc, r, c) -> {
            String text = v != null ? v.toString() : "";
            JLabel lbl = new JLabel(text);
            lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
            lbl.setForeground(TEXT);
            lbl.setBorder(new EmptyBorder(0, 10, 0, 10));
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
            wrap.add(lbl, BorderLayout.CENTER);
            return wrap;
        });
 
        int[] widths = {90, 110, 120, 100, 260, 100};
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
 
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) showDetailDialog(row);
            }
        });
 
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 420));
        card.add(scroll, BorderLayout.CENTER);
 
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 240)),
                new EmptyBorder(10, 18, 10, 18)));
        JLabel hint = new JLabel("Click any row to view full comment details.");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        footer.add(hint, BorderLayout.WEST);
        card.add(footer, BorderLayout.SOUTH);
 
        return card;
    }
 
    private JPanel buildControls() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 10, 0));
 
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(200, 30));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true), new EmptyBorder(2, 8, 2, 8)));
        searchField.setToolTipText("Search comments...");
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilters(); }
        });
 
        ratingFilter = new JComboBox<>(new String[]{
            "All Ratings", "5 Stars", "4 Stars", "3 Stars", "2 Stars", "1 Star"
        });
        ratingFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        ratingFilter.setPreferredSize(new Dimension(150, 30));
        ratingFilter.setBackground(Color.WHITE);
        ratingFilter.addActionListener(e -> applyFilters());
 
        row.add(searchField);
        row.add(ratingFilter);
        return row;
    }
 
    private void applyFilters() {
        if (tableModel == null || allMyReviews == null) return;
 
        String query = searchField  != null ? searchField.getText().trim().toLowerCase() : "";
        int ratingIdx  = ratingFilter != null ? ratingFilter.getSelectedIndex() : 0;
 
        tableModel.setRowCount(0);
 
        for (StaffReview r : allMyReviews) {
            if (ratingIdx > 0 && reviewService.countByStarLevel(
                    java.util.Arrays.asList(r), ratingIdx) == 0) continue;
 
            if (!query.isEmpty()) {
                boolean hit = r.commentId.toLowerCase().contains(query)
                        || r.appointmentId.toLowerCase().contains(query)
                        || r.customerId.toLowerCase().contains(query)
                        || r.feedbackText.toLowerCase().contains(query)
                        || r.date.toLowerCase().contains(query)
                        || String.valueOf(r.rating).contains(query);
                if (!hit) continue;
            }
 
            String custName = lookupName(r.customerId);
            String starString = reviewService.buildStarString(r.rating);
 
            tableModel.addRow(new Object[]{
                r.commentId,
                r.appointmentId,
                custName + " (" + r.customerId + ")",
                starString,
                r.feedbackText,
                r.date
            });
        }
    }
 
    private void refreshSummary() {
        if (avgRatingLabel == null || totalReviewsLabel == null) return;
 
        double avg   = reviewService.calculateAverageRating(allMyReviews);
        int    total = allMyReviews.size();
 
        avgRatingLabel.setText(String.format("%.1f", avg));
        totalReviewsLabel.setText(total + (total == 1 ? " review" : " reviews"));
 
        starsPanel.removeAll();
        String stars = reviewService.buildStarString(avg);
        for (char c : stars.toCharArray()) {
            JLabel s = new JLabel(String.valueOf(c));
            s.setFont(new Font("SansSerif", Font.PLAIN, 20));
            s.setForeground(YELLOW);
            starsPanel.add(s);
        }
        starsPanel.revalidate();
        starsPanel.repaint();
 
        JPanel barsPanel = findBarsPanel();
        if (barsPanel != null) {
            barsPanel.removeAll();
            int max = Math.max(total, 1);
            for (int i = 5; i >= 1; i--) {
                int count = reviewService.countByStarLevel(allMyReviews, i);
                barsPanel.add(buildBarRow(i + " \u2605", count, max));
                barsPanel.add(Box.createVerticalStrut(6));
            }
            barsPanel.revalidate();
            barsPanel.repaint();
        }
    }
 
    private JPanel findBarsPanel() {
        for (Component c : getComponents()) {
            JPanel found = searchForBarsPanel(c);
            if (found != null) return found;
        }
        return null;
    }
 
    private JPanel searchForBarsPanel(Component c) {
        if (c instanceof JPanel) {
            JPanel p = (JPanel) c;
            if ("barsPanel".equals(p.getName())) return p;
            for (Component child : p.getComponents()) {
                JPanel found = searchForBarsPanel(child);
                if (found != null) return found;
            }
        }
        if (c instanceof JScrollPane) {
            return searchForBarsPanel(((JScrollPane) c).getViewport().getView());
        }
        return null;
    }
 
    private void showDetailDialog(int row) {
        String commentId = (String) tableModel.getValueAt(row, 0);
        String appointmentId = (String) tableModel.getValueAt(row, 1);
        String customer = (String) tableModel.getValueAt(row, 2);
        String rating = (String) tableModel.getValueAt(row, 3);
        String feedback = (String) tableModel.getValueAt(row, 4);
        String date = (String) tableModel.getValueAt(row, 5);
 
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Comment — " + commentId, true);
        dlg.setSize(480, 360);
        dlg.setResizable(false);
        dlg.setLocationRelativeTo(this);
 
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(28, 36, 24, 36));
 
        JLabel title = new JLabel("Customer Comment Details");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(TEXT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(title);
        form.add(Box.createVerticalStrut(4));
 
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        form.add(sep);
        form.add(Box.createVerticalStrut(18));
 
        String[][] fields = {
            {"Comment ID", commentId},
            {"Appointment ID", appointmentId},
            {"Customer", customer},
            {"Date", date},
        };
 
        for (String[] field : fields) {
            JPanel fieldRow = new JPanel(new BorderLayout(12, 0));
            fieldRow.setOpaque(false);
            fieldRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            fieldRow.setAlignmentX(Component.LEFT_ALIGNMENT);
 
            JLabel key = new JLabel(field[0]);
            key.setFont(new Font("SansSerif", Font.BOLD, 13));
            key.setForeground(MUTED);
            key.setPreferredSize(new Dimension(120, 20));
 
            JLabel val = new JLabel(field[1]);
            val.setFont(new Font("SansSerif", Font.PLAIN, 13));
            val.setForeground(TEXT);
 
            fieldRow.add(key, BorderLayout.WEST);
            fieldRow.add(val, BorderLayout.CENTER);
            form.add(fieldRow);
            form.add(Box.createVerticalStrut(8));
        }
 
        JPanel ratingRow = new JPanel(new BorderLayout(12, 0));
        ratingRow.setOpaque(false);
        ratingRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        ratingRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel ratingKey = new JLabel("Rating");
        ratingKey.setFont(new Font("SansSerif", Font.BOLD, 13));
        ratingKey.setForeground(MUTED);
        ratingKey.setPreferredSize(new Dimension(120, 20));
        JLabel ratingVal = new JLabel(rating);
        ratingVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        ratingVal.setForeground(YELLOW);
        ratingRow.add(ratingKey, BorderLayout.WEST);
        ratingRow.add(ratingVal, BorderLayout.CENTER);
        form.add(ratingRow);
        form.add(Box.createVerticalStrut(12));
 
        JLabel feedbackKey = new JLabel("Feedback");
        feedbackKey.setFont(new Font("SansSerif", Font.BOLD, 13));
        feedbackKey.setForeground(MUTED);
        feedbackKey.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(feedbackKey);
        form.add(Box.createVerticalStrut(6));
 
        JTextArea feedbackArea = new JTextArea(feedback);
        feedbackArea.setFont(new Font("SansSerif", Font.PLAIN, 13));
        feedbackArea.setForeground(TEXT);
        feedbackArea.setBackground(new Color(245, 246, 250));
        feedbackArea.setEditable(false);
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));
        feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(feedbackArea);
        form.add(Box.createVerticalGlue());
        form.add(Box.createVerticalStrut(16));
 
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
 
        JButton closeBtn = dialogBtn("Close", Color.WHITE, TEXT, BORDER);
        closeBtn.addActionListener(e -> dlg.dispose());
        btnRow.add(closeBtn);
        form.add(btnRow);
 
        dlg.setContentPane(form);
        dlg.setVisible(true);
    }
 
    private String lookupName(String id) {
        for (model.User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id)) return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }
 
    private JPanel roundedCardWithBar(Color barColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, 10, getHeight(), 14, 14);
                g2.setColor(CARD);
                g2.fillRect(5, 0, 10, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 20));
        return card;
    }
 
    private JPanel roundedCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }
 
    private JButton dialogBtn(String label, Color bg, Color fg, Color border) {
        JButton btn = new JButton(label) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true; repaint(); }
                public void mouseExited(MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
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
        btn.setPreferredSize(new Dimension(100, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
 