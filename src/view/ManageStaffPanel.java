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
import java.util.List;

/**
 * Admin panel for Create / Read / Update / Delete of staff accounts.
 * Roles managed: admin, staff, technician  (customers excluded here).
 *
 * OOP highlights:
 *  - Encapsulation : all file ops delegated to AccountService
 *  - Inheritance   : extends JPanel
 *  - Abstraction   : UI widgets via UIFactory
 *  - Polymorphism  : refreshTable() works for any role filter
 */
public class ManageStaffPanel extends JPanel {

    private final AccountService accountService;

    private DefaultTableModel tableModel;
    private JTable table;
    private JComboBox<String> roleFilter;

    // Form fields (reused for add and edit)
    private JTextField nameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JComboBox<String> roleCombo;

    private static final String[] ROLES     = {"all", "admin", "staff", "technician"};
    private static final String[] FORM_ROLES = {"admin", "staff", "technician"};
    private static final String[] COLUMNS   = {"#", "Name", "Email", "Role"};

    public ManageStaffPanel(AccountService accountService) {
        this.accountService = accountService;
        setLayout(new BorderLayout(0, 0));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        add(buildToolbar(), BorderLayout.NORTH);
        add(buildTable(),   BorderLayout.CENTER);

        refreshTable("all");
    }

