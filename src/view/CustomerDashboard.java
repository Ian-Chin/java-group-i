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

public class CustomerDashboard extends JPanel {

    // ── App window reference ──────────────────────────────────────
    private final AppFrame app;

    // ── Page-switching ────────────────────────────────────────────
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

    // ── Profile card — read-mode labels ──────────────────────────
    private JLabel profileNameDisplay;
    private JLabel profileEmailDisplay;
    private JLabel profileRoleDisplay;

    // ── Profile card — edit-mode text fields ─────────────────────
    private JTextField profileNameField;
    private JTextField profileEmailField;

    // ── Panels that swap between read and edit mode ───────────────
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
    private JPanel    vehicleCard;
    private JButton[] navButtons;

    // ── Other section pages ───────────────────────────────────────
    private ServiceHistoryPage serviceHistoryPage;
    private PaymentHistoryPage paymentHistoryPage;
    private StaffReviewPage    staffReviewPage;
    private MyFeedbackPage     myFeedbackPage;

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

    // ── Images ────────────────────────────────────────────────────
    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;
    private JPanel        profileBanner;
    private JLabel        profilePicLabel;

    // ── Colours ───────────────────────────────────────────────────
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);

    // ── Sidebar navigation ────────────────────────────────────────
    private static final String[] NAV_ITEMS = {
            "Profile", "Service History", "Payment History", "Staff Review", "My Feedback"
    };
    private static String navIcon(int cp) {
        return new StringBuilder().appendCodePoint(cp).toString();
    }
    private static final String[] NAV_ICONS = {
            navIcon(0x1F464), navIcon(0x1F504), navIcon(0x1F4B5), "\u2605", navIcon(0x1F4AC)
    };

    // ── Right column (rebuilt on every refreshUser call) ──────────
    private JPanel rightColumn;

    // ── Type dropdown in the add form ─────────────────────────────
    private JComboBox<String> addFormTypeCombo;

    // ── Layout constants ──────────────────────────────────────────
    private static final int MAX_VISIBLE = 4;
    private static final int RIGHT_COL_W = 460;
    private static final int ROW_H       = 64;
    private static final int EDIT_H      = 64;
    private static final int ADD_H       = 64;

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
                    @Override public void rebuildList(List<String[]> v) { rebuildVehicleList(v); }
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
                appointmentService,
                paymentService,
                serviceHistoryService,
                vehicleService
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
        staffReviewPage    = new StaffReviewPage(app.getLoggedInUserObj());
        myFeedbackPage     = new MyFeedbackPage(app.getLoggedInUserObj());

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
    // resetDashboardState()
    // ─────────────────────────────────────────────────────────────
    private void resetDashboardState() {
        if (vehicleAddPanel != null) {
            vehicleAddPanel.setVisible(false);
            clearTextFieldsIn(vehicleAddPanel);
        }
        if (editBtn != null && !editBtn.isVisible()) exitEditMode();
        activeNav = "Profile";
        if (headerTitle   != null) headerTitle.setText("Profile");
        if (contentLayout != null) contentLayout.show(contentPanel, "Profile");
        if (navButtons    != null)
            for (int j = 0; j < navButtons.length; j++)
                updateNavButtonStyle(navButtons[j], NAV_ITEMS[j].equals("Profile"));
    }

    private void clearTextFieldsIn(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTextField) ((JTextField) comp).setText("");
            else if (comp instanceof Container) clearTextFieldsIn((Container) comp);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // refreshRightColumn()
    // ─────────────────────────────────────────────────────────────
    private void refreshRightColumn() {
        if (rightColumn == null) return;
        rightColumn.removeAll();
        rightColumn.add(buildUpcomingCard());
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(buildPaymentSummaryCard());
        rightColumn.revalidate();
        rightColumn.repaint();
    }

    // ─────────────────────────────────────────────────────────────
    // refreshUser()
    //
    // CHANGE: Removed the autoCompleteExpired() call.
    //         Expired appointments are now hidden automatically by
    //         AppointmentService.getPendingAppointments() which checks
    //         the end time of each appointment without touching the file.
    // ─────────────────────────────────────────────────────────────
    public void refreshUser() {
        resetDashboardState();

        // NOTE: autoCompleteExpired() has been removed from here.
        //       Previously it changed appointment statuses in the file.
        //       Now, getPendingAppointments() simply skips appointments
        //       whose end time has already passed — no file changes needed.

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
            profileImage = profilePicStorage.loadImage(user.getUserId());
            bannerImage  = backgroundStorage.loadImage(user.getUserId());
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (profileBanner   != null) profileBanner.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        }

        vehicleController.refreshList();

        // Update StaffReviewPage and MyFeedbackPage with the current logged-in user
        if (staffReviewPage != null) {
            staffReviewPage.setUser(app.getLoggedInUserObj());
        }

        if (myFeedbackPage != null) {
            myFeedbackPage.setUser(app.getLoggedInUserObj());
        }

        if (rightColumn != null) {
            rightColumn.removeAll();
            rightColumn.add(buildUpcomingCard());
            rightColumn.add(Box.createVerticalStrut(14));
            rightColumn.add(buildPaymentSummaryCard());
            rightColumn.revalidate();
            rightColumn.repaint();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // rebuildVehicleList()
    //
    // The add form is inserted directly after the last vehicle row
    // inside vehicleListPanel, so it always appears right below the
    // last vehicle entry.
    //
    // Layout order inside vehicleListPanel:
    //   [vehicle row 1]
    //   [gap]
    //   [vehicle row 2]   <- last vehicle
    //   [gap]
    //   [add form]        <- appears right here when visible
    //   [view all row]    <- only when vehicles > MAX_VISIBLE
    // ─────────────────────────────────────────────────────────────
    private void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        // Remember if the add form was open before we rebuild
        boolean addFormWasVisible = (vehicleAddPanel != null) && vehicleAddPanel.isVisible();

        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            JLabel empty = makeEmptyLabel("No vehicles registered.");
            vehicleListPanel.add(empty);
        } else {
            int show = Math.min(MAX_VISIBLE, vehicles.size());
            for (int i = 0; i < show; i++) {
                String[] v = vehicles.get(i);
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(row);
                if (i < show - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(8));
                }
            }
        }

        // Add the add form right after the last vehicle row
        if (vehicleAddPanel != null) {
            vehicleListPanel.add(Box.createVerticalStrut(8));
            vehicleListPanel.add(vehicleAddPanel);
            vehicleAddPanel.setVisible(addFormWasVisible);
        }

        // "View All" button — only shown when there are more than MAX_VISIBLE vehicles
        if (vehicles.size() > MAX_VISIBLE) {
            vehicleListPanel.add(Box.createVerticalStrut(4));
            JPanel viewAllRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            viewAllRow.setOpaque(false);
            viewAllRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            viewAllRow.setName("vehicleViewAllRow");
            JButton btn = createTextLinkButton("View All (" + vehicles.size() + ")");
            final List<String[]> snap = vehicles;
            btn.addActionListener(e -> showViewAllVehiclesDialog(snap));
            viewAllRow.add(btn);
            vehicleListPanel.add(viewAllRow);
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();

        if (vehicleCard != null) {
            vehicleCard.revalidate();
            vehicleCard.repaint();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // PROFILE PAGE
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        JScrollPane scroll = new JScrollPane(buildProfileInner());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
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
        inner.add(Box.createVerticalStrut(20));

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setBackground(UIConstants.BG_CONTENT);
        body.setBorder(new EmptyBorder(0, 20, 0, 20));

        JPanel leftColumn = new JPanel();
        leftColumn.setLayout(new BoxLayout(leftColumn, BoxLayout.Y_AXIS));
        leftColumn.setOpaque(false);
        leftColumn.add(buildPersonalInfoCard());
        leftColumn.add(Box.createVerticalStrut(14));
        leftColumn.add(buildVehicleCard());
        body.add(leftColumn, BorderLayout.CENTER);

        rightColumn = new JPanel();
        rightColumn.setLayout(new BoxLayout(rightColumn, BoxLayout.Y_AXIS));
        rightColumn.setOpaque(false);
        rightColumn.setPreferredSize(new Dimension(RIGHT_COL_W, 0));
        rightColumn.setMinimumSize(new Dimension(RIGHT_COL_W, 0));
        rightColumn.setMaximumSize(new Dimension(RIGHT_COL_W, Integer.MAX_VALUE));
        rightColumn.add(buildUpcomingCard());
        rightColumn.add(Box.createVerticalStrut(14));
        rightColumn.add(buildPaymentSummaryCard());
        body.add(rightColumn, BorderLayout.EAST);

        inner.add(body);
        return inner;
    }

    // ── Banner + circular profile picture ────────────────────────
    private JPanel buildBannerHero() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHov = {false}, avatarHov = {false};

        profileBanner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, BANNER_BLUE,
                            getWidth(), getHeight(), new Color(60, 90, 210)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (bannerHov[0]) {
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
            @Override public void mouseEntered(MouseEvent e) { bannerHov[0] = true;  profileBanner.repaint(); }
            @Override public void mouseExited (MouseEvent e) { bannerHov[0] = false; profileBanner.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { chooseBannerImage(); }
        });

        profilePicLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih), cx = (iw - crop) / 2, cy = (ih - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size, cx, cy, cx + crop, cy + crop, null);
                    g2.setClip(null);
                } else { drawDefaultAvatar(g2, size); }
                if (avatarHov[0]) {
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
            @Override public void mouseEntered(MouseEvent e) { avatarHov[0] = true;  profilePicLabel.repaint(); }
            @Override public void mouseExited (MouseEvent e) { avatarHov[0] = false; profilePicLabel.repaint(); }
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
        hero.setComponentZOrder(profileBanner, 1);
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
        int ey = size * 38 / 100, eo = size * 18 / 100, er = size / 14;
        g2.fillOval(size / 2 - eo - er, ey - er, er * 2, er * 2);
        g2.fillOval(size / 2 + eo - er, ey - er, er * 2, er * 2);
        g2.drawArc(size * 28 / 100, size * 44 / 100, size * 44 / 100, size * 26 / 100, 200, 140);
    }

    // ═══════════════════════════════════════════════════════════════
    // PERSONAL INFORMATION CARD
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildPersonalInfoCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel titleLabel = new JLabel("Personal Information");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);
        editBtn   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        saveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        cancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        editBtn.setPreferredSize(new Dimension(70, 30));
        saveBtn.setPreferredSize(new Dimension(70, 30));
        cancelBtn.setPreferredSize(new Dimension(85, 30));
        saveBtn.setVisible(false);
        cancelBtn.setVisible(false);
        buttonPanel.add(editBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);
        titleRow.add(buttonPanel, BorderLayout.EAST);
        card.add(titleRow);
        card.add(Box.createVerticalStrut(10));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(12));

        User u = app.getLoggedInUserObj();
        String curName  = u != null ? u.getName()  : "—";
        String curEmail = u != null ? u.getEmail() : "—";
        String curRole  = u != null ? u.getRole()  : "—";
        if (!curRole.equals("—")) curRole = curRole.substring(0, 1).toUpperCase() + curRole.substring(1);

        nameDisplayPanel = makeReadRow("Username", curName);
        profileNameDisplay = (JLabel) ((BorderLayout) nameDisplayPanel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        card.add(nameDisplayPanel);

        nameEditPanel = new JPanel(new BorderLayout(8, 0));
        nameEditPanel.setOpaque(false);
        nameEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel nle = new JLabel("Username");
        nle.setFont(UIConstants.FONT_SMALL_BOLD);
        nle.setForeground(UIConstants.TEXT_MUTED);
        nle.setPreferredSize(new Dimension(80, 18));
        profileNameField = new JTextField(curName);
        profileNameField.setFont(UIConstants.FONT_BODY);
        profileNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(2, 5, 2, 5)));
        nameEditPanel.add(nle, BorderLayout.WEST);
        nameEditPanel.add(profileNameField, BorderLayout.CENTER);
        nameEditPanel.setVisible(false);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(8));

        emailDisplayPanel = makeReadRow("Email", curEmail);
        profileEmailDisplay = (JLabel) ((BorderLayout) emailDisplayPanel.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
        card.add(emailDisplayPanel);

        emailEditPanel = new JPanel(new BorderLayout(8, 0));
        emailEditPanel.setOpaque(false);
        emailEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel ele = new JLabel("Email");
        ele.setFont(UIConstants.FONT_SMALL_BOLD);
        ele.setForeground(UIConstants.TEXT_MUTED);
        ele.setPreferredSize(new Dimension(80, 18));
        profileEmailField = new JTextField(curEmail);
        profileEmailField.setFont(UIConstants.FONT_BODY);
        profileEmailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 2),
                new EmptyBorder(2, 5, 2, 5)));
        emailEditPanel.add(ele, BorderLayout.WEST);
        emailEditPanel.add(profileEmailField, BorderLayout.CENTER);
        emailEditPanel.setVisible(false);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(8));

        buildInfoRow(card, "Role", curRole);
        card.add(Box.createVerticalStrut(4));

        editBtn.addActionListener(e -> enterEditMode());
        cancelBtn.addActionListener(e -> exitEditMode());
        saveBtn.addActionListener(e -> handleSave());
        KeyAdapter enterToSave = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) handleSave();
            }
        };
        profileNameField.addKeyListener(enterToSave);
        profileEmailField.addKeyListener(enterToSave);

        return card;
    }

    private JPanel makeReadRow(String fieldName, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel lbl = new JLabel(fieldName);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(80, 18));
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 13));
        val.setForeground(UIConstants.TEXT_PRIMARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
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
        if (profileNameField  != null) profileNameField.setText(profileController.getCurrentName());
        if (profileEmailField != null) profileEmailField.setText(profileController.getCurrentEmail());
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
            JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes",
                    JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }
        try {
            boolean saved = profileController.saveProfile(newName, newEmail);
            if (saved) {
                exitEditMode();
                refreshUser();
                JOptionPane.showMessageDialog(app, "Profile updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app, "Failed to save. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(app, ex.getMessage(), "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private JLabel buildInfoRow(JPanel card, String fieldName, String value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel fl = new JLabel(fieldName);
        fl.setFont(new Font("SansSerif", Font.BOLD, 13));
        fl.setForeground(UIConstants.TEXT_MUTED);
        fl.setPreferredSize(new Dimension(80, 18));
        JLabel vl = new JLabel(value);
        vl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        vl.setForeground(UIConstants.TEXT_PRIMARY);
        row.add(fl, BorderLayout.WEST);
        row.add(vl, BorderLayout.CENTER);
        card.add(row);
        if ("Role".equals(fieldName)) profileRoleDisplay = vl;
        return vl;
    }

    // ═══════════════════════════════════════════════════════════════
    // MY VEHICLE CARD
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildVehicleCard() {
        vehicleCard = createCard();
        vehicleCard.setLayout(new BoxLayout(vehicleCard, BoxLayout.Y_AXIS));
        vehicleCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        JLabel titleLabel = new JLabel("My Vehicle");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        titleRow.add(titleLabel, BorderLayout.WEST);

        JButton addButton = createActionButton("+ Add", new Color(80, 110, 230), Color.WHITE);
        addButton.setPreferredSize(new Dimension(80, 30));
        addButton.setMinimumSize(new Dimension(80, 30));
        titleRow.add(addButton, BorderLayout.EAST);

        vehicleCard.add(titleRow);
        vehicleCard.add(Box.createVerticalStrut(8));
        vehicleCard.add(makeSeparator());
        vehicleCard.add(Box.createVerticalStrut(8));

        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        vehicleListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vehicleListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        vehicleCard.add(vehicleListPanel);

        vehicleAddPanel = buildVehicleAddForm();
        vehicleAddPanel.setVisible(false);

        addButton.addActionListener(e -> {
            boolean nowVisible = !vehicleAddPanel.isVisible();
            vehicleAddPanel.setVisible(nowVisible);
            if (nowVisible) {
                clearTextFieldsIn(vehicleAddPanel);
                if (addFormTypeCombo != null) addFormTypeCombo.setSelectedIndex(0);
            }
            vehicleCard.revalidate();
            vehicleCard.repaint();
        });

        return vehicleCard;
    }

    // ─────────────────────────────────────────────────────────────
    // buildVehicleAddForm()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildVehicleAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(6, 8, 6, 8)));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, ADD_H));
        form.setPreferredSize(new Dimension(0, ADD_H));

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.insets  = new Insets(0, 1, 0, 1);
        gbc.gridy   = 0;
        gbc.weighty = 1.0;

        addFormTypeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        addFormTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JTextField plateField  = makeCompactField("");
        JTextField brandField  = makeCompactField("");
        JTextField yearField   = makeCompactField("");
        JTextField colourField = makeCompactField("");

        gbc.gridx = 0; gbc.weightx = 0; gbc.ipadx = 28;
        fieldsPanel.add(makeLabelledCombo("Type", addFormTypeCombo), gbc);

        gbc.gridx = 1; gbc.weightx = 0; gbc.ipadx = 40;
        fieldsPanel.add(makeLabelledField("Car Plate", plateField), gbc);

        gbc.gridx = 2; gbc.weightx = 0.7; gbc.ipadx = 0;
        fieldsPanel.add(makeLabelledField("Brand / Model", brandField), gbc);

        gbc.gridx = 3; gbc.weightx = 0; gbc.ipadx = 26;
        fieldsPanel.add(makeLabelledField("Year", yearField), gbc);

        gbc.gridx = 4; gbc.weightx = 0; gbc.ipadx = 34;
        fieldsPanel.add(makeLabelledField("Colour", colourField), gbc);

        form.add(fieldsPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);
        JButton formSaveBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton formCancelBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        formSaveBtn.setPreferredSize(new Dimension(65, 28));
        formSaveBtn.setMinimumSize(new Dimension(65, 28));
        formCancelBtn.setPreferredSize(new Dimension(80, 28));
        formCancelBtn.setMinimumSize(new Dimension(80, 28));
        btnPanel.add(formSaveBtn);
        btnPanel.add(formCancelBtn);
        form.add(btnPanel, BorderLayout.EAST);

        formCancelBtn.addActionListener(e -> {
            vehicleAddPanel.setVisible(false);
            clearTextFieldsIn(vehicleAddPanel);
            if (addFormTypeCombo != null) addFormTypeCombo.setSelectedIndex(0);
            if (vehicleCard != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
        });

        Runnable doAdd = () -> {
            String type = (String) addFormTypeCombo.getSelectedItem();
            String[] fields = {
                type,
                plateField.getText().trim(),
                brandField.getText().trim(),
                yearField.getText().trim(),
                colourField.getText().trim()
            };
            boolean saved = vehicleController.handleAdd(fields);
            if (saved) {
                clearTextFieldsIn(vehicleAddPanel);
                if (addFormTypeCombo != null) addFormTypeCombo.setSelectedIndex(0);
                vehicleAddPanel.setVisible(false);
                if (vehicleCard != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
                JOptionPane.showMessageDialog(app, "New vehicle has been added.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        formSaveBtn.addActionListener(e -> doAdd.run());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doAdd.run();
            }
        };
        plateField.addKeyListener(enterKey);
        brandField.addKeyListener(enterKey);
        yearField.addKeyListener(enterKey);
        colourField.addKeyListener(enterKey);

        final boolean[] popupOpen = { false };
        addFormTypeCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = true; }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
        });
        addFormTypeCombo.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !popupOpen[0]) doAdd.run();
            }
        });

        return form;
    }

    // ═══════════════════════════════════════════════════════════════
    // buildVehicleRow()
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildVehicleRow(String vehicleType, String plate,
                                   String brand, String year, String colour) {

        JPanel displayCard = new JPanel(new BorderLayout(0, 0));
        displayCard.setOpaque(false);
        displayCard.setBorder(new EmptyBorder(10, 12, 10, 8));

        String emoji = "Motor".equalsIgnoreCase(vehicleType) ? "\uD83C\uDFCD" : "\uD83D\uDE97";
        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(new Font("SansSerif", Font.PLAIN, 22));
        emojiLbl.setHorizontalAlignment(SwingConstants.CENTER);
        emojiLbl.setVerticalAlignment(SwingConstants.CENTER);
        emojiLbl.setPreferredSize(new Dimension(40, 40));
        displayCard.add(emojiLbl, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel brandLbl = new JLabel(brand);
        brandLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLbl.setForeground(UIConstants.TEXT_PRIMARY);
        brandLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLbl = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLbl.setForeground(UIConstants.TEXT_MUTED);
        detailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(brandLbl);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(detailLbl);
        infoPanel.add(Box.createVerticalGlue());
        displayCard.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);
        JButton editBtn_   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        JButton removeBtn_ = createActionButton("Remove", new Color(220, 80, 80),  Color.WHITE);
        editBtn_.setPreferredSize(new Dimension(65, 30));
        editBtn_.setMinimumSize(new Dimension(65, 30));
        removeBtn_.setPreferredSize(new Dimension(90, 30));
        removeBtn_.setMinimumSize(new Dimension(90, 30));
        btnPanel.add(editBtn_);
        btnPanel.add(removeBtn_);
        displayCard.add(btnPanel, BorderLayout.EAST);

        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);
        editCard.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel editFields = new JPanel(new GridBagLayout());
        editFields.setOpaque(false);
        GridBagConstraints ec = new GridBagConstraints();
        ec.fill    = GridBagConstraints.BOTH;
        ec.insets  = new Insets(0, 1, 0, 1);
        ec.gridy   = 0;
        ec.weighty = 1.0;

        JComboBox<String> editTypeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        editTypeCombo.setSelectedItem(vehicleType);
        editTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JTextField editPlate  = makeCompactField(plate);
        JTextField editBrand  = makeCompactField(brand);
        JTextField editYear   = makeCompactField(year);
        JTextField editColour = makeCompactField(colour);

        ec.gridx = 0; ec.weightx = 0; ec.ipadx = 28;
        editFields.add(makeLabelledCombo("Type", editTypeCombo), ec);

        ec.gridx = 1; ec.weightx = 0; ec.ipadx = 40;
        editFields.add(makeLabelledField("Plate", editPlate), ec);

        ec.gridx = 2; ec.weightx = 0.7; ec.ipadx = 0;
        editFields.add(makeLabelledField("Brand", editBrand), ec);

        ec.gridx = 3; ec.weightx = 0; ec.ipadx = 26;
        editFields.add(makeLabelledField("Year", editYear), ec);

        ec.gridx = 4; ec.weightx = 0; ec.ipadx = 34;
        editFields.add(makeLabelledField("Colour", editColour), ec);

        editCard.add(editFields, BorderLayout.CENTER);

        JPanel editBtns = new JPanel(new GridBagLayout());
        editBtns.setOpaque(false);
        GridBagConstraints ebc = new GridBagConstraints();
        ebc.gridx = 0; ebc.gridy = 0;
        ebc.anchor = GridBagConstraints.CENTER;
        ebc.insets = new Insets(0, 4, 0, 0);
        JButton saveVBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelVBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        saveVBtn.setPreferredSize(new Dimension(65, 30));
        saveVBtn.setMinimumSize(new Dimension(65, 30));
        cancelVBtn.setPreferredSize(new Dimension(80, 30));
        cancelVBtn.setMinimumSize(new Dimension(80, 30));
        editBtns.add(saveVBtn, ebc);
        ebc.gridx = 1;
        editBtns.add(cancelVBtn, ebc);
        editCard.add(editBtns, BorderLayout.EAST);

        CardLayout switcher = new CardLayout();
        JPanel switcherPanel = new JPanel(switcher);
        switcherPanel.setOpaque(false);
        switcherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
        switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display");

        JPanel rowWrapper = new JPanel(new BorderLayout());
        rowWrapper.setOpaque(false);
        rowWrapper.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        rowWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
        rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        rowWrapper.add(switcherPanel, BorderLayout.CENTER);

        editBtn_.addActionListener(e -> {
            editTypeCombo.setSelectedItem(vehicleType);
            editPlate.setText(plate);
            editBrand.setText(brand);
            editYear.setText(year);
            editColour.setText(colour);
            switcherPanel.setPreferredSize(new Dimension(0, EDIT_H));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            rowWrapper.setPreferredSize(new Dimension(0, EDIT_H));
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            switcher.show(switcherPanel, "edit");
            editPlate.requestFocusInWindow();
            if (rowWrapper.getParent() != null)
                rowWrapper.getParent().revalidate();
        });

        cancelVBtn.addActionListener(e -> {
            switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            switcher.show(switcherPanel, "display");
            if (rowWrapper.getParent() != null)
                rowWrapper.getParent().revalidate();
        });

        Runnable doSave = () -> {
            String nt = (String) editTypeCombo.getSelectedItem();
            if (nt == null) return;

            String np = editPlate.getText().trim();
            String nb = editBrand.getText().trim();
            String ny = editYear.getText().trim();
            String nc = editColour.getText().trim();

            if (nt.equals(vehicleType) && np.equals(plate) && nb.equals(brand)
                    && ny.equals(year) && nc.equals(colour)) {
                JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes",
                        JOptionPane.INFORMATION_MESSAGE);
                switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
                rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switcherPanel, "display");
                if (rowWrapper.getParent() != null)
                    rowWrapper.getParent().revalidate();
                return;
            }

            String[] fields = { nt, np, nb, ny, nc };
            boolean updated = vehicleController.handleEdit(plate, fields);
            if (updated) {
                switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
                rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switcherPanel, "display");
                if (rowWrapper.getParent() != null)
                    rowWrapper.getParent().revalidate();

                refreshRightColumn();

                JOptionPane.showMessageDialog(app, "Vehicle updated.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };

        saveVBtn.addActionListener(e -> doSave.run());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        editPlate.addKeyListener(enterKey);
        editBrand.addKeyListener(enterKey);
        editYear.addKeyListener(enterKey);
        editColour.addKeyListener(enterKey);

        final boolean[] popupOpen = { false };
        editTypeCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = true; }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
        });
        editTypeCombo.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !popupOpen[0]) doSave.run();
            }
        });

        removeBtn_.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(
                    app,
                    "Are you sure you want to remove this vehicle?",
                    "Confirm Remove",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (choice == JOptionPane.YES_OPTION) {
                User user = app.getLoggedInUserObj();
                if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                    vehicleController.refreshList();
                    refreshRightColumn();
                } else {
                    JOptionPane.showMessageDialog(app, "Failed to remove vehicle.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return rowWrapper;
    }

    // ═══════════════════════════════════════════════════════════════
    // "ALL MY VEHICLES" POPUP DIALOG
    // ═══════════════════════════════════════════════════════════════
    private void showViewAllVehiclesDialog(List<String[]> initialVehicles) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this), "All My Vehicles", true);
        dialog.setSize(820, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 22, 12, 22));
        JLabel titleLbl = new JLabel("All My Vehicles");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(titleLbl, BorderLayout.WEST);
        JLabel countLbl = new JLabel(initialVehicles.size() + " vehicles");
        countLbl.setFont(UIConstants.FONT_SMALL);
        countLbl.setForeground(UIConstants.TEXT_MUTED);
        header.add(countLbl, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(8, 18, 18, 18));

        Runnable[] refreshDialogList = {null};
        refreshDialogList[0] = () -> {
            User user = app.getLoggedInUserObj();
            List<String[]> latest = (user != null)
                    ? vehicleController.getAllVehiclesForUser(user.getUserId())
                    : new java.util.ArrayList<>();

            countLbl.setText(latest.size() + " vehicles");
            listPanel.removeAll();

            if (latest.isEmpty()) {
                listPanel.add(makeEmptyLabel("No vehicles registered."));
            } else {
                for (int i = 0; i < latest.size(); i++) {
                    JPanel row = buildDialogVehicleRow(latest.get(i), dialog, refreshDialogList[0]);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(row);
                    if (i < latest.size() - 1) listPanel.add(Box.createVerticalStrut(8));
                }
            }

            listPanel.revalidate();
            listPanel.repaint();

            vehicleController.refreshList();
            refreshRightColumn();

            if (latest.size() <= MAX_VISIBLE) dialog.dispose();
        };

        for (int i = 0; i < initialVehicles.size(); i++) {
            JPanel row = buildDialogVehicleRow(initialVehicles.get(i), dialog, refreshDialogList[0]);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(row);
            if (i < initialVehicles.size() - 1) listPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
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
    // buildDialogVehicleRow()
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildDialogVehicleRow(String[] v, JDialog parentDialog, Runnable onChanged) {
        String vehicleType = v[1];
        String plate       = v[2];
        String brand       = v[3];
        String year        = v[4];
        String colour      = v[5];

        JPanel displayCard = new JPanel(new BorderLayout(0, 0));
        displayCard.setOpaque(false);
        displayCard.setBorder(new EmptyBorder(10, 12, 10, 8));

        String emoji = "Motor".equalsIgnoreCase(vehicleType) ? "\uD83C\uDFCD" : "\uD83D\uDE97";
        JLabel emojiLbl = new JLabel(emoji);
        emojiLbl.setFont(new Font("SansSerif", Font.PLAIN, 22));
        emojiLbl.setHorizontalAlignment(SwingConstants.CENTER);
        emojiLbl.setVerticalAlignment(SwingConstants.CENTER);
        emojiLbl.setPreferredSize(new Dimension(40, 40));
        displayCard.add(emojiLbl, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(0, 8, 0, 0));

        JLabel brandLbl = new JLabel(brand);
        brandLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLbl.setForeground(UIConstants.TEXT_PRIMARY);
        brandLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLbl = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLbl.setForeground(UIConstants.TEXT_MUTED);
        detailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(brandLbl);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(detailLbl);
        infoPanel.add(Box.createVerticalGlue());
        displayCard.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);
        JButton editButton   = createActionButton("Edit",   new Color(80, 110, 230), Color.WHITE);
        JButton removeButton = createActionButton("Remove", new Color(220, 80, 80),  Color.WHITE);
        editButton.setPreferredSize(new Dimension(65, 30));
        editButton.setMinimumSize(new Dimension(65, 30));
        removeButton.setPreferredSize(new Dimension(90, 30));
        removeButton.setMinimumSize(new Dimension(90, 30));
        btnPanel.add(editButton);
        btnPanel.add(removeButton);
        displayCard.add(btnPanel, BorderLayout.EAST);

        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);
        editCard.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel editFields = new JPanel(new GridBagLayout());
        editFields.setOpaque(false);
        GridBagConstraints efGbc = new GridBagConstraints();
        efGbc.fill    = GridBagConstraints.BOTH;
        efGbc.insets  = new Insets(0, 1, 0, 1);
        efGbc.gridy   = 0;
        efGbc.weighty = 1.0;

        JComboBox<String> editTypeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        editTypeCombo.setSelectedItem(vehicleType);
        editTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JTextField editPlate  = makeCompactField(plate);
        JTextField editBrand  = makeCompactField(brand);
        JTextField editYear   = makeCompactField(year);
        JTextField editColour = makeCompactField(colour);

        efGbc.gridx = 0; efGbc.weightx = 0; efGbc.ipadx = 28;
        editFields.add(makeLabelledCombo("Type", editTypeCombo), efGbc);

        efGbc.gridx = 1; efGbc.weightx = 0; efGbc.ipadx = 40;
        editFields.add(makeLabelledField("Plate", editPlate), efGbc);

        efGbc.gridx = 2; efGbc.weightx = 0.7; efGbc.ipadx = 0;
        editFields.add(makeLabelledField("Brand", editBrand), efGbc);

        efGbc.gridx = 3; efGbc.weightx = 0; efGbc.ipadx = 26;
        editFields.add(makeLabelledField("Year", editYear), efGbc);

        efGbc.gridx = 4; efGbc.weightx = 0; efGbc.ipadx = 34;
        editFields.add(makeLabelledField("Colour", editColour), efGbc);

        editCard.add(editFields, BorderLayout.CENTER);

        JPanel editBtns = new JPanel(new GridBagLayout());
        editBtns.setOpaque(false);
        GridBagConstraints ebGbc = new GridBagConstraints();
        ebGbc.gridx = 0; ebGbc.gridy = 0;
        ebGbc.anchor = GridBagConstraints.CENTER;
        ebGbc.insets = new Insets(0, 4, 0, 0);
        JButton saveVBtn   = createActionButton("Save",   new Color(80, 190, 110), Color.WHITE);
        JButton cancelVBtn = createActionButton("Cancel", new Color(150, 150, 165), Color.WHITE);
        saveVBtn.setPreferredSize(new Dimension(65, 30));
        saveVBtn.setMinimumSize(new Dimension(65, 30));
        cancelVBtn.setPreferredSize(new Dimension(80, 30));
        cancelVBtn.setMinimumSize(new Dimension(80, 30));
        editBtns.add(saveVBtn, ebGbc);
        ebGbc.gridx = 1;
        editBtns.add(cancelVBtn, ebGbc);
        editCard.add(editBtns, BorderLayout.EAST);

        CardLayout switcher = new CardLayout();
        JPanel switcherPanel = new JPanel(switcher);
        switcherPanel.setOpaque(false);
        switcherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
        switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display");

        JPanel rowWrapper = new JPanel(new BorderLayout());
        rowWrapper.setOpaque(false);
        rowWrapper.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        rowWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
        rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        rowWrapper.add(switcherPanel, BorderLayout.CENTER);

        editButton.addActionListener(e -> {
            editTypeCombo.setSelectedItem(vehicleType);
            editPlate.setText(plate);
            editBrand.setText(brand);
            editYear.setText(year);
            editColour.setText(colour);
            switcherPanel.setPreferredSize(new Dimension(0, EDIT_H));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            rowWrapper.setPreferredSize(new Dimension(0, EDIT_H));
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            switcher.show(switcherPanel, "edit");
            editPlate.requestFocusInWindow();
            if (rowWrapper.getParent() != null)
                rowWrapper.getParent().revalidate();
        });

        cancelVBtn.addActionListener(e -> {
            switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
            rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
            switcher.show(switcherPanel, "display");
            if (rowWrapper.getParent() != null)
                rowWrapper.getParent().revalidate();
        });

        Runnable doSave = () -> {
            String nt = (String) editTypeCombo.getSelectedItem();
            if (nt == null) return;

            String np = editPlate.getText().trim();
            String nb = editBrand.getText().trim();
            String ny = editYear.getText().trim();
            String nc = editColour.getText().trim();
            boolean noChange = nt.equals(vehicleType) && np.equals(plate)
                    && nb.equals(brand) && ny.equals(year) && nc.equals(colour);
            if (noChange) {
                JOptionPane.showMessageDialog(parentDialog, "No changes were made.",
                        "No Changes", JOptionPane.INFORMATION_MESSAGE);
                switcherPanel.setPreferredSize(new Dimension(0, ROW_H));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                rowWrapper.setPreferredSize(new Dimension(0, ROW_H));
                rowWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switcherPanel, "display");
                if (rowWrapper.getParent() != null)
                    rowWrapper.getParent().revalidate();
                return;
            }
            String[] fields = { nt, np, nb, ny, nc };
            boolean updated = vehicleController.handleEdit(plate, fields);
            if (updated) {
                JOptionPane.showMessageDialog(parentDialog, "Vehicle updated successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                onChanged.run();
            }
        };

        saveVBtn.addActionListener(e -> doSave.run());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        editPlate.addKeyListener(enterKey);
        editBrand.addKeyListener(enterKey);
        editYear.addKeyListener(enterKey);
        editColour.addKeyListener(enterKey);

        final boolean[] popupOpen = { false };
        editTypeCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = true; }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
            @Override public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) { popupOpen[0] = false; }
        });
        editTypeCombo.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !popupOpen[0]) doSave.run();
            }
        });

        removeButton.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(parentDialog,
                    "Remove " + brand + " (" + plate + ")?",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                User user = app.getLoggedInUserObj();
                if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                    JOptionPane.showMessageDialog(parentDialog,
                            brand + " removed successfully.", "Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    onChanged.run();
                } else {
                    JOptionPane.showMessageDialog(parentDialog, "Failed to remove vehicle.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return rowWrapper;
    }

    // ── Image choosers ────────────────────────────────────────────
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Profile Picture", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read image.", "Error",
                        JOptionPane.ERROR_MESSAGE); return;
            }
            if (!profilePicStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save picture.", "Error",
                        JOptionPane.ERROR_MESSAGE); return;
            }
            profileImage = img;
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        } catch (java.io.IOException ex) { ex.printStackTrace(); }
    }

    private void chooseBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Background Image", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read image.", "Error",
                        JOptionPane.ERROR_MESSAGE); return;
            }
            if (!backgroundStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save image.", "Error",
                        JOptionPane.ERROR_MESSAGE); return;
            }
            bannerImage = img;
            if (profileBanner != null) profileBanner.repaint();
        } catch (java.io.IOException ex) { ex.printStackTrace(); }
    }

    private String resolveUserName(String userId) {
        return appointmentController.resolveUserName(
                app.getAccountService().getAllUsers(), userId);
    }

    // ═══════════════════════════════════════════════════════════════
    // UPCOMING APPOINTMENTS CARD
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildUpcomingCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel tr = new JPanel(new BorderLayout());
        tr.setOpaque(false);
        tr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel("Upcoming Appointments");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        tr.add(title, BorderLayout.WEST);
        card.add(tr);
        card.add(Box.createVerticalStrut(8));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(8));

        // This now only returns appointments whose end time is in the future.
        // Expired appointments are filtered out inside getPendingAppointments().
        List<String[]> all = appointmentController.getPendingAppointments();

        if (all.isEmpty()) {
            card.add(makeEmptyLabel("No upcoming appointments."));
        } else {
            int show = Math.min(2, all.size());
            for (int i = 0; i < show; i++) {
                card.add(buildUpcomingRow(all.get(i)));
                if (i < show - 1) card.add(Box.createVerticalStrut(6));
            }
            if (all.size() > 2) {
                card.add(Box.createVerticalStrut(6));
                JPanel vr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                vr.setOpaque(false);
                vr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                JButton btn = createTextLinkButton("View All (" + all.size() + ")");
                btn.addActionListener(e -> showViewAllDialog("All Upcoming Appointments", all, false));
                vr.add(btn);
                card.add(vr);
            }
        }
        card.add(Box.createVerticalStrut(2));
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    // PENDING PAYMENT CARD
    // ═══════════════════════════════════════════════════════════════
    private JPanel buildPaymentSummaryCard() {
        JPanel card = createCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel tr = new JPanel(new BorderLayout());
        tr.setOpaque(false);
        tr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        JLabel title = new JLabel("Pending Payment");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(UIConstants.TEXT_PRIMARY);
        tr.add(title, BorderLayout.WEST);
        card.add(tr);
        card.add(Box.createVerticalStrut(8));
        card.add(makeSeparator());
        card.add(Box.createVerticalStrut(8));

        List<String[]> all = appointmentController.getUnpaidAppointments();

        if (all.isEmpty()) {
            card.add(makeEmptyLabel("No pending payments."));
        } else {
            int show = Math.min(2, all.size());
            for (int i = 0; i < show; i++) {
                card.add(buildPaymentRow(all.get(i), null));
                if (i < show - 1) card.add(Box.createVerticalStrut(6));
            }
            if (all.size() > 2) {
                card.add(Box.createVerticalStrut(6));
                JPanel vr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                vr.setOpaque(false);
                vr.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
                JButton btn = createTextLinkButton("View All (" + all.size() + ")");
                btn.addActionListener(e -> showViewAllDialog("All Pending Payments", all, true));
                vr.add(btn);
                card.add(vr);
            }
        }
        card.add(Box.createVerticalStrut(2));
        return card;
    }

    // ─────────────────────────────────────────────────────────────
    // buildUpcomingRow()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildUpcomingRow(String[] row) {
        String apptId      = row[0];
        String vehicleId   = row[1];
        String techId      = row[2];
        String serviceType = row[3];
        String status      = row[4];
        String dateTime    = row[5];
        String duration    = row[6];
        String techName     = resolveUserName(techId);
        String vehicleLabel = appointmentController.getVehicleLabel(vehicleId);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel icon = new JLabel("\uD83D\uDCC5");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 16));
        icon.setVerticalAlignment(SwingConstants.CENTER);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(24, 0));
        panel.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel l1 = new JLabel(apptId + "  \u00B7  " + serviceType);
        l1.setFont(new Font("SansSerif", Font.BOLD, 12));
        l1.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel l2 = new JLabel("Vehicle: " + vehicleLabel);
        l2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l2.setForeground(UIConstants.TEXT_MUTED);
        JLabel l3 = new JLabel("Tech: " + techName + "  |  " + dateTime + "  \u00B7  " + duration + " hr(s)");
        l3.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l3.setForeground(UIConstants.TEXT_MUTED);
        info.add(Box.createVerticalGlue());
        info.add(l1);
        info.add(Box.createVerticalStrut(2));
        info.add(l2);
        info.add(Box.createVerticalStrut(2));
        info.add(l3);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        JLabel badge = new JLabel(status);
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setForeground(status.equalsIgnoreCase("In Progress")
                ? new Color(40, 130, 220) : new Color(230, 160, 40));
        badge.setVerticalAlignment(SwingConstants.CENTER);
        badge.setHorizontalAlignment(SwingConstants.RIGHT);
        badge.setPreferredSize(new Dimension(65, 0));
        panel.add(badge, BorderLayout.EAST);
        return panel;
    }

    // ─────────────────────────────────────────────────────────────
    // buildPaymentRow()
    // ─────────────────────────────────────────────────────────────
    private JPanel buildPaymentRow(String[] row, JDialog parentDialog) {
        String apptId      = row[0];
        String vehicleId   = row[1];
        String techId      = row[2];
        String serviceType = row[3];
        String duration    = row[6];
        String techName     = resolveUserName(techId);
        String vehicleLabel = appointmentController.getVehicleLabel(vehicleId);
        String amountStr    = appointmentController.calculateAmount(serviceType, duration);

        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel icon = new JLabel("\uD83D\uDCB3");
        icon.setFont(new Font("SansSerif", Font.PLAIN, 16));
        icon.setVerticalAlignment(SwingConstants.CENTER);
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(24, 0));
        panel.add(icon, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel l1 = new JLabel(apptId + "  \u00B7  " + serviceType);
        l1.setFont(new Font("SansSerif", Font.BOLD, 12));
        l1.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel l2 = new JLabel("Vehicle: " + vehicleLabel);
        l2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l2.setForeground(UIConstants.TEXT_MUTED);
        JLabel l3 = new JLabel("Tech: " + techName + "  |  RM " + amountStr);
        l3.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l3.setForeground(UIConstants.TEXT_MUTED);
        info.add(Box.createVerticalGlue());
        info.add(l1);
        info.add(Box.createVerticalStrut(2));
        info.add(l2);
        info.add(Box.createVerticalStrut(2));
        info.add(l3);
        info.add(Box.createVerticalGlue());
        panel.add(info, BorderLayout.CENTER);

        final String fa = amountStr;

        JButton payBtn = new JButton("Pay") {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(60, 170, 90) : new Color(80, 190, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        payBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        payBtn.setForeground(Color.WHITE);
        payBtn.setContentAreaFilled(false);
        payBtn.setBorderPainted(false);
        payBtn.setFocusPainted(false);
        payBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        payBtn.setPreferredSize(new Dimension(72, 34));
        payBtn.setMinimumSize(new Dimension(72, 34));
        payBtn.setMaximumSize(new Dimension(72, 34));
        payBtn.addActionListener(e -> showPaymentInvoiceDialog(
                apptId, vehicleId, serviceType, duration, fa, row, parentDialog));
        panel.add(payBtn, BorderLayout.EAST);
        return panel;
    }

    private void showViewAllDialog(String dialogTitle, List<String[]> allRows, boolean isPayment) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                dialogTitle, true);
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
        JLabel cl = new JLabel(allRows.size() + " records");
        cl.setFont(UIConstants.FONT_SMALL);
        cl.setForeground(UIConstants.TEXT_MUTED);
        header.add(cl, BorderLayout.EAST);
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

    private void showPaymentInvoiceDialog(String apptId, String vehicleId,
            String serviceType, String duration, String amountStr,
            String[] row, JDialog parentDialog) {

        User user = app.getLoggedInUserObj();
        if (user == null) return;

        String customerName = user.getName();
        String techName     = resolveUserName(row[2]);
        String dateTime     = row[5];
        String vehicleLabel = appointmentController.getVehicleLabel(vehicleId);
        int hours = 1;
        try { hours = Integer.parseInt(duration.trim()); } catch (NumberFormatException ignored) {}
        double totalAmount;
        try { totalAmount = Double.parseDouble(amountStr); }
        catch (NumberFormatException e) { totalAmount = 150.00; }

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Payment Invoice — " + apptId, true);
        dialog.setSize(430, 490);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel orgName = new JLabel("APU Automotive Service Centre");
        orgName.setFont(new Font("SansSerif", Font.BOLD, 13));
        orgName.setForeground(UIConstants.PRIMARY);
        orgName.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(orgName);
        JLabel orgSub = new JLabel("Official Payment Invoice");
        orgSub.setFont(UIConstants.FONT_SMALL);
        orgSub.setForeground(UIConstants.TEXT_MUTED);
        orgSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(orgSub);
        content.add(Box.createVerticalStrut(12));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(6));
        content.add(makeInvoiceRow("Appointment ID", apptId,             false));
        content.add(makeInvoiceRow("Customer",        customerName,       true));
        content.add(makeInvoiceRow("Vehicle",         vehicleLabel,       false));
        content.add(makeInvoiceRow("Technician",      techName,           true));
        content.add(makeInvoiceRow("Service Type",    serviceType,        false));
        content.add(makeInvoiceRow("Date & Time",     dateTime,           true));
        content.add(makeInvoiceRow("Service Hours",   hours + " hour(s)", false));
        content.add(Box.createVerticalStrut(6));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(10));

        JPanel methodRow = new JPanel(new BorderLayout(10, 0));
        methodRow.setOpaque(false);
        methodRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        methodRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel ml = new JLabel("Payment Type");
        ml.setFont(UIConstants.FONT_SMALL_BOLD);
        ml.setForeground(UIConstants.TEXT_MUTED);
        ml.setPreferredSize(new Dimension(105, 18));
        JComboBox<String> methodCombo = new JComboBox<>(new String[]{"Cash", "Card", "Online"});
        methodCombo.setFont(UIConstants.FONT_BODY);
        methodCombo.setBackground(Color.WHITE);
        methodRow.add(ml, BorderLayout.WEST);
        methodRow.add(methodCombo, BorderLayout.CENTER);
        content.add(methodRow);
        content.add(Box.createVerticalStrut(10));
        content.add(makeInvoiceSeparator());
        content.add(Box.createVerticalStrut(10));

        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel tLabel = new JLabel("Total Amount");
        tLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        tLabel.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel tValue = new JLabel(String.format("RM %.2f", totalAmount));
        tValue.setFont(new Font("SansSerif", Font.BOLD, 15));
        tValue.setForeground(new Color(40, 160, 80));
        tValue.setHorizontalAlignment(SwingConstants.RIGHT);
        totalRow.add(tLabel, BorderLayout.WEST);
        totalRow.add(tValue, BorderLayout.EAST);
        content.add(totalRow);
        content.add(Box.createVerticalStrut(14));

        final String fas = String.format("%.2f", totalAmount);

        JButton confirmBtn = new JButton("Confirm & Pay") {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(60, 170, 90) : new Color(80, 190, 110));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        confirmBtn.setForeground(Color.WHITE);
        confirmBtn.setContentAreaFilled(false);
        confirmBtn.setBorderPainted(false);
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        confirmBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        confirmBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        confirmBtn.setPreferredSize(new Dimension(Integer.MAX_VALUE, 38));

        confirmBtn.addActionListener(e -> {
            String method = (String) methodCombo.getSelectedItem();
            boolean saved = appointmentController.savePayment(apptId, vehicleId, fas, method);
            if (saved) {
                dialog.dispose();
                JOptionPane.showMessageDialog(this,
                        "Payment of RM " + fas + " via " + method + " recorded successfully.",
                        "Payment Successful", JOptionPane.INFORMATION_MESSAGE);
                refreshUser();
                if (parentDialog != null && parentDialog.isVisible()) {
                    List<String[]> remaining = appointmentController.getUnpaidAppointments();
                    if (remaining.size() <= 2) parentDialog.dispose();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Failed to save payment. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        content.add(confirmBtn);
        dialog.add(content, BorderLayout.CENTER);
        dialog.setVisible(true);
    }

    // ── Invoice helpers ───────────────────────────────────────────
    private JPanel makeInvoiceRow(String label, String value, boolean shaded) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(true);
        row.setBackground(shaded ? new Color(245, 246, 248) : Color.WHITE);
        row.setBorder(new EmptyBorder(5, 4, 5, 4));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(115, 18));
        JLabel val = new JLabel(value);
        val.setFont(UIConstants.FONT_SMALL_BOLD);
        val.setForeground(UIConstants.TEXT_PRIMARY);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JSeparator makeInvoiceSeparator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(220, 222, 228));
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JButton createTextLinkButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.BOLD, 12));
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
        headerTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
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
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih), cx = (iw - crop) / 2, cy = (ih - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38, cx, cy, cx + crop, cy + crop, null);
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
        profileLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));

        JLabel da = new JLabel("\u25BE");
        da.setFont(new Font("SansSerif", Font.PLAIN, 12));
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
                if (getClientProperty("active") == Boolean.TRUE) g2.setColor(UIConstants.SIDEBAR_ACTIVE);
                else if (getModel().isRollover()) g2.setColor(UIConstants.SIDEBAR_HOVER);
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
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.PRIMARY, 1),
                new EmptyBorder(2, 3, 2, 3)));
        return field;
    }

    private JPanel makeLabelledField(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 1));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel makeLabelledCombo(String label, JComboBox<String> combo) {
        JPanel panel = new JPanel(new BorderLayout(0, 1));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        return panel;
    }

    private JLabel makeEmptyLabel(String message) {
        JLabel label = new JLabel(message);
        label.setFont(new Font("SansSerif", Font.PLAIN, 13));
        label.setForeground(UIConstants.TEXT_SECONDARY);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return label;
    }

    private JButton createActionButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited (MouseEvent e) { hov = false; repaint(); }
            }); }
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