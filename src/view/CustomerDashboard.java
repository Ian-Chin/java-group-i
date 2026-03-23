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

/**
 * CustomerDashboard is the main screen that customers see after logging in.
 * It shows the customer's profile, vehicles, appointments, and payment history.
 */
public class CustomerDashboard extends JPanel {

    // ── Reference to the main application window ─────────────────
    private final AppFrame app;

    // ── Used to switch between different pages (Profile, Appointments, etc.) ─
    private CardLayout contentLayout;
    private JPanel contentPanel;
    private String activeNav = "Profile"; // tracks which nav button is selected

    // ── Labels shown in the top header bar ───────────────────────
    private JLabel profileLabel;  // shows the customer's name
    private JLabel avatarLabel;   // shows the small circle avatar icon
    private JLabel headerTitle;   // shows the current page name

    // ── Avatar circle colours and icons (one per profile picture option) ─
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
    private int selectedAvatarIndex = 0; // which avatar is currently selected

    // ── Labels on the Profile page that show user information ────
    private JLabel profileNameDisplay;
    private JLabel profileEmailDisplay;
    private JLabel profileRoleDisplay;

    // ── Text fields shown when editing personal information ───────
    private JTextField profileNameField;
    private JTextField profileEmailField;

    // ── Panels that swap between "display" and "edit" mode ───────
    private JPanel nameDisplayPanel;  // shows the name label (read mode)
    private JPanel nameEditPanel;     // shows the name text field (edit mode)
    private JPanel emailDisplayPanel; // shows the email label (read mode)
    private JPanel emailEditPanel;    // shows the email text field (edit mode)

    // ── Buttons for editing personal information ──────────────────
    private JButton editBtn;   // shown in read mode
    private JButton saveBtn;   // shown in edit mode
    private JButton cancelBtn; // shown in edit mode

    // ── Vehicle section ───────────────────────────────────────────
    private JPanel vehicleListPanel; // holds the list of vehicle rows
    private JPanel vehicleAddPanel;  // holds the "add new vehicle" form
    private final VehicleService vehicleService = new VehicleService();

    // ── Image storage — saves/loads profile picture and background ─
    private final ProfilePicStorage profilePictureService = new ProfilePicStorage();
    private final BackgroundImageStorage backgroundImageService = new BackgroundImageStorage();

    // ── The actual images shown on screen ─────────────────────────
    private BufferedImage profileImage = null; // null = show default blue smiley
    private BufferedImage bannerImage  = null; // null = show default blue gradient

    // ── References to the banner and profile picture components ───
    private JPanel profileBanner;
    private JLabel profilePicLabel;

    // ── Controller that handles saving and validating profile data ─
    private final CustomerProfileController profileController;

    // ── Brand colours used throughout the dashboard ───────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230); // main blue
    private static final Color BANNER_BLUE = new Color(100, 130, 240); // lighter blue for banner

    // ── Sidebar navigation items ──────────────────────────────────
    private static final String[] NAV_ITEMS = {
            "Profile",
            "Appointment Booking",
            "Service History",
            "Payment History",
            "My Feedback",
            "Staff Review"
    };

    // Helper method: converts a Unicode code point to a String emoji
    private static String navIcon(int codePoint) {
        return new StringBuilder().appendCodePoint(codePoint).toString();
    }

    // Icons for each navigation item
    private static final String[] NAV_ICONS = {
            navIcon(0x1F464), // 👤 person
            navIcon(0x1F4C5), // 📅 calendar
            navIcon(0x1F504), // 🔄 cycle
            navIcon(0x1F4B5), // 💵 money
            navIcon(0x1F4AC), // 💬 chat bubble
            "\u2605"          // ★ star
    };

    // ═══════════════════════════════════════════════════════════════
    // CONSTRUCTOR — builds the dashboard when the customer logs in
    // ═══════════════════════════════════════════════════════════════
    public CustomerDashboard(AppFrame app) {
        this.app = app;

        // Set up the profile controller.
        // We use an AppFrameAccessor so the controller (model) never
        // directly imports the AppFrame class (view) — keeps layers separate.
        profileController = new CustomerProfileController(
                app.getAccountService(),
                new CustomerProfileController.AppFrameAccessor() {
                    @Override public User   getLoggedInUserObj()        { return app.getLoggedInUserObj(); }
                    @Override public String getLoggedInUser()           { return app.getLoggedInUser(); }
                    @Override public void   setLoggedInUser(String n)   { app.setLoggedInUser(n); }
                    @Override public void   setLoggedInUserObj(User u)  { app.setLoggedInUserObj(u); }
                }
        );

        // Use BorderLayout: sidebar on the LEFT, everything else in the CENTER
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);

