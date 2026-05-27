package view;

import model.AccountService;
import model.User;
import view.UIConstants;
import view.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ManageStaffPanel extends JPanel {

    private final AccountService accountService;

    private DefaultTableModel  tableModel;
    private JTable             table;
    private JComboBox<String>  roleFilter;

    private JTextField        nameField;
    private JTextField        emailField;
    private JPasswordField    passwordField;
    private JComboBox<String> roleCombo; // hidden data model — chips write to this

    private static final String[] ROLES      = {"all", "admin", "staff", "technician"};
    private static final String[] FORM_ROLES = {"admin", "staff", "technician"};
    private static final String[] COLUMNS    = {"#", "Name", "Email", "Role"};

    // ── Dialog colours ────────────────────────────────────────────
    private static final Color DLG_BG       = new Color(250, 250, 252);
    private static final Color FIELD_BG     = Color.WHITE;
    private static final Color FIELD_BORDER = new Color(210, 213, 225);
    private static final Color ACCENT       = new Color(80, 110, 230);
    private static final Color LABEL_FG     = new Color(75, 85, 110);
    private static final Color CHIP_BG      = new Color(237, 241, 255);

    public ManageStaffPanel(AccountService accountService) {
        this.accountService = accountService;
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);
        refreshTable("all");
    }

    // ─── Toolbar ─────────────────────────────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.BG_CONTENT);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel title = new JLabel("Staff & Technician Management");
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(UIConstants.BG_CONTENT);

        JLabel filterLbl = new JLabel("Filter:");
        filterLbl.setFont(UIConstants.FONT_SMALL);
        filterLbl.setForeground(UIConstants.TEXT_MUTED);

        roleFilter = new JComboBox<>(ROLES);
        roleFilter.setFont(UIConstants.FONT_SMALL);
        roleFilter.setPreferredSize(new Dimension(130, 34));
        roleFilter.addActionListener(e -> refreshTable((String) roleFilter.getSelectedItem()));

        JButton addBtn = accentBtn("+ Add User", 110);
        addBtn.addActionListener(e -> showFormDialog(null));

        right.add(filterLbl); right.add(roleFilter); right.add(addBtn);
        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ─── Table ───────────────────────────────────────────────────

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable(table);

        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(40);
        cm.getColumn(1).setPreferredWidth(180);
        cm.getColumn(2).setPreferredWidth(220);
        cm.getColumn(3).setPreferredWidth(100);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UIConstants.BG_CARD);
        card.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(UIConstants.BG_CARD);
        JButton editBtn = outlineBtn("Edit",   UIConstants.PRIMARY, 80);
        JButton delBtn  = outlineBtn("Delete", UIConstants.TEXT_DANGER, 80);
        editBtn.addActionListener(e -> editSelected());
        delBtn .addActionListener(e -> deleteSelected());
        actions.add(editBtn); actions.add(delBtn);

        card.add(new JScrollPane(table), BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return new JScrollPane(card);
    }

    private void styleTable(JTable t) {
        t.setRowHeight(40);
        t.setFont(UIConstants.FONT_BODY);
        t.setForeground(UIConstants.TEXT_DARK);
        t.setGridColor(new Color(235, 235, 240));
        t.setShowVerticalLines(false);
        t.setSelectionBackground(new Color(230, 235, 255));
        t.setSelectionForeground(UIConstants.TEXT_DARK);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(new Color(248, 248, 252));
        t.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        t.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_DEFAULT));
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        t.getColumnModel().getColumn(0).setCellRenderer(centre);
    }

    // ─── Data refresh ────────────────────────────────────────────

    private void refreshTable(String role) {
        tableModel.setRowCount(0);
        List<User> users = "all".equalsIgnoreCase(role)
                ? accountService.getAllUsers()
                : accountService.getUsersByRole(role);
        int row = 1;
        for (User u : users) {
            if (u.getRole().equalsIgnoreCase("customer")) continue;
            tableModel.addRow(new Object[]{row++, u.getName(), u.getEmail(), u.getRole()});
        }
    }

    // ─── Dialog ──────────────────────────────────────────────────

    private void showFormDialog(User existing) {
        boolean isEdit = existing != null;

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Member" : "Add Staff", true);
        dlg.setResizable(false);
        dlg.setSize(460, isEdit ? 400 : 480);
        dlg.setLocationRelativeTo(this);

        // ── Root ─────────────────────────────────────────────────
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(DLG_BG);
        dlg.setContentPane(root);

        // ── Coloured header ───────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ACCENT);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel hTitle = new JLabel(isEdit ? "Edit Member" : "Add Staff / Technician");
        hTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        hTitle.setForeground(Color.WHITE);

        JLabel hSub = new JLabel(isEdit
                ? "Update the member's details below."
                : "Fill in the details to create a new account.");
        hSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hSub.setForeground(new Color(200, 210, 255));

        JPanel hText = new JPanel();
        hText.setLayout(new BoxLayout(hText, BoxLayout.Y_AXIS));
        hText.setOpaque(false);
        hText.add(hTitle);
        hText.add(Box.createVerticalStrut(3));
        hText.add(hSub);
        header.add(hText);
        root.add(header, BorderLayout.NORTH);

        // ── Form — pure GridBagLayout, no nested BoxLayout ────────
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(DLG_BG);
        form.setBorder(new EmptyBorder(20, 40, 10, 40));

        GridBagConstraints gc = new GridBagConstraints();
        gc.fill      = GridBagConstraints.HORIZONTAL;
        gc.weightx   = 1.0;
        gc.gridx     = 0;
        gc.gridwidth = 1;
        gc.insets    = new Insets(0, 0, 0, 0);

        // ── Role label ────────────────────────────────────────────
        gc.gridy = 0; gc.insets = new Insets(0, 0, 6, 0);
        JLabel roleLbl = centredFieldLabel("Role");
        form.add(roleLbl, gc);

        // ── Role chips ────────────────────────────────────────────
        gc.gridy = 1; gc.insets = new Insets(0, 0, 18, 0);
        roleCombo = new JComboBox<>(FORM_ROLES);
        roleCombo.setSelectedItem(isEdit ? existing.getRole() : "staff");
        roleCombo.setVisible(false);
        JPanel chipsPanel = buildRoleChips(isEdit ? existing.getRole() : "staff");
        form.add(chipsPanel, gc);

        // ── Name label ────────────────────────────────────────────
        gc.gridy = 2; gc.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("Full Name"), gc);

        // ── Name field ────────────────────────────────────────────
        gc.gridy = 3; gc.insets = new Insets(0, 0, 14, 0);
        nameField = roundedField("e.g. John Smith");
        if (isEdit) { nameField.setText(existing.getName()); nameField.setForeground(Color.BLACK); }
        form.add(nameField, gc);

        // ── Email label ───────────────────────────────────────────
        gc.gridy = 4; gc.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("Email Address"), gc);

        // ── Email field ───────────────────────────────────────────
        gc.gridy = 5; gc.insets = new Insets(0, 0, 14, 0);
        emailField = roundedField("e.g. john@apu.asc.com");
        if (isEdit) { emailField.setText(existing.getEmail()); emailField.setForeground(Color.BLACK); }
        form.add(emailField, gc);

        // ── Password (Add only) ───────────────────────────────────
        if (!isEdit) {
            gc.gridy = 6; gc.insets = new Insets(0, 0, 6, 0);
            form.add(fieldLabel("Password"), gc);

            gc.gridy = 7; gc.insets = new Insets(0, 0, 14, 0);
            passwordField = roundedPasswordField();
            form.add(passwordField, gc);
        }

        root.add(form, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(DLG_BG);
        footer.setBorder(new EmptyBorder(0, 24, 16, 24));

        JSeparator sep = new JSeparator();
        sep.setForeground(FIELD_BORDER);
        footer.add(sep, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        btnRow.setBackground(DLG_BG);

        JButton cancelBtn = ghostBtn("Cancel", 88);
        cancelBtn.addActionListener(e -> dlg.dispose());

        JButton saveBtn = accentBtn(isEdit ? "Save Changes" : "Add Member", 130);
        saveBtn.addActionListener(e -> {
            if (isEdit) handleEdit(existing, dlg);
            else        handleAdd(dlg);
        });

        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        footer.add(btnRow, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    /** Builds the centred pill-chip row. Writes selection back to {@code roleCombo}. */
    private JPanel buildRoleChips(String defaultRole) {
        // FlowLayout.CENTER keeps all chips centred automatically
        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        row.setOpaque(false);

        JLabel[] chips = new JLabel[FORM_ROLES.length];

        for (int i = 0; i < FORM_ROLES.length; i++) {
            final String name = FORM_ROLES[i];
            boolean      sel  = name.equals(defaultRole);

            chips[i] = new JLabel(capitalise(name)) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(name.equals(roleCombo.getSelectedItem()) ? ACCENT : CHIP_BG);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            chips[i].setFont(new Font("SansSerif", Font.BOLD, 12));
            chips[i].setForeground(sel ? Color.WHITE : ACCENT);
            chips[i].setBorder(new EmptyBorder(7, 20, 7, 20));
            chips[i].setCursor(new Cursor(Cursor.HAND_CURSOR));
            chips[i].setOpaque(false);

            final JLabel[] all = chips;
            chips[i].addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    roleCombo.setSelectedItem(name);
                    for (int j = 0; j < FORM_ROLES.length; j++) {
                        all[j].setForeground(FORM_ROLES[j].equals(name) ? Color.WHITE : ACCENT);
                        all[j].repaint();
                    }
                }
            });
            row.add(chips[i]);
        }

        // Keep hidden combo in the component tree so handlers can read it
        row.add(roleCombo);
        return row;
    }

    // ─── CRUD handlers ───────────────────────────────────────────

    private void handleAdd(JDialog dlg) {
        String name  = textOf(nameField,  "e.g. John Smith");
        String email = textOf(emailField, "e.g. john@apu.asc.com");
        String pw    = new String(passwordField.getPassword()).trim();
        String role  = (String) roleCombo.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || pw.isEmpty()) { error("All fields are required."); return; }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) { error("Enter a valid email address."); return; }
        
        // 1. If the input name and email match existing user in accounts.txt
        boolean userExists = accountService.getAllUsers().stream()
                .anyMatch(u -> u.getName().equalsIgnoreCase(name) && u.getEmail().equalsIgnoreCase(email));
        if (userExists) { 
            // 2. System prompts error message: "User already existed."
            error("User already existed."); 
            return; 
        }
        
        // Additional check: if only the email exists, prompt accordingly.
        if (accountService.emailExists(email)) { error("Email already existed."); return; }

        accountService.register(new User(name, email, pw, role));
        dlg.dispose();
        refreshTable((String) roleFilter.getSelectedItem());
        success("New " + role + " added successfully.");
    }

    private void handleEdit(User orig, JDialog dlg) {
        String name  = textOf(nameField,  "e.g. John Smith");
        String email = textOf(emailField, "e.g. john@apu.asc.com");
        String role  = (String) roleCombo.getSelectedItem();

        if (name.isEmpty() || email.isEmpty()) { error("Name and email cannot be empty."); return; }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) { error("Enter a valid email address."); return; }
        
        // 1. If the updated email already exists in accounts.txt for another account
        if (!email.equalsIgnoreCase(orig.getEmail()) && accountService.emailExists(email)) { 
            // 2. System prompts error message: "Email already existed."
            error("Email already existed."); 
            return; 
        }

        accountService.updateUser(orig.getEmail(),
                new User(name, email, orig.getPassword(), role, orig.getProfilePicture()));
        dlg.dispose();
        refreshTable((String) roleFilter.getSelectedItem());
        success("Member updated successfully.");
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Please select a row first."); return; }
        String email = (String) tableModel.getValueAt(row, 2);
        accountService.getAllUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().ifPresent(this::showFormDialog);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Please select a row first."); return; }
        String name  = (String) tableModel.getValueAt(row, 1);
        String email = (String) tableModel.getValueAt(row, 2);
        if (JOptionPane.showConfirmDialog(this, "Delete " + name + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            accountService.deleteUser(email);
            refreshTable((String) roleFilter.getSelectedItem());
        }
    }

    // ─── Widget helpers ───────────────────────────────────────────

    private JLabel centredFieldLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(LABEL_FG);
        return l;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(LABEL_FG);
        return l;
    }

    private JTextField roundedField(String placeholder) {
        JTextField f = new JTextField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? ACCENT : FIELD_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setForeground(new Color(160, 165, 180));
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setPreferredSize(new Dimension(0, 42)); // width = 0 lets GridBag stretch it
        f.setText(placeholder);
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusGained(java.awt.event.FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(Color.BLACK); }
            }
            @Override public void focusLost(java.awt.event.FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(new Color(160, 165, 180)); }
            }
        });
        return f;
    }

    private JPasswordField roundedPasswordField() {
        JPasswordField f = new JPasswordField() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isFocusOwner() ? ACCENT : FIELD_BORDER);
                g2.setStroke(new BasicStroke(isFocusOwner() ? 2f : 1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 10, 10);
                g2.dispose();
            }
        };
        f.setOpaque(false);
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(new EmptyBorder(10, 14, 10, 14));
        f.setPreferredSize(new Dimension(0, 42)); // width = 0 lets GridBag stretch it
        f.setEchoChar('●');
        return f;
    }

    /** Solid blue button. */
    private JButton accentBtn(String text, int width) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT.darker() : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setForeground(Color.WHITE);
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(width, 38));
        b.setRolloverEnabled(true);
        return b;
    }

    /** Ghost / outline button for Cancel and table actions. */
    private JButton ghostBtn(String text, int width) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? new Color(240, 242, 255) : DLG_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
            @Override protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(FIELD_BORDER);
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                g2.dispose();
            }
        };
        b.setFont(new Font("SansSerif", Font.PLAIN, 13));
        b.setForeground(LABEL_FG);
        b.setContentAreaFilled(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(width, 38));
        b.setRolloverEnabled(true);
        return b;
    }

    /** Coloured outline button used in the table action row. */
    private JButton outlineBtn(String text, Color fg, int width) {
        JButton b = new JButton(text);
        b.setFont(UIConstants.FONT_SMALL);
        b.setForeground(fg);
        b.setBackground(Color.WHITE);
        b.setBorder(BorderFactory.createLineBorder(fg, 1));
        b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(width, 30));
        return b;
    }

    private String textOf(JTextField f, String placeholder) {
        String t = f.getText().trim();
        return t.equals(placeholder) ? "" : t;
    }

    private String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void error(String m)   { JOptionPane.showMessageDialog(this, m, "Error",   JOptionPane.ERROR_MESSAGE);       }
    private void success(String m) { JOptionPane.showMessageDialog(this, m, "Success", JOptionPane.INFORMATION_MESSAGE); }
}