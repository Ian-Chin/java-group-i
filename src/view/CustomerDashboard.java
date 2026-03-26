package view;

import model.CustomerProfileController;
import model.User;
import model.VehicleService;
import model.VehicleSectionController;
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

// The main screen customers see after logging in.
// This file ONLY builds and displays the UI.
// All logic (save, validate, file reading) is in CustomerDashboardController
// and VehicleSectionController.
//
// Java OOP principles:
//   Class        : organises all UI code in one place
//   Object       : one instance created per login session
//   Encapsulation: UI components are private; controller updates them via interface methods
//   Abstraction  : calls controller.handleSave() without knowing HOW saving works
//   Inheritance  : implements VehicleSectionController.SectionView interface
//   Polymorphism : SectionView methods update real Swing components here
public class CustomerDashboard extends JPanel {

    // ── Reference to the main application window ─────────────────
    private final AppFrame app;

    // ── Page switching ────────────────────────────────────────────
    private CardLayout contentLayout;
    private JPanel     contentPanel;
    private String     activeNav = "Profile"; // which nav item is currently selected

    // ── Header labels ─────────────────────────────────────────────
    private JLabel profileLabel; // customer's name shown in the header
    private JLabel avatarLabel;  // small circle avatar in the header
    private JLabel headerTitle;  // current page name shown in the header