    // ─── Toolbar (filter + add button) ──────────────────────────

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UIConstants.BG_CONTENT);
        bar.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Left: title
        JLabel title = new JLabel("Staff Management");
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);

        // Right: role filter + add button
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setBackground(UIConstants.BG_CONTENT);

        JLabel filterLabel = new JLabel("Filter:");
        filterLabel.setFont(UIConstants.FONT_SMALL);
        filterLabel.setForeground(UIConstants.TEXT_MUTED);

        roleFilter = new JComboBox<>(ROLES);
        roleFilter.setFont(UIConstants.FONT_SMALL);
        roleFilter.setPreferredSize(new Dimension(130, 34));
        roleFilter.addActionListener(e -> refreshTable((String) roleFilter.getSelectedItem()));

        JButton addBtn = new JButton("+ Add Staff");
        addBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(UIConstants.PRIMARY);
        addBtn.setBorderPainted(false);
        addBtn.setFocusPainted(false);
        addBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        addBtn.setPreferredSize(new Dimension(110, 34));
        addBtn.addActionListener(e -> showFormDialog(null));

        right.add(filterLabel);
        right.add(roleFilter);
        right.add(addBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(right,  BorderLayout.EAST);
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

        // Double-click to edit
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
        });

        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(UIConstants.BG_CARD);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(0, 0, 0, 0)));

        // Action row below table
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        actions.setBackground(UIConstants.BG_CARD);

        JButton editBtn = smallBtn("Edit", UIConstants.PRIMARY);
        JButton delBtn  = smallBtn("Delete", UIConstants.TEXT_DANGER);
        editBtn.addActionListener(e -> editSelected());
        delBtn .addActionListener(e -> deleteSelected());

        actions.add(editBtn);
        actions.add(delBtn);

        tableCard.add(new JScrollPane(table), BorderLayout.CENTER);
        tableCard.add(actions, BorderLayout.SOUTH);
        return new JScrollPane(tableCard);
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

        // Header style
        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(new Color(248, 248, 252));
        t.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_DEFAULT));

        // Centre # column
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

        // Exclude customers from staff table
        int row = 1;
        for (User u : users) {
            if (u.getRole().equalsIgnoreCase("customer")) continue;
            tableModel.addRow(new Object[]{row++, u.getName(), u.getEmail(), u.getRole()});
        }
    }

    // ─── Add / Edit dialog ───────────────────────────────────────

    /** Pass null to open an "Add" dialog; pass a User to open "Edit". */
    private void showFormDialog(User existing) {
        boolean isEdit = existing != null;
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                isEdit ? "Edit Staff" : "Add Staff", true);
        dialog.setSize(420, isEdit ? 360 : 400);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.getRootPane().setBorder(new EmptyBorder(24, 32, 24, 32));

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);

        // Name
        form.add(UIFactory.createFieldLabel("Name"));
        form.add(Box.createVerticalStrut(6));
        nameField = UIFactory.createTextField("Full name");
        form.add(nameField);
        form.add(Box.createVerticalStrut(14));

        // Email
        form.add(UIFactory.createFieldLabel("Email"));
        form.add(Box.createVerticalStrut(6));
        emailField = UIFactory.createTextField("Email address");
        form.add(emailField);
        form.add(Box.createVerticalStrut(14));

        // Password (only on Add)
        if (!isEdit) {
            form.add(UIFactory.createFieldLabel("Password"));
            form.add(Box.createVerticalStrut(6));
            passwordField = UIFactory.createPasswordField();
            form.add(UIFactory.createPasswordFieldPanel(passwordField));
            form.add(Box.createVerticalStrut(14));
        }

        // Role
        form.add(UIFactory.createFieldLabel("Role"));
        form.add(Box.createVerticalStrut(6));
        roleCombo = new JComboBox<>(FORM_ROLES);
        roleCombo.setFont(UIConstants.FONT_BODY);
        roleCombo.setMaximumSize(UIConstants.FIELD_SIZE);
        roleCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        form.add(roleCombo);
        form.add(Box.createVerticalStrut(22));

        // Pre-fill if editing
        if (isEdit) {
            nameField .setText(existing.getName());
            nameField .setForeground(Color.BLACK);
            emailField.setText(existing.getEmail());
            emailField.setForeground(Color.BLACK);
            roleCombo .setSelectedItem(existing.getRole());
        }

        // Save button
        JButton saveBtn = UIFactory.createPrimaryButton(isEdit ? "Save Changes" : "Add Staff");
        saveBtn.addActionListener(e -> {
            if (isEdit) handleEdit(existing, dialog);
            else        handleAdd(dialog);
        });
        form.add(saveBtn);

        dialog.add(new JScrollPane(form), BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ─── CRUD handlers ───────────────────────────────────────────

    private void handleAdd(JDialog dialog) {
        String name  = UIFactory.getFieldValue(nameField,  "Full name");
        String email = UIFactory.getFieldValue(emailField, "Email address");
        String pw    = new String(passwordField.getPassword()).trim();
        String role  = (String) roleCombo.getSelectedItem();

        if (name.isEmpty() || email.isEmpty() || pw.isEmpty()) {
            error("All fields are required."); return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            error("Enter a valid email."); return;
        }
        if (accountService.emailExists(email)) {
            error("Email already registered."); return;
        }

        accountService.register(new User(name, email, pw, role));
        dialog.dispose();
        refreshTable((String) roleFilter.getSelectedItem());
        success("Staff member added.");
    }

    private void handleEdit(User original, JDialog dialog) {
        String name  = UIFactory.getFieldValue(nameField,  "Full name");
        String email = UIFactory.getFieldValue(emailField, "Email address");
        String role  = (String) roleCombo.getSelectedItem();

        if (name.isEmpty() || email.isEmpty()) {
            error("Name and email cannot be empty."); return;
        }
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            error("Enter a valid email."); return;
        }
        if (!email.equalsIgnoreCase(original.getEmail()) && accountService.emailExists(email)) {
            error("Email already taken."); return;
        }

        User updated = new User(name, email, original.getPassword(), role, original.getProfilePicture());
        accountService.updateUser(original.getEmail(), updated);
        dialog.dispose();
        refreshTable((String) roleFilter.getSelectedItem());
        success("Staff member updated.");
    }

    private void editSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a row first."); return; }
        String email = (String) tableModel.getValueAt(row, 2);
        User user = accountService.getAllUsers().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst().orElse(null);
        if (user != null) showFormDialog(user);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) { error("Select a row first."); return; }
        String name  = (String) tableModel.getValueAt(row, 1);
        String email = (String) tableModel.getValueAt(row, 2);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + name + "?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            accountService.deleteUser(email);
            refreshTable((String) roleFilter.getSelectedItem());
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

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

    private void error(String msg)   { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE);       }
    private void success(String msg) { JOptionPane.showMessageDialog(this, msg, "Success", JOptionPane.INFORMATION_MESSAGE); }
}