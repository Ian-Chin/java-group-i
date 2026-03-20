package view;

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
        if (avatarLabel != null) avatarLabel.setText(String.valueOf(name.charAt(0)).toUpperCase());
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

        JLabel headerTitle = new JLabel("Admin Panel");
        headerTitle.setFont(UIConstants.FONT_BODY);
        headerTitle.setForeground(UIConstants.TEXT_MUTED);
        header.add(headerTitle, BorderLayout.WEST);

        header.add(buildProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        // Avatar circle
        avatarLabel = new JLabel("A") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.PRIMARY);
                g2.fillOval(0, 0, 38, 38);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarLabel.setFont(UIConstants.FONT_SIDEBAR);
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
        welcomeLabel.setFont(UIConstants.FONT_HEADING_1);
        welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);

        page.add(welcomeLabel, BorderLayout.NORTH);
        return page;
    }

    // ─── Placeholder pages ───────────────────────────────────────

    private JPanel buildPlaceholderContent(String title, String description) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(UIConstants.BG_CONTENT);
        topBar.setBorder(new EmptyBorder(25, 35, 15, 35));

        JLabel pageTitle = new JLabel(title);
        pageTitle.setFont(UIConstants.FONT_HEADING_4);
        pageTitle.setForeground(UIConstants.TEXT_PRIMARY);
        topBar.add(pageTitle, BorderLayout.WEST);
        page.add(topBar, BorderLayout.NORTH);

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
