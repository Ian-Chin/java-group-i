package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CounterStaffDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Dashboard";

    private JLabel welcomeLabel;
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // Profile section
    private JTextField profileNameField;
    private JTextField profileEmailField;
    private JLabel profileRoleLabel;
    private JLabel profileAvatarDisplay;
    private int selectedAvatarIndex = 0;
    private JPanel avatarSelectionPanel;

    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230), new Color(230, 80, 80),  new Color(80, 190, 110),
            new Color(230, 160, 40), new Color(160, 80, 230), new Color(40, 180, 200),
            new Color(230, 80, 160), new Color(100, 100, 120),
    };
    private static final String[] AVATAR_ICONS = {
            "\u263A", "\u2605", "\u2665", "\u2666",
            "\u263C", "\u2708", "\u266B", "\u2618"
    };

    private static final String[] NAV_ITEMS = {
            "Dashboard", "Customer Management", "Appointments", "Payment Collection"
    };
    private static final String[] NAV_ICONS = {
            "\u2302", "\u2663", "\u2637", "\u2696"
    };

    public CounterStaffDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        contentPanel.add(buildDashboardContent(),                        "Dashboard");
        contentPanel.add(new CustomerManagementPanel(app.getAccountService()), "Customer Management");
        contentPanel.add(buildPlaceholder("Appointments"),        "Appointments");
        contentPanel.add(buildPlaceholder("Payment Collection"),  "Payment Collection");
        contentPanel.add(buildProfileContent(),            "Profile");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    public void refreshUser() {
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Staff";
        if (welcomeLabel != null) welcomeLabel.setText("Welcome back, " + name);
        if (profileLabel  != null) profileLabel.setText(name);

        User user = app.getLoggedInUserObj();
        if (user != null) selectedAvatarIndex = user.getProfilePicture();
        if (avatarLabel != null) { avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]); avatarLabel.repaint(); }
    }

    // ─── Placeholder page ─────────────────────────────────────────
    private JPanel buildPlaceholder(String title) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(40, 40, 40, 40));

        JLabel label = new JLabel(title);
        label.setFont(UIConstants.FONT_HEADING_2);
        label.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel sub = new JLabel("This section is under development.");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(UIConstants.BG_CONTENT);
        top.add(label);
        top.add(sub);

        page.add(top, BorderLayout.NORTH);
        return page;
    }

    // ─── Header ──────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));

        headerTitle = new JLabel("Dashboard");
        headerTitle.setFont(UIConstants.FONT_BODY_BOLD);
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
                g2.fillOval(0, 0, 38, 38);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(38, 38));

        profileLabel = new JLabel("Staff");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        JLabel arrow = new JLabel("\u25BE");
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        arrow.setForeground(UIConstants.TEXT_MUTED);

        JPanel profileBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileBtn.setBackground(UIConstants.BG_HEADER);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.add(avatarLabel); profileBtn.add(profileLabel); profileBtn.add(arrow);

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)));
        menu.setBackground(Color.WHITE);

        JMenuItem viewProfile = menuItem("View Profile");
        viewProfile.addActionListener(e -> {
            activeNav = "";
            headerTitle.setText("My Profile");
            contentLayout.show(contentPanel, "Profile");
            refreshProfileFields();
        });
        JMenuItem logout = menuItem("Logout");
        logout.setForeground(UIConstants.TEXT_DANGER);
        logout.addActionListener(e -> {
            app.setLoggedInUser("");
            app.setLoggedInUserObj(null);
            app.showPage(PageName.ONBOARDING);
        });

        menu.add(viewProfile); menu.add(logout);

        profileBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                menu.show(profileBtn, profileBtn.getWidth() - menu.getPreferredSize().width, profileBtn.getHeight());
            }
        });
        menu.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (!menu.getBounds().contains(e.getPoint())) menu.setVisible(false);
            }
        });

        profileArea.add(profileBtn);
        return profileArea;
    }

    private JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(UIConstants.FONT_SMALL);
        item.setForeground(UIConstants.TEXT_DARK);
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(8, 20, 8, 30));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return item;
    }

    // ─── Sidebar ─────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));

        // Logo + brand
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(UIConstants.SIDEBAR_BG);
        header.setBorder(new EmptyBorder(25, 20, 25, 20));
        header.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));

        try {
            ImageIcon raw = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
            header.add(new JLabel(new ImageIcon(raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH))));
        } catch (Exception ignored) {}

        JLabel brand = new JLabel("APU ASC Staff");
        brand.setFont(UIConstants.FONT_SIDEBAR);
        brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(8, 0, 0, 0));
        header.add(brand);
        sidebar.add(header);

        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.SIDEBAR_DIVIDER);
        sep.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(sep);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setFont(UIConstants.FONT_LABEL);
        menuLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        menuLabel.setBorder(new EmptyBorder(10, 24, 10, 20));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35));
        sidebar.add(menuLabel);

        // Nav buttons
        JButton[] btns = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String name = NAV_ITEMS[i];
            btns[i] = navButton(NAV_ICONS[i] + "   " + name, name.equals(activeNav));
            final int fi = i;
            btns[i].addActionListener(e -> {
                activeNav = name;
                for (int j = 0; j < btns.length; j++) styleNavBtn(btns[j], NAV_ITEMS[j].equals(activeNav));
                headerTitle.setText(name);
                contentLayout.show(contentPanel, name);
            });
            sidebar.add(btns[i]);
            sidebar.add(Box.createVerticalStrut(2));
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JButton navButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getClientProperty("active") == Boolean.TRUE)
                    g2.setColor(UIConstants.SIDEBAR_ACTIVE);
                else if (getModel().isRollover())
                    g2.setColor(UIConstants.SIDEBAR_HOVER);
                else { g2.dispose(); super.paintComponent(g); return; }
                g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 42));
        btn.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 42));
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 14));
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 20, 0, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("active", active);
        btn.setRolloverEnabled(true);
        return btn;
    }

    private void styleNavBtn(JButton btn, boolean active) {
        btn.putClientProperty("active", active);
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }

    // ─── Dashboard content ───────────────────────────────────────

    private JPanel buildDashboardContent() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(40, 40, 40, 40));

        welcomeLabel = new JLabel("Welcome back, Staff");
        welcomeLabel.setFont(UIConstants.FONT_HEADING_2);
        welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel sub = new JLabel("Use the sidebar to manage customers, appointments, and payments.");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(8, 0, 0, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setBackground(UIConstants.BG_CONTENT);
        top.add(welcomeLabel);
        top.add(sub);

        page.add(top, BorderLayout.NORTH);
        return page;
    }

    // ─── Profile content ─────────────────────────────────────────

    private void refreshProfileFields() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        if (profileNameField  != null) { profileNameField.setText(user.getName());  profileNameField.setForeground(Color.BLACK); }
        if (profileEmailField != null) { profileEmailField.setText(user.getEmail()); profileEmailField.setForeground(Color.BLACK); }
        if (profileRoleLabel  != null) {
            String r = user.getRole();
            profileRoleLabel.setText(r.substring(0, 1).toUpperCase() + r.substring(1));
        }
        selectedAvatarIndex = user.getProfilePicture();
        updateAvatarSelection();
    }

    private void updateAvatarSelection() {
        if (profileAvatarDisplay != null) { profileAvatarDisplay.setText(AVATAR_ICONS[selectedAvatarIndex]); profileAvatarDisplay.repaint(); }
        if (avatarSelectionPanel != null) {
            Component[] avatars = avatarSelectionPanel.getComponents();
            for (int i = 0; i < avatars.length; i++) {
                if (avatars[i] instanceof JLabel) {
                    ((JLabel) avatars[i]).setBorder(i == selectedAvatarIndex
                            ? BorderFactory.createLineBorder(UIConstants.PRIMARY, 3)
                            : BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
                }
            }
        }
        if (avatarLabel != null) { avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]); avatarLabel.repaint(); }
    }

    private JPanel buildProfileContent() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);

        JPanel card = UIFactory.createCard();
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Large avatar display
        profileAvatarDisplay = new JLabel(AVATAR_ICONS[0]) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
                g2.fillOval(0, 0, 80, 80);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        profileAvatarDisplay.setFont(new Font("SansSerif", Font.PLAIN, 36));
        profileAvatarDisplay.setForeground(Color.WHITE);
        profileAvatarDisplay.setHorizontalAlignment(SwingConstants.CENTER);
        profileAvatarDisplay.setVerticalAlignment(SwingConstants.CENTER);
        profileAvatarDisplay.setPreferredSize(new Dimension(80, 80));
        profileAvatarDisplay.setMaximumSize(new Dimension(80, 80));
        profileAvatarDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(profileAvatarDisplay);
        card.add(Box.createVerticalStrut(20));

        JLabel chooseLabel = new JLabel("Choose Profile Picture");
        chooseLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        chooseLabel.setForeground(UIConstants.TEXT_DARK);
        chooseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(chooseLabel);
        card.add(Box.createVerticalStrut(12));

        // Avatar picker grid
        avatarSelectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        avatarSelectionPanel.setOpaque(false);
        avatarSelectionPanel.setMaximumSize(new Dimension(460, 60));
        for (int i = 0; i < AVATAR_COLORS.length; i++) {
            final int idx = i;
            JLabel av = new JLabel(AVATAR_ICONS[i]) {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AVATAR_COLORS[idx]);
                    g2.fillOval(2, 2, 42, 42);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            av.setFont(new Font("SansSerif", Font.PLAIN, 20));
            av.setForeground(Color.WHITE);
            av.setHorizontalAlignment(SwingConstants.CENTER);
            av.setVerticalAlignment(SwingConstants.CENTER);
            av.setPreferredSize(new Dimension(46, 46));
            av.setCursor(new Cursor(Cursor.HAND_CURSOR));
            av.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
            av.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { selectedAvatarIndex = idx; updateAvatarSelection(); }
            });
            avatarSelectionPanel.add(av);
        }
        card.add(avatarSelectionPanel);
        card.add(Box.createVerticalStrut(28));

        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(380, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(22));

        card.add(UIFactory.createFieldLabel("Name")); card.add(Box.createVerticalStrut(6));
        profileNameField = UIFactory.createTextField("Enter your name"); card.add(profileNameField);
        card.add(Box.createVerticalStrut(16));

        card.add(UIFactory.createFieldLabel("Email")); card.add(Box.createVerticalStrut(6));
        profileEmailField = UIFactory.createTextField("Enter your email"); card.add(profileEmailField);
        card.add(Box.createVerticalStrut(16));

        card.add(UIFactory.createFieldLabel("Role")); card.add(Box.createVerticalStrut(6));
        profileRoleLabel = new JLabel("\u2014");
        profileRoleLabel.setFont(UIConstants.FONT_BODY);
        profileRoleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        profileRoleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileRoleLabel.setMaximumSize(new Dimension(380, 30));
        profileRoleLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.add(profileRoleLabel);
        card.add(Box.createVerticalStrut(28));

        JButton saveBtn = UIFactory.createPrimaryButton("Save Changes");
        saveBtn.addActionListener(e -> handleProfileSave());
        card.add(saveBtn);

        center.add(card);
        JScrollPane scroll = new JScrollPane(center);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private void handleProfileSave() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        String newName  = UIFactory.getFieldValue(profileNameField,  "Enter your name");
        String newEmail = UIFactory.getFieldValue(profileEmailField, "Enter your email");

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(app, "Name and email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        if (!newName.matches("[a-zA-Z ]{2,50}")) {
            JOptionPane.showMessageDialog(app, "Name must be 2-50 characters (letters only).", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            JOptionPane.showMessageDialog(app, "Please enter a valid email address.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        AccountService svc = app.getAccountService();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && svc.emailExists(newEmail)) {
            JOptionPane.showMessageDialog(app, "An account with this email already exists.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }

        User updated = new User(user.getUserId(), newName, newEmail, user.getPassword(), user.getRole(), selectedAvatarIndex);
        if (svc.updateUser(user.getEmail(), updated)) {
            app.setLoggedInUser(newName);
            app.setLoggedInUserObj(updated);
            refreshUser();
            JOptionPane.showMessageDialog(app, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(app, "Failed to save profile.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}