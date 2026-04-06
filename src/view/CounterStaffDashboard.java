package view;

import model.AccountService;
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.PaymentService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class CounterStaffDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Dashboard";

    private JLabel welcomeLabel;
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // Day detail / Gantt
    private JPanel dayDetailPanel;

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
            "Dashboard", "Customer Management", "Appointments", "Calendar", "Payment Collection"
    };
    private static final String[] NAV_ICONS = {
            "\u2302", "\u2663", "\u2637", "\u2339", "\u2696"
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
        contentPanel.add(new AppointmentPanel(app.getAccountService()), "Appointments");
        contentPanel.add(buildCalendarContent(),                        "Calendar");
        contentPanel.add(new PaymentCollectionPanel(app.getAccountService()), "Payment Collection");
        dayDetailPanel = new JPanel(new BorderLayout());
        dayDetailPanel.setBackground(UIConstants.BG_CONTENT);
        contentPanel.add(dayDetailPanel, "DayDetail");
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
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(30, 36, 30, 36));

        // Welcome header
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setBackground(UIConstants.BG_CONTENT);
        headerSection.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerSection.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        welcomeLabel = new JLabel("Welcome back, Staff");
        welcomeLabel.setFont(UIConstants.FONT_HEADING_2);
        welcomeLabel.setForeground(UIConstants.TEXT_PRIMARY);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel("Here\u2019s an overview of today\u2019s activity.");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_MUTED);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setBorder(new EmptyBorder(4, 0, 0, 0));

        headerSection.add(welcomeLabel);
        headerSection.add(sub);
        page.add(headerSection);
        page.add(Box.createVerticalStrut(24));

        // Gather data
        AppointmentService apptService = new AppointmentService();
        PaymentService payService = new PaymentService();
        AccountService acctService = app.getAccountService();

        List<Appointment> allAppts = apptService.getAll();
        List<String[]> allPayments = payService.getAllPayments();
        String today = LocalDate.now().toString();

        int totalAppts = allAppts.size();
        int todayAppts = 0;
        int pendingAppts = 0;
        int completedAppts = 0;
        int inProgressAppts = 0;
        int totalServiceHours = 0;

        // Collect upcoming appointments (future date, not completed)
        List<Appointment> upcomingList = new ArrayList<>();

        for (Appointment a : allAppts) {
            String dt = a.getDateTime();
            String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
            if (apptDate.equals(today)) todayAppts++;
            totalServiceHours += a.getDurationHours();
            switch (a.getStatus()) {
                case "Pending":     pendingAppts++; break;
                case "In Progress": inProgressAppts++; break;
                case "Completed":   completedAppts++; break;
            }
            // Upcoming = date >= today and not completed
            if (apptDate.compareTo(today) >= 0 && !"Completed".equals(a.getStatus())) {
                upcomingList.add(a);
            }
        }

        // Sort upcoming by date
        upcomingList.sort((a1, a2) -> a1.getDateTime().compareTo(a2.getDateTime()));

        int totalCustomers = acctService.getUsersByRole("customer").size();
        int totalTechnicians = acctService.getUsersByRole("technician").size();

        // ── TOP ROW: 4 small KPI cards ──────────────────────────────
        JPanel topRow = new JPanel(new GridLayout(1, 4, 16, 0));
        topRow.setOpaque(false);
        topRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));

        topRow.add(buildKpiCard("Total Appointments", String.valueOf(totalAppts),
                "\u2637", new Color(80, 110, 230), new Color(235, 240, 255)));
        topRow.add(buildKpiCard("Today's Appointments", String.valueOf(todayAppts),
                "\u2339", new Color(40, 167, 69), new Color(220, 245, 225)));
        topRow.add(buildKpiCard("Pending", String.valueOf(pendingAppts),
                "\u231B", new Color(255, 165, 0), new Color(255, 243, 220)));
        topRow.add(buildKpiCard("Completed", String.valueOf(completedAppts),
                "\u2714", new Color(40, 167, 69), new Color(220, 245, 225)));

        page.add(topRow);
        page.add(Box.createVerticalStrut(16));

        // ── MIDDLE ROW: 2 big cards ─────────────────────────────────
        JPanel midRow = new JPanel(new GridLayout(1, 2, 16, 0));
        midRow.setOpaque(false);
        midRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        midRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        midRow.add(buildUpcomingAppointmentsCard(upcomingList, acctService));
        midRow.add(buildStatusBreakdownCard(pendingAppts, inProgressAppts, completedAppts, totalAppts));

        page.add(midRow);
        page.add(Box.createVerticalStrut(16));

        // ── BOTTOM ROW: 2 big cards ─────────────────────────────────
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        bottomRow.add(buildServiceOverviewCard(totalServiceHours, totalAppts, totalCustomers, totalTechnicians));
        bottomRow.add(buildPaymentSummaryCard(allPayments));

        page.add(bottomRow);
        page.add(Box.createVerticalGlue());

        return page;
    }

    // ─── KPI card (matches admin report style) ────────────────────

    private JPanel buildKpiCard(String title, String value, String icon, Color accentColor, Color iconBg) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Left accent stripe
                g2.setColor(accentColor);
                g2.fillRoundRect(0, 0, 6, getHeight(), 4, 4);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 16));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 26));
        iconLabel.setForeground(accentColor);
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        valueLabel.setForeground(UIConstants.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UIConstants.FONT_SMALL);
        titleLabel.setForeground(UIConstants.TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(iconLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(titleLabel);
        return card;
    }

    // ─── Upcoming Appointments card ─────────────────────────────────

    private JPanel buildUpcomingAppointmentsCard(List<Appointment> upcoming, AccountService acctService) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel badge = new JLabel(String.valueOf(upcoming.size()));
        badge.setFont(new Font("SansSerif", Font.BOLD, 12));
        badge.setForeground(new Color(80, 110, 230));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(28, 22));
        badge.setOpaque(false);
        JPanel badgeWrapper = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 240, 255));
                g2.fillRoundRect(0, 2, 28, 22, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badgeWrapper.setOpaque(false);
        badgeWrapper.setPreferredSize(new Dimension(28, 24));
        badgeWrapper.add(badge);

        header.add(title, BorderLayout.WEST);
        header.add(badgeWrapper, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        // List of upcoming appointments
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        int maxShow = Math.min(upcoming.size(), 4);
        if (maxShow == 0) {
            JLabel empty = new JLabel("No upcoming appointments");
            empty.setFont(UIConstants.FONT_BODY);
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            empty.setBorder(new EmptyBorder(10, 0, 0, 0));
            listPanel.add(empty);
        } else {
            for (int i = 0; i < maxShow; i++) {
                Appointment a = upcoming.get(i);
                String custName = resolveUserName(acctService, a.getCustomerEmail());
                String techName = resolveUserName(acctService, a.getTechnicianEmail());
                String dt = a.getDateTime();
                String date = dt.contains(" ") ? dt.split(" ")[0] : dt;
                String time = dt.contains(" ") ? dt.split(" ")[1] : "";

                Color statusColor;
                switch (a.getStatus()) {
                    case "In Progress": statusColor = new Color(255, 165, 0); break;
                    default:            statusColor = new Color(108, 117, 125); break;
                }

                JPanel row = new JPanel(new BorderLayout(10, 0)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(248, 249, 252));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.dispose();
                    }
                };
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(8, 12, 8, 12));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

                // Left color bar
                final Color barCol = statusColor;
                JPanel bar = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(barCol);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                        g2.dispose();
                    }
                };
                bar.setOpaque(false);
                bar.setPreferredSize(new Dimension(4, 0));
                row.add(bar, BorderLayout.WEST);

                // Center info
                JPanel info = new JPanel();
                info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
                info.setOpaque(false);

                JLabel nameLine = new JLabel(custName + "  \u2022  " + a.getServiceType());
                nameLine.setFont(UIConstants.FONT_SMALL_BOLD);
                nameLine.setForeground(UIConstants.TEXT_DARK);
                nameLine.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel detailLine = new JLabel(date + " at " + time + "  |  Tech: " + techName);
                detailLine.setFont(new Font("SansSerif", Font.PLAIN, 11));
                detailLine.setForeground(UIConstants.TEXT_MUTED);
                detailLine.setAlignmentX(Component.LEFT_ALIGNMENT);

                info.add(nameLine);
                info.add(Box.createVerticalStrut(2));
                info.add(detailLine);
                row.add(info, BorderLayout.CENTER);

                // Right status
                JLabel statusLbl = new JLabel(a.getStatus());
                statusLbl.setFont(new Font("SansSerif", Font.BOLD, 10));
                statusLbl.setForeground(statusColor);
                statusLbl.setHorizontalAlignment(SwingConstants.RIGHT);
                statusLbl.setPreferredSize(new Dimension(70, 20));
                row.add(statusLbl, BorderLayout.EAST);

                listPanel.add(row);
                if (i < maxShow - 1) listPanel.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane scroll = new JScrollPane(listPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(10);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    // ─── Appointment Status Breakdown card ───────────────────────────

    private JPanel buildStatusBreakdownCard(int pending, int inProgress, int completed, int total) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Appointment Status");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Breakdown by current status");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        card.add(buildStatusBar("Pending", pending, total, new Color(108, 117, 125), new Color(235, 235, 240)));
        card.add(Box.createVerticalStrut(14));
        card.add(buildStatusBar("In Progress", inProgress, total, new Color(255, 165, 0), new Color(255, 243, 220)));
        card.add(Box.createVerticalStrut(14));
        card.add(buildStatusBar("Completed", completed, total, new Color(40, 167, 69), new Color(220, 245, 225)));

        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildStatusBar(String label, int count, int total, Color barColor, Color bgColor) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        JPanel labelRow = new JPanel(new BorderLayout());
        labelRow.setOpaque(false);
        labelRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        labelRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLabel.setForeground(UIConstants.TEXT_DARK);

        int pct = total > 0 ? Math.round((float) count / total * 100) : 0;
        JLabel countLabel = new JLabel(count + " (" + pct + "%)");
        countLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        countLabel.setForeground(barColor);

        labelRow.add(nameLabel, BorderLayout.WEST);
        labelRow.add(countLabel, BorderLayout.EAST);
        row.add(labelRow);
        row.add(Box.createVerticalStrut(5));

        final float fraction = total > 0 ? (float) count / total : 0f;
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                int fillWidth = Math.max(0, (int) (getWidth() * fraction));
                if (fillWidth > 0) {
                    g2.setColor(barColor);
                    g2.fillRoundRect(0, 0, fillWidth, getHeight(), 6, 6);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 8));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        row.add(bar);

        return row;
    }

    // ─── Service Overview card (bottom left) ────────────────────────

    private JPanel buildServiceOverviewCard(int totalHours, int totalAppts, int totalCustomers, int totalTechnicians) {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Service Overview");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Staff and service statistics");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        // 2x2 stat grid
        JPanel grid = new JPanel(new GridLayout(2, 2, 12, 12));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        grid.add(buildStatTile("Total Hours", totalHours + "h", "\u23F1", new Color(80, 110, 230), new Color(235, 240, 255)));
        grid.add(buildStatTile("Customers", String.valueOf(totalCustomers), "\u2663", new Color(160, 80, 230), new Color(240, 230, 255)));
        grid.add(buildStatTile("Technicians", String.valueOf(totalTechnicians), "\u2692", new Color(40, 167, 69), new Color(220, 245, 225)));
        double avg = totalAppts > 0 ? (double) totalHours / totalAppts : 0;
        grid.add(buildStatTile("Avg Duration", String.format("%.1fh", avg), "\u2338", new Color(255, 165, 0), new Color(255, 243, 220)));

        card.add(grid);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JPanel buildStatTile(String label, String value, String icon, Color accentColor, Color bgColor) {
        JPanel tile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        tile.setOpaque(false);
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Icon box
        JLabel iconLabel = new JLabel(icon) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 30));
                g2.fillRoundRect(0, 0, 28, 28, 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        iconLabel.setForeground(accentColor);
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setPreferredSize(new Dimension(28, 28));
        iconLabel.setMaximumSize(new Dimension(28, 28));
        iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(iconLabel);
        tile.add(Box.createVerticalStrut(8));

        JLabel valLabel = new JLabel(value);
        valLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        valLabel.setForeground(UIConstants.TEXT_PRIMARY);
        valLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(valLabel);
        tile.add(Box.createVerticalStrut(2));

        JLabel nameLabel = new JLabel(label);
        nameLabel.setFont(UIConstants.FONT_SMALL);
        nameLabel.setForeground(UIConstants.TEXT_MUTED);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        tile.add(nameLabel);

        return tile;
    }

    // ─── Payment Summary card (bottom right) ────────────────────────

    private JPanel buildPaymentSummaryCard(List<String[]> allPayments) {
        int paidCount = 0;
        double totalPaid = 0;
        double todayTotal = 0;
        int todayCount = 0;
        String today = LocalDate.now().toString();

        for (String[] p : allPayments) {
            String status = p[8].trim();
            double amount = 0;
            try { amount = Double.parseDouble(p[5].trim()); } catch (NumberFormatException ignored) {}
            if ("Paid".equalsIgnoreCase(status)) {
                paidCount++;
                totalPaid += amount;
                if (p[6].trim().equals(today)) {
                    todayTotal += amount;
                    todayCount++;
                }
            }
        }

        // Count unpaid completed appointments (no payment record)
        AppointmentService apptSvc = new AppointmentService();
        List<Appointment> allAppts = apptSvc.getAll();
        Set<String> paidApptIds = new HashSet<>();
        for (String[] p : allPayments) {
            paidApptIds.add(p[3].trim());
        }
        int unpaidCount = 0;
        for (Appointment a : allAppts) {
            if ("Completed".equals(a.getStatus()) && !paidApptIds.contains(a.getId())) {
                unpaidCount++;
            }
        }

        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JLabel title = new JLabel("Payment Summary");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(4));

        JLabel subtitle = new JLabel("Collection status overview");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(20));

        // Total collected
        JLabel totalLabel = new JLabel(String.format("RM %.2f", totalPaid));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        totalLabel.setForeground(new Color(40, 167, 69));
        totalLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(totalLabel);
        card.add(Box.createVerticalStrut(2));

        JLabel totalNote = new JLabel("Total collected from " + paidCount + " payment" + (paidCount != 1 ? "s" : ""));
        totalNote.setFont(UIConstants.FONT_SMALL);
        totalNote.setForeground(UIConstants.TEXT_MUTED);
        totalNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(totalNote);
        card.add(Box.createVerticalStrut(18));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(235, 235, 240));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        card.add(sep);
        card.add(Box.createVerticalStrut(14));

        // Paid / Unpaid / Today row
        JPanel statsRow = new JPanel(new GridLayout(1, 3, 10, 0));
        statsRow.setOpaque(false);
        statsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        statsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));

        // Paid tile
        JPanel paidTile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220, 245, 225));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        paidTile.setOpaque(false);
        paidTile.setLayout(new BoxLayout(paidTile, BoxLayout.Y_AXIS));
        paidTile.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel paidVal = new JLabel(String.valueOf(paidCount) + " Paid");
        paidVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        paidVal.setForeground(new Color(40, 167, 69));
        paidVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        paidTile.add(paidVal);

        JLabel paidAmt = new JLabel(String.format("RM %.2f", totalPaid));
        paidAmt.setFont(UIConstants.FONT_SMALL);
        paidAmt.setForeground(new Color(40, 167, 69));
        paidAmt.setAlignmentX(Component.LEFT_ALIGNMENT);
        paidTile.add(paidAmt);

        statsRow.add(paidTile);

        // Unpaid tile
        final int unpaidFinal = unpaidCount;
        JPanel unpaidTile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 243, 220));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        unpaidTile.setOpaque(false);
        unpaidTile.setLayout(new BoxLayout(unpaidTile, BoxLayout.Y_AXIS));
        unpaidTile.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel unpaidVal = new JLabel(String.valueOf(unpaidCount) + " Unpaid");
        unpaidVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        unpaidVal.setForeground(new Color(255, 165, 0));
        unpaidVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        unpaidTile.add(unpaidVal);

        JLabel unpaidNote = new JLabel("awaiting collection");
        unpaidNote.setFont(UIConstants.FONT_SMALL);
        unpaidNote.setForeground(new Color(255, 165, 0));
        unpaidNote.setAlignmentX(Component.LEFT_ALIGNMENT);
        unpaidTile.add(unpaidNote);

        statsRow.add(unpaidTile);

        // Today tile
        JPanel todayTile = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(235, 240, 255));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
            }
        };
        todayTile.setOpaque(false);
        todayTile.setLayout(new BoxLayout(todayTile, BoxLayout.Y_AXIS));
        todayTile.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel todayVal = new JLabel(todayCount + " Today");
        todayVal.setFont(new Font("SansSerif", Font.BOLD, 16));
        todayVal.setForeground(new Color(80, 110, 230));
        todayVal.setAlignmentX(Component.LEFT_ALIGNMENT);
        todayTile.add(todayVal);

        JLabel todayAmt = new JLabel(String.format("RM %.2f", todayTotal));
        todayAmt.setFont(UIConstants.FONT_SMALL);
        todayAmt.setForeground(new Color(80, 110, 230));
        todayAmt.setAlignmentX(Component.LEFT_ALIGNMENT);
        todayTile.add(todayAmt);

        statsRow.add(todayTile);

        card.add(statsRow);
        card.add(Box.createVerticalGlue());

        return card;
    }

    // ─── Calendar content ─────────────────────────────────────────

    private JPanel buildCalendarContent() {
        JPanel page = new JPanel(new BorderLayout(16, 0));
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(30, 36, 30, 36));

        AppointmentService apptService = new AppointmentService();
        AccountService acctService = app.getAccountService();
        final YearMonth[] currentMonth = { YearMonth.now() };
        final LocalDate[] selectedDate = { LocalDate.now() };

        // ── LEFT: Calendar card (big) ────────────────────────────────
        JPanel calCard = new JPanel(new BorderLayout()) {
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
        calCard.setOpaque(false);
        calCard.setBorder(new EmptyBorder(28, 32, 24, 32));

        // Month navigation header
        JPanel navRow = new JPanel(new BorderLayout());
        navRow.setOpaque(false);
        navRow.setBorder(new EmptyBorder(0, 0, 16, 0));

        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        monthLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JButton prevBtn = calNavBtn("\u25C0");
        JButton nextBtn = calNavBtn("\u25B6");

        navRow.add(prevBtn, BorderLayout.WEST);
        navRow.add(monthLabel, BorderLayout.CENTER);
        navRow.add(nextBtn, BorderLayout.EAST);
        calCard.add(navRow, BorderLayout.NORTH);

        // Calendar grid
        JPanel calBody = new JPanel(new BorderLayout(0, 8));
        calBody.setOpaque(false);

        JPanel dowRow = new JPanel(new GridLayout(1, 7, 4, 0));
        dowRow.setOpaque(false);
        dowRow.setBorder(new EmptyBorder(0, 0, 4, 0));
        for (String d : new String[]{"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}) {
            JLabel dl = new JLabel(d, SwingConstants.CENTER);
            dl.setFont(new Font("SansSerif", Font.BOLD, 13));
            dl.setForeground(UIConstants.TEXT_MUTED);
            dowRow.add(dl);
        }
        calBody.add(dowRow, BorderLayout.NORTH);

        JPanel calGrid = new JPanel(new GridLayout(0, 7, 4, 4));
        calGrid.setOpaque(false);
        calBody.add(calGrid, BorderLayout.CENTER);

        calCard.add(calBody, BorderLayout.CENTER);

        // ── RIGHT: Summary card ──────────────────────────────────────
        JPanel summaryCard = new JPanel(new BorderLayout()) {
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
        summaryCard.setOpaque(false);
        summaryCard.setBorder(new EmptyBorder(24, 24, 24, 24));
        summaryCard.setPreferredSize(new Dimension(420, 0));
        summaryCard.setMinimumSize(new Dimension(420, 0));

        JLabel summaryTitle = new JLabel("Appointments for today");
        summaryTitle.setFont(new Font("SansSerif", Font.BOLD, 16));
        summaryTitle.setForeground(UIConstants.TEXT_PRIMARY);
        summaryTitle.setBorder(new EmptyBorder(0, 0, 14, 0));
        summaryCard.add(summaryTitle, BorderLayout.NORTH);

        JPanel summaryContent = new JPanel();
        summaryContent.setLayout(new BoxLayout(summaryContent, BoxLayout.Y_AXIS));
        summaryContent.setOpaque(false);

        JScrollPane summaryScroll = new JScrollPane(summaryContent,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        summaryScroll.setBorder(null);
        summaryScroll.setOpaque(false);
        summaryScroll.getViewport().setOpaque(false);
        summaryScroll.getVerticalScrollBar().setUnitIncrement(12);
        summaryCard.add(summaryScroll, BorderLayout.CENTER);

        // ── Refresh summary cards ────────────────────────────────────
        Runnable refreshSummary = () -> {
            summaryContent.removeAll();
            List<Appointment> all = apptService.getAll();
            String dateStr = selectedDate[0].toString();
            summaryTitle.setText("Appointments for " + dateStr);
            int count = 0;
            for (Appointment a : all) {
                String dt = a.getDateTime();
                String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
                if (!apptDate.equals(dateStr)) continue;
                count++;
                String time = dt.contains(" ") ? dt.split(" ")[1] : "";
                String custName = resolveUserName(acctService, a.getCustomerEmail());
                String techName = resolveUserName(acctService, a.getTechnicianEmail());
                summaryContent.add(buildAppointmentCard(a.getId(), custName, techName, a.getServiceType(), time, a.getDurationHours(), a.getStatus()));
                summaryContent.add(Box.createVerticalStrut(8));
            }
            if (count == 0) {
                JLabel empty = new JLabel("No appointments on this date");
                empty.setFont(UIConstants.FONT_BODY);
                empty.setForeground(UIConstants.TEXT_MUTED);
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                empty.setBorder(new EmptyBorder(12, 0, 0, 0));
                summaryContent.add(empty);
            }
            summaryContent.revalidate();
            summaryContent.repaint();
        };

        // ── Calendar grid refresh ────────────────────────────────────
        Runnable[] calRefresh = new Runnable[1];
        calRefresh[0] = () -> {
            monthLabel.setText(currentMonth[0].getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentMonth[0].getYear());
            calGrid.removeAll();

            // Collect appointment statuses per date
            Map<String, List<String>> apptsByDate = new HashMap<>();
            for (Appointment a : apptService.getAll()) {
                String dt = a.getDateTime();
                String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
                apptsByDate.computeIfAbsent(apptDate, k -> new ArrayList<>()).add(a.getStatus());
            }

            LocalDate first = currentMonth[0].atDay(1);
            int startDow = first.getDayOfWeek().getValue() % 7;
            int daysInMonth = currentMonth[0].lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int i = 0; i < startDow; i++) calGrid.add(new JLabel(""));

            for (int d = 1; d <= daysInMonth; d++) {
                LocalDate date = currentMonth[0].atDay(d);
                List<String> statuses = apptsByDate.getOrDefault(date.toString(), Collections.emptyList());
                boolean isToday = date.equals(today);
                boolean isSel = date.equals(selectedDate[0]);
                final int day = d;

                JPanel cell = new JPanel(new BorderLayout(0, 1)) {
                    @Override protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        if (isSel) {
                            g2.setColor(UIConstants.PRIMARY);
                            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                        } else if (isToday) {
                            g2.setColor(new Color(235, 240, 255));
                            g2.fillRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 10, 10);
                        }
                        // Border for all date cells
                        g2.setColor(isSel ? UIConstants.PRIMARY : new Color(220, 222, 230));
                        g2.setStroke(new BasicStroke(1f));
                        g2.drawRoundRect(2, 2, getWidth() - 5, getHeight() - 5, 10, 10);
                        g2.dispose();
                    }
                };
                cell.setOpaque(false);
                cell.setPreferredSize(new Dimension(44, 56));

                JLabel dayLabel = new JLabel(String.valueOf(d), SwingConstants.CENTER);
                dayLabel.setFont(new Font("SansSerif", isToday || isSel ? Font.BOLD : Font.PLAIN, 14));
                dayLabel.setForeground(isSel ? Color.WHITE : (isToday ? UIConstants.PRIMARY : UIConstants.TEXT_DARK));
                cell.add(dayLabel, BorderLayout.CENTER);

                // Status-colored bars at the bottom, one per appointment
                if (!statuses.isEmpty()) {
                    JPanel barsPanel = new JPanel();
                    barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
                    barsPanel.setOpaque(false);
                    barsPanel.setBorder(new EmptyBorder(0, 6, 3, 6));

                    int maxBars = Math.min(statuses.size(), 3);
                    for (int si = 0; si < maxBars; si++) {
                        String status = statuses.get(si);
                        Color barColor;
                        switch (status) {
                            case "Completed":   barColor = new Color(40, 167, 69); break;
                            case "In Progress": barColor = new Color(255, 165, 0); break;
                            case "Pending":     barColor = new Color(108, 117, 125); break;
                            default:            barColor = new Color(108, 117, 125); break;
                        }
                        final Color fc = isSel ? new Color(255, 255, 255, 200) : barColor;
                        JPanel bar = new JPanel() {
                            @Override protected void paintComponent(Graphics g) {
                                Graphics2D g2 = (Graphics2D) g.create();
                                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                                g2.setColor(fc);
                                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                                g2.dispose();
                            }
                        };
                        bar.setOpaque(false);
                        bar.setPreferredSize(new Dimension(Integer.MAX_VALUE, 4));
                        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
                        bar.setAlignmentX(Component.LEFT_ALIGNMENT);
                        barsPanel.add(bar);
                        if (si < maxBars - 1) barsPanel.add(Box.createVerticalStrut(1));
                    }
                    // Show "+N" if more than 3
                    if (statuses.size() > 3) {
                        JLabel more = new JLabel("+" + (statuses.size() - 3), SwingConstants.CENTER);
                        more.setFont(new Font("SansSerif", Font.BOLD, 8));
                        more.setForeground(isSel ? Color.WHITE : UIConstants.TEXT_MUTED);
                        more.setAlignmentX(Component.LEFT_ALIGNMENT);
                        barsPanel.add(more);
                    }
                    cell.add(barsPanel, BorderLayout.SOUTH);
                }

                cell.setCursor(new Cursor(Cursor.HAND_CURSOR));
                cell.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        LocalDate clicked = currentMonth[0].atDay(day);
                        if (clicked.equals(selectedDate[0])) {
                            // Second click on same date -> open Gantt chart
                            showDayDetail(selectedDate[0]);
                        } else {
                            // First click -> just select and show summary
                            selectedDate[0] = clicked;
                            calRefresh[0].run();
                            refreshSummary.run();
                        }
                    }
                });
                calGrid.add(cell);
            }
            calGrid.revalidate();
            calGrid.repaint();
        };

        prevBtn.addActionListener(e -> { currentMonth[0] = currentMonth[0].minusMonths(1); calRefresh[0].run(); });
        nextBtn.addActionListener(e -> { currentMonth[0] = currentMonth[0].plusMonths(1); calRefresh[0].run(); });

        // Initial render
        calRefresh[0].run();
        refreshSummary.run();

        // Layout: calendar (big) on left, summaries on right
        page.add(calCard, BorderLayout.CENTER);
        page.add(summaryCard, BorderLayout.EAST);
        return page;
    }

    private JPanel buildAppointmentCard(String id, String customer, String technician,
                                         String service, String time, int hours, String status) {
        Color barColor;
        switch (status) {
            case "Completed":   barColor = new Color(40, 167, 69); break;
            case "In Progress": barColor = new Color(255, 165, 0); break;
            default:            barColor = new Color(108, 117, 125); break;
        }

        JPanel card = new JPanel(new BorderLayout(8, 0)) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(248, 249, 252));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(230, 232, 240));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        card.setMinimumSize(new Dimension(0, 72));
        card.setBorder(new EmptyBorder(8, 8, 8, 10));

        // Left: color bar
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(4, 0));
        card.add(bar, BorderLayout.WEST);

        // Center: all details stacked vertically
        JPanel details = new JPanel();
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.setOpaque(false);

        JLabel line1 = new JLabel(service + "  \u2022  " + hours + "h  \u2022  " + time);
        line1.setFont(UIConstants.FONT_SMALL_BOLD);
        line1.setForeground(UIConstants.TEXT_PRIMARY);
        line1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel line2 = new JLabel(customer + "  |  Tech: " + technician);
        line2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        line2.setForeground(UIConstants.TEXT_SECONDARY);
        line2.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Status badge
        JLabel statusLabel = new JLabel(status) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                switch (status) {
                    case "Completed":   bg = new Color(220, 245, 225); break;
                    case "In Progress": bg = new Color(255, 243, 220); break;
                    default:            bg = new Color(235, 235, 240); break;
                }
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        statusLabel.setForeground(barColor);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusLabel.setBorder(new EmptyBorder(2, 8, 2, 8));
        statusLabel.setMaximumSize(new Dimension(80, 18));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        details.add(line1);
        details.add(Box.createVerticalStrut(2));
        details.add(line2);
        details.add(Box.createVerticalStrut(4));
        details.add(statusLabel);
        card.add(details, BorderLayout.CENTER);

        return card;
    }

    private JButton calNavBtn(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setPreferredSize(new Dimension(40, 32));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private String resolveUserName(AccountService svc, String idOrEmail) {
        for (User u : svc.getAllUsers()) {
            if (u.getEmail().equalsIgnoreCase(idOrEmail)) return u.getName();
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(idOrEmail)) return u.getName();
        }
        return idOrEmail;
    }

    // ─── Day Detail / Gantt chart ───────────────────────────────

    private void showDayDetail(LocalDate date) {
        dayDetailPanel.removeAll();
        dayDetailPanel.add(buildDayDetailContent(date), BorderLayout.CENTER);
        dayDetailPanel.revalidate();
        dayDetailPanel.repaint();
        headerTitle.setText("Schedule \u2014 " + date.toString());
        contentLayout.show(contentPanel, "DayDetail");
    }

    private JPanel buildDayDetailContent(LocalDate date) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(24, 36, 24, 36));

        // ── Top bar: Back button + date title + legend ──
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        topBar.setBorder(new EmptyBorder(0, 0, 16, 0));

        JButton backBtn = new JButton("\u25C0  Back to Calendar");
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setForeground(UIConstants.PRIMARY);
        backBtn.setContentAreaFilled(false);
        backBtn.setBorderPainted(false);
        backBtn.setFocusPainted(false);
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        backBtn.addActionListener(e -> {
            headerTitle.setText("Calendar");
            contentLayout.show(contentPanel, "Calendar");
        });
        topBar.add(backBtn, BorderLayout.WEST);

        JLabel dateTitle = new JLabel(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + ", " + date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + date.getDayOfMonth() + ", " + date.getYear(), SwingConstants.CENTER);
        dateTitle.setFont(new Font("SansSerif", Font.BOLD, 18));
        dateTitle.setForeground(UIConstants.TEXT_PRIMARY);
        topBar.add(dateTitle, BorderLayout.CENTER);

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        legend.setOpaque(false);
        legend.add(legendDot(new Color(108, 117, 125), "Pending"));
        legend.add(legendDot(new Color(255, 165, 0), "In Progress"));
        legend.add(legendDot(new Color(40, 167, 69), "Completed"));
        topBar.add(legend, BorderLayout.EAST);

        page.add(topBar, BorderLayout.NORTH);

        // ── Gather appointments for this date ──
        AppointmentService apptService = new AppointmentService();
        AccountService acctService = app.getAccountService();
        List<Appointment> allAppts = apptService.getAll();
        String dateStr = date.toString();

        List<Appointment> dayAppts = allAppts.stream()
                .filter(a -> {
                    String dt = a.getDateTime();
                    String apptDate = dt.contains(" ") ? dt.split(" ")[0] : dt;
                    return apptDate.equals(dateStr);
                })
                .collect(Collectors.toList());

        if (dayAppts.isEmpty()) {
            JLabel empty = new JLabel("No appointments scheduled for this date.");
            empty.setFont(new Font("SansSerif", Font.PLAIN, 16));
            empty.setForeground(UIConstants.TEXT_MUTED);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setVerticalAlignment(SwingConstants.CENTER);
            page.add(empty, BorderLayout.CENTER);
            return page;
        }

        // Group by technician
        Map<String, List<Appointment>> byTech = new LinkedHashMap<>();
        for (Appointment a : dayAppts) {
            String techName = resolveUserName(acctService, a.getTechnicianEmail());
            byTech.computeIfAbsent(techName, k -> new ArrayList<>()).add(a);
        }

        // ── Gantt chart panel (stretches to fill width, no left labels) ──
        int startHour = 7;
        int endHour = 20;
        int totalHours = endHour - startHour;
        int leftPad = 16;
        int rowHeight = 100;
        int headerHeight = 32;

        JPanel ganttChart = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int panelW = getWidth();
                int chartAreaW = panelW - leftPad * 2;
                int hourWidth = Math.max(50, chartAreaW / totalHours);

                // Background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, panelW, getHeight(), 16, 16);
                g2.setColor(UIConstants.BORDER_DEFAULT);
                g2.drawRoundRect(0, 0, panelW - 1, getHeight() - 1, 16, 16);

                int y0 = headerHeight;

                // ── Hour header labels ──
                g2.setFont(new Font("SansSerif", Font.BOLD, 11));
                for (int h = 0; h < totalHours; h++) {
                    int x = leftPad + h * hourWidth;
                    int hour = startHour + h;
                    String lbl = (hour <= 12 ? hour : hour - 12) + ":00 " + (hour < 12 ? "AM" : "PM");
                    g2.setColor(UIConstants.TEXT_MUTED);
                    g2.drawString(lbl, x + 4, y0 - 8);
                }

                // ── Horizontal row separators only ──
                for (int r = 0; r <= byTech.size(); r++) {
                    int y = y0 + r * rowHeight;
                    g2.setColor(new Color(235, 237, 242));
                    g2.drawLine(leftPad, y, leftPad + totalHours * hourWidth, y);
                }

                // ── Technician rows (no left name label) ──
                int rowIdx = 0;
                for (Map.Entry<String, List<Appointment>> entry : byTech.entrySet()) {
                    String techName = entry.getKey();
                    List<Appointment> appts = entry.getValue();
                    int rowY = y0 + rowIdx * rowHeight;

                    // ── Draw appointment bars ──
                    for (Appointment a : appts) {
                        String dt = a.getDateTime();
                        String timePart = dt.contains(" ") ? dt.split(" ")[1] : "09:00";
                        String[] hm = timePart.split(":");
                        int apptHour = 9, apptMin = 0;
                        try {
                            apptHour = Integer.parseInt(hm[0]);
                            if (hm.length > 1) apptMin = Integer.parseInt(hm[1]);
                        } catch (NumberFormatException ignored) {}

                        double startOffset = (apptHour - startHour) + apptMin / 60.0;
                        double duration = a.getDurationHours();
                        if (startOffset < 0) { duration += startOffset; startOffset = 0; }
                        if (startOffset + duration > totalHours) duration = totalHours - startOffset;
                        if (duration <= 0) continue;

                        int barX = leftPad + (int)(startOffset * hourWidth);
                        int barW = (int)(duration * hourWidth);
                        int barY = rowY + 6;
                        int barH = rowHeight - 12;

                        // Bar color based on status
                        Color barColor;
                        switch (a.getStatus()) {
                            case "Completed":   barColor = new Color(40, 167, 69); break;
                            case "In Progress": barColor = new Color(255, 165, 0); break;
                            default:            barColor = new Color(108, 117, 125); break;
                        }

                        // Shadow
                        g2.setColor(new Color(0, 0, 0, 30));
                        g2.fillRoundRect(barX + 2, barY + 2, barW, barH, 12, 12);

                        // Main bar
                        g2.setColor(barColor);
                        g2.fillRoundRect(barX, barY, barW, barH, 12, 12);

                        // Left accent stripe
                        g2.setColor(new Color(255, 255, 255, 60));
                        g2.fillRoundRect(barX, barY, 5, barH, 4, 4);

                        // 4-line bar text (bigger bar = more room)
                        String custName = resolveUserName(acctService, a.getCustomerEmail());
                        g2.setColor(Color.WHITE);

                        // Line 1: technician name (bold)
                        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
                        FontMetrics fm1 = g2.getFontMetrics();
                        if (fm1.stringWidth(techName) < barW - 16) {
                            g2.drawString(techName, barX + 12, barY + 18);
                        }

                        // Line 2: service type
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
                        FontMetrics fm2 = g2.getFontMetrics();
                        if (fm2.stringWidth(a.getServiceType()) < barW - 16) {
                            g2.drawString(a.getServiceType(), barX + 12, barY + 35);
                        }

                        // Line 3: customer name
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
                        FontMetrics fm3 = g2.getFontMetrics();
                        String custLine = "Customer: " + custName;
                        if (fm3.stringWidth(custLine) < barW - 16) {
                            g2.drawString(custLine, barX + 12, barY + 51);
                        }

                        // Line 4: time + duration + status
                        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                        FontMetrics fm4 = g2.getFontMetrics();
                        String line4 = timePart + "  \u2022  " + a.getDurationHours() + "h  \u2022  " + a.getStatus();
                        if (fm4.stringWidth(line4) < barW - 16) {
                            g2.drawString(line4, barX + 12, barY + 66);
                        }
                    }
                    rowIdx++;
                }

                g2.dispose();
            }

            @Override public Dimension getPreferredSize() {
                int h = headerHeight + byTech.size() * rowHeight + 20;
                return new Dimension(super.getPreferredSize().width, h);
            }
        };
        ganttChart.setOpaque(false);

        JScrollPane scroll = new JScrollPane(ganttChart,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel legendDot(Color color, String text) {
        JPanel dot = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        dot.setOpaque(false);
        JLabel circle = new JLabel("\u25CF");
        circle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        circle.setForeground(color);
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(UIConstants.TEXT_DARK);
        dot.add(circle);
        dot.add(label);
        return dot;
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