        // Right side contains the header (top) and the content pages (center)
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);

        // ContentPanel uses CardLayout so we can switch between pages
        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);

        // Add all the pages to the content panel
        contentPanel.add(buildProfilePage(), "Profile");
        contentPanel.add(buildPlaceholder("Appointment Booking", "Book and manage your service appointments.",         navIcon(0x1F4C5), 40), "Appointment Booking");
        contentPanel.add(buildPlaceholder("Service History",     "View your past service records and details.",        navIcon(0x1F504), 40), "Service History");
        contentPanel.add(buildPlaceholder("Payment History",     "Review your payment transactions and invoices.",     navIcon(0x1F4B5), 40), "Payment History");
        contentPanel.add(buildPlaceholder("My Feedback",         "View feedback on your individual appointments.",     navIcon(0x1F4AC), 40), "My Feedback");
        contentPanel.add(buildPlaceholder("Staff Review",        "Submit comments for counter staff and technicians.", "\u2605",         48), "Staff Review");

        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }

    // Called automatically by Java when this panel is added to the screen
    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    /**
     * Refreshes all displayed information using the currently logged-in user.
     * Called after login and after any profile changes.
     */
    public void refreshUser() {
        // Get the logged-in user's name
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) {
            name = "Customer"; // fallback if name is missing
        }

        // Update the name shown in the header
        if (profileLabel != null) {
            profileLabel.setText(name);
        }

        // Get the full User object (contains email, role, etc.)
        User user = app.getLoggedInUserObj();

        // Update the avatar icon index
        if (user != null) {
            selectedAvatarIndex = user.getProfilePicture();
        }

        // Repaint the avatar circle in the header
        if (avatarLabel != null) {
            avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]);
            avatarLabel.repaint();
        }

        // Update the labels on the Profile page
        if (user != null) {
            if (profileNameDisplay  != null) profileNameDisplay.setText(user.getName());
            if (profileEmailDisplay != null) profileEmailDisplay.setText(user.getEmail());
            if (profileRoleDisplay  != null) {
                // Capitalise the first letter of the role (e.g. "customer" → "Customer")
                String role = user.getRole();
                String capitalisedRole = role.substring(0, 1).toUpperCase() + role.substring(1);
                profileRoleDisplay.setText(capitalisedRole);
            }

            // Load the saved profile picture and background from disk
            profileImage = profilePictureService.loadImage(user.getEmail());
            bannerImage  = backgroundImageService.loadImage(user.getEmail());

            // Repaint the components so they show the loaded images
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (profileBanner   != null) profileBanner.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }

        // Reload the vehicle list
        refreshVehicleList();
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFILE PAGE
    // ═══════════════════════════════════════════════════════════════

    /** Builds the Profile page with a scroll pane. */
    private JPanel buildProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        // Wrap the content in a scroll pane so it scrolls vertically
        JScrollPane scroll = new JScrollPane(buildProfileInner());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // no horizontal scroll
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(16); // scroll speed

        page.add(scroll, BorderLayout.CENTER);
        return page;
    }

    /** Builds the inner content of the Profile page (banner + two columns). */
    private JPanel buildProfileInner() {
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS)); // stack vertically
        inner.setBackground(UIConstants.BG_CONTENT);
        inner.setBorder(new EmptyBorder(0, 0, 40, 0)); // padding at the bottom

        // Add the banner + profile picture hero section at the top
        inner.add(buildBannerHero());
        inner.add(Box.createVerticalStrut(24)); // gap between banner and body

        // Body uses BorderLayout: left column stretches, right column is fixed width
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(UIConstants.BG_CONTENT);
        body.setBorder(new EmptyBorder(0, 30, 0, 30)); // left and right padding

        // Left column: Personal Information card + My Vehicle card
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildPersonalInfoCard());
        leftColumn.add(Box.createVerticalStrut(16)); // gap between cards
        leftColumn.add(buildVehicleCard());
        body.add(leftColumn, BorderLayout.CENTER);

        // Right column: Upcoming Appointments + Payment History (fixed 440px wide)
        JPanel rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(440, 0));
        rightColumn.setMinimumSize(new Dimension(440, 0));
        rightColumn.setMaximumSize(new Dimension(440, Integer.MAX_VALUE));
        rightColumn.add(buildUpcomingCard());
        rightColumn.add(Box.createVerticalStrut(16)); // gap between cards
        rightColumn.add(buildPaymentSummaryCard());
        body.add(rightColumn, BorderLayout.EAST);

        inner.add(body);
        return inner;
    }

    // ═══════════════════════════════════════════════════════════════
    // BANNER + PROFILE PICTURE HERO
    // ═══════════════════════════════════════════════════════════════

    /**
     * Builds the banner (background image area) and the circular
     * profile picture that overlaps the bottom of the banner.
     * Clicking either one opens a file chooser to change the image.
     */
    private JPanel buildBannerHero() {
        // Use null layout so we can position banner and profile pic manually
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        // Track whether the mouse is hovering over the banner or avatar
        boolean[] bannerHovered = {false};
        boolean[] avatarHovered = {false};

        // ── Build the banner panel ────────────────────────────────
        profileBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw background image if one was chosen, otherwise draw blue gradient
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    // Default: gradient from light blue to darker blue
                    GradientPaint gradient = new GradientPaint(
                            0, 0, BANNER_BLUE,
                            getWidth(), getHeight(), new Color(60, 90, 210));
                    g2.setPaint(gradient);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }

                // When hovering: darken the banner and show camera icon + text
                if (bannerHovered[0]) {
                    g2.setColor(new Color(0, 0, 0, 110)); // semi-transparent black
                    g2.fillRect(0, 0, getWidth(), getHeight());

                    // Draw camera icon in the centre
                    int centerX = getWidth() / 2;
                    int centerY = getHeight() / 2 - 10;
                    drawCameraIcon(g2, centerX, centerY, 28, Color.WHITE);

                    // Draw "Click to change" text below the camera icon
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String message = "Click to change";
                    int textX = centerX - fm.stringWidth(message) / 2;
                    g2.drawString(message, textX, centerY + 44);
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
            @Override public void mouseClicked(MouseEvent e) { chooseBannerImage(); } // open file chooser
        });

        // ── Build the circular profile picture ────────────────────
        profilePicLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()); // circle size

                // Draw the profile image if one was chosen, otherwise draw default smiley
                if (profileImage != null) {
                    // Clip the image into a circle shape
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size, null);
                    g2.setClip(null);
                } else {
                    drawDefaultAvatar(g2, size); // blue circle with smiley face
                }

                // When hovering: darken the circle and show camera icon
                if (avatarHovered[0]) {
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillOval(0, 0, size, size);
                    g2.setClip(null);
                    drawCameraIcon(g2, size / 2, size / 2, 20, Color.WHITE);
                }

                // Always draw a white border ring around the circle
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
            @Override public void mouseClicked(MouseEvent e) { chooseProfileImage(); } // open file chooser
        });

        // Reposition banner and profile pic whenever the hero panel is resized
        hero.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                profileBanner.setBounds(0, 0, hero.getWidth(), 170);
                profilePicLabel.setBounds(30, 90, 110, 110);
            }
        });

        // Set initial positions
        profileBanner.setBounds(0, 0, 800, 170);
        profilePicLabel.setBounds(30, 90, 110, 110);

        // Add banner first (painted first = behind), then profile pic (on top)
        hero.add(profileBanner);
        hero.add(profilePicLabel);

        // Make sure profile pic is drawn on top of the banner
        hero.setComponentZOrder(profilePicLabel, 0); // 0 = front
        hero.setComponentZOrder(profileBanner,   1); // 1 = behind

        return hero;
    }

    /** Draws a simple camera icon at the given centre position. */
    private void drawCameraIcon(Graphics2D g2, int cx, int cy, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int bodyWidth  = size;
        int bodyHeight = size * 7 / 10;
        int bodyX = cx - bodyWidth / 2;
        int bodyY = cy - bodyHeight / 2;

        g2.drawRoundRect(bodyX, bodyY, bodyWidth, bodyHeight, size / 5, size / 5); // camera body
        int lensRadius = size * 22 / 100;
        g2.drawOval(cx - lensRadius, cy - lensRadius + size / 20, lensRadius * 2, lensRadius * 2); // lens
        g2.drawRoundRect(bodyX + size / 6, bodyY - size / 6, size / 4, size / 6, 2, 2); // viewfinder bump
    }

    /** Draws a blue circle with a white smiley face (the default profile picture). */
    private void drawDefaultAvatar(Graphics2D g2, int size) {
        // Draw the blue circle background
        g2.setColor(BRAND_BLUE);
        g2.fillOval(0, 0, size, size);

        // Draw white eyes and smile
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(size / 18f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        int eyeY      = size * 38 / 100;
        int eyeOffset = size * 18 / 100;
        int eyeRadius = size / 14;

        // Left eye
        g2.fillOval(size / 2 - eyeOffset - eyeRadius, eyeY - eyeRadius, eyeRadius * 2, eyeRadius * 2);
        // Right eye
        g2.fillOval(size / 2 + eyeOffset - eyeRadius, eyeY - eyeRadius, eyeRadius * 2, eyeRadius * 2);
        // Smile arc
        g2.drawArc(size * 28 / 100, size * 44 / 100, size * 44 / 100, size * 26 / 100, 200, 140);
    }

    // ═══════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION CARD
    // ═══════════════════════════════════════════════════════════════

    /**
     * Builds the Personal Information card.
     * Shows Username, Email, Role in read mode.
     * Clicking Edit switches to inline text fields for editing.
     */
    private JPanel buildPersonalInfoCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(28, 28, 28, 28));

        // ── Title row: "Personal Information" + buttons ───────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        JLabel titleLabel = new JLabel("Personal Information");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        // Button panel on the right side of the title row
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttonPanel.setOpaque(false);

        editBtn   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        saveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        cancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);

        // Make Cancel button wider so text is not clipped
        cancelBtn.setPreferredSize(new Dimension(90, 32));
        cancelBtn.setMinimumSize(new Dimension(90, 32));
        cancelBtn.setMaximumSize(new Dimension(90, 32));

        // Save and Cancel are hidden until Edit is clicked
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

        // ── Get the current user's details ────────────────────────
        User currentUser = app.getLoggedInUserObj();
        String currentName  = currentUser != null ? currentUser.getName()  : "—";
        String currentEmail = currentUser != null ? currentUser.getEmail() : "—";
        String currentRole  = currentUser != null ? currentUser.getRole()  : "—";

        // Capitalise the role
        if (!currentRole.equals("—")) {
            currentRole = currentRole.substring(0, 1).toUpperCase() + currentRole.substring(1);
        }

        // ── Username row (display mode) ───────────────────────────
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

        // ── Username row (edit mode — hidden by default) ──────────
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
        nameEditPanel.setVisible(false); // hidden until Edit is clicked

        card.add(nameDisplayPanel);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

        // ── Email row (display mode) ──────────────────────────────
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

        // ── Email row (edit mode — hidden by default) ─────────────
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
        emailEditPanel.setVisible(false); // hidden until Edit is clicked

        card.add(emailDisplayPanel);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));

        // ── Role row (always read-only, never editable) ───────────
        buildInfoRow(card, "Role", currentRole);
        card.add(Box.createVerticalStrut(4));

        // ── Wire up the button actions ────────────────────────────
        editBtn.addActionListener(e   -> enterEditMode());
        cancelBtn.addActionListener(e -> exitEditMode());
        saveBtn.addActionListener(e   -> handleSave());

        return card;
    }

    /** Switches to edit mode: shows text fields, hides labels. */
    private void enterEditMode() {
        // Fill in the current values so the user can edit from there
        profileNameField.setText(profileController.getCurrentName());
        profileEmailField.setText(profileController.getCurrentEmail());

        // Hide the display labels, show the text fields
        nameDisplayPanel.setVisible(false);
        emailDisplayPanel.setVisible(false);
        nameEditPanel.setVisible(true);
        emailEditPanel.setVisible(true);

        // Swap the buttons: hide Edit, show Save + Cancel
        editBtn.setVisible(false);
        saveBtn.setVisible(true);
        cancelBtn.setVisible(true);

        // Put the cursor in the name field so the user can start typing
        profileNameField.requestFocusInWindow();
    }

    /** Switches back to read mode: hides text fields, shows labels. */
    private void exitEditMode() {
        // Hide the text fields, show the display labels
        nameEditPanel.setVisible(false);
        emailEditPanel.setVisible(false);
        nameDisplayPanel.setVisible(true);
        emailDisplayPanel.setVisible(true);

        // Swap the buttons back: show Edit, hide Save + Cancel
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        editBtn.setVisible(true);
    }

    /** Called when the Save button is clicked. Validates and saves the profile. */
    private void handleSave() {
        String newName  = profileNameField.getText().trim();
        String newEmail = profileEmailField.getText().trim();

        // Check if anything actually changed
        if (profileController.hasNoChanges(newName, newEmail)) {
            JOptionPane.showMessageDialog(app, "No changes were made.",
                    "No Changes", JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }

        // Try to save — the controller handles validation
        try {
            boolean saved = profileController.saveProfile(newName, newEmail);
            if (saved) {
                exitEditMode();
                refreshUser(); // update all displayed labels
                JOptionPane.showMessageDialog(app, "Profile updated successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app, "Failed to save. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            // The controller threw a validation error — show it to the user
            JOptionPane.showMessageDialog(app, ex.getMessage(),
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

    /**
     * Helper: adds one field row (label + value) to a card.
     * Also assigns the value label to profileRoleDisplay if the field is "Role".
     */
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

        // Keep a reference to the Role label so refreshUser() can update it
        if ("Role".equals(fieldName)) {
            profileRoleDisplay = valueLabel;
        }

        return valueLabel;
    }

    // ═══════════════════════════════════════════════════════════════
    // MY VEHICLE CARD
    // ═══════════════════════════════════════════════════════════════

    /** Builds the My Vehicle card with a list of vehicles and an Add button. */
    private JPanel buildVehicleCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(22, 28, 22, 28));

        // ── Title row: "My Vehicle" + "+ Add" button ──────────────
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

        // Panel that holds all the vehicle rows
        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        card.add(vehicleListPanel);

        // Build the add form but keep it hidden until "+ Add" is clicked
        vehicleAddPanel = buildVehicleAddForm();
        vehicleAddPanel.setVisible(false);
        card.add(vehicleAddPanel);

        // Toggle the add form when the + Add button is clicked
        addButton.addActionListener(e -> {
            boolean currentlyVisible = vehicleAddPanel.isVisible();
            vehicleAddPanel.setVisible(!currentlyVisible); // flip visibility
            card.revalidate();
            card.repaint();
        });

        return card;
    }

    /**
     * Reads vehicles from vehicles.txt and rebuilds the vehicle list.
     * Shows a maximum of 3 vehicles.
     * Called on login and after add/remove operations.
     */
    private void refreshVehicleList() {
        if (vehicleListPanel == null) return;

        // Remove all existing rows first
        vehicleListPanel.removeAll();

        User user = app.getLoggedInUserObj();

        if (user == null) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
        } else {
            List<String[]> vehicles = vehicleService.getVehiclesByEmail(user.getEmail());

            if (vehicles.isEmpty()) {
                vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
            } else {
                // Only show the first 3 vehicles (top 3)
                int numberOfVehiclesToShow = Math.min(3, vehicles.size());

                for (int i = 0; i < numberOfVehiclesToShow; i++) {
                    String[] vehicle = vehicles.get(i);
                    // vehicle array: [vehicleID, plate, brand, year, colour]
                    String vehicleID = vehicle[0];
                    String plate     = vehicle[1];
                    String brand     = vehicle[2];
                    String year      = vehicle[3];
                    String colour    = vehicle[4];

                    vehicleListPanel.add(buildVehicleRow(vehicleID, plate, brand, year, colour));

                    // Add a gap between rows (except after the last one)
                    if (i < numberOfVehiclesToShow - 1) {
                        vehicleListPanel.add(Box.createVerticalStrut(10));
                    }
                }
            }
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
    }

    /**
     * Builds the inline "Add Vehicle" form that appears when + Add is clicked.
     * Has fields for Car Plate, Brand/Model, Year, Colour.
     * Save writes to vehicles.txt via VehicleService.
     */
    private JPanel buildVehicleAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(10, 14, 10, 14)));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));

        // ── Four empty input fields ────────────────────────────────
        JPanel fieldsPanel = new JPanel(new GridLayout(1, 4, 6, 0));
        fieldsPanel.setOpaque(false);

        JTextField plateField  = makeCompactField("");
        JTextField brandField  = makeCompactField("");
        JTextField yearField   = makeCompactField("");
        JTextField colourField = makeCompactField("");

        fieldsPanel.add(makeLabelledField("Car Plate",    plateField));
        fieldsPanel.add(makeLabelledField("Brand / Model", brandField));
        fieldsPanel.add(makeLabelledField("Year",          yearField));
        fieldsPanel.add(makeLabelledField("Colour",        colourField));
        form.add(fieldsPanel, BorderLayout.CENTER);

        // ── Save and Cancel buttons ────────────────────────────────
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

        // Cancel: clear all fields and hide the form
        cancelBtn.addActionListener(e -> {
            plateField.setText("");
            brandField.setText("");
            yearField.setText("");
            colourField.setText("");
            vehicleAddPanel.setVisible(false);
        });

        // Save: validate inputs then call VehicleService to write to vehicles.txt
        saveBtn.addActionListener(e -> {
            User user = app.getLoggedInUserObj();
            if (user == null) return;

            // Get the text from each field
            String plate  = plateField.getText().trim();
            String brand  = brandField.getText().trim();
            String year   = yearField.getText().trim();
            String colour = colourField.getText().trim();

            // Validate all fields using shared validation method
            String validationError = validateVehicleFields(plate, brand, year, colour);
            if (validationError != null) {
                JOptionPane.showMessageDialog(app, validationError,
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // All valid — save to vehicles.txt via VehicleService
            boolean saved = vehicleService.addVehicle(user.getEmail(), plate, brand, year, colour);

            if (saved) {
                // Clear fields and hide the form
                plateField.setText("");
                brandField.setText("");
                yearField.setText("");
                colourField.setText("");
                vehicleAddPanel.setVisible(false);
                refreshVehicleList(); // reload the list to show the new vehicle
            } else {
                JOptionPane.showMessageDialog(app, "Failed to add vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return form;
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE VALIDATION
    // ═══════════════════════════════════════════════════════════════

    /**
     * Validates all four vehicle fields.
     * Returns an error message if any field is invalid, or null if all valid.
     *
     * Rules:
     *  Car Plate   — must have BOTH letters and numbers, no special characters
     *                e.g. "WXY1234" ✅   "ABC" ❌   "1234" ❌   "WXY-123" ❌
     *  Brand/Model — letters and/or numbers only, no special characters
     *                e.g. "Toyota Vios" ✅   "BMW i5" ✅   "BMW-i5" ❌
     *  Year        — exactly 4 digits, numbers only
     *                e.g. "2025" ✅   "25" ❌   "202A" ❌
     *  Colour      — letters only (spaces allowed), no numbers or special characters
     *                e.g. "White" ✅   "Dark Blue" ✅   "Blue2" ❌   "Blue-Red" ❌
     */
    private String validateVehicleFields(String plate, String brand, String year, String colour) {
        // ── Car Plate ─────────────────────────────────────────────
        if (plate.isEmpty()) {
            return "Car Plate cannot be empty.";
        }
        if (!plate.matches("[a-zA-Z0-9 ]+")) {
            return "Car Plate can only contain letters and numbers (no special characters).";
        }
        boolean plateHasLetter = plate.matches(".*[a-zA-Z].*");
        boolean plateHasNumber = plate.matches(".*[0-9].*");
        if (!plateHasLetter || !plateHasNumber) {
            return "Car Plate must contain both letters and numbers (e.g. WXY1234).";
        }

        // ── Brand / Model ─────────────────────────────────────────
        if (brand.isEmpty()) {
            return "Brand / Model cannot be empty.";
        }
        if (!brand.matches("[a-zA-Z0-9 ]+")) {
            return "Brand / Model can only contain letters and numbers (no special characters).";
        }

        // ── Year ──────────────────────────────────────────────────
        if (year.isEmpty()) {
            return "Year cannot be empty.";
        }
        if (!year.matches("\\d{4}")) {
            return "Year must be exactly 4 digits (e.g. 2025).";
        }

        // ── Colour ────────────────────────────────────────────────
        if (colour.isEmpty()) {
            return "Colour cannot be empty.";
        }
        if (!colour.matches("[a-zA-Z ]+")) {
            return "Colour can only contain letters (e.g. White, Dark Blue).";
        }

        return null; // null = all fields are valid
    }

    // ═══════════════════════════════════════════════════════════════
    // IMAGE CHOOSERS
    // ═══════════════════════════════════════════════════════════════

    /**
     * Opens the native OS file chooser so the user can pick a profile picture.
     * Saves the image to src/ProfilePic/{email}.jpg and repaints immediately.
     */
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        // Open the native Windows file chooser
        FileDialog fileChooser = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Profile Picture",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp"); // filter by image types
        fileChooser.setVisible(true);

        // If the user cancelled, do nothing
        if (fileChooser.getFile() == null) return;

        try {
            java.io.File selectedFile = new java.io.File(fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save the image to the ProfilePic folder via ProfilePicStorage
            boolean saved = profilePictureService.saveImage(user.getEmail(), image);
            if (!saved) {
                JOptionPane.showMessageDialog(app, "Failed to save profile picture.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update in memory and repaint immediately
            profileImage = image;
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Opens the native OS file chooser so the user can pick a background image.
     * Saves the image to src/BackgroundImg/{email}.jpg and repaints immediately.
     */
    private void chooseBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        FileDialog fileChooser = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Background Image",
                FileDialog.LOAD);
        fileChooser.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fileChooser.setVisible(true);

        if (fileChooser.getFile() == null) return;

        try {
            java.io.File selectedFile = new java.io.File(fileChooser.getDirectory(), fileChooser.getFile());
            BufferedImage image = ImageIO.read(selectedFile);

            if (image == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Save via BackgroundImageStorage
            boolean saved = backgroundImageService.saveImage(user.getEmail(), image);
            if (!saved) {
                JOptionPane.showMessageDialog(app, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Update in memory and repaint the banner immediately
            bannerImage = image;
            if (profileBanner != null) profileBanner.repaint();

        } catch (java.io.IOException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // VEHICLE ROW
    // ═══════════════════════════════════════════════════════════════

    /**
     * Builds one vehicle row.
     * Uses CardLayout to switch between display mode and edit mode
     * without changing the size of the row box.
     *
     * vehicleID — stored in vehicles.txt, used for delete (not shown on screen)
     * plate     — car plate number
     * brand     — car brand/model
     * year      — year of manufacture
     * colour    — colour of the car
     */
    private JPanel buildVehicleRow(String vehicleID, String plate,
                                   String brand, String year, String colour) {
        // Outer box — fixed height, always stays the same size
        JPanel outerBox = new JPanel(new BorderLayout());
        outerBox.setOpaque(false);
        outerBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        outerBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(10, 14, 10, 14)));

        // CardLayout: switches between "display" card and "edit" card
        CardLayout switcher = new CardLayout();
        JPanel switcherPanel = new JPanel(switcher);
        switcherPanel.setOpaque(false);

        // ── DISPLAY MODE card ─────────────────────────────────────
        JPanel displayCard = new JPanel(new BorderLayout(8, 0));
        displayCard.setOpaque(false);

        // Car emoji icon on the left
        JLabel carEmoji = new JLabel(new StringBuilder().appendCodePoint(0x1F697).toString());
        carEmoji.setFont(new Font("SansSerif", Font.PLAIN, 26));
        carEmoji.setVerticalAlignment(SwingConstants.CENTER);
        displayCard.add(carEmoji, BorderLayout.WEST);

        // Info: Brand on top, Plate · Year · Colour on bottom
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel brandLine = new JLabel(brand);
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

        // Edit and Remove buttons on the right
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

        // ── EDIT MODE card ────────────────────────────────────────
        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);

        // Four compact fields in a row
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

        // Save and Cancel buttons on the right of the edit card
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

        // Add both cards to the switcher
        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display"); // start in display mode

        outerBox.add(switcherPanel, BorderLayout.CENTER);

        // ── Button actions ────────────────────────────────────────

        // Edit button: switch to edit mode and fill in current values
        editButton.addActionListener(e -> {
            editPlateField.setText(plate);
            editBrandField.setText(brand);
            editYearField.setText(year);
            editColourField.setText(colour);
            switcher.show(switcherPanel, "edit");
        });

        // Cancel button: go back to display mode without saving
        cancelVehicleBtn.addActionListener(e -> {
            switcher.show(switcherPanel, "display");
        });

        // Save button: validate, update vehicles.txt IN PLACE, refresh the list
        saveVehicleBtn.addActionListener(e -> {
            String newPlate  = editPlateField.getText().trim();
            String newBrand  = editBrandField.getText().trim();
            String newYear   = editYearField.getText().trim();
            String newColour = editColourField.getText().trim();

            // Check if anything changed
            boolean nothingChanged = newPlate.equals(plate) && newBrand.equals(brand)
                    && newYear.equals(year) && newColour.equals(colour);
            if (nothingChanged) {
                JOptionPane.showMessageDialog(app, "No changes were made.",
                        "No Changes", JOptionPane.INFORMATION_MESSAGE);
                switcher.show(switcherPanel, "display");
                return;
            }

            // Validate all fields before saving
            String validationError = validateVehicleFields(newPlate, newBrand, newYear, newColour);
            if (validationError != null) {
                JOptionPane.showMessageDialog(app, validationError,
                        "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Update the record in place — preserves the original order in vehicles.txt
            User user = app.getLoggedInUserObj();
            if (user != null) {
                boolean updated = vehicleService.updateVehicle(
                        user.getEmail(), plate, newPlate, newBrand, newYear, newColour);
                if (updated) {
                    refreshVehicleList();
                } else {
                    JOptionPane.showMessageDialog(app, "Failed to update vehicle.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Remove button: ask for confirmation, then delete from vehicles.txt
        removeButton.addActionListener(e -> {
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
            // If the user clicked No, just close the dialog — nothing happens
        });

        return outerBox;
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS CARD
    // ═══════════════════════════════════════════════════════════════

    /** Builds the Upcoming Appointments card (top-right). */
    private JPanel buildUpcomingCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        // Title row: "Upcoming Appointments" on the left, "Top 3" on the right
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);

        JLabel topThreeHint = new JLabel("Top 3");
        topThreeHint.setFont(UIConstants.FONT_SMALL);
        topThreeHint.setForeground(UIConstants.TEXT_MUTED);
        titleRow.add(topThreeHint, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        // Placeholder message — replace with real data later
        JLabel emptyMessage = new JLabel("No upcoming appointments.");
        emptyMessage.setFont(UIConstants.FONT_BODY);
        emptyMessage.setForeground(UIConstants.TEXT_SECONDARY);
        emptyMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(emptyMessage);
        card.add(Box.createVerticalStrut(4));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // PAYMENT HISTORY CARD
    // ═══════════════════════════════════════════════════════════════

    /** Builds the Payment History card (bottom-right). */
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

        JLabel topThreeHint = new JLabel("Top 3");
        topThreeHint.setFont(UIConstants.FONT_SMALL);
        topThreeHint.setForeground(UIConstants.TEXT_MUTED);
        titleRow.add(topThreeHint, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        JLabel emptyMessage = new JLabel("No payment records found.");
        emptyMessage.setFont(UIConstants.FONT_BODY);
        emptyMessage.setForeground(UIConstants.TEXT_SECONDARY);
        emptyMessage.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(emptyMessage);
        card.add(Box.createVerticalStrut(4));

        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // HEADER
    // ═══════════════════════════════════════════════════════════════

    /** Builds the top header bar with the page title and user avatar. */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));

        // Page title on the left (e.g. "Profile", "Appointment Booking")
        headerTitle = new JLabel("Profile");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);

        // User avatar area on the right
        header.add(buildHeaderProfileArea(), BorderLayout.EAST);

        return header;
    }

    /**
     * Builds the avatar circle + name + dropdown arrow on the right side of the header.
     * Hovering shows a popup menu with "View Profile" and "Logout".
     */
    private JPanel buildHeaderProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        // The small avatar circle in the header
        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (profileImage != null) {
                    // Show the user's profile photo clipped to a circle
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38, null);
                    g2.setClip(null);
                } else {
                    // Show the coloured circle with icon
                    g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
                    g2.fillOval(0, 0, 38, 38);
                    g2.dispose();
                    super.paintComponent(g); // draws the icon text on top
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

        // Customer name label
        profileLabel = new JLabel("—");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        // Small dropdown arrow
        JLabel dropdownArrow = new JLabel("\u25BE");
        dropdownArrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dropdownArrow.setForeground(UIConstants.TEXT_MUTED);

        // Clickable area containing avatar + name + arrow
        JPanel profileButton = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileButton.setBackground(UIConstants.BG_HEADER);
        profileButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileButton.add(avatarLabel);
        profileButton.add(profileLabel);
        profileButton.add(dropdownArrow);

        // Popup menu shown when hovering
        JPopupMenu dropdownMenu = new JPopupMenu();
        dropdownMenu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 220, 225), 1),
                new EmptyBorder(6, 0, 6, 0)));
        dropdownMenu.setBackground(Color.WHITE);

        JMenuItem viewProfileItem = createMenuItem("View Profile");
        viewProfileItem.addActionListener(e -> {
            // Navigate to the Profile page
            activeNav = "Profile";
            headerTitle.setText("Profile");
            contentLayout.show(contentPanel, "Profile");
        });

        JMenuItem logoutItem = createMenuItem("Logout");
        logoutItem.setForeground(UIConstants.TEXT_DANGER);
        logoutItem.addActionListener(e -> {
            // Clear the session and go back to the login screen
            app.setLoggedInUser("");
            app.setLoggedInUserObj(null);
            app.showPage(PageName.ONBOARDING);
        });

        dropdownMenu.add(viewProfileItem);
        dropdownMenu.add(logoutItem);

        // Show the dropdown when the mouse enters the profile button area
        profileButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                dropdownMenu.show(profileButton,
                        profileButton.getWidth() - dropdownMenu.getPreferredSize().width,
                        profileButton.getHeight());
            }
        });

        // Hide the dropdown when the mouse leaves the menu
        dropdownMenu.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                Point mousePos = e.getPoint();
                if (!dropdownMenu.getBounds().contains(mousePos)) {
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

    /** Builds the left sidebar with the logo and navigation buttons. */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(UIConstants.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 0));

        // ── Logo area at the top ──────────────────────────────────
        JPanel logoArea = new JPanel();
        logoArea.setLayout(new BoxLayout(logoArea, BoxLayout.Y_AXIS));
        logoArea.setBackground(UIConstants.SIDEBAR_BG);
        logoArea.setBorder(new EmptyBorder(25, 20, 25, 20));
        logoArea.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 100));

        ImageIcon logoIcon = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image scaledLogo = logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledLogo));
        logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandName = new JLabel("APU ASC");
        brandName.setFont(UIConstants.FONT_SIDEBAR);
        brandName.setForeground(Color.WHITE);
        brandName.setAlignmentX(Component.LEFT_ALIGNMENT);
        brandName.setBorder(new EmptyBorder(8, 0, 0, 0));

        logoArea.add(logoLabel);
        logoArea.add(brandName);
        sidebar.add(logoArea);

        // Divider line
        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.SIDEBAR_DIVIDER);
        divider.setBackground(UIConstants.SIDEBAR_BG);
        divider.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(divider);
        sidebar.add(Box.createVerticalStrut(10));

        // "MENU" label
        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setFont(UIConstants.FONT_LABEL);
        menuLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        menuLabel.setBorder(new EmptyBorder(10, 24, 10, 20));
        menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35));
        sidebar.add(menuLabel);

        // ── Navigation buttons ────────────────────────────────────
        JButton[] navButtons = new JButton[NAV_ITEMS.length];

        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String pageName = NAV_ITEMS[i];

            // Staff Review uses HTML to make the star icon bigger
            String buttonText = pageName.equals("Staff Review")
                    ? "<html><font size='5'>\u2605</font>&nbsp;&nbsp;&nbsp;" + pageName + "</html>"
                    : NAV_ICONS[i] + "   " + pageName;

            navButtons[i] = createNavButton(buttonText, pageName.equals(activeNav));

            navButtons[i].addActionListener(e -> {
                activeNav = pageName; // update which page is active

                // Update the style of all nav buttons
                for (int j = 0; j < navButtons.length; j++) {
                    boolean isActive = NAV_ITEMS[j].equals(activeNav);
                    updateNavButtonStyle(navButtons[j], isActive);
                }

                // Switch to the selected page
                headerTitle.setText(pageName);
                contentLayout.show(contentPanel, pageName);
            });

            sidebar.add(navButtons[i]);
            sidebar.add(Box.createVerticalStrut(2));
        }

        sidebar.add(Box.createVerticalGlue()); // push nav buttons to the top
        return sidebar;
    }

    /** Creates a styled sidebar navigation button. */
    private JButton createNavButton(String text, boolean isActive) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (getClientProperty("active") == Boolean.TRUE) {
                    // Active page: draw a rounded highlight
                    g2.setColor(UIConstants.SIDEBAR_ACTIVE);
                    g2.fillRoundRect(4, 0, getWidth() - 8, getHeight(), 8, 8);
                } else if (getModel().isRollover()) {
                    // Hover: draw a lighter rounded highlight
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

    /** Updates a nav button's appearance based on whether it is the active page. */
    private void updateNavButtonStyle(JButton btn, boolean isActive) {
        btn.putClientProperty("active", isActive);
        btn.setForeground(isActive ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", isActive ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }

    // ═══════════════════════════════════════════════════════════════
    // PLACEHOLDER PAGES (for nav items not yet built)
    // ═══════════════════════════════════════════════════════════════

    /**
     * Builds a placeholder page shown for sections that are not yet implemented.
     * Shows a large icon, a title, and a description in the centre.
     */
    private JPanel buildPlaceholder(String title, String description, String icon, int iconSize) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);

        // Centre the card in the middle of the page
        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setBackground(UIConstants.BG_CONTENT);

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
    // SHARED UI HELPER METHODS
    // ═══════════════════════════════════════════════════════════════

    /** Creates a white rounded card panel used as a container. */
    private JPanel createCard() {
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
        return card;
    }

    /** Creates a thin horizontal separator line. */
    private JSeparator makeSeparator() {
        JSeparator separator = new JSeparator();
        separator.setForeground(UIConstants.BORDER_DEFAULT);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return separator;
    }

    /**
     * Creates a small compact text field used inside vehicle rows.
     * Pre-filled with the given value.
     */
    private JTextField makeCompactField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("SansSerif", Font.PLAIN, 11));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 1),
                new EmptyBorder(2, 4, 2, 4)));
        return field;
    }

    /**
     * Creates a panel with a small label above a text field.
     * Used inside vehicle rows for each editable field.
     */
    private JPanel makeLabelledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 2));
        panel.setOpaque(false);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font("SansSerif", Font.BOLD, 10));
        labelComponent.setForeground(UIConstants.TEXT_MUTED);

        panel.add(labelComponent, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    /** Creates a centred label shown when there are no items to display. */
    private JLabel makeEmptyLabel(String message) {
        JLabel label = new JLabel(message);
        label.setFont(UIConstants.FONT_BODY);
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    /**
     * Creates a coloured rounded button used for actions like Edit, Save, Remove.
     * Changes to a darker shade when the mouse hovers over it.
     */
    private JButton createActionButton(String text, Color backgroundColor, Color textColor) {
        JButton btn = new JButton(text) {
            private boolean isHovered = false;

            {
                // Track hover state for the darker colour effect
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { isHovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { isHovered = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Use a darker shade when hovered
                Color paintColor = isHovered ? backgroundColor.darker() : backgroundColor;
                g2.setColor(paintColor);
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

    /** Creates a styled dropdown menu item. */
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
