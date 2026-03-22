package view;

import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class CustomerDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Profile";

    // Labels that update with username
    private JLabel welcomeLabel;
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // Built-in avatar colors (used for header avatar circle only)
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

    private int selectedAvatarIndex = 0;

    private static final String[] NAV_ITEMS = {
            "Profile", "Appointment Booking", "Service History", "Payment History", "My Feedback"
    };

    // Helper: builds emoji string from a Unicode code point above U+FFFF
    private static String navIcon(int codePoint) {
        return new StringBuilder().appendCodePoint(codePoint).toString();
    }

    // Profile=👤  Appointment=📅  Service History=🔄  Payment=💵  Feedback=💬
    private static final String[] NAV_ICONS = {
            navIcon(0x1F464), // 👤 person          → Profile
            navIcon(0x1F4C5), // 📅 calendar        → Appointment Booking
            navIcon(0x1F504), // 🔄 repeat arrows   → Service History
            navIcon(0x1F4B5), // 💵 dollar banknote → Payment History
            navIcon(0x1F4AC)  // 💬 speech bubble   → My Feedback
    };

    public CustomerDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());

        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        // All sections are placeholders for now — implement content later
        contentPanel.add(buildPlaceholderContent("Profile",            "Your profile information will appear here.",         navIcon(0x1F464)), "Profile");
        contentPanel.add(buildPlaceholderContent("Appointment Booking","Book and manage your service appointments.",          navIcon(0x1F4C5)), "Appointment Booking");
        contentPanel.add(buildPlaceholderContent("Service History",    "View your past service records and details.",         navIcon(0x1F504)), "Service History");
        contentPanel.add(buildPlaceholderContent("Payment History",    "Review your payment transactions and invoices.",      navIcon(0x1F4B5)), "Payment History");
        contentPanel.add(buildPlaceholderContent("My Feedback",        "Submit and view your feedback for services.",         navIcon(0x1F4AC)), "My Feedback");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    /**
     * Called by AppFrame.showPage() right after login.
     * Reads the name that was loaded from accounts.txt via AccountService.authenticate()
     * and stored in AppFrame, so all labels show the real customer name.
     */
    public void refreshUser() {
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Customer";
        if (welcomeLabel != null) welcomeLabel.setText("Welcome back, " + name + "!");
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

        headerTitle = new JLabel("Profile");
        headerTitle.setFont(UIConstants.FONT_BODY_BOLD);
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);

        header.add(buildProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        // Avatar circle in header
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

        // Name next to avatar — populated by refreshUser() from accounts.txt
        profileLabel = new JLabel("—");
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
            activeNav = "Profile";
            headerTitle.setText("Profile");
            contentLayout.show(contentPanel, "Profile");
        });

        JMenuItem logout = createMenuItem("Logout");
        logout.setForeground(UIConstants.TEXT_DANGER);
        logout.addActionListener(e -> {
            app.setLoggedInUser("");
            app.setLoggedInUserObj(null);
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
                if (!profileMenu.getBounds().contains(p)) profileMenu.setVisible(false);
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

        JLabel brandLabel = new JLabel("APU ASC");
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

        // Nav section label
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

    // ─── Placeholder pages ───────────────────────────────────────

    /**
     * Generic placeholder panel used for ALL sections until real content is built.
     * The welcome label is embedded only in the Profile placeholder so it
     * still greets the customer by name when they land on the page.
     */
    private JPanel buildPlaceholderContent(String title, String description, String icon) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        // Welcome label only on the Profile page
        if (title.equals("Profile")) {
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBackground(UIConstants.BG_CONTENT);
            topPanel.setBorder(new EmptyBorder(30, 40, 0, 40));
            welcomeLabel = new JLabel("Welcome back!");
            welcomeLabel.setFont(UIConstants.FONT_HEADING_2);
            welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);
            topPanel.add(welcomeLabel, BorderLayout.WEST);
            page.add(topPanel, BorderLayout.NORTH);
        }

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

        JLabel iconLabel = new JLabel(icon);
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
