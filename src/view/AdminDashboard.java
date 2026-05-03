package view;

import model.AccountService;
import model.BackgroundImageStorage;
import model.DashboardData;
import model.ProfilePicStorage;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.FileDialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * Admin dashboard.
 * OOP: Encapsulation (private builders), Inheritance (extends JPanel),
 *      Abstraction (DashboardCards hides painting, AccountService hides I/O),
 *      Polymorphism (DashboardCards methods reused by CounterStaffDashboard).
 */
public class AdminDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel     contentPanel;
    private String     activeNav = "Dashboard";

    private JLabel welcomeLabel;
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    private JTextField profileNameField;
    private JTextField profileEmailField;
    private JLabel     profileRoleLabel;

    private BufferedImage        profileImage    = null;
    private BufferedImage        bannerImage     = null;
    private JPanel               profileBanner;
    private JLabel               profilePicLabel;
    private final ProfilePicStorage      profilePicStorage = new ProfilePicStorage();
    private final BackgroundImageStorage backgroundStorage = new BackgroundImageStorage();

    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);
    private static final String SEP = File.separator;
    private static final String BASE = "src" + SEP + "TxtFile" + SEP;

    private static final String[] NAV_ITEMS = {
            "Dashboard", "Manage Staff", "Service Price", "View Feedback", "Report"
    };
    private static final String[] NAV_ICONS = {
            "\u2302", "\u2663", "\u2696", "\u2605", "\u2637"
    };

    public AdminDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
        JPanel right = new JPanel(new BorderLayout());
        right.setBackground(UIConstants.BG_CONTENT);
        right.add(buildHeader(), BorderLayout.NORTH);
        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);
        AccountService svc = app.getAccountService();
        contentPanel.add(buildDashboardContent(),   "Dashboard");
        contentPanel.add(buildProfileContent(),     "Profile");
        contentPanel.add(new ManageStaffPanel(svc), "Manage Staff");
        contentPanel.add(new ServicePricePanel(),   "Service Price");
        contentPanel.add(new ViewFeedbackPanel(),   "View Feedback");
        contentPanel.add(new ReportPanel(svc),      "Report");
        right.add(contentPanel, BorderLayout.CENTER);
        add(right, BorderLayout.CENTER);
    }

    @Override public void addNotify() { super.addNotify(); refreshUser(); }

    public void refreshUser() {
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Admin";
        if (welcomeLabel != null) welcomeLabel.setText("Welcome back, " + name);
        if (profileLabel  != null) profileLabel.setText(name);
        User u = app.getLoggedInUserObj();
        if (u != null) {
            profileImage = profilePicStorage.loadImage(u.getEmail());
            bannerImage  = backgroundStorage.loadImage(u.getEmail());
            if (profileBanner   != null) profileBanner.repaint();
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }
    }

    // ── Dashboard page ───────────────────────────────────────────────────────

    private JPanel buildDashboardContent() {
        DashboardData data = new DashboardData();
        AccountService svc = app.getAccountService();

        int totalStaff = svc.getUsersByRole("staff").size()
                       + svc.getUsersByRole("technician").size()
                       + svc.getUsersByRole("admin").size();
        int totalAppts   = data.totalAppointments();
        double revenue   = data.totalRevenue();
        double avgRating = data.averageRating();

        // Payment split
        int paidCount = 0, unpaidCount = 0;
        double totalPaid = 0, totalUnpaid = 0;
        for (String[] p : csv(BASE + "payments.txt")) {
            if (p.length < 9) continue;
            double amt = dbl(p[5]);
            if ("Paid".equalsIgnoreCase(p[8].trim())) { paidCount++;   totalPaid   += amt; }
            else                                       { unpaidCount++; totalUnpaid += amt; }
        }

        // Name map
        Map<String, String> names = new HashMap<>();
        for (String[] p : csv(BASE + "accounts.txt"))
            if (p.length >= 2) names.put(p[0].trim(), p[1].trim());

        // Upcoming (date >= today, not Completed)
        String today = LocalDate.now().toString();
        List<String[]> upcoming = new ArrayList<>();
        for (String[] r : csv(BASE + "appointments.txt")) {
            if (r.length < 7) continue;
            String dt = r[6].trim(), date = dt.contains(" ") ? dt.split(" ")[0] : dt;
            if (date.compareTo(today) >= 0 && !"Completed".equalsIgnoreCase(r[5].trim()))
                upcoming.add(r);
        }
        upcoming.sort((a, b) -> a[6].trim().compareTo(b[6].trim()));

        Map<String, Integer> svcMap = data.serviceTypeBreakdown();

        // Build page
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(UIConstants.BG_CONTENT);
        page.setBorder(new EmptyBorder(28, 36, 28, 36));

        welcomeLabel = lbl("Welcome back, Admin", UIConstants.FONT_HEADING_2, UIConstants.TEXT_PRIMARY);
        JLabel sub   = lbl("Live overview of your service centre.", UIConstants.FONT_BODY, UIConstants.TEXT_MUTED);
        sub.setBorder(new EmptyBorder(4, 0, 20, 0));
        page.add(welcomeLabel); page.add(sub);

        // Row 1: KPI cards
        JPanel r1 = row(4, 108);
        r1.add(DashboardCards.buildKpiCard("Total Staff",     String.valueOf(totalStaff),
                "admin · staff · technician", "\u2663", DashboardCards.BLUE));
        r1.add(DashboardCards.buildKpiCard("Appointments",    String.valueOf(totalAppts),
                data.completedCount() + " completed", "\u2714", DashboardCards.GREEN));
        r1.add(DashboardCards.buildKpiCard("Revenue (RM)",    String.format("%.0f", revenue),
                "from paid appointments", "\u2605", DashboardCards.TEAL));
        r1.add(DashboardCards.buildKpiCard("Avg Rating",
                avgRating == 0 ? "N/A" : String.format("%.1f / 5", avgRating),
                "from customer reviews", "\u2665", DashboardCards.AMBER));
        page.add(r1); page.add(Box.createVerticalStrut(16));

        // Row 2: Upcoming + status breakdown
        JPanel r2 = row(2, 260);
        r2.add(buildUpcomingCard(upcoming, names));
        Map<String, Integer> statusMap = data.statusBreakdown();
        r2.add(DashboardCards.buildStatusBreakdownCard("Appointment Status",
                "Breakdown by current status", statusMap,
                new Color[]{DashboardCards.GREEN, DashboardCards.AMBER, DashboardCards.GRAY},
                totalAppts));
        page.add(r2); page.add(Box.createVerticalStrut(16));

        // Row 3: Payment summary + service overview
        JPanel r3 = row(2, 220);
        r3.add(DashboardCards.buildPaymentSummaryCard(paidCount, unpaidCount, totalPaid, totalUnpaid));
        r3.add(buildServiceOverviewCard(svcMap, svc));
        page.add(r3); page.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(page);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(UIConstants.BG_CONTENT);
        wrap.add(scroll);
        return wrap;
    }

    private JPanel buildUpcomingCard(List<String[]> upcoming, Map<String, String> names) {
        JPanel card = DashboardCards.roundedCard();
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel hdr = new JPanel(new BorderLayout()); hdr.setOpaque(false);
        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel badge = new JLabel("  " + upcoming.size() + "  ");
        badge.setFont(UIConstants.FONT_SMALL_BOLD);
        badge.setForeground(DashboardCards.BLUE);
        badge.setOpaque(true); badge.setBackground(new Color(235, 240, 255));
        hdr.add(title, BorderLayout.WEST); hdr.add(badge, BorderLayout.EAST);
        card.add(hdr, BorderLayout.NORTH);

        JPanel list = new JPanel(); list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS)); list.setOpaque(false);
        int show = Math.min(upcoming.size(), 4);
        if (show == 0) {
            JLabel empty = new JLabel("No upcoming appointments");
            empty.setFont(UIConstants.FONT_BODY); empty.setForeground(UIConstants.TEXT_MUTED);
            list.add(empty);
        } else {
            for (int i = 0; i < show; i++) {
                String[] r = upcoming.get(i);
                // [0]apptID [1]custID [2]vehicleID [3]techID [4]svcType [5]status [6]dateTime
                String cust = names.getOrDefault(r[1].trim(), r[1].trim());
                String tech = names.getOrDefault(r[3].trim(), r[3].trim());
                String dt   = r.length > 6 ? r[6].trim() : "";
                String date = dt.contains(" ") ? dt.split(" ")[0] : dt;
                String time = dt.contains(" ") ? dt.split(" ")[1] : "";
                list.add(DashboardCards.buildAppointmentRow(r[0].trim(), cust, tech,
                        r.length > 4 ? r[4].trim() : "", date, time,
                        r.length > 5 ? r[5].trim() : ""));
                if (i < show - 1) list.add(Box.createVerticalStrut(6));
            }
        }
        JScrollPane sc = new JScrollPane(list, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null); sc.setOpaque(false); sc.getViewport().setOpaque(false);
        card.add(sc, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildServiceOverviewCard(Map<String, Integer> svcMap, AccountService svc) {
        JPanel card = DashboardCards.roundedCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JLabel t = new JLabel("Service Overview");
        t.setFont(new Font("SansSerif", Font.BOLD, 15)); t.setForeground(UIConstants.TEXT_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel s = new JLabel("Staff and service statistics");
        s.setFont(UIConstants.FONT_SMALL); s.setForeground(UIConstants.TEXT_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT); s.setBorder(new EmptyBorder(2, 0, 14, 0));
        card.add(t); card.add(s);

        JPanel grid = new JPanel(new GridLayout(2, 2, 10, 10));
        grid.setOpaque(false); grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        grid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        grid.add(DashboardCards.buildStatTile("Normal Service",
                String.valueOf(svcMap.getOrDefault("Normal Service", 0)),
                "\u25CB", DashboardCards.BLUE, new Color(235, 240, 255)));
        grid.add(DashboardCards.buildStatTile("Major Service",
                String.valueOf(svcMap.getOrDefault("Major Service", 0)),
                "\u25CF", DashboardCards.PURPLE, new Color(240, 230, 255)));
        grid.add(DashboardCards.buildStatTile("Technicians",
                String.valueOf(svc.getUsersByRole("technician").size()),
                "\u2692", DashboardCards.GREEN, new Color(220, 245, 225)));
        grid.add(DashboardCards.buildStatTile("Customers",
                String.valueOf(svc.getUsersByRole("customer").size()),
                "\u2663", DashboardCards.TEAL, new Color(225, 248, 255)));
        card.add(grid); card.add(Box.createVerticalGlue());
        return card;
    }

    // ── Header ───────────────────────────────────────────────────────────────

    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(UIConstants.BG_HEADER);
        h.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        h.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));
        headerTitle = new JLabel("Dashboard");
        headerTitle.setFont(UIConstants.FONT_BODY_BOLD);
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        h.add(headerTitle, BorderLayout.WEST);
        h.add(buildProfileArea(), BorderLayout.EAST);
        return h;
    }

    private JPanel buildProfileArea() {
        JPanel area = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        area.setBackground(UIConstants.BG_HEADER);
        avatarLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    int w = profileImage.getWidth(), hh = profileImage.getHeight(), c = Math.min(w, hh);
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38,
                            (w-c)/2, (hh-c)/2, (w-c)/2+c, (hh-c)/2+c, null);
                } else {
                    g2.setColor(BRAND_BLUE); g2.fillOval(0, 0, 38, 38);
                    g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.PLAIN, 16));
                    g2.drawString("\u263A", 10, 26);
                }
                g2.dispose();
            }
        };
        avatarLabel.setPreferredSize(new Dimension(38, 38));
        profileLabel = new JLabel("Admin");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));
        JLabel arrow = new JLabel("\u25BE");
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        arrow.setForeground(UIConstants.TEXT_MUTED);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btn.setBackground(UIConstants.BG_HEADER); btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.add(avatarLabel); btn.add(profileLabel); btn.add(arrow);
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220,220,225), 1), new EmptyBorder(6,0,6,0)));
        menu.setBackground(Color.WHITE);
        JMenuItem vp = menuItem("View Profile");
        vp.addActionListener(e -> { activeNav=""; headerTitle.setText("My Profile");
            contentLayout.show(contentPanel,"Profile"); refreshProfileFields(); });
        JMenuItem lo = menuItem("Logout"); lo.setForeground(UIConstants.TEXT_DANGER);
        lo.addActionListener(e -> { app.setLoggedInUser(""); app.showPage(PageName.ONBOARDING); });
        menu.add(vp); menu.add(lo);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                menu.show(btn, btn.getWidth() - menu.getPreferredSize().width, btn.getHeight());
            }
        });
        menu.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (!menu.getBounds().contains(e.getPoint())) menu.setVisible(false);
            }
        });
        area.add(btn);
        return area;
    }

    private JMenuItem menuItem(String text) {
        JMenuItem i = new JMenuItem(text); i.setFont(UIConstants.FONT_SMALL);
        i.setForeground(UIConstants.TEXT_DARK); i.setBackground(Color.WHITE);
        i.setBorder(new EmptyBorder(8,20,8,30)); i.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return i;
    }

    // ── Sidebar ───────────────────────────────────────────────────────────────

    private JPanel buildSidebar() {
        JPanel sb = new JPanel(); sb.setLayout(new BoxLayout(sb, BoxLayout.Y_AXIS));
        sb.setBackground(UIConstants.SIDEBAR_BG); sb.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));
        JPanel logo = new JPanel(); logo.setLayout(new BoxLayout(logo, BoxLayout.Y_AXIS));
        logo.setBackground(UIConstants.SIDEBAR_BG); logo.setBorder(new EmptyBorder(25,20,25,20));
        logo.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));
        try { ImageIcon raw = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
            logo.add(new JLabel(new ImageIcon(raw.getImage().getScaledInstance(40,40,Image.SCALE_SMOOTH)))); }
        catch (Exception ignored) {}
        JLabel brand = new JLabel("APU ASC Admin"); brand.setFont(UIConstants.FONT_SIDEBAR);
        brand.setForeground(Color.WHITE); brand.setAlignmentX(Component.LEFT_ALIGNMENT);
        brand.setBorder(new EmptyBorder(8,0,0,0)); logo.add(brand); sb.add(logo);
        JSeparator sep = new JSeparator(); sep.setForeground(UIConstants.SIDEBAR_DIVIDER);
        sep.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1)); sb.add(sep);
        sb.add(Box.createVerticalStrut(10));
        JLabel ml = new JLabel("MENU"); ml.setFont(UIConstants.FONT_LABEL);
        ml.setForeground(UIConstants.TEXT_NAV_LABEL); ml.setBorder(new EmptyBorder(10,24,10,20));
        ml.setAlignmentX(Component.LEFT_ALIGNMENT); ml.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH,35));
        sb.add(ml);
        JButton[] btns = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String name = NAV_ITEMS[i];
            btns[i] = navBtn(NAV_ICONS[i] + "   " + name, name.equals(activeNav));
            btns[i].addActionListener(e -> { activeNav = name;
                for (int j=0;j<btns.length;j++) styleNav(btns[j], NAV_ITEMS[j].equals(activeNav));
                headerTitle.setText(name); contentLayout.show(contentPanel, name); });
            sb.add(btns[i]); sb.add(Box.createVerticalStrut(2));
        }
        sb.add(Box.createVerticalGlue()); return sb;
    }

    private JButton navBtn(String text, boolean active) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(getClientProperty("active")==Boolean.TRUE) g2.setColor(UIConstants.SIDEBAR_ACTIVE);
                else if(getModel().isRollover()) g2.setColor(UIConstants.SIDEBAR_HOVER);
                else{g2.dispose();super.paintComponent(g);return;}
                g2.fillRoundRect(4,0,getWidth()-8,getHeight(),8,8);g2.dispose();super.paintComponent(g);
            }
        };
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH,42));
        b.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH,42));
        b.setFont(new Font("SansSerif",active?Font.BOLD:Font.PLAIN,14));
        b.setForeground(active?Color.WHITE:UIConstants.TEXT_SIDEBAR);
        b.setHorizontalAlignment(SwingConstants.LEFT); b.setBorder(new EmptyBorder(0,20,0,20));
        b.setContentAreaFilled(false); b.setBorderPainted(false); b.setFocusPainted(false);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.putClientProperty("active",active);
        b.setRolloverEnabled(true); return b;
    }

    private void styleNav(JButton b, boolean active) {
        b.putClientProperty("active",active);
        b.setForeground(active?Color.WHITE:UIConstants.TEXT_SIDEBAR);
        b.setFont(new Font("SansSerif",active?Font.BOLD:Font.PLAIN,14)); b.repaint();
    }

    // ── Profile page ─────────────────────────────────────────────────────────

    private void refreshProfileFields() {
        User u = app.getLoggedInUserObj(); if(u==null) return;
        if(profileNameField !=null){profileNameField .setText(u.getName());  profileNameField .setForeground(Color.BLACK);}
        if(profileEmailField!=null){profileEmailField.setText(u.getEmail()); profileEmailField.setForeground(Color.BLACK);}
        if(profileRoleLabel !=null){String r=u.getRole(); profileRoleLabel.setText(r.substring(0,1).toUpperCase()+r.substring(1));}
        profileImage=profilePicStorage.loadImage(u.getEmail());
        bannerImage =backgroundStorage.loadImage(u.getEmail());
        if(profileBanner  !=null) profileBanner.repaint();
        if(profilePicLabel!=null) profilePicLabel.repaint();
    }

    private JPanel buildProfileContent() {
        JPanel page = new JPanel(new BorderLayout()); page.setBackground(UIConstants.BG_CONTENT);
        JPanel inner = new JPanel(); inner.setLayout(new BoxLayout(inner,BoxLayout.Y_AXIS));
        inner.setBackground(UIConstants.BG_CONTENT); inner.setBorder(new EmptyBorder(0,0,40,0));
        inner.add(buildBannerHero()); inner.add(Box.createVerticalStrut(24));
        JPanel c=new JPanel(new GridBagLayout()); c.setBackground(UIConstants.BG_CONTENT);
        c.add(buildProfileFormCard()); inner.add(c);
        JScrollPane sc=new JScrollPane(inner); sc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.setBorder(null); sc.getViewport().setBackground(UIConstants.BG_CONTENT);
        sc.getVerticalScrollBar().setUnitIncrement(16); page.add(sc,BorderLayout.CENTER); return page;
    }

    private JPanel buildBannerHero() {
        JPanel hero=new JPanel(null); hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0,200)); hero.setMaximumSize(new Dimension(Integer.MAX_VALUE,200));
        boolean[] bh={false}, ah={false};
        profileBanner=new JPanel(){
            @Override protected void paintComponent(Graphics g){
                super.paintComponent(g); Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                if(bannerImage!=null) g2.drawImage(bannerImage,0,0,getWidth(),getHeight(),null);
                else{g2.setPaint(new GradientPaint(0,0,BANNER_BLUE,getWidth(),getHeight(),new Color(60,90,210)));
                    g2.fillRect(0,0,getWidth(),getHeight());}
                if(bh[0]){g2.setColor(new Color(0,0,0,110));g2.fillRect(0,0,getWidth(),getHeight());
                    int cx=getWidth()/2,cy=getHeight()/2-10; drawCam(g2,cx,cy,28,Color.WHITE);
                    g2.setColor(Color.WHITE);g2.setFont(new Font("SansSerif",Font.BOLD,13));
                    FontMetrics fm=g2.getFontMetrics();String msg="Click to change";
                    g2.drawString(msg,cx-fm.stringWidth(msg)/2,cy+44);}
                g2.dispose();}};
        profileBanner.setOpaque(false); profileBanner.setLayout(null);
        profileBanner.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBanner.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){bh[0]=true;profileBanner.repaint();}
            @Override public void mouseExited(MouseEvent e){bh[0]=false;profileBanner.repaint();}
            @Override public void mouseClicked(MouseEvent e){chooseBanner();}});
        profilePicLabel=new JLabel(){
            @Override protected void paintComponent(Graphics g){
                Graphics2D g2=(Graphics2D)g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                int size=Math.min(getWidth(),getHeight());
                if(profileImage!=null){int iw=profileImage.getWidth(),ih=profileImage.getHeight(),crop=Math.min(iw,ih);
                    g2.setClip(new Ellipse2D.Float(0,0,size,size));
                    g2.drawImage(profileImage,0,0,size,size,(iw-crop)/2,(ih-crop)/2,(iw-crop)/2+crop,(ih-crop)/2+crop,null);
                    g2.setClip(null);}
                else{g2.setColor(BRAND_BLUE);g2.fillOval(0,0,size,size);g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(size/18f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
                    int ey=size*38/100,eo=size*18/100,er=size/14;
                    g2.fillOval(size/2-eo-er,ey-er,er*2,er*2);g2.fillOval(size/2+eo-er,ey-er,er*2,er*2);
                    g2.drawArc(size*28/100,size*44/100,size*44/100,size*26/100,200,140);}
                if(ah[0]){g2.setClip(new Ellipse2D.Float(0,0,size,size));g2.setColor(new Color(0,0,0,110));
                    g2.fillOval(0,0,size,size);g2.setClip(null);drawCam(g2,size/2,size/2,20,Color.WHITE);}
                g2.setColor(Color.WHITE);g2.setStroke(new BasicStroke(4));g2.drawOval(2,2,size-4,size-4);g2.dispose();}};
        profilePicLabel.setPreferredSize(new Dimension(110,110));
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profilePicLabel.addMouseListener(new MouseAdapter(){
            @Override public void mouseEntered(MouseEvent e){ah[0]=true;profilePicLabel.repaint();}
            @Override public void mouseExited(MouseEvent e){ah[0]=false;profilePicLabel.repaint();}
            @Override public void mouseClicked(MouseEvent e){choosePic();}});
        hero.addComponentListener(new java.awt.event.ComponentAdapter(){
            @Override public void componentResized(java.awt.event.ComponentEvent e){
                profileBanner.setBounds(0,0,hero.getWidth(),170); profilePicLabel.setBounds(30,90,110,110);}});
        profileBanner.setBounds(0,0,800,170); profilePicLabel.setBounds(30,90,110,110);
        hero.add(profileBanner); hero.add(profilePicLabel);
        hero.setComponentZOrder(profilePicLabel,0); hero.setComponentZOrder(profileBanner,1);
        return hero;
    }

    private void drawCam(Graphics2D g2,int cx,int cy,int size,Color c){
        g2.setColor(c); g2.setStroke(new BasicStroke(size/10f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        int bw=size,bh=size*7/10,bx=cx-bw/2,by=cy-bh/2;
        g2.drawRoundRect(bx,by,bw,bh,size/5,size/5); int lr=size*22/100;
        g2.drawOval(cx-lr,cy-lr+size/20,lr*2,lr*2); g2.drawRoundRect(bx+size/6,by-size/6,size/4,size/6,2,2);}

    private void choosePic(){
        User u=app.getLoggedInUserObj();if(u==null)return;
        FileDialog fd=new FileDialog((Frame)SwingUtilities.getWindowAncestor(this),"Choose Profile Picture",FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");fd.setVisible(true);if(fd.getFile()==null)return;
        try{BufferedImage img=ImageIO.read(new File(fd.getDirectory(),fd.getFile()));
            if(img==null){err("Could not read image.");return;}
            if(!profilePicStorage.saveImage(u.getEmail(),img)){err("Failed to save.");return;}
            profileImage=img;if(profilePicLabel!=null)profilePicLabel.repaint();if(avatarLabel!=null)avatarLabel.repaint();
        }catch(IOException ex){err("Failed to read image.");}}

    private void chooseBanner(){
        User u=app.getLoggedInUserObj();if(u==null)return;
        FileDialog fd=new FileDialog((Frame)SwingUtilities.getWindowAncestor(this),"Choose Background",FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");fd.setVisible(true);if(fd.getFile()==null)return;
        try{BufferedImage img=ImageIO.read(new File(fd.getDirectory(),fd.getFile()));
            if(img==null){err("Could not read image.");return;}
            if(!backgroundStorage.saveImage(u.getEmail(),img)){err("Failed to save.");return;}
            bannerImage=img;if(profileBanner!=null)profileBanner.repaint();
        }catch(IOException ex){err("Failed to read image.");}}

    private JPanel buildProfileFormCard(){
        JPanel card=UIFactory.createCard(); card.setBorder(new EmptyBorder(32,50,40,50));
        JLabel h=new JLabel("Profile Information"); h.setFont(new Font("SansSerif",Font.BOLD,16));
        h.setForeground(UIConstants.TEXT_PRIMARY); h.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(h); card.add(Box.createVerticalStrut(18));
        JSeparator sep=new JSeparator(); sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(380,1)); sep.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sep); card.add(Box.createVerticalStrut(20));
        card.add(UIFactory.createFieldLabel("Name")); card.add(Box.createVerticalStrut(6));
        profileNameField=UIFactory.createTextField("Enter your name"); card.add(profileNameField);
        card.add(Box.createVerticalStrut(16));
        card.add(UIFactory.createFieldLabel("Email")); card.add(Box.createVerticalStrut(6));
        profileEmailField=UIFactory.createTextField("Enter your email"); card.add(profileEmailField);
        card.add(Box.createVerticalStrut(16));
        card.add(UIFactory.createFieldLabel("Role")); card.add(Box.createVerticalStrut(6));
        profileRoleLabel=new JLabel("\u2014"); profileRoleLabel.setFont(UIConstants.FONT_BODY);
        profileRoleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        profileRoleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileRoleLabel.setMaximumSize(new Dimension(380,30)); profileRoleLabel.setBorder(new EmptyBorder(8,14,8,14));
        card.add(profileRoleLabel); card.add(Box.createVerticalStrut(28));
        JButton sv=UIFactory.createPrimaryButton("Save Changes"); sv.addActionListener(e->saveProfile()); card.add(sv);
        return card;}

    private void saveProfile(){
        User u=app.getLoggedInUserObj();if(u==null)return;
        String n=UIFactory.getFieldValue(profileNameField,"Enter your name");
        String e=UIFactory.getFieldValue(profileEmailField,"Enter your email");
        if(n.isEmpty()||e.isEmpty()){err("Name and email cannot be empty.");return;}
        if(!n.matches("[a-zA-Z ]{2,50}")){err("Name must be 2-50 characters (letters only).");return;}
        if(!e.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")){err("Enter a valid email.");return;}
        AccountService svc=app.getAccountService();
        if(!e.equalsIgnoreCase(u.getEmail())&&svc.emailExists(e)){err("Email already taken.");return;}
        User up=new User(u.getUserId(),n,e,u.getPassword(),u.getRole(),0);
        if(svc.updateUser(u.getEmail(),up)){app.setLoggedInUser(n);app.setLoggedInUserObj(up);refreshUser();
            JOptionPane.showMessageDialog(app,"Profile updated!","Success",JOptionPane.INFORMATION_MESSAGE);
        }else err("Failed to save.");}

    // ── Tiny helpers ─────────────────────────────────────────────────────────

    private JLabel lbl(String t, Font f, Color c){
        JLabel l=new JLabel(t);l.setFont(f);l.setForeground(c);l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}

    private JPanel row(int cols, int maxH){
        JPanel p=new JPanel(new GridLayout(1,cols,16,0));
        p.setBackground(UIConstants.BG_CONTENT); p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE,maxH)); return p;}

    private void err(String m){JOptionPane.showMessageDialog(app,m,"Error",JOptionPane.ERROR_MESSAGE);}

    private List<String[]> csv(String path){
        List<String[]> rows=new ArrayList<>();File f=new File(path);if(!f.exists())return rows;
        try(BufferedReader r=new BufferedReader(new FileReader(f))){
            String line;while((line=r.readLine())!=null)if(!line.isBlank())rows.add(line.split(",",-1));
        }catch(IOException e){e.printStackTrace();}return rows;}

    private double dbl(String s){try{return Double.parseDouble(s.trim());}catch(NumberFormatException e){return 0;}}
}