    // ── Avatar circle colours and icons ──────────────────────────
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230),  // blue
            new Color(230, 80, 80),   // red
            new Color(80, 190, 110),  // green
            new Color(230, 160, 40),  // orange
            new Color(160, 80, 230),  // purple
            new Color(40, 180, 200),  // teal
            new Color(230, 80, 160),  // pink
            new Color(100, 100, 120), // grey
    };
    private static final String[] AVATAR_ICONS = {
            "\u263A", // smiley face
            "\u2605", // star
            "\u2665", // heart
            "\u2666", // diamond
            "\u263C", // sun
            "\u2708", // plane
            "\u266B", // music note
            "\u2618"  // clover
    };
    private int selectedAvatarIndex = 0; // which avatar colour/icon to show

    // ── Profile page labels (read mode) ──────────────────────────
    private JLabel profileNameDisplay;
    private JLabel profileEmailDisplay;
    private JLabel profileRoleDisplay;

    // ── Profile page text fields (edit mode) ─────────────────────
    private JTextField profileNameField;
    private JTextField profileEmailField;

    // ── Panels that swap between read mode and edit mode ─────────
    private JPanel nameDisplayPanel;  // shown in read mode
    private JPanel nameEditPanel;     // shown in edit mode
    private JPanel emailDisplayPanel; // shown in read mode
    private JPanel emailEditPanel;    // shown in edit mode

    // ── Edit / Save / Cancel buttons on the Personal Info card ───
    private JButton editBtn;
    private JButton saveBtn;
    private JButton cancelBtn;

    // ── Vehicle section ───────────────────────────────────────────
    private JPanel vehicleListPanel; // holds all vehicle rows
    private JPanel vehicleAddPanel;  // the inline add form (hidden by default)
    private JButton[] navButtons;    // sidebar nav buttons — kept as a field so View Profile can highlight Profile

    // The four section pages — kept as fields so we can call refresh() on them
    private ServiceHistoryPage  serviceHistoryPage;
    private PaymentHistoryPage  paymentHistoryPage;
    private StaffReviewPage     staffReviewPage;
    private MyFeedbackPage      myFeedbackPage;

    // ── Controllers and services ──────────────────────────────────
    private final CustomerProfileController profileController;
    private final VehicleSectionController  vehicleController;
    private final VehicleService            vehicleService = new VehicleService();
    private final ProfilePicStorage         profilePicStorage = new ProfilePicStorage();
    private final BackgroundImageStorage    backgroundStorage = new BackgroundImageStorage();

    // ── Images currently shown on screen ─────────────────────────
    private BufferedImage profileImage = null; // null = show default smiley
    private BufferedImage bannerImage  = null; // null = show default blue gradient

    // ── The banner and profile picture components ─────────────────
    private JPanel profileBanner;
    private JLabel profilePicLabel;

    // ── Brand colours ─────────────────────────────────────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);

    // ── Sidebar navigation items (Appointment Booking removed) ───
    private static final String[] NAV_ITEMS = {
            "Profile",
            "Service History",
            "Payment History",
            "Staff Review",
            "My Feedback"
    };

    // Converts a Unicode code point to a String emoji
    private static String navIcon(int codePoint) {
        return new StringBuilder().appendCodePoint(codePoint).toString();
    }

    private static final String[] NAV_ICONS = {
            navIcon(0x1F464), // 👤 person  (Profile)
            navIcon(0x1F504), // 🔄 cycle   (Service History)
            navIcon(0x1F4B5), // 💵 money   (Payment History)
            "\u2605",         // ★ star     (Staff Review)
            navIcon(0x1F4AC)  // 💬 chat    (My Feedback)
    };

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR
    // ═══════════════════════════════════════════════════════════════

    public CustomerDashboard(AppFrame app) {
        this.app = app;

        // ── Set up ProfileController ──────────────────────────────
        // AppFrameAccessor lets the controller update the session without
        // importing AppFrame (keeps model independent of view)
        profileController = new CustomerProfileController(
                app.getAccountService(),
                new CustomerProfileController.AppFrameAccessor() {
                    @Override public User   getLoggedInUserObj()       { return app.getLoggedInUserObj(); }
                    @Override public String getLoggedInUser()          { return app.getLoggedInUser(); }
                    @Override public void   setLoggedInUser(String n)  { app.setLoggedInUser(n); }
                    @Override public void   setLoggedInUserObj(User u) { app.setLoggedInUserObj(u); }
                }
        );

        // ── Set up VehicleSectionController ──────────────────────
        // SectionView lets the controller update the vehicle list UI
        // without importing CustomerDashboard directly
        vehicleController = new VehicleSectionController(
                vehicleService,
                new VehicleSectionController.SectionView() {
                    @Override
                    public User getLoggedInUser() {
                        return app.getLoggedInUserObj();
                    }

                    @Override
                    public void rebuildList(List<String[]> vehicles) {
                        // Rebuild the vehicle list panel with the given vehicles
                        rebuildVehicleList(vehicles);
                    }

                    @Override
                    public void showMessage(String message, String title, int type) {
                        JOptionPane.showMessageDialog(app, message, title, type);
                    }

                    @Override
                    public java.awt.Window getWindow() {
                        return SwingUtilities.getWindowAncestor(CustomerDashboard.this);
                    }
                }
        );

        // ── Build the layout ──────────────────────────────────────
        // Sidebar on the LEFT, header + content pages on the RIGHT
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        // ContentPanel uses CardLayout to switch between pages
        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        // Add each page — Appointment Booking removed
        contentPanel.add(buildProfilePage(),   "Profile");

        // Create real section pages
        serviceHistoryPage = new ServiceHistoryPage();
        paymentHistoryPage = new PaymentHistoryPage();
        staffReviewPage    = new StaffReviewPage();
        myFeedbackPage     = new MyFeedbackPage();

        contentPanel.add(serviceHistoryPage, "Service History");
        contentPanel.add(paymentHistoryPage, "Payment History");
        contentPanel.add(staffReviewPage,    "Staff Review");
        contentPanel.add(myFeedbackPage,     "My Feedback");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    // Called automatically by Java when this panel is shown on screen
    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    /**
     * Refreshes all information displayed on the dashboard.
     * Called after login and after any profile changes.
     */
    public void refreshUser() {
        // Update the header name
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Customer";
        if (profileLabel != null) profileLabel.setText(name);

        // Update the avatar icon
        User user = app.getLoggedInUserObj();
        if (user != null) selectedAvatarIndex = user.getProfilePicture();
        if (avatarLabel != null) {
            avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]);
            avatarLabel.repaint();
        }

        // Update the profile page labels
        if (user != null) {
            if (profileNameDisplay  != null) profileNameDisplay.setText(user.getName());
            if (profileEmailDisplay != null) profileEmailDisplay.setText(user.getEmail());
            if (profileRoleDisplay  != null) {
                String role = user.getRole();
                String capitalisedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
                profileRoleDisplay.setText(capitalisedRole);
            }

            // Load saved profile picture and background image from disk
            profileImage = profilePicStorage.loadImage(user.getUserId());
            bannerImage  = backgroundStorage.loadImage(user.getUserId());
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (profileBanner   != null) profileBanner.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }

        // Reload the vehicle list via the controller
        vehicleController.refreshList();
    }

    /**
     * Called by VehicleSectionController.SectionView.rebuildList().
     * Clears the vehicle list panel and rebuilds it with the given vehicles.
     *
     * Each vehicle array: [vehicleID, plate, brand, year, colour]
     */
    /** Clears the vehicle list panel and rebuilds it using the given vehicle data. */
    private void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
        } else {
            for (int i = 0; i < vehicles.size(); i++) {
                String[] vehicle  = vehicles.get(i);
                String vehicleID = vehicle[0];
                String plate     = vehicle[1];
                String brand     = vehicle[2];
                String year      = vehicle[3];
                String colour    = vehicle[4];

                vehicleListPanel.add(buildVehicleRow(vehicleID, plate, brand, year, colour));

                // Add a small gap between rows (not after the last one)
                if (i < vehicles.size() - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(10));
                }
            }
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFILE PAGE
    // ═══════════════════════════════════════════════════════════════

    /** Builds the Profile page wrapped in a scroll pane so it scrolls vertically. */
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

    /** Builds the inner content of the Profile page — banner at the top, two columns below. */
    private JPanel buildProfileInner() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(UIConstants.BG_CONTENT);
        inner.setBorder(new EmptyBorder(0, 0, 40, 0));

        inner.add(buildBannerHero());
        inner.add(Box.createVerticalStrut(24));

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(UIConstants.BG_CONTENT);
        body.setBorder(new EmptyBorder(0, 30, 0, 30));

        // Left column: Personal Info card + My Vehicle card
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildPersonalInfoCard());
        leftColumn.add(Box.createVerticalStrut(16));
        leftColumn.add(buildVehicleCard());
        body.add(leftColumn, BorderLayout.CENTER);

        // Right column: fixed 440px wide
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(440, 0));
        rightColumn.setMinimumSize(new Dimension(440, 0));
        rightColumn.setMaximumSize(new Dimension(440, Integer.MAX_VALUE));
        rightColumn.add(buildUpcomingCard());
        rightColumn.add(Box.createVerticalStrut(16));
        rightColumn.add(buildPaymentSummaryCard());
        body.add(rightColumn, BorderLayout.EAST);

        inner.add(body);
        return inner;
    }

    // ── Banner + profile picture ──────────────────────────────────
    /** Builds the banner (background image) and the overlapping circular profile picture. */
    private JPanel buildBannerHero() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHovered = {false};
        boolean[] avatarHovered = {false};

        profileBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    GradientPaint gradient = new GradientPaint(
                            0, 0, BANNER_BLUE, getWidth(), getHeight(), new Color(60, 90, 210));
                    g2.setPaint(gradient);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (bannerHovered[0]) {
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
            @Override public void mouseEntered(MouseEvent e) { bannerHovered[0] = true;  profileBanner.repaint(); }
            @Override public void mouseExited (MouseEvent e) { bannerHovered[0] = false; profileBanner.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { chooseBannerImage(); }
        });

        profilePicLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    // Crop to square from the centre so the image is never stretched
                    int imgW    = profileImage.getWidth();
                    int imgH    = profileImage.getHeight();
                    int crop    = Math.min(imgW, imgH);
                    int cropX   = (imgW - crop) / 2;
                    int cropY   = (imgH - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size,
                            cropX, cropY, cropX + crop, cropY + crop, null);
                    g2.setClip(null);
                } else {
                    drawDefaultAvatar(g2, size);
                }
                if (avatarHovered[0]) {
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillOval(0, 0, size, size);
                    g2.setClip(null);
                    drawCameraIcon(g2, size / 2, size / 2, 20, Color.WHITE);
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
            @Override public void mouseEntered(MouseEvent e) { avatarHovered[0] = true;  profilePicLabel.repaint(); }
            @Override public void mouseExited (MouseEvent e) { avatarHovered[0] = false; profilePicLabel.repaint(); }
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

    /** Draws a simple camera icon — shown as a hover overlay on the banner and profile picture. */
    private void drawCameraIcon(Graphics2D g2, int cx, int cy, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bw = size, bh = size * 7 / 10, bx = cx - bw / 2, by = cy - bh / 2;
        g2.drawRoundRect(bx, by, bw, bh, size / 5, size / 5);
        int lr = size * 22 / 100;
        g2.drawOval(cx - lr, cy - lr + size / 20, lr * 2, lr * 2);
        g2.drawRoundRect(bx + size / 6, by - size / 6, size / 4, size / 6, 2, 2);
    }

    /** Draws a blue circle with a white smiley face — the default profile picture. */
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

    // ── Personal Information card ─────────────────────────────────
    /** Builds the Personal Information card showing Username, Email and Role. */
    private JPanel buildPersonalInfoCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel titleLabel = new JLabel("Personal Information");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);
        editBtn   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        saveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        cancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setMinimumSize(new Dimension(90, 32));
        cancelBtn.setMaximumSize(new Dimension(90, 32));
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        buttonPanel.add(editBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        titleRow.add(buttonPanel, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(16));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(20));

        User currentUser    = app.getLoggedInUserObj();
        String currentName  = currentUser != null ? currentUser.getName()  : "—";
        String currentEmail = currentUser != null ? currentUser.getEmail() : "—";
        String currentRole  = currentUser != null ? currentUser.getRole()  : "—";
        if (!currentRole.equals("—")) {
            currentRole = currentRole.substring(0, 1).toUpperCase() + currentRole.substring(1);
        }

        // Username display row
        nameDisplayPanel = new JPanel(new BorderLayout(12, 0));
        nameDisplayPanel.setOpaque(false);
        nameDisplayPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel nameLabel = new JLabel("Username");
        nameLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLabel.setForeground(UIConstants.TEXT_MUTED);
        nameLabel.setPreferredSize(new Dimension(90, 20));
        profileNameDisplay = new JLabel(currentName);
        profileNameDisplay.setFont(UIConstants.FONT_BODY);
        profileNameDisplay.setForeground(UIConstants.TEXT_PRIMARY);
        nameDisplayPanel.add(nameLabel, BorderLayout.WEST);
        nameDisplayPanel.add(profileNameDisplay, BorderLayout.CENTER);

        // Username edit row (hidden by default)
        nameEditPanel = new JPanel(new BorderLayout(12, 0));
        nameEditPanel.setOpaque(false);
        nameEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel nameLabelEdit = new JLabel("Username");
        nameLabelEdit.setFont(UIConstants.FONT_SMALL_BOLD);
        nameLabelEdit.setForeground(UIConstants.TEXT_MUTED);
        nameLabelEdit.setPreferredSize(new Dimension(90, 20));
        profileNameField = new JTextField(currentName);
        profileNameField.setFont(UIConstants.FONT_BODY);
        profileNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(4, 8, 4, 8)));
        nameEditPanel.add(nameLabelEdit, BorderLayout.WEST);
        nameEditPanel.add(profileNameField, BorderLayout.CENTER);
        nameEditPanel.setVisible(false);

        card.add(nameDisplayPanel);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

        // Email display row
        emailDisplayPanel = new JPanel(new BorderLayout(12, 0));
        emailDisplayPanel.setOpaque(false);
        emailDisplayPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel emailLabel = new JLabel("Email");
        emailLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLabel.setForeground(UIConstants.TEXT_MUTED);
        emailLabel.setPreferredSize(new Dimension(90, 20));
        profileEmailDisplay = new JLabel(currentEmail);
        profileEmailDisplay.setFont(UIConstants.FONT_BODY);
        profileEmailDisplay.setForeground(UIConstants.TEXT_PRIMARY);
        emailDisplayPanel.add(emailLabel, BorderLayout.WEST);
        emailDisplayPanel.add(profileEmailDisplay, BorderLayout.CENTER);

        // Email edit row (hidden by default)
        emailEditPanel = new JPanel(new BorderLayout(12, 0));
        emailEditPanel.setOpaque(false);
        emailEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel emailLabelEdit = new JLabel("Email");
        emailLabelEdit.setFont(UIConstants.FONT_SMALL_BOLD);
        emailLabelEdit.setForeground(UIConstants.TEXT_MUTED);
        emailLabelEdit.setPreferredSize(new Dimension(90, 20));
        profileEmailField = new JTextField(currentEmail);
        profileEmailField.setFont(UIConstants.FONT_BODY);
        profileEmailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(4, 8, 4, 8)));
        emailEditPanel.add(emailLabelEdit, BorderLayout.WEST);
        emailEditPanel.add(profileEmailField, BorderLayout.CENTER);
        emailEditPanel.setVisible(false);

        card.add(emailDisplayPanel);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));

        buildInfoRow(card, "Role", currentRole); // role is always read-only
        card.add(Box.createVerticalStrut(4));

        editBtn.addActionListener(e   -> enterEditMode());
        cancelBtn.addActionListener(e -> exitEditMode());
        saveBtn.addActionListener(e   -> handleSave());

        return card;
    }

    /** Switches to edit mode: shows text fields, hides labels. */
    /** Switches to edit mode — hides the labels and shows the text fields for typing. */
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

    /** Switches back to read mode: shows labels, hides text fields. */
    /** Switches back to read mode — hides the text fields and shows the labels again. */
    private void exitEditMode() {
        nameEditPanel.setVisible(false);
        emailEditPanel.setVisible(false);
        nameDisplayPanel.setVisible(true);
        emailDisplayPanel.setVisible(true);
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        editBtn.setVisible(true);
    }

    /** Called when Save is clicked — validates and saves the profile. */
    /** Called when Save is clicked — checks for changes, validates, and saves the profile. */
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
            boolean saved = profileController.saveProfile(newName, newEmail);
            if (saved) {
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

    /** Adds one read-only field row to a card. */
    private JLabel buildInfoRow(JPanel card, String fieldName, String value) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        JLabel fieldLabel = new JLabel(fieldName);
        fieldLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        fieldLabel.setForeground(UIConstants.TEXT_MUTED);
        fieldLabel.setPreferredSize(new Dimension(90, 20));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(UIConstants.FONT_BODY);
        valueLabel.setForeground(UIConstants.TEXT_PRIMARY);
        row.add(fieldLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
        card.add(row);
        if ("Role".equals(fieldName)) profileRoleDisplay = valueLabel;
        return valueLabel;
    }

    // ── My Vehicle card ───────────────────────────────────────────
    /** Builds the My Vehicle card with the vehicle list and the + Add button. */
    private JPanel buildVehicleCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel titleLabel = new JLabel("My Vehicle");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JButton addButton = createActionButton("+ Add", new Color(80, 110, 230), Color.WHITE);
        titleRow.add(addButton, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        card.add(vehicleListPanel);

        vehicleAddPanel = buildVehicleAddForm();
        vehicleAddPanel.setVisible(false);
        card.add(vehicleAddPanel);

        // Toggle the add form when + Add is clicked
        addButton.addActionListener(e -> {
            boolean currentlyVisible = vehicleAddPanel.isVisible();
            vehicleAddPanel.setVisible(!currentlyVisible);
            card.revalidate();
            card.repaint();
        });

        return card;
    }

    /** Builds the inline Add Vehicle form (hidden by default). */
    /** Builds the inline Add Vehicle form that appears when + Add is clicked. */
    private JPanel buildVehicleAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(10, 14, 10, 14)));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        JPanel fieldsPanel = new JPanel(new GridLayout(1, 4, 6, 0));
        fieldsPanel.setOpaque(false);
        JTextField plateField  = makeCompactField("");
        JTextField brandField  = makeCompactField("");
        JTextField yearField   = makeCompactField("");
        JTextField colourField = makeCompactField("");
        fieldsPanel.add(makeLabelledField("Car Plate",     plateField));
        fieldsPanel.add(makeLabelledField("Brand / Model", brandField));
        fieldsPanel.add(makeLabelledField("Year",          yearField));
        fieldsPanel.add(makeLabelledField("Colour",        colourField));
        form.add(fieldsPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonPanel.setOpaque(false);
        JButton saveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setMinimumSize(new Dimension(90, 32));
        cancelBtn.setMaximumSize(new Dimension(90, 32));
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        form.add(buttonPanel, BorderLayout.EAST);

        // Cancel: clear fields and hide the form
        cancelBtn.addActionListener(e -> {
            plateField.setText("");
            brandField.setText("");
            yearField.setText("");
            colourField.setText("");
            vehicleAddPanel.setVisible(false);
        });

        // Save: pass fields to the controller — it handles validation and saving
        saveBtn.addActionListener(e -> {
            String[] fields = {
                plateField.getText().trim(),
                brandField.getText().trim(),
                yearField.getText().trim(),
                colourField.getText().trim()
            };

            // Delegate to VehicleSectionController.handleAdd()
            boolean saved = vehicleController.handleAdd(fields);

            if (saved) {
                // Clear fields and hide the form on success
                plateField.setText("");
                brandField.setText("");
                yearField.setText("");
                colourField.setText("");
                vehicleAddPanel.setVisible(false);
                JOptionPane.showMessageDialog(app, "New vehicle has been added.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        return form;
    }

    // ── Image choosers ────────────────────────────────────────────

    /** Opens the OS file chooser to pick a profile picture. */
    /** Opens the OS file chooser so the user can pick a new profile picture. */
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Profile Picture", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage image = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (image == null) { JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            if (!profilePicStorage.saveImage(user.getUserId(), image)) { JOptionPane.showMessageDialog(app, "Failed to save profile picture.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            profileImage = image;
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Opens the OS file chooser to pick a background image. */
    /** Opens the OS file chooser so the user can pick a new background image. */
    private void chooseBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Background Image", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage image = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (image == null) { JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            if (!backgroundStorage.saveImage(user.getUserId(), image)) { JOptionPane.showMessageDialog(app, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            bannerImage = image;
            if (profileBanner != null) profileBanner.repaint();
        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ── Vehicle row ───────────────────────────────────────────────

    /**
     * Builds one vehicle row using CardLayout to switch between display and edit mode.
     * vehicleID is kept for backend operations but not displayed on screen.
     */
    /** Builds one vehicle row that can switch between display mode and edit mode. */
    private JPanel buildVehicleRow(String vehicleID, String plate,
                                   String brand, String year, String colour) {
        JPanel outerBox = new JPanel(new BorderLayout());
        outerBox.setOpaque(false);
        outerBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        outerBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(10, 14, 10, 14)));

        CardLayout switcher = new CardLayout();
        JPanel switcherPanel = new JPanel(switcher);
        switcherPanel.setOpaque(false);

        // ── Display mode ──────────────────────────────────────────
        JPanel displayCard = new JPanel(new BorderLayout(8, 0));
        displayCard.setOpaque(false);
        JLabel carEmoji = new JLabel(new StringBuilder().appendCodePoint(0x1F697).toString());
        carEmoji.setFont(new Font("SansSerif", Font.PLAIN, 26));
        carEmoji.setVerticalAlignment(SwingConstants.CENTER);
        displayCard.add(carEmoji, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        JLabel brandLine  = new JLabel(brand);
        brandLine.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLine.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel detailLine = new JLabel(plate + "   ·   " + year + "   ·   " + colour);
        detailLine.setFont(UIConstants.FONT_SMALL);
        detailLine.setForeground(UIConstants.TEXT_MUTED);
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(brandLine);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(detailLine);
        infoPanel.add(Box.createVerticalGlue());
        displayCard.add(infoPanel, BorderLayout.CENTER);

        JPanel displayButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        displayButtons.setOpaque(false);
        JButton editButton   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        JButton removeButton = createActionButton("Remove", new Color(220, 80, 80),  Color.WHITE);
        removeButton.setPreferredSize(new Dimension(90, 32));
        removeButton.setMinimumSize(new Dimension(90, 32));
        removeButton.setMaximumSize(new Dimension(90, 32));
        displayButtons.add(editButton);
        displayButtons.add(removeButton);
        displayCard.add(displayButtons, BorderLayout.EAST);

        // ── Edit mode ─────────────────────────────────────────────
        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);
        JPanel editFields = new JPanel(new GridLayout(1, 4, 6, 0));
        editFields.setOpaque(false);
        JTextField editPlateField  = makeCompactField(plate);
        JTextField editBrandField  = makeCompactField(brand);
        JTextField editYearField   = makeCompactField(year);
        JTextField editColourField = makeCompactField(colour);
        editFields.add(makeLabelledField("Car Plate",     editPlateField));
        editFields.add(makeLabelledField("Brand / Model", editBrandField));
        editFields.add(makeLabelledField("Year",          editYearField));
        editFields.add(makeLabelledField("Colour",        editColourField));
        editCard.add(editFields, BorderLayout.CENTER);

        JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        editButtons.setOpaque(false);
        JButton saveVehicleBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelVehicleBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelVehicleBtn.setPreferredSize(new Dimension(90, 32));
        cancelVehicleBtn.setMinimumSize(new Dimension(90, 32));
        cancelVehicleBtn.setMaximumSize(new Dimension(90, 32));
        editButtons.add(saveVehicleBtn);
        editButtons.add(cancelVehicleBtn);
        editCard.add(editButtons, BorderLayout.EAST);

        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display");
        outerBox.add(switcherPanel, BorderLayout.CENTER);

        // ── Button actions ────────────────────────────────────────

        // Edit: pre-fill the edit fields and switch to edit mode
        editButton.addActionListener(e -> {
            editPlateField.setText(plate);
            editBrandField.setText(brand);
            editYearField.setText(year);
            editColourField.setText(colour);
            switcher.show(switcherPanel, "edit");
        });

        // Cancel: go back to display mode without saving
        cancelVehicleBtn.addActionListener(e -> switcher.show(switcherPanel, "display"));

        // Save: check for changes, then delegate to VehicleSectionController.handleEdit()
        saveVehicleBtn.addActionListener(e -> {
            String newPlate  = editPlateField.getText().trim();
            String newBrand  = editBrandField.getText().trim();
            String newYear   = editYearField.getText().trim();
            String newColour = editColourField.getText().trim();

            // Check if anything actually changed (view-level check, no file I/O)
            boolean nothingChanged = newPlate.equals(plate) && newBrand.equals(brand)
                    && newYear.equals(year) && newColour.equals(colour);
            if (nothingChanged) {
                JOptionPane.showMessageDialog(app, "No changes were made.",
                        "No Changes", JOptionPane.INFORMATION_MESSAGE);
                switcher.show(switcherPanel, "display");
                return;
            }

            // Delegate validation + in-place update to the controller
            String[] newFields = { newPlate, newBrand, newYear, newColour };
            boolean updated = vehicleController.handleEdit(plate, newFields);

            if (updated) {
                switcher.show(switcherPanel, "display");
                JOptionPane.showMessageDialog(app, "Vehicle information has been updated.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        // Remove: delegate confirmation + deletion to the controller
        removeButton.addActionListener(e -> vehicleController.handleDelete(plate));

        return outerBox;
    }

    // ── Upcoming Appointments card ────────────────────────────────
    /** Builds the Upcoming Appointments summary card shown on the right side. */
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

    // ── Payment History card ──────────────────────────────────────
    /** Builds the Payment History summary card shown on the right side. */
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

    // ═══════════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════════
    /** Builds the top header bar with the page title on the left and avatar on the right. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));
        headerTitle = new JLabel("Profile");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildHeaderProfileArea(), BorderLayout.EAST);
        return header;
    }

    /** Builds the avatar circle, customer name, and dropdown arrow in the header. */
    private JPanel buildHeaderProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    // Crop to square from centre so image is never stretched
                    int imgW  = profileImage.getWidth();
                    int imgH  = profileImage.getHeight();
                    int crop  = Math.min(imgW, imgH);
                    int cropX = (imgW - crop) / 2;
                    int cropY = (imgH - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38,
                            cropX, cropY, cropX + crop, cropY + crop, null);
                    g2.setClip(null);
                } else {
                    g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
                    g2.fillOval(0, 0, 38, 38);
                    g2.dispose();
                    super.paintComponent(g);
                    return;
                }
                g2.dispose();
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

        JLabel dropdownArrow = new JLabel("\u25BE");
        dropdownArrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dropdownArrow.setForeground(UIConstants.TEXT_MUTED);

        JPanel profileButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileButton.setBackground(UIConstants.BG_HEADER);
        profileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileButton.add(avatarLabel);
        profileButton.add(profileLabel);
        profileButton.add(dropdownArrow);

        JPopupMenu dropdownMenu = new JPopupMenu();
        dropdownMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)));
        dropdownMenu.setBackground(Color.WHITE);

        JMenuItem viewProfileItem = createMenuItem("View Profile");
        viewProfileItem.addActionListener(e -> {
            // Switch to Profile page
            activeNav = "Profile";
            headerTitle.setText("Profile");
            contentLayout.show(contentPanel, "Profile");
            dropdownMenu.setVisible(false);

            // Update the sidebar so Profile button is highlighted
            if (navButtons != null) {
                for (int j = 0; j < navButtons.length; j++) {
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals("Profile"));
                }
            }
        });

        JMenuItem logoutItem = createMenuItem("Logout");
        logoutItem.setForeground(UIConstants.TEXT_DANGER);
        logoutItem.addActionListener(e -> {
            app.setLoggedInUser("");
            app.setLoggedInUserObj(null);
            app.showPage(PageName.ONBOARDING);
        });

        dropdownMenu.add(viewProfileItem);
        dropdownMenu.add(logoutItem);

        profileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                dropdownMenu.show(profileButton,
                        profileButton.getWidth() - dropdownMenu.getPreferredSize().width,
                        profileButton.getHeight());
            }
        });
        dropdownMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
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
    /** Builds the left sidebar with the APU logo and navigation buttons. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));

        JPanel logoArea = new JPanel();
        logoArea.setLayout(new BoxLayout(logoArea, BoxLayout.Y_AXIS));
        logoArea.setBackground(UIConstants.SIDEBAR_BG);
        logoArea.setBorder(new EmptyBorder(25, 20, 25, 20));
        logoArea.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));

        ImageIcon logoIcon   = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image     scaledLogo = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel    logoLabel  = new JLabel(new ImageIcon(scaledLogo));
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandName = new JLabel("APU ASC");
        brandName.setFont(UIConstants.FONT_SIDEBAR);
        brandName.setForeground(Color.WHITE);
        brandName.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandName.setBorder(new EmptyBorder(8, 0, 0, 0));

        logoArea.add(logoLabel);
        logoArea.add(brandName);
        sidebar.add(logoArea);

        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.SIDEBAR_DIVIDER);
        divider.setBackground(UIConstants.SIDEBAR_BG);
        divider.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));

        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setFont(UIConstants.FONT_LABEL);
        menuLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        menuLabel.setBorder(new EmptyBorder(10, 24, 10, 20));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35));
        sidebar.add(menuLabel);

        navButtons = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String pageName = NAV_ITEMS[i];
            String buttonText = pageName.equals("Staff Review")
                    ? "<html><font size='5'>\u2605</font>&nbsp;&nbsp;&nbsp;" + pageName + "</html>"
                    : NAV_ICONS[i] + "   " + pageName;
            navButtons[i] = createNavButton(buttonText, pageName.equals(activeNav));
            navButtons[i].addActionListener(e -> {
                activeNav = pageName;
                for (int j = 0; j < navButtons.length; j++) {
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals(activeNav));
                }
                headerTitle.setText(pageName);
                contentLayout.show(contentPanel, pageName);

                // Refresh the page data each time the user navigates to it
                if (pageName.equals("Service History") && serviceHistoryPage != null) serviceHistoryPage.refresh();
                if (pageName.equals("Payment History") && paymentHistoryPage != null) paymentHistoryPage.refresh();
                if (pageName.equals("Staff Review")    && staffReviewPage    != null) staffReviewPage.refresh();
                if (pageName.equals("My Feedback")     && myFeedbackPage     != null) myFeedbackPage.refresh();
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

    /** Updates a nav button to look active (white + bold) or inactive (grey + plain). */
    private void updateNavButtonStyle(JButton btn, boolean isActive) {
        btn.putClientProperty("active", isActive);
        btn.setForeground(isActive ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }

    // ── Placeholder pages ─────────────────────────────────────────
    /** Builds a placeholder page for sections not yet implemented — shows icon + title + description. */
    private JPanel buildPlaceholder(String title, String description, String icon, int iconSize) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setBackground(UIConstants.BG_CONTENT);
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
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        titleLabel.setForeground(UIConstants.TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8));
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(UIConstants.FONT_BODY);
        descLabel.setForeground(UIConstants.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLabel);
        centreWrapper.add(card);
        page.add(centreWrapper, BorderLayout.CENTER);
        return page;
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED UI HELPERS
    // ═══════════════════════════════════════════════════════════════

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

    private JTextField makeCompactField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("SansSerif", Font.PLAIN, 11));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 1),
                new EmptyBorder(2, 4, 2, 4)));
        return field;
    }

    private JPanel makeLabelledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl,   BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JLabel makeEmptyLabel(String message) {
        JLabel label = new JLabel(message);
        label.setFont(UIConstants.FONT_BODY);
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JButton createActionButton(String text, Color backgroundColor, Color textColor) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { isHovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { isHovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHovered ? backgroundColor.darker() : backgroundColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_SMALL_BOLD);
        btn.setForeground(textColor);
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