package view;

import model.AppointmentService;
import model.CustomerProfileController;
import model.PaymentService;
import model.ServiceHistoryService;
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
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Set;

/**
 * CustomerDashboard — the main profile screen customers see after logging in.
 *
 * FIXES IN THIS VERSION:
 * 1. rebuildVehicleList() now receives ALL vehicles (VehicleSectionController
 *    no longer limits to 3). This method shows top 3 and decides whether to
 *    show the "View All" button — which previously never appeared because the
 *    controller was cutting the list before passing it here.
 *
 * 2. Pay button is now wide enough to show the full word "Pay" (not "P...").
 *
 * 3. Invoice popup is compact — no horizontal scrollbar, all info visible at once.
 *
 * 4. Enter key in vehicle edit fields triggers the Save button.
 */
public class CustomerDashboard extends JPanel {

    // ── App reference ─────────────────────────────────────────────
    private final AppFrame app;

    // ── Page switching ────────────────────────────────────────────
    private CardLayout contentLayout;
    private JPanel     contentPanel;
    private String     activeNav = "Profile";

    // ── Header labels ─────────────────────────────────────────────
    private JLabel profileLabel;
    private JLabel avatarLabel;
    private JLabel headerTitle;

    // ── Avatar options ────────────────────────────────────────────
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230), new Color(230, 80, 80),
            new Color(80, 190, 110), new Color(230, 160, 40),
            new Color(160, 80, 230), new Color(40, 180, 200),
            new Color(230, 80, 160), new Color(100, 100, 120),
    };
    private static final String[] AVATAR_ICONS = {
            "\u263A", "\u2605", "\u2665", "\u2666",
            "\u263C", "\u2708", "\u266B", "\u2618"
    };
    private int selectedAvatarIndex = 0;

    // ── Profile card display labels ───────────────────────────────
    private JLabel profileNameDisplay;
    private JLabel profileEmailDisplay;
    private JLabel profileRoleDisplay;

    // ── Profile card edit fields ──────────────────────────────────
    private JTextField profileNameField;
    private JTextField profileEmailField;

    // ── Profile card panels (read/edit mode swap) ─────────────────
    private JPanel nameDisplayPanel;
    private JPanel nameEditPanel;
    private JPanel emailDisplayPanel;
    private JPanel emailEditPanel;

    // ── Profile card buttons ──────────────────────────────────────
    private JButton editBtn;
    private JButton saveBtn;
    private JButton cancelBtn;

    // ── Vehicle section ───────────────────────────────────────────
    private JPanel    vehicleListPanel;
    private JPanel    vehicleAddPanel;
    private JButton[] navButtons;

    // ── Section pages ─────────────────────────────────────────────
    private ServiceHistoryPage serviceHistoryPage;
    private PaymentHistoryPage paymentHistoryPage;
    private StaffReviewPage    staffReviewPage;
    private MyFeedbackPage     myFeedbackPage;

    // ── Services ──────────────────────────────────────────────────
    private final CustomerProfileController profileController;
    private final VehicleSectionController  vehicleController;
    private final VehicleService            vehicleService        = new VehicleService();
    private final ProfilePicStorage         profilePicStorage     = new ProfilePicStorage();
    private final BackgroundImageStorage    backgroundStorage     = new BackgroundImageStorage();
    private final AppointmentService        appointmentService    = new AppointmentService();
    private final PaymentService            paymentService        = new PaymentService();
    private final ServiceHistoryService     serviceHistoryService = new ServiceHistoryService();

    // ── Images ────────────────────────────────────────────────────
    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;
    private JPanel        profileBanner;
    private JLabel        profilePicLabel;

    // ── Brand colours ─────────────────────────────────────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);

    // ── Sidebar navigation ────────────────────────────────────────
    private static final String[] NAV_ITEMS = {
            "Profile", "Service History", "Payment History", "Staff Review", "My Feedback"
    };
    private static String navIcon(int cp) { return new StringBuilder().appendCodePoint(cp).toString(); }
    private static final String[] NAV_ICONS = {
            navIcon(0x1F464), navIcon(0x1F504), navIcon(0x1F4B5), "\u2605", navIcon(0x1F4AC)
    };

    // ── RIGHT COLUMN — class field so refreshUser() can rebuild it ─
    // This MUST be a class field. If it were a local variable inside
    // buildProfileInner(), Java would forget it after that method ends
    // and refreshUser() would have no way to update the cards.
    private JPanel rightColumn;

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
                    @Override public User   getLoggedInUser() { return app.getLoggedInUserObj(); }
                    // rebuildList receives ALL vehicles — the dashboard limits display to 3
                    @Override public void   rebuildList(List<String[]> v) { rebuildVehicleList(v); }
                    @Override public void   showMessage(String msg, String title, int type) {
                        JOptionPane.showMessageDialog(app, msg, title, type);
                    }
                    @Override public java.awt.Window getWindow() {
                        return SwingUtilities.getWindowAncestor(CustomerDashboard.this);
                    }
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

        contentPanel.add(buildProfilePage(), "Profile");

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

    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }

    // ─────────────────────────────────────────────────────────────
    // refreshUser() — rebuilds everything after login or any change
    // ─────────────────────────────────────────────────────────────
    public void refreshUser() {
        // Step 1: auto-complete any appointments whose time has passed
        User user = app.getLoggedInUserObj();
        if (user != null) {
            appointmentService.autoCompleteExpiredAppointments(serviceHistoryService);
        }

        // Step 2: update header name and avatar icon
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Customer";
        if (profileLabel != null) profileLabel.setText(name);

        if (user != null) selectedAvatarIndex = user.getProfilePicture();
        if (avatarLabel != null) { avatarLabel.setText(AVATAR_ICONS[selectedAvatarIndex]); avatarLabel.repaint(); }

        // Step 3: update profile card labels
        if (user != null) {
            if (profileNameDisplay  != null) profileNameDisplay.setText(user.getName());
            if (profileEmailDisplay != null) profileEmailDisplay.setText(user.getEmail());
            if (profileRoleDisplay  != null) {
                String role = user.getRole();
                profileRoleDisplay.setText(role.substring(0, 1).toUpperCase() + role.substring(1));
            }
            profileImage = profilePicStorage.loadImage(user.getUserId());
            bannerImage  = backgroundStorage.loadImage(user.getUserId());
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (profileBanner   != null) profileBanner.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }

        // Step 4: rebuild vehicle list
        vehicleController.refreshList();

        // Step 5: rebuild the right column (Upcoming + Pending Payment cards)
        // rightColumn is a CLASS FIELD so we can reach it from here.
        if (rightColumn != null) {
            rightColumn.removeAll();
            rightColumn.add(buildUpcomingCard());
            rightColumn.add(Box.createVerticalStrut(16));
            rightColumn.add(buildPaymentSummaryCard());
            rightColumn.revalidate();
            rightColumn.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // rebuildVehicleList() — called by VehicleSectionController
    //
    // FIX EXPLAINED:
    // VehicleSectionController.refreshList() now passes ALL vehicles.
    // This method shows the FIRST 3 in the scrollable area.
    // If there are MORE than 3, it shows a "View All (N)" link below.
    // Before this fix, the controller was passing only 3, so this method
    // could never know there were more — the "View All" button never appeared.
    // ─────────────────────────────────────────────────────────────
    private void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;
        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered."));
        } else {
            // Show only the FIRST 3 vehicles in the fixed-height scroll area
            int displayCount = Math.min(3, vehicles.size());
            for (int i = 0; i < displayCount; i++) {
                String[] v = vehicles.get(i);
                vehicleListPanel.add(buildVehicleRow(v[0], v[1], v[2], v[3], v[4]));
                if (i < displayCount - 1) vehicleListPanel.add(Box.createVerticalStrut(10));
            }
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();

        // Find the "View All" placeholder row inside the vehicle card
        // and populate it if there are more than 3 vehicles
        JPanel viewAllRow = findNamedPanel("vehicleViewAllRow");
        if (viewAllRow != null) {
            viewAllRow.removeAll();
            if (vehicles.size() > 3) {
                // Show the "View All (N)" link button
                JButton viewAllBtn = createTextLinkButton("View All (" + vehicles.size() + ")");
                final List<String[]> all = vehicles; // capture for use inside lambda
                viewAllBtn.addActionListener(e -> showViewAllVehiclesDialog(all));
                viewAllRow.add(viewAllBtn);
            }
            viewAllRow.revalidate();
            viewAllRow.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // findNamedPanel() — walks up the component tree from vehicleListPanel
    // and searches sibling components for a JPanel with the given name.
    // Used to find the "vehicleViewAllRow" placeholder after it is built.
    // ─────────────────────────────────────────────────────────────
    private JPanel findNamedPanel(String name) {
        if (vehicleListPanel == null) return null;
        Component comp = vehicleListPanel;
        // Walk up at most 6 levels of parents
        for (int level = 0; level < 6 && comp != null; level++) {
            comp = comp.getParent();
            if (comp instanceof JPanel) {
                // Search the direct children of this panel
                for (Component child : ((JPanel) comp).getComponents()) {
                    if (child instanceof JPanel && name.equals(((JPanel) child).getName())) {
                        return (JPanel) child;
                    }
                }
            }
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFILE PAGE
    // ═══════════════════════════════════════════════════════════════

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

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(UIConstants.BG_CONTENT);
        body.setBorder(new EmptyBorder(0, 30, 0, 30));

        // Left column: Personal Info + My Vehicle
        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildPersonalInfoCard());
        leftColumn.add(Box.createVerticalStrut(16));
        leftColumn.add(buildVehicleCard());
        body.add(leftColumn, BorderLayout.CENTER);

        // Right column: Upcoming Appointments + Pending Payment
        // NOTE: we assign to the CLASS FIELD — not a new local JPanel variable.
        // This is essential so refreshUser() can call rightColumn.removeAll() later.
        rightColumn = new JPanel();
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

    // ── Banner + overlapping profile picture ──────────────────────
    private JPanel buildBannerHero() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHovered = {false};
        boolean[] avatarHovered = {false};

        profileBanner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, BANNER_BLUE, getWidth(), getHeight(), new Color(60, 90, 210)));
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
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    int imgW = profileImage.getWidth(), imgH = profileImage.getHeight();
                    int crop = Math.min(imgW, imgH);
                    int cropX = (imgW - crop) / 2, cropY = (imgH - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size, cropX, cropY, cropX + crop, cropY + crop, null);
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
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
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
        g2.drawRoundRect(bx + size / 6, by - size / 6, size / 4, size / 6, 2, 2);
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

    // ── Personal Information card ─────────────────────────────────
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

        User u = app.getLoggedInUserObj();
        String currentName  = u != null ? u.getName()  : "—";
        String currentEmail = u != null ? u.getEmail() : "—";
        String currentRole  = u != null ? u.getRole()  : "—";
        if (!currentRole.equals("—"))
            currentRole = currentRole.substring(0, 1).toUpperCase() + currentRole.substring(1);

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
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2), new EmptyBorder(4, 8, 4, 8)));
        nameEditPanel.add(nameLabelEdit, BorderLayout.WEST);
        nameEditPanel.add(profileNameField, BorderLayout.CENTER);
        nameEditPanel.setVisible(false);

        card.add(nameDisplayPanel);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

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
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2), new EmptyBorder(4, 8, 4, 8)));
        emailEditPanel.add(emailLabelEdit, BorderLayout.WEST);
        emailEditPanel.add(profileEmailField, BorderLayout.CENTER);
        emailEditPanel.setVisible(false);

        card.add(emailDisplayPanel);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));
        buildInfoRow(card, "Role", currentRole);
        card.add(Box.createVerticalStrut(4));

        editBtn.addActionListener(e   -> enterEditMode());
        cancelBtn.addActionListener(e -> exitEditMode());
        saveBtn.addActionListener(e   -> handleSave());

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
            JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes", JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }
        try {
            boolean saved = profileController.saveProfile(newName, newEmail);
            if (saved) {
                exitEditMode();
                refreshUser();
                JOptionPane.showMessageDialog(app, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app, "Failed to save. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(app, ex.getMessage(), "Validation Error", JOptionPane.WARNING_MESSAGE);
        }
    }

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

    // ═══════════════════════════════════════════════════════════════
    // MY VEHICLE CARD
    // Shows TOP 3 vehicles. A "View All (N)" link appears when > 3.
    // ═══════════════════════════════════════════════════════════════
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

        JPanel titleButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        titleButtons.setOpaque(false);
        JButton addButton = createActionButton("+ Add", new Color(80, 110, 230), Color.WHITE);
        titleButtons.add(addButton);
        titleRow.add(titleButtons, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        // vehicleListPanel holds the visible vehicle rows (up to 3)
        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        vehicleListPanel.setBorder(new EmptyBorder(2, 0, 2, 0));

        // Height for exactly 3 vehicle rows: 66px each + 10px gap = 218px total
        JScrollPane vehicleScroll = new JScrollPane(vehicleListPanel);
        vehicleScroll.setOpaque(false);
        vehicleScroll.getViewport().setOpaque(false);
        vehicleScroll.setBorder(null);
        vehicleScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vehicleScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        vehicleScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 218));
        vehicleScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 218));
        card.add(vehicleScroll);

        // ── "View All" placeholder row ────────────────────────────
        // This panel is initially EMPTY. rebuildVehicleList() fills it with
        // a "View All (N)" link if there are more than 3 vehicles.
        // We give it a name so findNamedPanel() can locate it later.
        JPanel viewAllVehicleRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        viewAllVehicleRow.setOpaque(false);
        viewAllVehicleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        viewAllVehicleRow.setName("vehicleViewAllRow"); // tag used in findNamedPanel()
        card.add(viewAllVehicleRow);

        // ── Inline add form ───────────────────────────────────────
        vehicleAddPanel = buildVehicleAddForm();
        vehicleAddPanel.setVisible(false);
        card.add(vehicleAddPanel);

        addButton.addActionListener(e -> {
            vehicleAddPanel.setVisible(!vehicleAddPanel.isVisible());
            card.revalidate();
            card.repaint();
        });

        return card;
    }

    // ── View All vehicles popup ───────────────────────────────────
    private void showViewAllVehiclesDialog(List<String[]> allVehicles) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "All My Vehicles", true);
        dialog.setSize(620, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(18, 24, 14, 24));
        JLabel title = new JLabel("All My Vehicles");
        title.setFont(new Font("SansSerif", Font.BOLD, 16));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        JLabel count = new JLabel(allVehicles.size() + " vehicles");
        count.setFont(UIConstants.FONT_SMALL);
        count.setForeground(UIConstants.TEXT_MUTED);
        header.add(count, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 18, 18, 18));
        for (int i = 0; i < allVehicles.size(); i++) {
            String[] v = allVehicles.get(i);
            listPanel.add(buildVehicleRow(v[0], v[1], v[2], v[3], v[4]));
            if (i < allVehicles.size() - 1) listPanel.add(Box.createVerticalStrut(10));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_DEFAULT));
        JButton closeBtn = createActionButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(80, 34));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

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
        JButton saveVehicleBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelVehicleBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        cancelVehicleBtn.setPreferredSize(new Dimension(90, 32));
        cancelVehicleBtn.setMinimumSize(new Dimension(90, 32));
        cancelVehicleBtn.setMaximumSize(new Dimension(90, 32));
        buttonPanel.add(saveVehicleBtn);
        buttonPanel.add(cancelVehicleBtn);
        form.add(buttonPanel, BorderLayout.EAST);

        cancelVehicleBtn.addActionListener(e -> {
            plateField.setText(""); brandField.setText("");
            yearField.setText("");  colourField.setText("");
            vehicleAddPanel.setVisible(false);
        });

        Runnable doSave = () -> {
            String[] fields = {
                plateField.getText().trim(), brandField.getText().trim(),
                yearField.getText().trim(),  colourField.getText().trim()
            };
            boolean saved = vehicleController.handleAdd(fields);
            if (saved) {
                plateField.setText(""); brandField.setText("");
                yearField.setText("");  colourField.setText("");
                vehicleAddPanel.setVisible(false);
                JOptionPane.showMessageDialog(app, "New vehicle has been added.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        saveVehicleBtn.addActionListener(e -> doSave.run());

        // Allow pressing Enter in any field to trigger Save
        KeyAdapter enterToSave = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        plateField.addKeyListener(enterToSave);
        brandField.addKeyListener(enterToSave);
        yearField.addKeyListener(enterToSave);
        colourField.addKeyListener(enterToSave);

        return form;
    }

    // ── Image choosers ────────────────────────────────────────────
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Choose Profile Picture", FileDialog.LOAD);
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
        } catch (java.io.IOException ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void chooseBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Choose Background Image", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage image = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (image == null) { JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            if (!backgroundStorage.saveImage(user.getUserId(), image)) { JOptionPane.showMessageDialog(app, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
            bannerImage = image;
            if (profileBanner != null) profileBanner.repaint();
        } catch (java.io.IOException ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(app, "Failed to read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); }
    }

    // ── Vehicle row — with Enter key support for the edit form ────
    private JPanel buildVehicleRow(String vehicleID, String plate, String brand, String year, String colour) {
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

        editButton.addActionListener(e -> {
            editPlateField.setText(plate); editBrandField.setText(brand);
            editYearField.setText(year);   editColourField.setText(colour);
            switcher.show(switcherPanel, "edit");
            editPlateField.requestFocusInWindow(); // focus first field when edit opens
        });
        cancelVehicleBtn.addActionListener(e -> switcher.show(switcherPanel, "display"));

        // The actual save logic, extracted to a Runnable so both the button
        // AND the Enter key can trigger it without duplicating code.
        Runnable doEditSave = () -> {
            String newPlate  = editPlateField.getText().trim();
            String newBrand  = editBrandField.getText().trim();
            String newYear   = editYearField.getText().trim();
            String newColour = editColourField.getText().trim();
            if (newPlate.equals(plate) && newBrand.equals(brand) && newYear.equals(year) && newColour.equals(colour)) {
                JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes", JOptionPane.INFORMATION_MESSAGE);
                switcher.show(switcherPanel, "display");
                return;
            }
            boolean updated = vehicleController.handleEdit(plate, new String[]{ newPlate, newBrand, newYear, newColour });
            if (updated) {
                switcher.show(switcherPanel, "display");
                JOptionPane.showMessageDialog(app, "Vehicle information has been updated.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };

        saveVehicleBtn.addActionListener(e -> doEditSave.run());

        // ── Enter key in any edit field triggers Save ─────────────
        // KeyAdapter listens for key events on a text field.
        // When the user presses Enter (VK_ENTER), we run doEditSave.
        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doEditSave.run();
            }
        };
        editPlateField.addKeyListener(enterKey);
        editBrandField.addKeyListener(enterKey);
        editYearField.addKeyListener(enterKey);
        editColourField.addKeyListener(enterKey);

        removeButton.addActionListener(e -> vehicleController.handleDelete(plate));
        return outerBox;
    }

    // ─────────────────────────────────────────────────────────────
    // resolveUserName() — converts a user ID like "T3" to "Mike Tan"
    // Falls back to the raw ID if no match is found in accounts.txt.
    // ─────────────────────────────────────────────────────────────
    private String resolveUserName(String userId) {
        for (User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(userId)) return u.getName();
        }
        return userId;
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS CARD — top 2, "View All" if more than 2
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildUpcomingCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        User user = app.getLoggedInUserObj();
        if (user == null) {
            card.add(makeEmptyLabel("No upcoming appointments."));
        } else {
            List<String[]> allUpcoming = appointmentService.getPendingAppointments(user.getUserId());
            if (allUpcoming.isEmpty()) {
                card.add(makeEmptyLabel("No upcoming appointments."));
            } else {
                int show = Math.min(2, allUpcoming.size());
                for (int i = 0; i < show; i++) {
                    card.add(buildUpcomingRow(allUpcoming.get(i)));
                    if (i < show - 1) card.add(Box.createVerticalStrut(8));
                }
                if (allUpcoming.size() > 2) {
                    card.add(Box.createVerticalStrut(10));
                    JPanel viewAllRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                    viewAllRow.setOpaque(false);
                    viewAllRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    JButton btn = createTextLinkButton("View All (" + allUpcoming.size() + ")");
                    btn.addActionListener(e -> showViewAllDialog("All Upcoming Appointments", allUpcoming, false));
                    viewAllRow.add(btn);
                    card.add(viewAllRow);
                }
            }
        }
        card.add(Box.createVerticalStrut(4));
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING PAYMENT CARD — top 2, "View All" if more than 2
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildPaymentSummaryCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        JLabel title = new JLabel("Pending Payment");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(title, BorderLayout.WEST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(12));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        User user = app.getLoggedInUserObj();
        if (user == null) {
            card.add(makeEmptyLabel("No pending payments."));
        } else {
            Set<String> paidIds = paymentService.getPaidAppointmentIds(user.getUserId());
            List<String[]> allUnpaid = appointmentService.getUnpaidAppointments(user.getUserId(), paidIds);
            if (allUnpaid.isEmpty()) {
                card.add(makeEmptyLabel("No pending payments."));
            } else {
                int show = Math.min(2, allUnpaid.size());
                for (int i = 0; i < show; i++) {
                    card.add(buildPaymentRow(allUnpaid.get(i)));
                    if (i < show - 1) card.add(Box.createVerticalStrut(8));
                }
                if (allUnpaid.size() > 2) {
                    card.add(Box.createVerticalStrut(10));
                    JPanel viewAllRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                    viewAllRow.setOpaque(false);
                    viewAllRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
                    JButton btn = createTextLinkButton("View All (" + allUnpaid.size() + ")");
                    btn.addActionListener(e -> showViewAllDialog("All Pending Payments", allUnpaid, true));
                    viewAllRow.add(btn);
                    card.add(viewAllRow);
                }
            }
        }
        card.add(Box.createVerticalStrut(4));
        return card;
    }

    // ── One upcoming appointment row ──────────────────────────────
    // Row: [0]=apptID [1]=techID [2]=serviceType [3]=status [4]=dateTime [5]=duration
    private JPanel buildUpcomingRow(String[] row) {
        String apptId = row[0], techId = row[1], serviceType = row[2];
        String status = row[3], dateTime = row[4], duration = row[5];
        String techName = resolveUserName(techId);

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel icon = new JLabel("\uD83D\uDCC5");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        icon.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel line1 = new JLabel(apptId + "  ·  " + serviceType);
        line1.setFont(new Font("SansSerif", Font.BOLD, 12));
        line1.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel line2 = new JLabel("Tech: " + techName + "   |   " + dateTime + "   ·   " + duration + " hr(s)");
        line2.setFont(UIConstants.FONT_SMALL);
        line2.setForeground(UIConstants.TEXT_MUTED);
        info.add(Box.createVerticalGlue());
        info.add(line1);
        info.add(Box.createVerticalStrut(3));
        info.add(line2);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        JLabel badge = new JLabel(status);
        badge.setFont(new Font("SansSerif", Font.BOLD, 10));
        badge.setForeground(status.equalsIgnoreCase("In Progress") ? new Color(40, 130, 220) : new Color(230, 160, 40));
        badge.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(badge, BorderLayout.EAST);

        return panel;
    }

    // ── One pending payment row ───────────────────────────────────
    // FIX: Pay button is now 64px wide so "Pay" shows in full (was 56 → still cut off on some LAFs).
    // Row: [0]=apptID [1]=techID [2]=serviceType [3]=status [4]=dateTime [5]=duration
    private JPanel buildPaymentRow(String[] row) {
        String apptId = row[0], techId = row[1], serviceType = row[2], duration = row[5];
        String techName = resolveUserName(techId);

        double pricePerHour = serviceType.equalsIgnoreCase("Major Service") ? 350.00 : 150.00;
        int hours = 1;
        try { hours = Integer.parseInt(duration.trim()); } catch (NumberFormatException ignored) {}
        String amountStr = String.format("%.2f", pricePerHour * hours);

        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 12, 8, 12)));

        JLabel icon = new JLabel("\uD83D\uDCB3");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 20));
        icon.setVerticalAlignment(SwingConstants.CENTER);
        panel.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel line1 = new JLabel(apptId + "  ·  " + serviceType);
        line1.setFont(new Font("SansSerif", Font.BOLD, 12));
        line1.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel line2 = new JLabel("Tech: " + techName + "   |   RM " + amountStr);
        line2.setFont(UIConstants.FONT_SMALL);
        line2.setForeground(UIConstants.TEXT_MUTED);
        info.add(Box.createVerticalGlue());
        info.add(line1);
        info.add(Box.createVerticalStrut(3));
        info.add(line2);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        // ── Pay button — wide enough so "Pay" shows completely ─────
        // Using a plain JButton (not createActionButton) so we can
        // control the exact size reliably without the custom paint code
        // interfering with the preferred size.
        final String finalAmount = amountStr;
        JButton payBtn = new JButton("Pay") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(60, 170, 90) : new Color(80, 190, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        payBtn.setFont(UIConstants.FONT_SMALL_BOLD);
        payBtn.setForeground(Color.WHITE);
        payBtn.setContentAreaFilled(false);
        payBtn.setBorderPainted(false);
        payBtn.setFocusPainted(false);
        payBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        // Width = 64px so "Pay" is fully visible. Height = 36px to fill the row nicely.
        payBtn.setPreferredSize(new Dimension(64, 36));
        payBtn.setMinimumSize(new Dimension(64, 36));
        payBtn.setMaximumSize(new Dimension(64, 36));
        payBtn.addActionListener(e -> showPaymentInvoiceDialog(apptId, serviceType, duration, finalAmount, row));
        panel.add(payBtn, BorderLayout.EAST);

        return panel;
    }

    // ── View All popup (upcoming or payment) ──────────────────────
    private void showViewAllDialog(String dialogTitle, List<String[]> allRows, boolean isPayment) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), dialogTitle, true);
        dialog.setSize(580, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(18, 24, 14, 24));
        JLabel titleLabel = new JLabel(dialogTitle);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(titleLabel, BorderLayout.WEST);
        JLabel countLabel = new JLabel(allRows.size() + " records");
        countLabel.setFont(UIConstants.FONT_SMALL);
        countLabel.setForeground(UIConstants.TEXT_MUTED);
        header.add(countLabel, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(4, 18, 18, 18));

        for (int i = 0; i < allRows.size(); i++) {
            JPanel rowPanel = isPayment ? buildPaymentRow(allRows.get(i)) : buildUpcomingRow(allRows.get(i));
            listPanel.add(rowPanel);
            if (i < allRows.size() - 1) listPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 12));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER_DEFAULT));
        JButton closeBtn = createActionButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(80, 34));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────
    // showPaymentInvoiceDialog() — official invoice popup
    //
    // FIX: No horizontal scrollbar. The dialog is wide enough (460px)
    // and content uses a plain JPanel (not a JScrollPane with horizontal
    // scrolling). All info fits in one view without scrolling sideways.
    //
    // Shows: Appointment ID, Customer, Technician, Service Type,
    //        Date & Time, Service Hours, Payment Type dropdown,
    //        Total Amount, Confirm & Pay button.
    // ─────────────────────────────────────────────────────────────
    private void showPaymentInvoiceDialog(String apptId, String serviceType,
            String duration, String amountStr, String[] row) {

        User user = app.getLoggedInUserObj();
        if (user == null) return;

        String customerName = user.getName();
        String techName     = resolveUserName(row[1]);
        String dateTime     = row[4];
        int    hours        = 1;
        try { hours = Integer.parseInt(duration.trim()); } catch (NumberFormatException ignored) {}
        double totalAmount;
        try { totalAmount = Double.parseDouble(amountStr); } catch (NumberFormatException e) { totalAmount = 150.00; }

        // ── Dialog setup ──────────────────────────────────────────
        // Width = 460px — wide enough to show all labels without truncation.
        // Height = 480px — fits all rows comfortably without needing vertical scroll.
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Payment Invoice — " + apptId, true);
        dialog.setSize(460, 480);
        dialog.setResizable(false); // fixed size so layout is predictable
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        // ── Main content panel ────────────────────────────────────
        // We use a plain JPanel with BorderLayout — NO JScrollPane — so
        // there is absolutely no scrollbar (horizontal or vertical).
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 28, 20, 28));

        // ── Invoice header ────────────────────────────────────────
        JLabel orgName = new JLabel("APU Automotive Service Centre");
        orgName.setFont(new Font("SansSerif", Font.BOLD, 14));
        orgName.setForeground(UIConstants.PRIMARY);
        orgName.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(orgName);

        JLabel orgSub = new JLabel("Official Payment Invoice");
        orgSub.setFont(UIConstants.FONT_SMALL);
        orgSub.setForeground(UIConstants.TEXT_MUTED);
        orgSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(orgSub);
        content.add(Box.createVerticalStrut(14));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(10));

        // ── Detail rows (alternating shading) ────────────────────
        content.add(makeInvoiceRow("Appointment ID", apptId,       false));
        content.add(makeInvoiceRow("Customer",       customerName, true));
        content.add(makeInvoiceRow("Technician",     techName,     false));
        content.add(makeInvoiceRow("Service Type",   serviceType,  true));
        content.add(makeInvoiceRow("Date & Time",    dateTime,     false));
        content.add(makeInvoiceRow("Service Hours",  hours + " hour(s)", true));

        content.add(Box.createVerticalStrut(10));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(12));

        // ── Payment type dropdown ─────────────────────────────────
        JPanel methodRow = new JPanel(new BorderLayout(12, 0));
        methodRow.setOpaque(false);
        methodRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel methodLabel = new JLabel("Payment Type");
        methodLabel.setFont(UIConstants.FONT_SMALL_BOLD);
        methodLabel.setForeground(UIConstants.TEXT_MUTED);
        methodLabel.setPreferredSize(new Dimension(110, 20));

        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Cash", "Card", "Online"});
        methodCombo.setFont(UIConstants.FONT_BODY);
        methodCombo.setBackground(Color.WHITE);

        methodRow.add(methodLabel, BorderLayout.WEST);
        methodRow.add(methodCombo, BorderLayout.CENTER);
        content.add(methodRow);
        content.add(Box.createVerticalStrut(12));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(12));

        // ── Total amount row ──────────────────────────────────────
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));

        JLabel totalLabel = new JLabel("Total Amount");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        totalLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel totalValue = new JLabel(String.format("RM %.2f", totalAmount));
        totalValue.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalValue.setForeground(new Color(40, 160, 80)); // green
        totalValue.setHorizontalAlignment(SwingConstants.RIGHT);

        totalRow.add(totalLabel, BorderLayout.WEST);
        totalRow.add(totalValue, BorderLayout.EAST);
        content.add(totalRow);
        content.add(Box.createVerticalStrut(18));

        // ── Confirm & Pay button ──────────────────────────────────
        final String finalAmountStr = String.format("%.2f", totalAmount);

        JButton confirmBtn = new JButton("Confirm & Pay") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? new Color(60, 170, 90) : new Color(80, 190, 110));
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
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        confirmBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 40));

        confirmBtn.addActionListener(e -> {
            String selectedMethod = (String) methodCombo.getSelectedItem();
            boolean saved = paymentService.savePayment(user.getUserId(), apptId, finalAmountStr, selectedMethod);
            if (saved) {
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        "Payment of RM " + finalAmountStr + " via " + selectedMethod + " recorded successfully.",
                        "Payment Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshUser(); // remove paid item from the card
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save payment. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        content.add(confirmBtn);

        // Add the content directly to the dialog (no wrapping JScrollPane)
        // so there is NO scrollbar at all.
        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ── Invoice helper: alternating-shaded label-value row ────────
    private JPanel makeInvoiceRow(String label, String value, boolean shaded) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(true);
        row.setBackground(shaded ? new Color(245, 246, 248) : Color.WHITE);
        row.setBorder(new EmptyBorder(7, 4, 7, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(120, 20));

        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_SMALL_BOLD);
        val.setForeground(UIConstants.TEXT_PRIMARY);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // ── Invoice separator line ────────────────────────────────────
    private JSeparator makeInvoiceSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 222, 228));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    // ── Text link button for "View All (N)" ───────────────────────
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
        headerTitle = new JLabel("Profile");
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 26));
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildHeaderProfileArea(), BorderLayout.EAST);
        return header;
    }

    private JPanel buildHeaderProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);

        avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    int imgW = profileImage.getWidth(), imgH = profileImage.getHeight();
                    int crop = Math.min(imgW, imgH);
                    int cropX = (imgW - crop) / 2, cropY = (imgH - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38, cropX, cropY, cropX + crop, cropY + crop, null);
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
            activeNav = "Profile";
            headerTitle.setText("Profile");
            contentLayout.show(contentPanel, "Profile");
            dropdownMenu.setVisible(false);
            if (navButtons != null)
                for (int j = 0; j < navButtons.length; j++)
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals("Profile"));
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
            @Override public void mouseEntered(MouseEvent e) {
                dropdownMenu.show(profileButton,
                        profileButton.getWidth() - dropdownMenu.getPreferredSize().width,
                        profileButton.getHeight());
            }
        });
        dropdownMenu.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                if (!dropdownMenu.getBounds().contains(e.getPoint())) dropdownMenu.setVisible(false);
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
                for (int j = 0; j < navButtons.length; j++)
                    updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals(activeNav));
                headerTitle.setText(pageName);
                contentLayout.show(contentPanel, pageName);
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
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 1), new EmptyBorder(2, 4, 2, 4)));
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

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? bg.darker() : bg);
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