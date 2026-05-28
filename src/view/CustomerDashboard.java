package view;

import model.AppointmentSectionController;
import model.AppointmentService;
import model.BackgroundImageStorage;
import model.CustomerProfileController;
import model.PaymentService;
import model.ProfilePicStorage;
import model.ServiceHistoryService;
import model.User;
import model.VehicleSectionController;
import model.VehicleService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomerDashboard extends JPanel {

    // ── Reference to the main application window ──────────────────
    private final AppFrame app;

    // ── Used to switch between pages ──────────────────────────────
    private CardLayout contentLayout;
    private JPanel     contentPanel;
    private String     activeNav = "Dashboard";

    // ── Labels shown in the top header bar ────────────────────────
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // ── Avatar colours ────────────────────────────────────────────
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230), new Color(230, 80, 80),
            new Color(80, 190, 110), new Color(230, 160, 40),
            new Color(160, 80, 230), new Color(40, 180, 200),
            new Color(230, 80, 160), new Color(100, 100, 120),
    };
    private int           selectedAvatarIndex = 0;
    private BufferedImage profileImage        = null;

    // ── Navigation buttons ────────────────────────────────────────
    private JButton[] navButtons;

    // ── Page panels ───────────────────────────────────────────────
    private ServiceHistoryPage serviceHistoryPage;
    private PaymentHistoryPage paymentHistoryPage;
    private StaffReviewPage    staffReviewPage;
    private MyFeedbackPage     myFeedbackPage;
    private ViewProfile        viewProfilePage;

    // ── Services ──────────────────────────────────────────────────
    private final CustomerProfileController    profileController;
    private final VehicleSectionController     vehicleController;
    private final AppointmentSectionController appointmentController;
    private final VehicleService               vehicleService        = new VehicleService();
    private final ProfilePicStorage            profilePicStorage     = new ProfilePicStorage();
    private final BackgroundImageStorage       backgroundStorage     = new BackgroundImageStorage();
    private final AppointmentService           appointmentService    = new AppointmentService();
    private final PaymentService               paymentService        = new PaymentService();
    private final ServiceHistoryService        serviceHistoryService = new ServiceHistoryService();

    // ── Dashboard home panel ──────────────────────────────────────
    private JPanel dashboardPanel;

    // ── Colours ───────────────────────────────────────────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color COLOR_GREEN = new Color(80, 190, 110);
    private static final Color COLOR_AMBER = new Color(230, 160, 40);
    private static final Color COLOR_TEAL  = new Color(40, 180, 200);

    // ── Nav items ─────────────────────────────────────────────────
    private static final String[] NAV_ITEMS = {
            "Dashboard", "Service History", "Payment History", "Staff Review", "My Feedback"
    };

    // ── Unicode icons for the left sidebar navigation ─────────────
    private static final String[] NAV_ICONS = {
            "\u2302",           // ⌂    Dashboard
            "\uD83D\uDD04",     // 🔄   Service History
            "\uD83D\uDCB5",     // 💵   Payment History
            "\u2605",           // ★   Staff Review
            "\uD83D\uDCAC"      // 💬   My Feedback
    };

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════
    public CustomerDashboard(AppFrame app) {
        this.app = app;

        profileController = new CustomerProfileController(
                app.getAccountService(),
                new CustomerProfileController.AppFrameAccessor() {
                    @Override public User   getLoggedInUserObj()       { return app.getLoggedInUserObj(); }
                    @Override public String getLoggedInUser()          { return app.getLoggedInUser(); }
                    @Override public void   setLoggedInUser(String n)  { app.setLoggedInUser(n); }
                    @Override public void   setLoggedInUserObj(User u) { app.setLoggedInUserObj(u); }
                }
        );

        vehicleController = new VehicleSectionController(
                vehicleService,
                new VehicleSectionController.SectionView() {
                    @Override public User getLoggedInUser()             { return app.getLoggedInUserObj(); }
                    @Override public void rebuildList(List<String[]> v) { }
                    @Override public void showMessage(String msg, String title, int type) {
                        JOptionPane.showMessageDialog(app, msg, title, type);
                    }
                    @Override public java.awt.Window getWindow() {
                        return SwingUtilities.getWindowAncestor(CustomerDashboard.this);
                    }
                }
        );

        appointmentController = new AppointmentSectionController(
                () -> app.getLoggedInUserObj(),
                appointmentService, paymentService, serviceHistoryService, vehicleService
        );

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        dashboardPanel = new JPanel(new BorderLayout());
        dashboardPanel.setBackground(UIConstants.BG_CONTENT);
        contentPanel.add(dashboardPanel, "Dashboard");

        serviceHistoryPage = new ServiceHistoryPage();
        paymentHistoryPage = new PaymentHistoryPage();
        staffReviewPage    = new StaffReviewPage(app.getLoggedInUserObj());
        myFeedbackPage     = new MyFeedbackPage(app.getLoggedInUserObj());
        viewProfilePage    = new ViewProfile(app, vehicleController, profileController,
                                              vehicleService, profilePicStorage, backgroundStorage);

        contentPanel.add(serviceHistoryPage, "Service History");
        contentPanel.add(paymentHistoryPage, "Payment History");
        contentPanel.add(staffReviewPage,    "Staff Review");
        contentPanel.add(myFeedbackPage,     "My Feedback");
        contentPanel.add(viewProfilePage,    "View Profile");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    private void resetDashboardState() {
        activeNav = "Dashboard";
        if (headerTitle   != null) headerTitle.setText("Dashboard");
        if (contentLayout != null) contentLayout.show(contentPanel, "Dashboard");
        if (navButtons    != null) {
            for (int j = 0; j < navButtons.length; j++) {
                updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals("Dashboard"));
            }
        }
    }

    public void refreshUser() {
        resetDashboardState();

        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Customer";
        if (profileLabel != null) profileLabel.setText(name);

        User user = app.getLoggedInUserObj();
        if (user != null) {
            selectedAvatarIndex = user.getProfilePicture();
            profileImage        = profilePicStorage.loadImage(user.getUserId());
        }
        if (avatarLabel != null) avatarLabel.repaint();

        if (staffReviewPage != null) staffReviewPage.setUser(app.getLoggedInUserObj());
        if (myFeedbackPage  != null) myFeedbackPage.setUser(app.getLoggedInUserObj());
        if (viewProfilePage != null) viewProfilePage.refreshUser();

        rebuildDashboard();
    }

    private void rebuildDashboard() {
        dashboardPanel.removeAll();
        dashboardPanel.add(buildDashboardInner(), BorderLayout.CENTER);
        dashboardPanel.revalidate();
        dashboardPanel.repaint();
    }

    // ═══════════════════════════════════════════════════════════════
    // buildDashboardInner()
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildDashboardInner() {
        User user = app.getLoggedInUserObj();

        // ── Load data via controllers (no file reading here) ──────
        List<String[]> allAppts = appointmentController.getAllAppointmentsForUser();
        List<String[]> upcoming = appointmentController.getPendingAppointments();
        Set<String>    paidIds  = appointmentController.getPaidAppointmentIds();
        List<String[]> unpaid   = appointmentController.getUnpaidAppointments();
        List<String[]> vehicles = (user != null)
                ? vehicleService.getVehiclesByUserId(user.getUserId())
                : new ArrayList<>();

        // ── Appointment calculations → appointmentController ──────
        int    totalAppts   = allAppts.size();
        int    vehicleCount = vehicles.size();
        String nextApptDate = appointmentController.getNextAppointmentDate(upcoming);

        // ── Payment calculations → paymentService ─────────────────
        double pendingAmount = paymentService.calcPendingAmount(unpaid, appointmentController);

        JPanel outer = new JPanel();
        outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
        outer.setBackground(UIConstants.BG_CONTENT);
        outer.setBorder(new EmptyBorder(18, 22, 18, 22));

        String userName = (user != null) ? user.getName() : "Customer";
        JLabel welcome  = new JLabel("Welcome back, " + userName
                + " \u2014 here\u2019s your overview");
        welcome.setFont(new Font("SansSerif", Font.PLAIN, 14));
        welcome.setForeground(UIConstants.TEXT_MUTED);
        welcome.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(welcome);
        outer.add(Box.createVerticalStrut(14));

        JPanel row1 = buildStatCardsRow(totalAppts, pendingAmount,
                unpaid.size(), vehicleCount, vehicles, nextApptDate, upcoming, allAppts);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(row1);
        outer.add(Box.createVerticalStrut(14));

        JPanel row2 = buildChartsRow(allAppts, paidIds);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(row2);
        outer.add(Box.createVerticalStrut(14));

        JPanel row3 = buildBottomRow(upcoming, unpaid);
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);
        outer.add(row3);

        return outer;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROW 1 — FOUR STAT CARDS
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildStatCardsRow(int totalAppts, double pendingAmount,
                                     int pendingCount, int vehicleCount,
                                     List<String[]> vehicles,
                                     String nextApptDate,
                                     List<String[]> upcoming,
                                     List<String[]> allAppts) {

        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        // ── Appointment stat card helpers → appointmentController ─
        int    thisMonth = appointmentController.countThisMonth(allAppts);
        String sub1      = thisMonth > 0 ? "\u25B2 " + thisMonth + " this month" : "No new this month";
        Color  sub1Color = thisMonth > 0 ? COLOR_GREEN : UIConstants.TEXT_MUTED;
        row.add(buildStatCard("Total appointments", String.valueOf(totalAppts),
                sub1, sub1Color, BRAND_BLUE));

        // ── Pending payment card — amount already calculated in buildDashboardInner ──
        String amtText   = String.format("RM %,.0f", pendingAmount);
        String sub2      = pendingCount + " invoice" + (pendingCount == 1 ? "" : "s") + " due";
        Color  sub2Color = pendingCount > 0 ? COLOR_AMBER : UIConstants.TEXT_MUTED;
        row.add(buildStatCard("Pending payment", amtText, sub2, sub2Color, COLOR_AMBER));

        // ── Vehicle subtitle → vehicleService ────────────────────
        String sub3 = vehicleCount == 0 ? "No vehicles"
                : vehicleService.buildVehicleSubtitle(vehicles);
        row.add(buildStatCard("Registered vehicles", String.valueOf(vehicleCount),
                sub3, UIConstants.TEXT_MUTED, COLOR_GREEN));

        // ── Days-until label → appointmentController ──────────────
        String sub4      = upcoming.isEmpty() ? ""
                : appointmentController.getDaysUntilLabel(upcoming.get(0)[5]);
        Color  sub4Color = upcoming.isEmpty() ? UIConstants.TEXT_MUTED : BRAND_BLUE;
        row.add(buildStatCard("Next appointment",
                nextApptDate.isEmpty() ? "None" : nextApptDate,
                sub4, sub4Color, COLOR_TEAL));

        return row;
    }

    private JPanel buildStatCard(String title, String value,
                                  String subtitle, Color subColor,
                                  Color accentColor) {

        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(
                        0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 5, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(200, 112));

        int textX = 16;
        int textW = 180;

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        titleLbl.setForeground(UIConstants.TEXT_MUTED);
        titleLbl.setBounds(textX, 12, textW, 18);
        card.add(titleLbl);

        JLabel valueLbl = new JLabel(value);
        valueLbl.setFont(new Font("SansSerif", Font.BOLD, 22));
        valueLbl.setForeground(UIConstants.TEXT_PRIMARY);
        valueLbl.setBounds(textX, 32, textW, 34);
        card.add(valueLbl);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("SansSerif", Font.PLAIN, 11));
        subLbl.setForeground(subColor);
        subLbl.setBounds(textX, 72, textW, 16);
        card.add(subLbl);

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROW 2 — CHARTS ROW
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildChartsRow(List<String[]> allAppts, Set<String> paidIds) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 370));
        row.setPreferredSize(new Dimension(Integer.MAX_VALUE, 370));

        row.add(buildActivityChartCard(allAppts, paidIds), BorderLayout.CENTER);
        row.add(buildBreakdownCard(allAppts),              BorderLayout.EAST);
        return row;
    }

    private JPanel buildActivityChartCard(List<String[]> allAppts, Set<String> paidIds) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 16, 12, 16));

        JLabel title = new JLabel("Service activity \u2014 last 6 months");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));
        card.add(title, BorderLayout.NORTH);

        // ── Bar chart data → appointmentController ────────────────
        Map<String, Integer> monthlyCounts = appointmentController.buildMonthlyCounts(allAppts, 6);
        BarChartPanel chartPanel = new BarChartPanel(monthlyCounts);
        chartPanel.setOpaque(false);
        card.add(chartPanel, BorderLayout.CENTER);

        // ── Total spent → paymentService ─────────────────────────
        double totalSpent  = paymentService.calcTotalSpent(allAppts, paidIds, appointmentController);
        double avgPerVisit = allAppts.isEmpty() ? 0 : totalSpent / allAppts.size();

        JPanel statsRow = new JPanel(new GridLayout(1, 3, 0, 0));
        statsRow.setOpaque(false);
        statsRow.setBorder(new EmptyBorder(22, 0, 0, 0));
        statsRow.setPreferredSize(new Dimension(0, 66));

        statsRow.add(buildSmallStat("Total Services",    String.valueOf(allAppts.size())));
        statsRow.add(buildSmallStat("Total Spent",        String.format("RM %,.0f", totalSpent)));
        statsRow.add(buildSmallStat("Average Per Visit",  String.format("RM %,.0f", avgPerVisit)));
        card.add(statsRow, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildSmallStat(String label, String value) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(UIConstants.TEXT_PRIMARY);
        val.setAlignmentX(Component.CENTER_ALIGNMENT);

        p.add(Box.createVerticalGlue());
        p.add(lbl);
        p.add(Box.createVerticalStrut(2));
        p.add(val);
        p.add(Box.createVerticalGlue());
        return p;
    }

    // ─────────────────────────────────────────────────────────────
    // Donut chart card
    // ─────────────────────────────────────────────────────────────
    private JPanel buildBreakdownCard(List<String[]> allAppts) {
        JPanel card = createCard();
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setPreferredSize(new Dimension(300, 370));
        card.setMinimumSize(new Dimension(300, 260));
        card.setMaximumSize(new Dimension(300, Integer.MAX_VALUE));

        JLabel title = new JLabel("Service breakdown");
        title.setFont(new Font("SansSerif", Font.BOLD, 13));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setBorder(new EmptyBorder(0, 0, 6, 0));
        card.add(title, BorderLayout.NORTH);

        // ── Donut data → appointmentController ───────────────────
        Map<String, Integer> breakdown = appointmentController.buildServiceBreakdown(allAppts);
        int totalSvc = allAppts.size();

        DonutChartPanel donut = new DonutChartPanel(breakdown, totalSvc);
        donut.setOpaque(false);
        donut.setPreferredSize(new Dimension(200, 200));

        JPanel donutWrapper = new JPanel(new GridBagLayout());
        donutWrapper.setOpaque(false);
        donutWrapper.add(donut);
        card.add(donutWrapper, BorderLayout.CENTER);

        JPanel legendPanel = new JPanel();
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setOpaque(false);
        legendPanel.setBorder(new EmptyBorder(6, 0, 0, 0));

        Color[] colours = DonutChartPanel.SEGMENT_COLORS;
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(breakdown.entrySet());
        for (int i = 0; i < entries.size() && i < colours.length; i++) {
            int pct = totalSvc > 0 ? (entries.get(i).getValue() * 100 / totalSvc) : 0;
            legendPanel.add(buildLegendRow(colours[i], entries.get(i).getKey(), pct + "%"));
            if (i < entries.size() - 1) legendPanel.add(Box.createVerticalStrut(4));
        }
        card.add(legendPanel, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildLegendRow(Color color, String label, String pct) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel dot = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillOval(0, 5, 11, 11);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(15, 20));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel pctLbl = new JLabel(pct);
        pctLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        pctLbl.setForeground(UIConstants.TEXT_MUTED);

        row.add(dot,    BorderLayout.WEST);
        row.add(lbl,    BorderLayout.CENTER);
        row.add(pctLbl, BorderLayout.EAST);
        return row;
    }

    // ═══════════════════════════════════════════════════════════════
    // ROW 3 — UPCOMING APPOINTMENTS + PENDING PAYMENTS
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildBottomRow(List<String[]> upcoming, List<String[]> unpaid) {
        JPanel row = new JPanel(new GridLayout(1, 2, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        row.add(buildUpcomingCard(upcoming));
        row.add(buildPendingPaymentsCard(unpaid));
        return row;
    }

    private JPanel buildUpcomingCard(List<String[]> upcoming) {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel titleLbl = new JLabel("Upcoming Appointments");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLbl.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLbl, BorderLayout.WEST);

        if (upcoming.size() > 2) {
            JButton viewAll = createTextLinkButton("View all");
            viewAll.addActionListener(e ->
                    showViewAllDialog("All Upcoming Appointments", upcoming, false));
            titleRow.add(viewAll, BorderLayout.EAST);
        }
        card.add(titleRow);
        card.add(Box.createVerticalStrut(8));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(8));

        if (upcoming.isEmpty()) {
            card.add(makeEmptyLabel("No upcoming appointments."));
        } else {
            int show = Math.min(2, upcoming.size());
            for (int i = 0; i < show; i++) {
                card.add(buildUpcomingRow(upcoming.get(i)));
                if (i < show - 1) card.add(Box.createVerticalStrut(6));
            }
        }
        return card;
    }

    private JPanel buildPendingPaymentsCard(List<String[]> unpaid) {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel titleLbl = new JLabel("Pending Payments");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        titleLbl.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLbl, BorderLayout.WEST);

        if (unpaid.size() > 2) {
            JButton viewAll = createTextLinkButton("View all");
            // ── FIX: pass null as parentDialog — this IS the top-level card button ──
            viewAll.addActionListener(e ->
                    showViewAllPendingDialog("All Pending Payments"));
            titleRow.add(viewAll, BorderLayout.EAST);
        }
        card.add(titleRow);
        card.add(Box.createVerticalStrut(8));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(8));

        if (unpaid.isEmpty()) {
            card.add(makeEmptyLabel("No pending payments."));
        } else {
            int show = Math.min(2, unpaid.size());
            for (int i = 0; i < show; i++) {
                card.add(buildPaymentRow(unpaid.get(i), null));
                if (i < show - 1) card.add(Box.createVerticalStrut(6));
            }
        }
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildUpcomingRow() — pure UI row for one upcoming appointment
    // ─────────────────────────────────────────────────────────────
    private JPanel buildUpcomingRow(String[] row) {
        String apptId      = row[0];
        String vehicleId   = row[1];
        String techId      = row[2];
        String serviceType = row[3];
        String status      = row[4];
        String dateTime    = row[5];

        // ── Lookups → vehicleService / appointmentController ─────
        String techName   = appointmentController.resolveUserName(
                app.getAccountService().getAllUsers(), techId);
        String vehicleLbl = vehicleService.getVehicleLabel(vehicleId);

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 82));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel icon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(80, 110, 230, 22));
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(BRAND_BLUE);
                g2.setStroke(new BasicStroke(1.5f));
                int cx = 16, cy = 16, r = 8;
                g2.drawOval(cx - r, cy - r, r * 2, r * 2);
                g2.drawLine(cx, cy, cx, cy - 5);
                g2.drawLine(cx, cy, cx + 4, cy + 2);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(32, 32));

        JPanel iconWrapper = new JPanel(new GridBagLayout());
        iconWrapper.setOpaque(false);
        iconWrapper.add(icon);
        panel.add(iconWrapper, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel l1 = new JLabel(apptId + " \u00B7 " + serviceType);
        l1.setFont(new Font("SansSerif", Font.BOLD, 12));

        JLabel l2 = new JLabel(dateTime + " \u00B7 " + vehicleLbl);
        l2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l2.setForeground(UIConstants.TEXT_MUTED);

        JLabel l3 = new JLabel("Tech: " + techName);
        l3.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l3.setForeground(UIConstants.TEXT_MUTED);

        info.add(Box.createVerticalGlue());
        info.add(l1);
        info.add(Box.createVerticalStrut(2));
        info.add(l2);
        info.add(Box.createVerticalStrut(1));
        info.add(l3);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        boolean inProgress = status.equalsIgnoreCase("In Progress");
        boolean awaiting   = status.equalsIgnoreCase("Waiting for Confirmation");
        final Color badgeBg = awaiting    ? new Color(150, 100, 200, 28)
                            : inProgress  ? new Color(40, 130, 220, 22)
                                          : new Color(230, 160, 40, 22);
        final Color badgeFg = awaiting    ? new Color(120, 70, 180)
                            : inProgress  ? new Color(40, 130, 220)
                                          : new Color(200, 130, 20);
        JLabel badge = new JLabel(awaiting ? "Awaiting" : status) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(badgeFg);
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(72, 22));
        badge.setBorder(new EmptyBorder(0, 4, 0, 4));

        JPanel east = new JPanel();
        east.setLayout(new BoxLayout(east, BoxLayout.Y_AXIS));
        east.setOpaque(false);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        east.add(Box.createVerticalGlue());
        east.add(badge);

        if (awaiting) {
            JButton confirmBtn = new JButton("Confirm") {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 100, 200));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 10));
            confirmBtn.setForeground(Color.WHITE);
            confirmBtn.setContentAreaFilled(false);
            confirmBtn.setBorderPainted(false);
            confirmBtn.setFocusPainted(false);
            confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            confirmBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            confirmBtn.setMaximumSize(new Dimension(72, 22));
            confirmBtn.setPreferredSize(new Dimension(72, 22));
            confirmBtn.addActionListener(e -> {
                if (appointmentService.updateStatus(apptId, "Pending")) {
                    rebuildDashboard();
                }
            });
            east.add(Box.createVerticalStrut(4));
            east.add(confirmBtn);
        }
        east.add(Box.createVerticalGlue());
        panel.add(east, BorderLayout.EAST);

        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildPaymentRow() — pure UI row for one pending payment
    //
    // parentDialog: when non-null, this row lives inside the "All
    // Pending Payments" dialog.  After a successful payment the
    // dialog's list panel is refreshed in-place so the paid item
    // disappears immediately without closing and reopening the dialog.
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPaymentRow(String[] row, JDialog parentDialog) {
        String apptId      = row[0];
        String vehicleId   = row[1];
        String techId      = row[2];
        String serviceType = row[3];
        String duration    = row[6];

        // ── Amount → appointmentController (pricing rule is in appointments) ──
        String techName  = appointmentController.resolveUserName(
                app.getAccountService().getAllUsers(), techId);
        String amountStr = appointmentController.calculateAmount(serviceType, duration);

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 8, 8, 8)));

        JLabel icon = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(80, 190, 110, 22));
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(COLOR_GREEN);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(6, 10, 20, 13, 3, 3);
                g2.fillRect(6, 14, 20, 3);
                g2.dispose();
            }
        };
        icon.setPreferredSize(new Dimension(32, 32));

        JPanel iconWrapper = new JPanel(new GridBagLayout());
        iconWrapper.setOpaque(false);
        iconWrapper.add(icon);
        panel.add(iconWrapper, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel l1 = new JLabel(apptId + " \u00B7 " + serviceType);
        l1.setFont(new Font("SansSerif", Font.BOLD, 12));
        l1.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel l2 = new JLabel("Tech: " + techName);
        l2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l2.setForeground(UIConstants.TEXT_MUTED);

        JLabel amountDueLbl = new JLabel("Amount Due:  RM " + amountStr);
        amountDueLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        amountDueLbl.setForeground(new Color(180, 110, 0));

        info.add(Box.createVerticalGlue());
        info.add(l1);
        info.add(Box.createVerticalStrut(2));
        info.add(l2);
        info.add(Box.createVerticalStrut(4));
        info.add(amountDueLbl);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        JButton payBtn = new JButton("Pay") {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(60, 170, 90) : COLOR_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        payBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
        payBtn.setForeground(Color.WHITE);
        payBtn.setContentAreaFilled(false);
        payBtn.setBorderPainted(false);
        payBtn.setFocusPainted(false);
        payBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        payBtn.setPreferredSize(new Dimension(60, 28));
        payBtn.setMaximumSize(new Dimension(60, 28));
        payBtn.addActionListener(e -> showPaymentInvoiceDialog(
                apptId, vehicleId, serviceType, duration, amountStr, row, parentDialog));

        JPanel east = new JPanel(new GridBagLayout());
        east.setOpaque(false);
        east.add(payBtn);
        panel.add(east, BorderLayout.EAST);

        return panel;
    }

    // ═══════════════════════════════════════════════════════════════
    // PAYMENT INVOICE DIALOG
    // ═══════════════════════════════════════════════════════════════
    private void showPaymentInvoiceDialog(String apptId, String vehicleId,
            String serviceType, String duration, String amountStr,
            String[] row, JDialog parentDialog) {

        User user = app.getLoggedInUserObj();
        if (user == null) return;

        String customerName = user.getName();
        String techName     = appointmentController.resolveUserName(
                app.getAccountService().getAllUsers(), row[2]);
        String dateTime     = row[5];
        // ── Vehicle label → vehicleService ────────────────────────
        String vehicleLabel = vehicleService.getVehicleLabel(vehicleId);

        int hours = 1;
        try { hours = Integer.parseInt(duration.trim()); } catch (NumberFormatException ignored) {}

        double totalAmount;
        try { totalAmount = Double.parseDouble(amountStr); }
        catch (NumberFormatException e) { totalAmount = 150.00; }

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Payment Invoice \u2014 " + apptId, true);
        dialog.setSize(520, 680);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel root = new JPanel();
        root.setBackground(Color.WHITE);
        root.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.gridy   = GridBagConstraints.RELATIVE;
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets  = new Insets(0, 0, 0, 0);

        JPanel logoArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        logoArea.setBackground(Color.WHITE);
        logoArea.setBorder(new EmptyBorder(22, 30, 10, 30));
        try {
            java.net.URL logoUrl = getClass().getResource("/Image/apu-logo.png");
            if (logoUrl != null) {
                ImageIcon rawIcon = new ImageIcon(logoUrl);
                if (rawIcon.getIconWidth() > 0) {
                    Image scaledLogo = rawIcon.getImage()
                            .getScaledInstance(110, 110, Image.SCALE_SMOOTH);
                    logoArea.add(new JLabel(new ImageIcon(scaledLogo)));
                }
            }
        } catch (Exception ignored) {}
        root.add(logoArea, gbc);

        JPanel orgPanel = new JPanel();
        orgPanel.setLayout(new BoxLayout(orgPanel, BoxLayout.Y_AXIS));
        orgPanel.setBackground(Color.WHITE);
        orgPanel.setBorder(new EmptyBorder(0, 30, 10, 30));
        JLabel orgName = new JLabel("APU Automotive Service Centre");
        orgName.setFont(new Font("SansSerif", Font.BOLD, 15));
        orgName.setForeground(UIConstants.PRIMARY);
        orgPanel.add(orgName);
        JLabel orgSub = new JLabel("Official Payment Invoice");
        orgSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        orgSub.setForeground(UIConstants.TEXT_MUTED);
        orgPanel.add(orgSub);
        root.add(orgPanel, gbc);

        root.add(makePaddedSeparator(30), gbc);

        root.add(makeInvoiceRowPanel("Appointment ID", apptId,             false, 30), gbc);
        root.add(makeInvoiceRowPanel("Customer",        customerName,       true,  30), gbc);
        root.add(makeInvoiceRowPanel("Vehicle",         vehicleLabel,       false, 30), gbc);
        root.add(makeInvoiceRowPanel("Technician",      techName,           true,  30), gbc);
        root.add(makeInvoiceRowPanel("Service Type",    serviceType,        false, 30), gbc);
        root.add(makeInvoiceRowPanel("Date & Time",     dateTime,           true,  30), gbc);
        root.add(makeInvoiceRowPanel("Service Hours",   hours + " hour(s)", false, 30), gbc);

        JPanel sep2 = makePaddedSeparator(30);
        sep2.setBorder(new EmptyBorder(8, 30, 0, 30));
        root.add(sep2, gbc);

        JPanel payTypePanel = new JPanel(null);
        payTypePanel.setBackground(Color.WHITE);
        payTypePanel.setPreferredSize(new Dimension(460, 44));
        payTypePanel.setBorder(new EmptyBorder(0, 30, 0, 30));
        JLabel payTypeLbl = new JLabel("Payment Type");
        payTypeLbl.setFont(new Font("SansSerif", Font.BOLD, 12));
        payTypeLbl.setForeground(UIConstants.TEXT_MUTED);
        payTypeLbl.setBounds(0, 12, 120, 20);
        payTypePanel.add(payTypeLbl);
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Cash", "Card", "Online"});
        methodCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        methodCombo.setBackground(Color.WHITE);
        methodCombo.setBounds(125, 8, 280, 28);
        payTypePanel.add(methodCombo);
        JPanel payTypeWrapper = new JPanel(new BorderLayout());
        payTypeWrapper.setBackground(Color.WHITE);
        payTypeWrapper.setBorder(new EmptyBorder(8, 30, 8, 30));
        payTypeWrapper.add(payTypePanel, BorderLayout.CENTER);
        root.add(payTypeWrapper, gbc);

        root.add(makePaddedSeparator(30), gbc);

        JPanel totalPanel = new JPanel(new BorderLayout());
        totalPanel.setBackground(Color.WHITE);
        totalPanel.setBorder(new EmptyBorder(12, 30, 12, 30));
        JLabel tLabel = new JLabel("Total Amount");
        tLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        tLabel.setForeground(UIConstants.TEXT_PRIMARY);
        final double ta = totalAmount;
        JLabel tValue = new JLabel(String.format("RM %.2f", ta));
        tValue.setFont(new Font("SansSerif", Font.BOLD, 17));
        tValue.setForeground(new Color(40, 160, 80));
        tValue.setHorizontalAlignment(SwingConstants.RIGHT);
        totalPanel.add(tLabel, BorderLayout.WEST);
        totalPanel.add(tValue, BorderLayout.EAST);
        root.add(totalPanel, gbc);

        final String fas = String.format("%.2f", ta);
        JButton confirmBtn = new JButton("Confirm & Pay") {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(60, 170, 90) : COLOR_GREEN);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setContentAreaFilled(false);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBtn.setPreferredSize(new Dimension(460, 42));

        confirmBtn.addActionListener(e -> {
            String  method = (String) methodCombo.getSelectedItem();
            // ── Save payment → appointmentController (which delegates to paymentService) ──
            boolean saved  = appointmentController.savePayment(apptId, vehicleId, fas, method);
            if (saved) {
                // 1. Close the invoice dialog first
                dialog.dispose();

                // 2. Show the success confirmation
                JOptionPane.showMessageDialog(this,
                        "Payment of RM " + fas + " via " + method + " recorded successfully.",
                        "Payment Successful", JOptionPane.INFORMATION_MESSAGE);

                // 3. If "All Pending Payments" dialog is open, refresh its list in-place.
                //    If no items remain, close the dialog instead.
                if (parentDialog != null && parentDialog.isVisible()) {
                    List<String[]> remaining = appointmentController.getUnpaidAppointments();
                    if (remaining.isEmpty()) {
                        // Nothing left to show — close the dialog entirely
                        parentDialog.dispose();
                    } else {
                        // Refresh the scrollable list inside the dialog
                        refreshPendingPaymentsDialogContent(parentDialog, remaining);
                    }
                }

                // 4. Rebuild the whole dashboard so stat cards + bottom row are up to date
                refreshUser();

            } else {
                JOptionPane.showMessageDialog(this,
                        "Failed to save payment. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel btnWrapper = new JPanel(new BorderLayout());
        btnWrapper.setBackground(Color.WHITE);
        btnWrapper.setBorder(new EmptyBorder(0, 30, 22, 30));
        btnWrapper.add(confirmBtn, BorderLayout.CENTER);
        root.add(btnWrapper, gbc);

        GridBagConstraints fillerGbc = new GridBagConstraints();
        fillerGbc.gridx   = 0;
        fillerGbc.gridy   = GridBagConstraints.RELATIVE;
        fillerGbc.fill    = GridBagConstraints.BOTH;
        fillerGbc.weightx = 1.0;
        fillerGbc.weighty = 1.0;
        root.add(new JPanel() {{ setBackground(Color.WHITE); }}, fillerGbc);

        JScrollPane scroll = new JScrollPane(root);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // refreshPendingPaymentsDialogContent()
    //
    // Called after a successful payment while "All Pending Payments"
    // dialog is still open.  Replaces the scroll-pane content with a
    // freshly built list that contains only the still-unpaid rows,
    // so the just-paid item disappears without closing the dialog.
    //
    // How it works:
    //   1. We stored a name-tag ("listScrollPane") on the dialog's
    //      content pane when we built it in showViewAllPendingDialog().
    //   2. Here we find that scroll-pane by tag, rebuild the inner
    //      list panel from the fresh `remaining` data, swap the
    //      viewport content, and revalidate/repaint.
    //
    // @param parentDialog  the "All Pending Payments" JDialog
    // @param remaining     up-to-date list of still-unpaid appointments
    // ═══════════════════════════════════════════════════════════════
    private void refreshPendingPaymentsDialogContent(JDialog parentDialog,
                                                      List<String[]> remaining) {
        // Walk the dialog's content pane to find the tagged scroll-pane
        Container contentPane = parentDialog.getContentPane();
        JScrollPane scrollPane = findNamedScrollPane(contentPane, "listScrollPane");

        if (scrollPane == null) return; // safety — should never happen

        // Rebuild the list panel with the fresh (smaller) data set
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 16, 16, 16));

        for (int i = 0; i < remaining.size(); i++) {
            // Pass parentDialog so each new Pay button can still refresh
            listPanel.add(buildPaymentRow(remaining.get(i), parentDialog));
            if (i < remaining.size() - 1) listPanel.add(Box.createVerticalStrut(6));
        }

        // Swap the viewport content and force a repaint
        scrollPane.setViewportView(listPanel);
        scrollPane.revalidate();
        scrollPane.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // findNamedScrollPane() — recursive helper that walks the
    // component tree looking for a JScrollPane whose name property
    // equals the given tag.  Returns null if not found.
    // ─────────────────────────────────────────────────────────────
    private JScrollPane findNamedScrollPane(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JScrollPane
                    && name.equals(((JScrollPane) comp).getName())) {
                return (JScrollPane) comp;
            }
            if (comp instanceof Container) {
                JScrollPane found = findNamedScrollPane((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private JPanel makePaddedSeparator(int hInset) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Color.WHITE);
        wrapper.setBorder(new EmptyBorder(0, hInset, 0, hInset));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 222, 228));
        wrapper.add(sep, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel makeInvoiceRowPanel(String label, String value, boolean shaded, int hInset) {
        JPanel outer = new JPanel(new BorderLayout());
        outer.setBackground(shaded ? new Color(245, 246, 248) : Color.WHITE);
        JPanel inner = new JPanel(new BorderLayout(8, 0));
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(7, hInset, 7, hInset));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(120, 18));
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 12));
        val.setForeground(UIConstants.TEXT_PRIMARY);
        inner.add(lbl, BorderLayout.WEST);
        inner.add(val, BorderLayout.CENTER);
        outer.add(inner, BorderLayout.CENTER);
        return outer;
    }

    // ═══════════════════════════════════════════════════════════════
    // showViewAllPendingDialog()
    // ═══════════════════════════════════════════════════════════════
    private void showViewAllPendingDialog(String dialogTitle) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), dialogTitle, true);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        // ── Header ────────────────────────────────────────────────
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 22, 12, 22));
        JLabel tl = new JLabel(dialogTitle);
        tl.setFont(new Font("SansSerif", Font.BOLD, 15));
        tl.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(tl, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        // ── Scrollable list (tagged so refreshPendingPaymentsDialogContent can find it) ──
        JScrollPane scrollPane = buildPendingListScrollPane(
                appointmentController.getUnpaidAppointments(), dialog);
        scrollPane.setName("listScrollPane"); // ← tag used by refresh method
        dialog.add(scrollPane, BorderLayout.CENTER);

        // ── Footer ────────────────────────────────────────────────
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_DEFAULT));
        JButton closeBtn = createActionButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(78, 32));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // buildPendingListScrollPane()
    // ─────────────────────────────────────────────────────────────
    private JScrollPane buildPendingListScrollPane(List<String[]> unpaidRows,
                                                    JDialog dialog) {
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 16, 16, 16));

        if (unpaidRows.isEmpty()) {
            listPanel.add(makeEmptyLabel("No pending payments."));
        } else {
            for (int i = 0; i < unpaidRows.size(); i++) {
                listPanel.add(buildPaymentRow(unpaidRows.get(i), dialog));
                if (i < unpaidRows.size() - 1) listPanel.add(Box.createVerticalStrut(6));
            }
        }

        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ═══════════════════════════════════════════════════════════════
    // VIEW ALL DIALOG — used only for Upcoming Appointments
    // ═══════════════════════════════════════════════════════════════
    private void showViewAllDialog(String dialogTitle,
                                   List<String[]> allRows,
                                   boolean isPayment) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), dialogTitle, true);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 22, 12, 22));
        JLabel tl = new JLabel(dialogTitle);
        tl.setFont(new Font("SansSerif", Font.BOLD, 15));
        tl.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(tl, BorderLayout.WEST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 16, 16, 16));

        for (int i = 0; i < allRows.size(); i++) {
            listPanel.add(isPayment
                    ? buildPaymentRow(allRows.get(i), dialog)
                    : buildUpcomingRow(allRows.get(i)));
            if (i < allRows.size() - 1) listPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane sp = new JScrollPane(listPanel);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(sp, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_DEFAULT));
        JButton closeBtn = createActionButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(78, 32));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));

        headerTitle = new JLabel("Dashboard");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildHeaderProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildHeaderProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        avatarLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih);
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38,
                            (iw - crop) / 2, (ih - crop) / 2,
                            (iw - crop) / 2 + crop, (ih - crop) / 2 + crop, null);
                    g2.setClip(null);
                } else {
                    g2.setColor(AVATAR_COLORS[selectedAvatarIndex % AVATAR_COLORS.length]);
                    g2.fillOval(0, 0, 38, 38);
                    String init = getInitial();
                    g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                    g2.setColor(Color.WHITE);
                    FontMetrics fm = g2.getFontMetrics();
                    g2.drawString(init,
                            (38 - fm.stringWidth(init)) / 2,
                            (38 + fm.getAscent() - fm.getDescent()) / 2);
                }
                g2.dispose();
            }
            private String getInitial() {
                String n = app.getLoggedInUser();
                return (n != null && !n.isEmpty())
                        ? String.valueOf(n.charAt(0)).toUpperCase() : "U";
            }
        };
        avatarLabel.setPreferredSize(new Dimension(38, 38));
        avatarLabel.setOpaque(false);

        profileLabel = new JLabel("\u2014");
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        JLabel da = new JLabel("v");
        da.setFont(new Font("SansSerif", Font.PLAIN, 10));
        da.setForeground(UIConstants.TEXT_MUTED);

        JPanel profileButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileButton.setBackground(UIConstants.BG_HEADER);
        profileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileButton.add(avatarLabel);
        profileButton.add(profileLabel);
        profileButton.add(da);

        JPopupMenu dropdownMenu = new JPopupMenu();
        dropdownMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)));
        dropdownMenu.setBackground(Color.WHITE);

        JMenuItem viewProfileItem = createMenuItem("View Profile");
        viewProfileItem.addActionListener(e -> {
            activeNav = "View Profile";
            headerTitle.setText("View Profile");
            contentLayout.show(contentPanel, "View Profile");
            dropdownMenu.setVisible(false);
            if (navButtons != null) {
                for (JButton btn : navButtons) updateNavButtonStyle(btn, false);
            }
            if (viewProfilePage != null) viewProfilePage.refreshUser();
        });

        JMenuItem logoutItem = createMenuItem("Logout");
        logoutItem.setForeground(UIConstants.TEXT_DANGER);
        logoutItem.addActionListener(e -> {
            resetDashboardState();
            app.setLoggedInUser("");
            app.setLoggedInUserObj(null);
            app.showPage(PageName.ONBOARDING);
        });

        dropdownMenu.add(viewProfileItem);
        dropdownMenu.add(logoutItem);

        profileButton.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                dropdownMenu.show(profileButton,
                        profileButton.getWidth() - dropdownMenu.getPreferredSize().width,
                        profileButton.getHeight());
            }
        });
        dropdownMenu.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (!dropdownMenu.getBounds().contains(e.getPoint())) {
                    dropdownMenu.setVisible(false);
                }
            }
        });

        profileArea.add(profileButton);
        return profileArea;
    }

    // ═══════════════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));

        sidebar.add(buildLogoArea());

        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.SIDEBAR_DIVIDER);
        divider.setBackground(UIConstants.SIDEBAR_BG);
        divider.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(6));

        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setFont(UIConstants.FONT_LABEL);
        menuLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        menuLabel.setBorder(new EmptyBorder(6, 24, 6, 20));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 30));
        sidebar.add(menuLabel);

        navButtons = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String pageName = NAV_ITEMS[i];
            final String icon     = NAV_ICONS[i];

            navButtons[i] = buildNavButton(icon + "   " + pageName, pageName.equals(activeNav));
            navButtons[i].addActionListener(e -> {
                activeNav = pageName;
                for (int j = 0; j < navButtons.length; j++) {
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals(activeNav));
                }
                headerTitle.setText(pageName);
                contentLayout.show(contentPanel, pageName);
                if (pageName.equals("Service History") && serviceHistoryPage != null)
                    serviceHistoryPage.refresh();
                if (pageName.equals("Payment History") && paymentHistoryPage != null)
                    paymentHistoryPage.refresh();
                if (pageName.equals("Staff Review")    && staffReviewPage    != null)
                    staffReviewPage.refresh();
                if (pageName.equals("My Feedback")     && myFeedbackPage     != null)
                    myFeedbackPage.refresh();
            });
            sidebar.add(navButtons[i]);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }

    private JPanel buildLogoArea() {
        JPanel area = new JPanel();
        area.setLayout(new BoxLayout(area, BoxLayout.Y_AXIS));
        area.setBackground(UIConstants.SIDEBAR_BG);
        area.setBorder(new EmptyBorder(14, 16, 10, 16));
        area.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));

        boolean logoLoaded = false;
        try {
            java.net.URL logoUrl = getClass().getResource("/Image/apu-logo.png");
            if (logoUrl != null) {
                ImageIcon rawIcon = new ImageIcon(logoUrl);
                if (rawIcon.getIconWidth() > 0) {
                    Image scaledLogo = rawIcon.getImage()
                            .getScaledInstance(55, 55, Image.SCALE_SMOOTH);
                    JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
                    logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                    logoLabel.setBorder(new EmptyBorder(0, 0, 4, 0));
                    area.add(logoLabel);
                    logoLoaded = true;
                }
            }
        } catch (Exception ignored) {}

        if (!logoLoaded) {
            JLabel fallback = new JLabel("APU ASC Customer");
            fallback.setFont(new Font("SansSerif", Font.BOLD, 22));
            fallback.setForeground(Color.WHITE);
            fallback.setAlignmentX(Component.LEFT_ALIGNMENT);
            area.add(fallback);
        }

        JLabel brandName = new JLabel("APU ASC Customer");
        brandName.setFont(UIConstants.FONT_SIDEBAR);
        brandName.setForeground(Color.WHITE);
        brandName.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandName.setBorder(new EmptyBorder(4, 0, 0, 0));
        area.add(brandName);

        return area;
    }

    private JButton buildNavButton(String text, boolean active) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
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

        btn.putClientProperty("active", active);
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
        btn.setRolloverEnabled(true);
        return btn;
    }

    private void updateNavButtonStyle(JButton btn, boolean isActive) {
        btn.putClientProperty("active", isActive);
        btn.setForeground(isActive ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }

    // ═══════════════════════════════════════════════════════════════
    // INNER CLASS — BarChartPanel (pure rendering, no business logic)
    // ═══════════════════════════════════════════════════════════════
    private static class BarChartPanel extends JPanel {
        private final Map<String, Integer> data;
        BarChartPanel(Map<String, Integer> data) {
            this.data = data;
            setOpaque(false);
            setMinimumSize(new Dimension(0, 80));
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (data == null || data.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), labelH = 18, topPad = 8;
            int chartH = h - labelH - topPad, n = data.size();
            if (n == 0 || chartH <= 0) { g2.dispose(); return; }
            int maxVal = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
            if (maxVal == 0) maxVal = 1;
            int colW = (w - 16) / n;
            int barW = Math.max(6, colW * 2 / 5);
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(data.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                int count = entries.get(i).getValue();
                int colX  = 8 + i * colW;
                int barX  = colX + (colW - barW) / 2;
                if (count > 0) {
                    int barH = (int) ((double) count / maxVal * chartH);
                    int barY = topPad + chartH - barH;
                    g2.setColor(new Color(80, 110, 230, 180));
                    g2.fillRoundRect(barX, barY, barW, barH, 4, 4);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                    g2.setColor(new Color(80, 110, 230));
                    FontMetrics fm = g2.getFontMetrics();
                    String cnt = String.valueOf(count);
                    int lx = barX + barW / 2 - fm.stringWidth(cnt) / 2;
                    if (barY - 3 > topPad) g2.drawString(cnt, lx, barY - 2);
                } else {
                    g2.setColor(new Color(80, 110, 230, 35));
                    g2.fillRoundRect(barX, topPad + chartH - 4, barW, 4, 2, 2);
                }
                g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g2.setColor(new Color(120, 130, 145));
                FontMetrics fm = g2.getFontMetrics();
                String month = entries.get(i).getKey();
                int lx = colX + (colW - fm.stringWidth(month)) / 2;
                g2.drawString(month, lx, h - 3);
            }
            g2.dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // INNER CLASS — DonutChartPanel (pure rendering, no business logic)
    // ═══════════════════════════════════════════════════════════════
    static class DonutChartPanel extends JPanel {
        static final Color[] SEGMENT_COLORS = {
                new Color(80, 110, 230), new Color(80, 190, 110),
                new Color(230, 160, 40), new Color(200, 200, 210),
        };
        private final Map<String, Integer> data;
        private final int                  total;
        DonutChartPanel(Map<String, Integer> data, int total) {
            this.data = data; this.total = total; setOpaque(false);
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int size = Math.min(w, h) - 6;
            int ox = (w - size) / 2, oy = (h - size) / 2;
            int hole  = size * 52 / 100;
            int holeX = ox + (size - hole) / 2;
            int holeY = oy + (size - hole) / 2;
            if (total == 0 || data.isEmpty()) {
                g2.setColor(new Color(220, 220, 230));
                g2.fillOval(ox, oy, size, size);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillOval(holeX, holeY, hole, hole);
                g2.dispose(); return;
            }
            double startAngle = -90.0;
            List<Map.Entry<String, Integer>> entries = new ArrayList<>(data.entrySet());
            for (int i = 0; i < entries.size(); i++) {
                double sweep = (double) entries.get(i).getValue() / total * 360.0;
                g2.setColor(SEGMENT_COLORS[i % SEGMENT_COLORS.length]);
                g2.fillArc(ox, oy, size, size, (int) startAngle, (int) sweep);
                startAngle += sweep;
            }
            g2.setColor(UIConstants.BG_CARD);
            g2.fillOval(holeX, holeY, hole, hole);
            int cx = w / 2, cy = h / 2;
            int bigFontSz = Math.max(18, hole / 5), smallFontSz = Math.max(11, hole / 8);
            g2.setFont(new Font("SansSerif", Font.BOLD, bigFontSz));
            FontMetrics fm1 = g2.getFontMetrics();
            String totalStr = String.valueOf(total);
            g2.setColor(UIConstants.TEXT_PRIMARY);
            g2.drawString(totalStr, cx - fm1.stringWidth(totalStr) / 2,
                    cy + fm1.getAscent() / 2 - 3);
            g2.setFont(new Font("SansSerif", Font.PLAIN, smallFontSz));
            FontMetrics fm2 = g2.getFontMetrics();
            String sub = "services";
            g2.setColor(UIConstants.TEXT_MUTED);
            g2.drawString(sub, cx - fm2.stringWidth(sub) / 2,
                    cy + fm1.getAscent() / 2 + fm2.getHeight() - 1);
            g2.dispose();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED UI HELPERS (pure layout/styling — no business logic)
    // ═══════════════════════════════════════════════════════════════

    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JSeparator makeSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JLabel makeEmptyLabel(String message) {
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private JButton createTextLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(UIConstants.PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 0, 0, 0));
        return btn;
    }

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hov = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(70, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
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
}