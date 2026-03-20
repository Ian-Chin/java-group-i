package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class AdminDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Dashboard";

    // Labels that update with username
    private JLabel welcomeLabel;
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // Profile section fields
    private JTextField profileNameField;
    private JTextField profileEmailField;
    private JLabel profileRoleLabel;
    private JLabel profileAvatarDisplay;
    private int selectedAvatarIndex = 0;
    private JPanel avatarSelectionPanel;

    // Built-in avatar colors
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230),   // Blue
            new Color(230, 80, 80),    // Red
            new Color(80, 190, 110),   // Green
            new Color(230, 160, 40),   // Orange
            new Color(160, 80, 230),   // Purple
            new Color(40, 180, 200),   // Teal
            new Color(230, 80, 160),   // Pink
            new Color(100, 100, 120),  // Gray
    };

    private static final String[] AVATAR_ICONS = {
            "\u263A", "\u2605", "\u2665", "\u2666",
            "\u263C", "\u2708", "\u266B", "\u2618"
    };

    private static final String[] NAV_ITEMS = {
            "Dashboard", "Manage Staff", "Service Price", "View Feedback", "Report"
    };

    private static final String[] NAV_ICONS = {
            "\u2302", "\u2663", "\u2696", "\u2605", "\u2637"
    };

    public AdminDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());

        // Left sidebar
        add(buildSidebar(), BorderLayout.WEST);

        // Right side (header + content)
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);

        rightSide.add(buildHeader(), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        contentPanel.add(buildDashboardContent(), "Dashboard");
        contentPanel.add(buildPlaceholderContent("Manage Staff", "Manage your staff members, roles, and schedules."), "Manage Staff");
        contentPanel.add(buildPlaceholderContent("Service Price", "Configure and update service pricing."), "Service Price");
        contentPanel.add(buildPlaceholderContent("View Feedback", "Review customer feedback and ratings."), "View Feedback");
        contentPanel.add(buildPlaceholderContent("Report", "Generate and view business reports."), "Report");
        contentPanel.add(buildProfileContent(), "Profile");

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
        if (name == null || name.isEmpty()) name = "Admin";
        if (welcomeLabel != null) welcomeLabel.setText("Welcome back, " + name);
        if (profileLabel != null) profileLabel.setText(name);

        User user = app.getLoggedInUserObj();
        if (user != null) {
            selectedAvatarIndex = user.getProfilePicture();
        }
        if (avatarLabel != null) {
            avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]);
            avatarLabel.repaint();
        }
    }

    // ─── Header ──────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)
        ));

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

        // Avatar circle
        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override
            protected void paintComponent(Graphics g) {
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

        profileLabel = new JLabel("Admin");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        JLabel dropdownArrow = new JLabel("\u25BE");
        dropdownArrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dropdownArrow.setForeground(UIConstants.TEXT_MUTED);

        JPanel profileBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileBtn.setBackground(UIConstants.BG_HEADER);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.add(avatarLabel);
        profileBtn.add(profileLabel);
        profileBtn.add(dropdownArrow);

        // Popup menu
        JPopupMenu profileMenu = new JPopupMenu();
        profileMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)
        ));
        profileMenu.setBackground(Color.WHITE);

        JMenuItem viewProfile = createMenuItem("View Profile");
        viewProfile.addActionListener(e -> {
            activeNav = "";
            headerTitle.setText("My Profile");
            contentLayout.show(contentPanel, "Profile");
            refreshProfileFields();
        });
        JMenuItem logout = createMenuItem("Logout");
        logout.setForeground(UIConstants.TEXT_DANGER);
        logout.addActionListener(e -> {
            app.setLoggedInUser("");
            app.showPage(PageName.ONBOARDING);
        });

        profileMenu.add(viewProfile);
        profileMenu.add(logout);

        profileBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                profileMenu.show(profileBtn,
                        profileBtn.getWidth() - profileMenu.getPreferredSize().width,
                        profileBtn.getHeight());
            }
        });

        profileMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = e.getPoint();
                if (!profileMenu.getBounds().contains(p)) {
                    profileMenu.setVisible(false);
                }
            }
        });

        profileArea.add(profileBtn);
        return profileArea;
    }

    private JMenuItem createMenuItem(String text) {
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

        // Sidebar header
        JPanel sidebarHeader = new JPanel();
        sidebarHeader.setLayout(new BoxLayout(sidebarHeader, BoxLayout.Y_AXIS));
        sidebarHeader.setBackground(UIConstants.SIDEBAR_BG);
        sidebarHeader.setBorder(new EmptyBorder(25, 20, 25, 20));
        sidebarHeader.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));

        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandLabel = new JLabel("APU ASC Admin");
        brandLabel.setFont(UIConstants.FONT_SIDEBAR);
        brandLabel.setForeground(Color.WHITE);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandLabel.setBorder(new EmptyBorder(8, 0, 0, 0));

        sidebarHeader.add(logoLabel);
        sidebarHeader.add(brandLabel);
        sidebar.add(sidebarHeader);

        // Divider
        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.SIDEBAR_DIVIDER);
        divider.setBackground(UIConstants.SIDEBAR_BG);
        divider.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));

        // Nav label
        JLabel navLabel = new JLabel("MENU");
        navLabel.setFont(UIConstants.FONT_LABEL);
        navLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        navLabel.setBorder(new EmptyBorder(10, 24, 10, 20));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35));
        sidebar.add(navLabel);

        // Nav buttons
        JButton[] navButtons = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String navName = NAV_ITEMS[i];
            navButtons[i] = createNavButton(NAV_ICONS[i] + "   " + navName, navName.equals(activeNav));
            navButtons[i].addActionListener(e -> {
                activeNav = navName;
                for (int j = 0; j < navButtons.length; j++) {
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals(activeNav));
                }
                headerTitle.setText(navName);
                contentLayout.show(contentPanel, navName);
            });
            sidebar.add(navButtons[i]);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JButton createNavButton(String text, boolean isActive) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getClientProperty("active") == Boolean.TRUE) {
                    g2.setColor(UIConstants.SIDEBAR_ACTIVE);
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    g2.setColor(UIConstants.SIDEBAR_HOVER);
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 8, 8);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 42));
        btn.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 42));
        btn.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        btn.setForeground(isActive ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 20, 0, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("active", isActive);
        btn.setRolloverEnabled(true);
        return btn;
    }

    private void updateNavButtonStyle(JButton btn, boolean isActive) {
        btn.putClientProperty("active", isActive);
        btn.setForeground(isActive ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }

    // ─── Dashboard content ───────────────────────────────────────

    private JPanel buildDashboardContent() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(40, 40, 40, 40));

        welcomeLabel = new JLabel("Welcome back, Admin");
        welcomeLabel.setFont(UIConstants.FONT_HEADING_2);
        welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);

        page.add(welcomeLabel, BorderLayout.NORTH);
        return page;
    }

    // ─── Profile section ─────────────────────────────────────────

    private void refreshProfileFields() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        if (profileNameField != null) {
            profileNameField.setText(user.getName());
            profileNameField.setForeground(Color.BLACK);
        }
        if (profileEmailField != null) {
            profileEmailField.setText(user.getEmail());
            profileEmailField.setForeground(Color.BLACK);
        }
        if (profileRoleLabel != null) {
            String role = user.getRole();
            profileRoleLabel.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
        }
        selectedAvatarIndex = user.getProfilePicture();
        updateAvatarSelection();
    }

    private void updateAvatarSelection() {
        if (profileAvatarDisplay != null) {
            profileAvatarDisplay.setText(AVATAR_ICONS[selectedAvatarIndex]);
            profileAvatarDisplay.repaint();
        }
        if (avatarSelectionPanel != null) {
            Component[] avatars = avatarSelectionPanel.getComponents();
            for (int i = 0; i < avatars.length; i++) {
                if (avatars[i] instanceof JLabel) {
                    JLabel lbl = (JLabel) avatars[i];
                    lbl.setBorder(i == selectedAvatarIndex
                            ? BorderFactory.createLineBorder(UIConstants.PRIMARY, 3)
                            : BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
                }
            }
        }
        // Also update header avatar
        if (avatarLabel != null) {
            avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]);
            avatarLabel.repaint();
        }
    }

    private JPanel buildProfileContent() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        // Scrollable center
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        // Profile avatar display (large)
        profileAvatarDisplay = new JLabel(AVATAR_ICONS[0]) {
            @Override
            protected void paintComponent(Graphics g) {
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

        // Avatar selection label
        JLabel chooseLabel = new JLabel("Choose Profile Picture");
        chooseLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        chooseLabel.setForeground(UIConstants.TEXT_DARK);
        chooseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(chooseLabel);
        card.add(Box.createVerticalStrut(12));

        // Avatar selection grid
        avatarSelectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        avatarSelectionPanel.setOpaque(false);
        avatarSelectionPanel.setMaximumSize(new Dimension(460, 60));

        for (int i = 0; i < AVATAR_COLORS.length; i++) {
            final int index = i;
            JLabel avatar = new JLabel(AVATAR_ICONS[i]) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(AVATAR_COLORS[index]);
                    g2.fillOval(2, 2, 42, 42);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            avatar.setFont(new Font("SansSerif", Font.PLAIN, 20));
            avatar.setForeground(Color.WHITE);
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatar.setVerticalAlignment(SwingConstants.CENTER);
            avatar.setPreferredSize(new Dimension(46, 46));
            avatar.setCursor(new Cursor(Cursor.HAND_CURSOR));
            avatar.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
            avatar.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedAvatarIndex = index;
                    updateAvatarSelection();
                }
            });
            avatarSelectionPanel.add(avatar);
        }
        card.add(avatarSelectionPanel);
        card.add(Box.createVerticalStrut(30));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(380, 1));
        sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sep);
        card.add(Box.createVerticalStrut(25));

        // Name field
        JLabel nameLabel = UIFactory.createFieldLabel("Name");
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(6));
        profileNameField = UIFactory.createTextField("Enter your name");
        card.add(profileNameField);
        card.add(Box.createVerticalStrut(16));

        // Email field
        JLabel emailLabel = UIFactory.createFieldLabel("Email");
        card.add(emailLabel);
        card.add(Box.createVerticalStrut(6));
        profileEmailField = UIFactory.createTextField("Enter your email");
        card.add(profileEmailField);
        card.add(Box.createVerticalStrut(16));

        // Role (read-only)
        JLabel roleLabel = UIFactory.createFieldLabel("Role");
        card.add(roleLabel);
        card.add(Box.createVerticalStrut(6));
        profileRoleLabel = new JLabel("—");
        profileRoleLabel.setFont(UIConstants.FONT_BODY);
        profileRoleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        profileRoleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileRoleLabel.setMaximumSize(new Dimension(380, 30));
        profileRoleLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.add(profileRoleLabel);
        card.add(Box.createVerticalStrut(28));

        // Save button
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

        String newName = UIFactory.getFieldValue(profileNameField, "Enter your name");
        String newEmail = UIFactory.getFieldValue(profileEmailField, "Enter your email");

        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(app, "Name and email cannot be empty.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newName.matches("[a-zA-Z ]{2,50}")) {
            JOptionPane.showMessageDialog(app, "Name must be 2-50 characters and contain only letters.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            JOptionPane.showMessageDialog(app, "Please enter a valid email address.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Check if email changed and already exists
        AccountService accountService = app.getAccountService();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && accountService.emailExists(newEmail)) {
            JOptionPane.showMessageDialog(app, "An account with this email already exists.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String originalEmail = user.getEmail();
        User updatedUser = new User(newName, newEmail, user.getPassword(), user.getRole(), selectedAvatarIndex);

        if (accountService.updateUser(originalEmail, updatedUser)) {
            app.setLoggedInUser(newName);
            app.setLoggedInUserObj(updatedUser);
            refreshUser();
            JOptionPane.showMessageDialog(app, "Profile updated successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(app, "Failed to save profile. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Placeholder pages ───────────────────────────────────────

    private JPanel buildPlaceholderContent(String title, String description) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(50, 60, 50, 60));

        JLabel iconLabel = new JLabel("\u2692");
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 40));
        iconLabel.setForeground(UIConstants.TEXT_SIDEBAR);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(16));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLbl.setForeground(UIConstants.TEXT_DARK);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLbl);
        card.add(Box.createVerticalStrut(8));

        JLabel descLbl = new JLabel(description);
        descLbl.setFont(UIConstants.FONT_BODY);
        descLbl.setForeground(UIConstants.TEXT_MUTED);
        descLbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLbl);

        center.add(card);
        page.add(center, BorderLayout.CENTER);
        return page;
    }
}
