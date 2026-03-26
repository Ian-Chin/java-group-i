package view;

import model.PriceConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.List;

/**
 * Admin panel for viewing and managing service pricing and the service catalogue.
 *
 * Layout:
 *   TOP  — two tier-price cards (Normal / Major) with live appointment counts
 *   BOTTOM — service catalogue table with Add / Edit / Delete
 *
 * OOP highlights:
 *  - Encapsulation : PriceConfig hides file I/O; appointment stats hidden in loadStats()
 *  - Inheritance   : extends JPanel
 *  - Abstraction   : UIFactory hides button/field widget details
 *  - Polymorphism  : buildTierCard() builds different cards from the same method
 */
public class ServicePricePanel extends JPanel {

    private static final String APPT_FILE = "src" + File.separator + "TxtFile"
            + File.separator + "appointments.txt";

    private final PriceConfig priceConfig;

    // Tier price fields
    private JTextField normalField;
    private JTextField majorField;

    // Catalogue table
    private DefaultTableModel tableModel;
    private JTable table;

    // Live usage counts loaded from appointments.txt
    private int normalCount = 0;
    private int majorCount  = 0;

    public ServicePricePanel() {
        this.priceConfig = new PriceConfig();
        loadStats();

        setLayout(new BorderLayout(0, 20));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(28, 36, 28, 36));

