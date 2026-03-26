package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class CustomerManagementPanel extends JPanel {

    private final AccountService accountService;

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private JTextField searchField;
    private JLabel countLabel;

    // Form fields (reused for add and edit)
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;

    private static final String[] COLUMNS = {"#", "Name", "Email", "Actions"};

    public CustomerManagementPanel(AccountService accountService) {
        this.accountService = accountService;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);

        refreshTable();
    }

    // ─── Toolbar (search + add button) ───────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(0, 0, 14, 0));
        bar.setPreferredSize(new Dimension(0, 52));

        // Left: search with icon
        JPanel searchWrap = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = searchField != null && searchField.hasFocus();
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(focused ? UIConstants.PRIMARY : UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        searchWrap.setOpaque(false);
        searchWrap.setPreferredSize(new Dimension(280, 38));

        JLabel searchIcon = new JLabel("\u2315");
        searchIcon.setFont(new Font("SansSerif", Font.PLAIN, 16));
        searchIcon.setForeground(UIConstants.TEXT_MUTED);
        searchIcon.setBorder(new EmptyBorder(0, 12, 0, 0));
        searchIcon.setPreferredSize(new Dimension(34, 38));

        searchField = new JTextField();
        searchField.setFont(UIConstants.FONT_BODY);
        searchField.setBorder(new EmptyBorder(6, 8, 6, 12));
        searchField.setOpaque(false);
        searchField.setText("Search by name or email...");
        searchField.setForeground(Color.GRAY);
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (searchField.getText().equals("Search by name or email...")) {
                    searchField.setText(""); searchField.setForeground(Color.BLACK);
                }
                searchWrap.repaint();
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isEmpty()) {
                    searchField.setText("Search by name or email..."); searchField.setForeground(Color.GRAY);
                }
                searchWrap.repaint();
            }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { applyFilter(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { applyFilter(); }
        });

        searchWrap.add(searchIcon, BorderLayout.WEST);
        searchWrap.add(searchField, BorderLayout.CENTER);

        // Right: add button
        JButton addBtn = new JButton("\u2795  Add Customer") {
            private boolean hovering = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? UIConstants.PRIMARY_HOVER : UIConstants.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        addBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        addBtn.setForeground(Color.WHITE);
        addBtn.setContentAreaFilled(false);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(160, 38));
        addBtn.addActionListener(e -> showFormDialog(null));

        bar.add(searchWrap, BorderLayout.WEST);
        bar.add(addBtn, BorderLayout.EAST);
        return bar;
    }

    // ─── Search filter ────────────────────────────────────────────

    private void applyFilter() {
        String text = searchField.getText().trim();
        if (text.equals("Search by name or email...") || text.isEmpty()) {
            rowSorter.setRowFilter(null);
        } else {
            rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1, 2));
        }
        updateCount();
    }

    private void updateCount() {
        int visible = table.getRowCount();
        int total   = tableModel.getRowCount();
        if (countLabel != null) {
            countLabel.setText(visible == total
                    ? "Showing all " + total + " customers"
                    : "Showing " + visible + " of " + total + " customers");
        }
    }

    // ─── Table ───────────────────────────────────────────────────

    private JPanel buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(rowSorter);
        styleTable(table);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(50);   // #
        cm.getColumn(0).setMaxWidth(60);
        cm.getColumn(1).setPreferredWidth(240);  // Name
        cm.getColumn(2).setPreferredWidth(300);  // Email
        cm.getColumn(3).setPreferredWidth(140);  // Actions
        cm.getColumn(3).setMaxWidth(160);

        // Double-click to edit
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int col = table.columnAtPoint(e.getPoint());
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;

                if (col == 3) {
                    // Determine which button was clicked based on x position
                    int cellX = e.getX() - table.getCellRect(row, col, false).x;
                    if (cellX < 65) {
                        table.setRowSelectionInterval(row, row);
                        editSelected();
                    } else {
                        table.setRowSelectionInterval(row, row);
                        deleteSelected();
                    }
                } else if (e.getClickCount() == 2) {
                    editSelected();
                }
            }
        });

        // ── Table card wrapper ─────────────────────────────────────
        JPanel tableCard = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        tableCard.setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        tableCard.add(scroll, BorderLayout.CENTER);

        // Footer with count
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 240)),
                new EmptyBorder(10, 18, 10, 18)));

        countLabel = new JLabel("Showing all 0 customers");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_MUTED);
        footer.add(countLabel, BorderLayout.WEST);

        tableCard.add(footer, BorderLayout.SOUTH);
        return tableCard;
    }

    private void styleTable(JTable t) {
        t.setRowHeight(52);
        t.setFont(UIConstants.FONT_BODY);
        t.setForeground(UIConstants.TEXT_DARK);
        t.setGridColor(new Color(240, 240, 245));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setSelectionBackground(new Color(235, 240, 255));
        t.setSelectionForeground(UIConstants.TEXT_DARK);
        t.setFillsViewportHeight(true);
        t.setIntercellSpacing(new Dimension(0, 0));

        // Header style
        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(new Color(250, 250, 253));
        t.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        t.getTableHeader().setPreferredSize(new Dimension(0, 44));
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 238)));

        // # column — centre
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        t.getColumnModel().getColumn(0).setCellRenderer(centre);

        // Name column — bold text
        t.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                label.setFont(UIConstants.FONT_BODY_BOLD);
                label.setForeground(UIConstants.TEXT_PRIMARY);
                label.setBorder(new EmptyBorder(0, 12, 0, 0));
                return label;
            }
        });

        // Actions column — Edit / Delete buttons
        t.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tbl, Object val,
                    boolean sel, boolean foc, int row, int col) {
                JPanel cell = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 10));
                cell.setOpaque(true);
                cell.setBackground(sel ? tbl.getSelectionBackground() : Color.WHITE);

                cell.add(actionLabel("\u270E Edit", UIConstants.PRIMARY, new Color(235, 240, 255)));
                cell.add(actionLabel("\u2715 Delete", UIConstants.TEXT_DANGER, new Color(255, 235, 235)));
                return cell;
            }
        });
    }

    private JLabel actionLabel(String text, Color fg, Color bg) {
        JLabel lbl = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
        lbl.setForeground(fg);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.setPreferredSize(new Dimension(62, 28));
        lbl.setOpaque(false);
        lbl.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return lbl;
    }

    // ─── Data refresh ────────────────────────────────────────────

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<User> customers = accountService.getUsersByRole("customer");
        int row = 1;
        for (User u : customers) {
            tableModel.addRow(new Object[]{row++, u.getName(), u.getEmail(), ""});
        }
        updateCount();
    }

    // ─── Add / Edit dialog ───────────────────────────────────────

    private void showFormDialog(User existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Customer" : "Add Customer", true);
        dialog.setUndecorated(true);
        dialog.setSize(480, isEdit ? 440 : 560);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // ── Outer wrapper with rounded border ──────────────────────
        JPanel outer = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(210, 210, 220));
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        outer.setOpaque(false);
        outer.setBackground(Color.WHITE);

        // ── Header section ─────────────────────────────────────────
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(28, 36, 20, 36));

        // Icon circle
        JLabel icon = new JLabel(isEdit ? "\u270E" : "\u2795") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 240, 255));
                g2.fillOval(0, 0, 48, 48);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        icon.setForeground(UIConstants.PRIMARY);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setVerticalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(48, 48));
        icon.setMaximumSize(new Dimension(48, 48));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(icon);
        header.add(Box.createVerticalStrut(14));

        JLabel title = new JLabel(isEdit ? "Edit Customer" : "Add New Customer");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(6));

        JLabel subtitle = new JLabel(isEdit
                ? "Update customer details below."
                : "Fill in the details to register a new customer.");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        header.add(subtitle);

        // Close button (top-right)
        JLabel closeBtn = new JLabel("\u2715");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        closeBtn.setForeground(UIConstants.TEXT_MUTED);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setBorder(new EmptyBorder(12, 0, 0, 16));
        closeBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { dialog.dispose(); }
            @Override public void mouseEntered(MouseEvent e) { closeBtn.setForeground(UIConstants.TEXT_DANGER); }
            @Override public void mouseExited(MouseEvent e)  { closeBtn.setForeground(UIConstants.TEXT_MUTED); }
        });

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        headerRow.add(header, BorderLayout.CENTER);
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(closeBtn);
        headerRow.add(closeWrap, BorderLayout.EAST);

        outer.add(headerRow, BorderLayout.NORTH);

        // ── Divider ────────────────────────────────────────────────
        JSeparator divider = new JSeparator();
        divider.setForeground(new Color(235, 235, 240));

        // ── Form body ──────────────────────────────────────────────
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(24, 36, 8, 36));

        form.add(UIFactory.createFieldLabel("Full Name"));
        form.add(Box.createVerticalStrut(6));
        nameField = UIFactory.createTextField("Enter customer's full name");
        form.add(nameField);
        form.add(Box.createVerticalStrut(16));

        form.add(UIFactory.createFieldLabel("Email Address"));
        form.add(Box.createVerticalStrut(6));
        emailField = UIFactory.createTextField("Enter customer's email");
        form.add(emailField);
        form.add(Box.createVerticalStrut(16));

        if (!isEdit) {
            form.add(UIFactory.createFieldLabel("Password"));
            form.add(Box.createVerticalStrut(6));
            passwordField = UIFactory.createPasswordField();
            form.add(UIFactory.createPasswordFieldPanel(passwordField));
            form.add(Box.createVerticalStrut(4));

            JLabel pwHint = new JLabel("Min 8 characters, must include letters and numbers");
            pwHint.setFont(new Font("SansSerif", Font.ITALIC, 11));
            pwHint.setForeground(UIConstants.TEXT_MUTED);
            pwHint.setAlignmentX(Component.CENTER_ALIGNMENT);
            pwHint.setMaximumSize(new Dimension(380, 18));
            form.add(pwHint);
            form.add(Box.createVerticalStrut(16));
        }

        if (isEdit) {
            nameField.setText(existing.getName());
            nameField.setForeground(Color.BLACK);
            emailField.setText(existing.getEmail());
            emailField.setForeground(Color.BLACK);
        }

        // ── Button row ─────────────────────────────────────────────
        JPanel btnRow = new JPanel();
        btnRow.setLayout(new BoxLayout(btnRow, BoxLayout.X_AXIS));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(380, 48));
        btnRow.setBorder(new EmptyBorder(6, 0, 0, 0));

        JButton cancelBtn = new JButton("Cancel") {
            private boolean hovering = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? new Color(245, 245, 250) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_OUTLINE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        cancelBtn.setFont(UIConstants.FONT_BODY_BOLD);
        cancelBtn.setForeground(UIConstants.TEXT_DARK);
        cancelBtn.setContentAreaFilled(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setPreferredSize(new Dimension(0, 44));
        cancelBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton saveBtn = new JButton(isEdit ? "Save Changes" : "Add Customer") {
            private boolean hovering = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? UIConstants.PRIMARY_HOVER : UIConstants.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        saveBtn.setFont(UIConstants.FONT_BODY_BOLD);
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setContentAreaFilled(false);
        saveBtn.setBorderPainted(false);
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        saveBtn.setPreferredSize(new Dimension(0, 44));
        saveBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        saveBtn.addActionListener(e -> {
            if (isEdit) handleEdit(existing, dialog);
            else        handleAdd(dialog);
        });

        btnRow.add(cancelBtn);
        btnRow.add(Box.createHorizontalStrut(12));
        btnRow.add(saveBtn);
        form.add(btnRow);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(divider, BorderLayout.NORTH);
        center.add(form, BorderLayout.CENTER);
        outer.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.setPreferredSize(new Dimension(0, 20));
        outer.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(outer);
        dialog.setBackground(new Color(0, 0, 0, 0));
        dialog.setVisible(true);
    }

    // ─── CRUD handlers ───────────────────────────────────────────

    private void handleAdd(JDialog dialog) {
        String name  = UIFactory.getFieldValue(nameField, "Enter customer's full name");
        String email = UIFactory.getFieldValue(emailField, "Enter customer's email");
        String pw    = new String(passwordField.getPassword()).trim();

        if (name.isEmpty() || email.isEmpty() || pw.isEmpty()) {
            error("All fields are required."); return;
        }
        if (!name.matches("[a-zA-Z ]{2,50}")) {
            error("Name must be 2-50 characters (letters only)."); return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            error("Enter a valid email."); return;
        }
        if (pw.length() < 8 || !pw.matches(".*[a-zA-Z].*") || !pw.matches(".*\\d.*")) {
            error("Password must be 8+ characters with letters and numbers."); return;
        }
        if (accountService.emailExists(email)) {
            error("Email already registered."); return;
        }

        accountService.register(new User(name, email, pw, "customer"));
        dialog.dispose();
        refreshTable();
        success("Customer added successfully.");
    }

    private void handleEdit(User original, JDialog dialog) {
        String name  = UIFactory.getFieldValue(nameField, "Enter customer's full name");
        String email = UIFactory.getFieldValue(emailField, "Enter customer's email");

        if (name.isEmpty() || email.isEmpty()) {
            error("Name and email cannot be empty."); return;
        }
        if (!name.matches("[a-zA-Z ]{2,50}")) {
            error("Name must be 2-50 characters (letters only)."); return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            error("Enter a valid email."); return;
        }
        if (!email.equalsIgnoreCase(original.getEmail()) && accountService.emailExists(email)) {
            error("Email already taken."); return;
        }

        User updated = new User(name, email, original.getPassword(), "customer", original.getProfilePicture());
        accountService.updateUser(original.getEmail(), updated);
        dialog.dispose();
        refreshTable();
        success("Customer updated successfully.");
    }

    private void editSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { error("Select a row first."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String email = (String) tableModel.getValueAt(modelRow, 2);
        User user = accountService.getUsersByRole("customer").stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
        if (user != null) showFormDialog(user);
    }

    private void deleteSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) { error("Select a row first."); return; }
        int modelRow = table.convertRowIndexToModel(viewRow);
        String name  = (String) tableModel.getValueAt(modelRow, 1);
        String email = (String) tableModel.getValueAt(modelRow, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete customer \"" + name + "\"?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            accountService.deleteUser(email);
            refreshTable();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void error(String msg)   { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE);       }
    private void success(String msg) { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
}