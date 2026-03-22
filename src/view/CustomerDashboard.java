package view;

import model.CustomerProfileController;
import model.User;
import model.VehicleService;
import model.ProfilePicStorage;
import model.BackgroundImageStorage;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.FileDialog;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

public class CustomerDashboard extends JPanel {

    private final AppFrame app;
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Profile";

    // ── Header labels ───────────────────────────────────────────
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // ── Avatar colors & icons (header circle) ───────────────────
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230),
            new Color(230, 80, 80),
            new Color(80, 190, 110),
            new Color(230, 160, 40),
            new Color(160, 80, 230),
            new Color(40, 180, 200),
            new Color(230, 80, 160),
            new Color(100, 100, 120),
    };
    private static final String[] AVATAR_ICONS = {
            "\u263A", "\u2605", "\u2665", "\u2666",
            "\u263C", "\u2708", "\u266B", "\u2618"
    };
    private int selectedAvatarIndex = 0;

    // ── Profile page UI refs (updated by refreshUser) ───────────
    private JLabel profileNameDisplay;
    private JLabel profileEmailDisplay;
    private JLabel profileRoleDisplay;

    // ── Inline edit refs (Personal Information) ──────────────────
    private JTextField profileNameField;
    private JTextField profileEmailField;
    private JPanel     nameDisplayPanel;
    private JPanel     nameEditPanel;
    private JPanel     emailDisplayPanel;
    private JPanel     emailEditPanel;
    private JButton    editBtn;
    private JButton    saveBtn;
    private JButton    cancelBtn;

    // ── Vehicle card ─────────────────────────────────────────────
    private JPanel vehicleListPanel;
    private final VehicleService vehicleService = new VehicleService();

    // ── Image storage services ────────────────────────────────────
    private final ProfilePicStorage profilePictureService = new ProfilePicStorage();
    private final BackgroundImageStorage backgroundImageService = new BackgroundImageStorage();

    // ── Profile/banner images (set by file chooser later) ───────
    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;
    private JPanel        profileBanner;
    private JLabel        profilePicLabel;

    // ── Controller ───────────────────────────────────────────────
    private final CustomerProfileController profileController;

    // ── Brand blue ───────────────────────────────────────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);

    // ── Nav ─────────────────────────────────────────────────────
    private static final String[] NAV_ITEMS = {
            "Profile", "Appointment Booking", "Service History",
            "Payment History", "My Feedback", "Staff Review"
    };
    private static String navIcon(int cp) {
        return new StringBuilder().appendCodePoint(cp).toString();
    }
    private static final String[] NAV_ICONS = {
            navIcon(0x1F464),
            navIcon(0x1F4C5),
            navIcon(0x1F504),
            navIcon(0x1F4B5),
            navIcon(0x1F4AC),
            "\u2605"
    };

    // ═══════════════════════════════════════════════════════════
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

        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        contentPanel.add(buildProfilePage(),          "Profile");
        contentPanel.add(buildPlaceholder("Appointment Booking", "Book and manage your service appointments.",         navIcon(0x1F4C5), 40), "Appointment Booking");
        contentPanel.add(buildPlaceholder("Service History",     "View your past service records and details.",        navIcon(0x1F504), 40), "Service History");
        contentPanel.add(buildPlaceholder("Payment History",     "Review your payment transactions and invoices.",     navIcon(0x1F4B5), 40), "Payment History");
        contentPanel.add(buildPlaceholder("My Feedback",         "View feedback on your individual appointments.",     navIcon(0x1F4AC), 40), "My Feedback");
        contentPanel.add(buildPlaceholder("Staff Review",        "Submit comments for counter staff and technicians.", "\u2605",         48), "Staff Review");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    // ── Lifecycle ────────────────────────────────────────────────
    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    public void refreshUser() {
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Customer";
        if (profileLabel != null) profileLabel.setText(name);

        User user = app.getLoggedInUserObj();
        if (user != null) selectedAvatarIndex = user.getProfilePicture();

        if (avatarLabel != null) {
            avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]);
            avatarLabel.repaint();
        }

        if (user != null) {
            if (profileNameDisplay  != null) profileNameDisplay.setText(user.getName());
            if (profileEmailDisplay != null) profileEmailDisplay.setText(user.getEmail());
            if (profileRoleDisplay  != null) {
                String r = user.getRole();
                profileRoleDisplay.setText(r.substring(0, 1).toUpperCase() + r.substring(1));
            }

            // Load saved profile picture and banner via model services
            profileImage = profilePictureService.loadImage(user.getEmail());
            bannerImage  = backgroundImageService.loadImage(user.getEmail());
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (profileBanner   != null) profileBanner.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }

        refreshVehicleList();
    }


    // ═══════════════════════════════════════════════════════════
    // PROFILE PAGE
    // ═══════════════════════════════════════════════════════════
    private JPanel buildProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        JScrollPane scroll = new JScrollPane(buildProfileInner());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildProfileInner() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(UIConstants.BG_CONTENT);
        inner.setBorder(new EmptyBorder(0, 0, 40, 0));

        inner.add(buildBannerHero());
        inner.add(Box.createVerticalStrut(24));

        // BorderLayout: left col stretches, right col stays fixed width
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(UIConstants.BG_CONTENT);
        body.setBorder(new EmptyBorder(0, 30, 0, 30));

        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);
        leftCol.add(buildPersonalInfoCard());
        leftCol.add(Box.createVerticalStrut(16));
        leftCol.add(buildVehicleCard());
        body.add(leftCol, BorderLayout.CENTER);

        // Right column — fixed 300px wide, always the same regardless of left col
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);
        rightCol.setPreferredSize(new Dimension(440, 0));
        rightCol.setMinimumSize(new Dimension(440, 0));
        rightCol.setMaximumSize(new Dimension(440, Integer.MAX_VALUE));
        rightCol.add(buildUpcomingCard());
        rightCol.add(Box.createVerticalStrut(16));
        rightCol.add(buildPaymentSummaryCard());
        body.add(rightCol, BorderLayout.EAST);

        inner.add(body);
        return inner;
    }

    // ── Banner + circular profile picture hero ───────────────────
    private JPanel buildBannerHero() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHover = {false};
        boolean[] avatarHover = {false};

        profileBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    GradientPaint gp = new GradientPaint(
                            0, 0, BANNER_BLUE,
                            getWidth(), getHeight(), new Color(60, 90, 210));
                    g2.setPaint(gp);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (bannerHover[0]) {
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    int cx = getWidth() / 2, cy = getHeight() / 2 - 10;
                    drawCameraIcon(g2, cx, cy, 28, Color.WHITE);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String msg = "Click to change";
                    g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 44);
                }
                g2.dispose();
            }
        };
        profileBanner.setOpaque(false);
        profileBanner.setLayout(null);
        profileBanner.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBanner.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { bannerHover[0] = true;  profileBanner.repaint(); }
            @Override public void mouseExited (MouseEvent e) { bannerHover[0] = false; profileBanner.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { chooseBannerImage(); }
        });

        profilePicLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    Shape clip = new Ellipse2D.Float(0, 0, size, size);
                    g2.setClip(clip);
                    g2.drawImage(profileImage, 0, 0, size, size, null);
                    g2.setClip(null);
                } else {
                    drawDefaultAvatar(g2, size);
                }
                if (avatarHover[0]) {
                    Shape clip = new Ellipse2D.Float(0, 0, size, size);
                    g2.setClip(clip);
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillOval(0, 0, size, size);
                    g2.setClip(null);
                    int cx = size / 2, cy = size / 2;
                    drawCameraIcon(g2, cx, cy, 20, Color.WHITE);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(2, 2, size - 4, size - 4);
                g2.dispose();
            }
        };
        profilePicLabel.setPreferredSize(new Dimension(110, 110));
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profilePicLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { avatarHover[0] = true;  profilePicLabel.repaint(); }
            @Override public void mouseExited (MouseEvent e) { avatarHover[0] = false; profilePicLabel.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { chooseProfileImage(); }
        });

        hero.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                profileBanner.setBounds(0, 0, hero.getWidth(), 170);
                profilePicLabel.setBounds(30, 90, 110, 110);
            }
        });
        profileBanner.setBounds(0, 0, 800, 170);
        profilePicLabel.setBounds(30, 90, 110, 110);

        hero.add(profileBanner);
        hero.add(profilePicLabel);
        hero.setComponentZOrder(profilePicLabel, 0);
        hero.setComponentZOrder(profileBanner,   1);
        return hero;
    }

    private void drawCameraIcon(Graphics2D g2, int cx, int cy, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bw = size, bh = size * 7 / 10, bx = cx - bw / 2, by = cy - bh / 2;
        g2.drawRoundRect(bx, by, bw, bh, size / 5, size / 5);
        int lr = size * 22 / 100;
        g2.drawOval(cx - lr, cy - lr + size / 20, lr * 2, lr * 2);
        int vw = size / 4, vh = size / 6;
        g2.drawRoundRect(bx + size / 6, by - vh, vw, vh, 2, 2);
    }

    private void drawDefaultAvatar(Graphics2D g2, int size) {
        g2.setColor(BRAND_BLUE);
        g2.fillOval(0, 0, size, size);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(size / 18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int eyeY = size * 38 / 100, eyeOff = size * 18 / 100, eyeR = size / 14;
        g2.fillOval(size / 2 - eyeOff - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
        g2.fillOval(size / 2 + eyeOff - eyeR, eyeY - eyeR, eyeR * 2, eyeR * 2);
        g2.drawArc(size * 28 / 100, size * 44 / 100, size * 44 / 100, size * 26 / 100, 200, 140);
    }

    // ── Personal information card ─────────────────────────────────
    private JPanel buildPersonalInfoCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel title = new JLabel("Personal Information");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        editBtn   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        saveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        cancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setMinimumSize(new Dimension(90, 32));
        cancelBtn.setMaximumSize(new Dimension(90, 32));
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        btnRow.add(editBtn);
        btnRow.add(saveBtn);
        btnRow.add(cancelBtn);
        titleRow.add(btnRow, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(16));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(20));

        User user = app.getLoggedInUserObj();
        String name  = user != null ? user.getName()  : "—";
        String email = user != null ? user.getEmail() : "—";
        String role  = user != null ? user.getRole()  : "—";
        if (!role.equals("—")) role = role.substring(0, 1).toUpperCase() + role.substring(1);

        // Username display/edit panels
        nameDisplayPanel = new JPanel(new BorderLayout(12, 0));
        nameDisplayPanel.setOpaque(false);
        nameDisplayPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel nameLbl = new JLabel("Username");
        nameLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLbl.setForeground(UIConstants.TEXT_MUTED);
        nameLbl.setPreferredSize(new Dimension(90, 20));
        profileNameDisplay = new JLabel(name);
        profileNameDisplay.setFont(UIConstants.FONT_BODY);
        profileNameDisplay.setForeground(UIConstants.TEXT_PRIMARY);
        nameDisplayPanel.add(nameLbl, BorderLayout.WEST);
        nameDisplayPanel.add(profileNameDisplay, BorderLayout.CENTER);

        nameEditPanel = new JPanel(new BorderLayout(12, 0));
        nameEditPanel.setOpaque(false);
        nameEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel nameLblE = new JLabel("Username");
        nameLblE.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLblE.setForeground(UIConstants.TEXT_MUTED);
        nameLblE.setPreferredSize(new Dimension(90, 20));
        profileNameField = new JTextField(name);
        profileNameField.setFont(UIConstants.FONT_BODY);
        profileNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(4, 8, 4, 8)));
        nameEditPanel.add(nameLblE, BorderLayout.WEST);
        nameEditPanel.add(profileNameField, BorderLayout.CENTER);
        nameEditPanel.setVisible(false);

        card.add(nameDisplayPanel);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

        // Email display/edit panels
        emailDisplayPanel = new JPanel(new BorderLayout(12, 0));
        emailDisplayPanel.setOpaque(false);
        emailDisplayPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel emailLbl = new JLabel("Email");
        emailLbl.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLbl.setForeground(UIConstants.TEXT_MUTED);
        emailLbl.setPreferredSize(new Dimension(90, 20));
        profileEmailDisplay = new JLabel(email);
        profileEmailDisplay.setFont(UIConstants.FONT_BODY);
        profileEmailDisplay.setForeground(UIConstants.TEXT_PRIMARY);
        emailDisplayPanel.add(emailLbl, BorderLayout.WEST);
        emailDisplayPanel.add(profileEmailDisplay, BorderLayout.CENTER);

        emailEditPanel = new JPanel(new BorderLayout(12, 0));
        emailEditPanel.setOpaque(false);
        emailEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel emailLblE = new JLabel("Email");
        emailLblE.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLblE.setForeground(UIConstants.TEXT_MUTED);
        emailLblE.setPreferredSize(new Dimension(90, 20));
        profileEmailField = new JTextField(email);
        profileEmailField.setFont(UIConstants.FONT_BODY);
        profileEmailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(4, 8, 4, 8)));
        emailEditPanel.add(emailLblE, BorderLayout.WEST);
        emailEditPanel.add(profileEmailField, BorderLayout.CENTER);
        emailEditPanel.setVisible(false);

        card.add(emailDisplayPanel);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));

        // Role (read-only)
        buildInfoRow(card, "Role", role);
        card.add(Box.createVerticalStrut(4));

        editBtn.addActionListener(e -> enterEditMode());
        cancelBtn.addActionListener(e -> exitEditMode());
        saveBtn.addActionListener(e  -> handleSave());

        return card;
    }

    private void enterEditMode() {
        profileNameField.setText(profileController.getCurrentName());
        profileEmailField.setText(profileController.getCurrentEmail());
        nameDisplayPanel.setVisible(false);
        emailDisplayPanel.setVisible(false);
        nameEditPanel.setVisible(true);
        emailEditPanel.setVisible(true);
        editBtn.setVisible(false);
        saveBtn.setVisible(true);
        cancelBtn.setVisible(true);
        profileNameField.requestFocusInWindow();
    }

    private void exitEditMode() {
        nameEditPanel.setVisible(false);
        emailEditPanel.setVisible(false);
        nameDisplayPanel.setVisible(true);
        emailDisplayPanel.setVisible(true);
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        editBtn.setVisible(true);
    }

    private void handleSave() {
        String newName  = profileNameField.getText().trim();
        String newEmail = profileEmailField.getText().trim();

        if (profileController.hasNoChanges(newName, newEmail)) {
            JOptionPane.showMessageDialog(app, "No changes were made.",
                    "No Changes", JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }

        try {
            boolean success = profileController.saveProfile(newName, newEmail);
            if (success) {
                exitEditMode();
                refreshUser();
                JOptionPane.showMessageDialog(app, "Profile updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app, "Failed to save. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(app, ex.getMessage(),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    private JLabel buildInfoRow(JPanel card, String fieldName, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel lbl = new JLabel(fieldName);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(90, 20));
        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_BODY);
        val.setForeground(UIConstants.TEXT_PRIMARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        card.add(row);
        if ("Role".equals(fieldName)) profileRoleDisplay = val;
        return val;
    }

    // ── My Vehicle card ──────────────────────────────────────────
    private JPanel buildVehicleCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel title = new JLabel("My Vehicle");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);

        JButton addBtn = createActionButton("+ Add", new Color(80, 110, 230), Color.WHITE);
        // TODO: addBtn.addActionListener(e -> controller.showAddVehicleDialog());
        titleRow.add(addBtn, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        card.add(vehicleListPanel);
        return card;
    }

    private void refreshVehicleList() {
        if (vehicleListPanel == null) return;
        vehicleListPanel.removeAll();

        User user = app.getLoggedInUserObj();
        if (user == null) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
        } else {
            List<String[]> vehicles = vehicleService.getVehiclesByEmail(user.getEmail());
            if (vehicles.isEmpty()) {
                vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
            } else {
                int limit = Math.min(3, vehicles.size());
                for (int i = 0; i < limit; i++) {
                    String[] v = vehicles.get(i);
                    vehicleListPanel.add(buildVehicleRow(v[0], v[1], v[2], v[3], v[4]));
                    if (i < limit - 1) vehicleListPanel.add(Box.createVerticalStrut(10));
                }
            }
        }
        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
    }

    // ── Image choosers ───────────────────────────────────────────

    /**
     * Opens a file chooser, lets the user pick an image, saves it to
     * src/ProfilePic/{email}.jpg, then immediately updates the
     * profile picture circle and the top-right header avatar.
     */
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Profile Picture", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);

        if (fd.getFile() == null) return;

        try {
            java.io.File selected = new java.io.File(fd.getDirectory(), fd.getFile());
            BufferedImage img = ImageIO.read(selected);
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Delegate saving to ProfilePicStorage (model layer)
            boolean saved = profilePictureService.saveImage(user.getEmail(), img);
            if (!saved) {
                JOptionPane.showMessageDialog(app, "Failed to save profile picture.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            profileImage = img;
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens a file chooser, lets the user pick an image, saves it to
     * src/BackgroundImg/{email}.jpg, then immediately updates the banner.
     */
    private void chooseBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Background Image", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);

        if (fd.getFile() == null) return;

        try {
            java.io.File selected = new java.io.File(fd.getDirectory(), fd.getFile());
            BufferedImage img = ImageIO.read(selected);
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Delegate saving to BackgroundImageStorage (model layer)
            boolean saved = backgroundImageService.saveImage(user.getEmail(), img);
            if (!saved) {
                JOptionPane.showMessageDialog(app, "Failed to save background image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            bannerImage = img;
            if (profileBanner != null) profileBanner.repaint();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Vehicle row using CardLayout to swap between display and edit views.
     * The outer box size never changes — CardLayout keeps both views the same height.
     */
    private JPanel buildVehicleRow(String plate, String brand,
                                   String year, String colour, String carType) {
        // Fixed-height outer box — same border as before
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        outer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(10, 14, 10, 14)
        ));

        // CardLayout swaps between "display" and "edit" inside the fixed box
        CardLayout rowCards = new CardLayout();
        JPanel rowContainer = new JPanel(rowCards);
        rowContainer.setOpaque(false);

        // ── DISPLAY card ─────────────────────────────────────────
        JPanel displayCard = new JPanel(new BorderLayout(12, 0));
        displayCard.setOpaque(false);

        JLabel carIcon = new JLabel(new StringBuilder().appendCodePoint(0x1F697).toString());
        carIcon.setFont(new Font("SansSerif", Font.PLAIN, 26));
        carIcon.setVerticalAlignment(SwingConstants.CENTER);
        displayCard.add(carIcon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        String typeText = (carType != null && !carType.isEmpty())
                ? brand + "   ·   " + carType : brand;
        JLabel topLine = new JLabel(typeText);
        topLine.setFont(new Font("SansSerif", Font.BOLD, 13));
        topLine.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel botLine = new JLabel(year + "   ·   " + colour);
        botLine.setFont(UIConstants.FONT_SMALL);
        botLine.setForeground(UIConstants.TEXT_MUTED);
        info.add(Box.createVerticalGlue());
        info.add(topLine);
        info.add(Box.createVerticalStrut(3));
        info.add(botLine);
        info.add(Box.createVerticalGlue());
        displayCard.add(info, BorderLayout.CENTER);

        JPanel readBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        readBtns.setOpaque(false);
        JButton editVehicleBtn = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        JButton removeBtn      = createActionButton("Remove", new Color(220, 80, 80),  Color.WHITE);
        removeBtn.setPreferredSize(new Dimension(90, 32));
        removeBtn.setMinimumSize(new Dimension(90, 32));
        removeBtn.setMaximumSize(new Dimension(90, 32));
        readBtns.add(editVehicleBtn);
        readBtns.add(removeBtn);
        displayCard.add(readBtns, BorderLayout.EAST);

        // ── EDIT card ────────────────────────────────────────────
        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);

        // Four compact fields side by side using GridLayout
        JPanel fields = new JPanel(new GridLayout(1, 4, 6, 0));
        fields.setOpaque(false);

        JTextField brandField  = makeCompactField(brand);
        JTextField yearField   = makeCompactField(year);
        JTextField colourField = makeCompactField(colour);
        JTextField typeField   = makeCompactField(carType);

        // Each field has a tiny label above it
        fields.add(makeLabelledField("Brand",   brandField));
        fields.add(makeLabelledField("Year",    yearField));
        fields.add(makeLabelledField("Colour",  colourField));
        fields.add(makeLabelledField("Type",    typeField));
        editCard.add(fields, BorderLayout.CENTER);

        // Save / Cancel buttons on the right
        JPanel editBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        editBtns.setOpaque(false);
        JButton saveVBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelVBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelVBtn.setPreferredSize(new Dimension(90, 32));
        cancelVBtn.setMinimumSize(new Dimension(90, 32));
        cancelVBtn.setMaximumSize(new Dimension(90, 32));
        editBtns.add(saveVBtn);
        editBtns.add(cancelVBtn);
        editCard.add(editBtns, BorderLayout.EAST);

        // Add both cards to the container
        rowContainer.add(displayCard, "display");
        rowContainer.add(editCard,    "edit");
        rowCards.show(rowContainer, "display");

        outer.add(rowContainer, BorderLayout.CENTER);

        // ── Button actions ────────────────────────────────────────

        // Edit — flip to edit card
        editVehicleBtn.addActionListener(e -> {
            brandField.setText(brand);
            yearField.setText(year);
            colourField.setText(colour);
            typeField.setText(carType);
            rowCards.show(rowContainer, "edit");
        });

        // Cancel — flip back to display card
        cancelVBtn.addActionListener(e -> rowCards.show(rowContainer, "display"));

        // Save — no-changes check, validate, update file
        saveVBtn.addActionListener(e -> {
            String newBrand  = brandField.getText().trim();
            String newYear   = yearField.getText().trim();
            String newColour = colourField.getText().trim();
            String newType   = typeField.getText().trim();

            if (newBrand.equals(brand) && newYear.equals(year)
                    && newColour.equals(colour) && newType.equals(carType)) {
                JOptionPane.showMessageDialog(app, "No changes were made.",
                        "No Changes", JOptionPane.INFORMATION_MESSAGE);
                rowCards.show(rowContainer, "display");
                return;
            }
            if (newBrand.isEmpty() || newYear.isEmpty()
                    || newColour.isEmpty() || newType.isEmpty()) {
                JOptionPane.showMessageDialog(app, "All fields must be filled in.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            User user = app.getLoggedInUserObj();
            if (user != null) {
                vehicleService.deleteVehicle(user.getEmail(), plate);
                vehicleService.addVehicle(user.getEmail(), plate,
                        newBrand, newYear, newColour, newType);
                refreshVehicleList();
            }
        });

        // Remove — confirm dialog, same JOptionPane style as all other popups
        removeBtn.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(app,
                    "Are you sure you want to remove this vehicle?",
                    "Confirm Remove",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                User user = app.getLoggedInUserObj();
                if (user != null && vehicleService.deleteVehicle(user.getEmail(), plate)) {
                    refreshVehicleList();
                } else {
                    JOptionPane.showMessageDialog(app, "Failed to remove vehicle.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return outer;
    }

    /** Compact text field for vehicle edit card. */
    private JTextField makeCompactField(String value) {
        JTextField f = new JTextField(value);
        f.setFont(new Font("SansSerif", Font.PLAIN, 11));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 1),
                new EmptyBorder(2, 4, 2, 4)));
        return f;
    }

    /** Tiny label above a compact field — fits inside the fixed row height. */
    private JPanel makeLabelledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        p.add(lbl,   BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JLabel makeEmptyLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    // ── Upcoming appointments card ───────────────────────────────
    private JPanel buildUpcomingCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Top 3");
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        titleRow.add(hint, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));
        JLabel empty = new JLabel("No upcoming appointments.");
        empty.setFont(UIConstants.FONT_BODY);
        empty.setForeground(UIConstants.TEXT_SECONDARY);
        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(empty);
        card.add(Box.createVerticalStrut(4));
        return card;
    }

    // ── Payment summary card ─────────────────────────────────────
    private JPanel buildPaymentSummaryCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel("Payment History");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);
        JLabel hint = new JLabel("Top 3");
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        titleRow.add(hint, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));
        JLabel empty = new JLabel("No payment records found.");
        empty.setFont(UIConstants.FONT_BODY);
        empty.setForeground(UIConstants.TEXT_SECONDARY);
        empty.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(empty);
        card.add(Box.createVerticalStrut(4));
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)
        ));
        headerTitle = new JLabel("Profile");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildHeaderProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildHeaderProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    // Draw the user's profile photo clipped to a circle
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38, null);
                    g2.setClip(null);
                } else {
                    g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
                    g2.fillOval(0, 0, 38, 38);
                    g2.dispose();
                    super.paintComponent(g); // draws the icon text
                    return;
                }
                g2.dispose();
                // Don't call super — no icon text needed when photo is shown
            }
        };
        avatarLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setVerticalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(38, 38));

        profileLabel = new JLabel("—");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        JLabel arrow = new JLabel("\u25BE");
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        arrow.setForeground(UIConstants.TEXT_MUTED);

        JPanel profileBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileBtn.setBackground(UIConstants.BG_HEADER);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.add(avatarLabel);
        profileBtn.add(profileLabel);
        profileBtn.add(arrow);

        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)
        ));
        menu.setBackground(Color.WHITE);

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
        menu.add(viewProfile);
        menu.add(logout);

        profileBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                menu.show(profileBtn,
                        profileBtn.getWidth() - menu.getPreferredSize().width,
                        profileBtn.getHeight());
            }
        });
        menu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = e.getPoint();
                if (!menu.getBounds().contains(p)) menu.setVisible(false);
            }
        });

        profileArea.add(profileBtn);
        return profileArea;
    }

    // ═══════════════════════════════════════════════════════════
    // SIDEBAR
    // ═══════════════════════════════════════════════════════════
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));

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

        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.SIDEBAR_DIVIDER);
        divider.setBackground(UIConstants.SIDEBAR_BG);
        divider.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel navLabel = new JLabel("MENU");
        navLabel.setFont(UIConstants.FONT_LABEL);
        navLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        navLabel.setBorder(new EmptyBorder(10, 24, 10, 20));
        navLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        navLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35));
        sidebar.add(navLabel);

        JButton[] navButtons = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String navName = NAV_ITEMS[i];
            String btnText = navName.equals("Staff Review")
                    ? "<html><font size='5'>\u2605</font>&nbsp;&nbsp;&nbsp;" + navName + "</html>"
                    : NAV_ICONS[i] + "   " + navName;
            navButtons[i] = createNavButton(btnText, navName.equals(activeNav));
            navButtons[i].addActionListener(e -> {
                activeNav = navName;
                for (int j = 0; j < navButtons.length; j++)
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals(activeNav));
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

    // ═══════════════════════════════════════════════════════════
    // PLACEHOLDER PAGES
    // ═══════════════════════════════════════════════════════════
    private JPanel buildPlaceholder(String title, String description, String icon, int iconSize) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
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
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, iconSize));
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

    // ═══════════════════════════════════════════════════════════
    // SHARED UI HELPERS
    // ═══════════════════════════════════════════════════════════
    private JPanel createCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
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

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hover = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(72, 32));
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