        add(buildTopSection(),    BorderLayout.NORTH);
        add(buildCatalogueSection(), BorderLayout.CENTER);
    }

    // ─── TOP: Tier price cards ────────────────────────────────────

    private JPanel buildTopSection() {
        JPanel section = new JPanel(new BorderLayout(0, 14));
        section.setBackground(UIConstants.BG_CONTENT);

        JLabel heading = new JLabel("Service Pricing");
        heading.setFont(UIConstants.FONT_BODY_BOLD);
        heading.setForeground(UIConstants.TEXT_PRIMARY);
        section.add(heading, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 16, 0));
        cards.setBackground(UIConstants.BG_CONTENT);

        // Normal service card
        normalField = priceField(priceConfig.getNormalPrice());
        cards.add(buildTierCard(
                "Normal Service", "1 hour",
                new Color(80, 110, 230),
                normalField,
                normalCount + " appointments"));

        // Major service card
        majorField = priceField(priceConfig.getMajorPrice());
        cards.add(buildTierCard(
                "Major Service", "3 hours",
                new Color(40, 180, 200),
                majorField,
                majorCount + " appointments"));

        section.add(cards, BorderLayout.CENTER);

        // Save prices button
        JButton saveBtn = UIFactory.createPrimaryButton("Save Tier Prices");
        saveBtn.setMaximumSize(new Dimension(200, 44));
        saveBtn.setPreferredSize(new Dimension(200, 44));
        saveBtn.addActionListener(e -> handleSavePrices());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        btnRow.setBackground(UIConstants.BG_CONTENT);
        btnRow.add(saveBtn);
        section.add(btnRow, BorderLayout.SOUTH);

        return section;
    }

    private JPanel buildTierCard(String tier, String duration, Color accent,
                                  JTextField priceField, String statsText) {
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
        card.setLayout(new BorderLayout(0, 0));
        card.setBorder(new EmptyBorder(20, 24, 20, 20));

        // Left: tier name + duration + stats
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel tierLabel = new JLabel(tier);
        tierLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        tierLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel durLabel = new JLabel(duration);
        durLabel.setFont(UIConstants.FONT_SMALL);
        durLabel.setForeground(UIConstants.TEXT_MUTED);
        durLabel.setBorder(new EmptyBorder(2, 0, 8, 0));

        JLabel statsLabel = new JLabel(statsText);
        statsLabel.setFont(UIConstants.FONT_SMALL);
        statsLabel.setForeground(accent);

        left.add(tierLabel);
        left.add(durLabel);
        left.add(statsLabel);

        // Right: RM label + price input
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);

        JLabel rm = new JLabel("RM");
        rm.setFont(new Font("SansSerif", Font.BOLD, 14));
        rm.setForeground(UIConstants.TEXT_MUTED);

        priceField.setMaximumSize(new Dimension(100, 38));
        priceField.setPreferredSize(new Dimension(100, 38));

        right.add(rm);
        right.add(priceField);

        card.add(left,  BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    // ─── BOTTOM: Service catalogue ───────────────────────────────

    private JPanel buildCatalogueSection() {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(UIConstants.BG_CONTENT);

        // Section header row
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_CONTENT);

        JLabel heading = new JLabel("Service Catalogue");
        heading.setFont(UIConstants.FONT_BODY_BOLD);
        heading.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(heading, BorderLayout.WEST);

        JButton addBtn = new JButton("+ Add Service");
        addBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(UIConstants.PRIMARY);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(120, 32));
        addBtn.addActionListener(e -> showServiceDialog(-1, "", PriceConfig.TIER_NORMAL));
        header.add(addBtn, BorderLayout.EAST);

        section.add(header, BorderLayout.NORTH);
        section.add(buildTable(), BorderLayout.CENTER);
        return section;
    }

    private JScrollPane buildTable() {
        String[] cols = {"#", "Service Name", "Tier", "Price (RM)"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);

        // Style
        table.setRowHeight(38);
        table.setFont(UIConstants.FONT_BODY);
        table.setForeground(UIConstants.TEXT_DARK);
        table.setGridColor(new Color(235, 235, 240));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(230, 235, 255));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(new Color(248, 248, 252));
        table.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_DEFAULT));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(36);
        table.getColumnModel().getColumn(1).setPreferredWidth(260);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.getColumnModel().getColumn(3).setPreferredWidth(100);

        // Centre # and price columns
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centre);
        table.getColumnModel().getColumn(3).setCellRenderer(centre);

        // Colour the tier column
        table.getColumnModel().getColumn(2).setCellRenderer(new TierRenderer());

        // Double-click to edit
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        // Populate
        refreshTable();

        // Action buttons below the table
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(UIConstants.BG_CARD);

        JButton editBtn = smallBtn("Edit",   UIConstants.PRIMARY);
        JButton delBtn  = smallBtn("Delete", UIConstants.TEXT_DANGER);
        editBtn.addActionListener(e -> editSelected());
        delBtn .addActionListener(e -> deleteSelected());
        actions.add(editBtn);
        actions.add(delBtn);

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIConstants.BG_CARD);
        tableCard.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        tableCard.add(actions, BorderLayout.SOUTH);

        JScrollPane scroll = new JScrollPane(tableCard);
        scroll.setBorder(null);
        return scroll;
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<String[]> services = priceConfig.getServices();
        for (int i = 0; i < services.size(); i++) {
            String[] s = services.get(i);
            double price = PriceConfig.TIER_MAJOR.equals(s[1])
                    ? priceConfig.getMajorPrice()
                    : priceConfig.getNormalPrice();
            tableModel.addRow(new Object[]{
                    i + 1,
                    s[0],
                    s[1],
                    String.format("%.2f", price)
            });
        }
    }

    // ─── Add / Edit dialog ───────────────────────────────────────

    /**
     * @param editIndex  row index to edit, or -1 for a new service
     */
    private void showServiceDialog(int editIndex, String currentName, String currentTier) {
        boolean isEdit = editIndex >= 0;
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Service" : "Add Service", true);
        dialog.setSize(400, 220);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(24, 32, 24, 32));

        form.add(UIFactory.createFieldLabel("Service Name"));
        form.add(Box.createVerticalStrut(6));
        JTextField nameField = UIFactory.createTextField("e.g. Oil & Filter Change");
        if (isEdit) { nameField.setText(currentName); nameField.setForeground(Color.BLACK); }
        form.add(nameField);
        form.add(Box.createVerticalStrut(14));

        form.add(UIFactory.createFieldLabel("Tier"));
        form.add(Box.createVerticalStrut(6));
        JComboBox<String> tierCombo = new JComboBox<>(
                new String[]{PriceConfig.TIER_NORMAL, PriceConfig.TIER_MAJOR});
        tierCombo.setSelectedItem(currentTier);
        tierCombo.setFont(UIConstants.FONT_BODY);
        tierCombo.setMaximumSize(UIConstants.FIELD_SIZE);
        tierCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(tierCombo);
        form.add(Box.createVerticalStrut(20));

        JButton saveBtn = UIFactory.createPrimaryButton(isEdit ? "Save Changes" : "Add Service");
        saveBtn.addActionListener(e -> {
            String name = UIFactory.getFieldValue(nameField, "e.g. Oil & Filter Change").trim();
            String tier = (String) tierCombo.getSelectedItem();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Service name cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            List<String[]> services = priceConfig.getServices();
            if (isEdit) {
                services.get(editIndex)[0] = name;
                services.get(editIndex)[1] = tier;
            } else {
                services.add(new String[]{name, tier});
            }
            priceConfig.setServices(services);
            priceConfig.saveServices();
            refreshTable();
            dialog.dispose();
        });
        form.add(saveBtn);

        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a service to edit."); return; }
        String name = (String) tableModel.getValueAt(row, 1);
        String tier = (String) tableModel.getValueAt(row, 2);
        showServiceDialog(row, name, tier);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a service to delete."); return; }
        String name = (String) tableModel.getValueAt(row, 1);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + name + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            priceConfig.removeService(row);
            priceConfig.saveServices();
            refreshTable();
        }
    }

    // ─── Tier price save ─────────────────────────────────────────

    private void handleSavePrices() {
        double normal, major;
        try {
            normal = Double.parseDouble(normalField.getText().trim());
            major  = Double.parseDouble(majorField.getText().trim());
        } catch (NumberFormatException ex) {
            error("Prices must be valid numbers (e.g. 80.00)"); return;
        }
        if (normal <= 0 || major <= 0) { error("Prices must be greater than zero."); return; }
        if (major <= normal) {
            JOptionPane.showMessageDialog(this,
                    "Major service price should be higher than Normal service price.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }
        priceConfig.setNormalPrice(normal);
        priceConfig.setMajorPrice(major);
        if (priceConfig.savePrices()) {
            refreshTable(); // update Price column
            JOptionPane.showMessageDialog(this,
                    "Prices saved!\n  Normal: RM " + normal + "\n  Major:  RM " + major,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } else {
            error("Failed to save prices. Please try again.");
        }
    }

    // ─── Load appointment stats ───────────────────────────────────

    private void loadStats() {
        normalCount = 0; majorCount = 0;
        File file = new File(APPT_FILE);
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] parts = line.split(",");
                if (parts.length < 4) continue;
                String type = parts[3].trim();
                if (PriceConfig.TIER_NORMAL.equalsIgnoreCase(type)) normalCount++;
                else if (PriceConfig.TIER_MAJOR.equalsIgnoreCase(type))  majorCount++;
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private JTextField priceField(double value) {
        JTextField f = new JTextField(String.valueOf(value));
        f.setFont(new Font("SansSerif", Font.BOLD, 16));
        f.setForeground(UIConstants.TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(4, 8, 4, 8)));
        f.setHorizontalAlignment(SwingConstants.RIGHT);
        return f;
    }

    private JButton smallBtn(String text, Color fg) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_SMALL);
        b.setForeground(fg);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createLineBorder(fg, 1));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(80, 30));
        return b;
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ─── Tier colour renderer ─────────────────────────────────────

    /** Colours Normal = blue, Major = teal in the table. */
    private static class TierRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object value, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            String tier = value == null ? "" : value.toString();
            if (PriceConfig.TIER_NORMAL.equals(tier)) {
                setForeground(new Color(50, 90, 200));
                setBackground(new Color(230, 237, 255));
            } else {
                setForeground(new Color(10, 120, 150));
                setBackground(new Color(220, 245, 250));
            }
            if (sel) { setBackground(new Color(200, 215, 255)); setForeground(UIConstants.TEXT_DARK); }
            setOpaque(true);
            return this;
        }
    }
}