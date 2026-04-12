package view;

import model.BackgroundImageStorage;
import model.CustomerProfileController;
import model.ProfilePicStorage;
import model.User;
import model.VehicleSectionController;
import model.VehicleService;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * ============================================================
 * ViewProfile.java — Customer Profile Page
 * ============================================================
 *
 * BUG FIX IN THIS VERSION:
 *
 *   PROBLEM — Add Vehicle Form Was Too Narrow
 *   -----------------------------------------
 *   The add form (vehicleAddPanel) appeared much smaller than the
 *   vehicle data rows above it. This happened because of TWO issues
 *   working together:
 *
 *   Issue 1: form.setPreferredSize(new Dimension(0, ROW_H))
 *     Setting the preferred WIDTH to 0 told BoxLayout "this panel
 *     wants to be 0 pixels wide". BoxLayout uses preferred size as
 *     a hint, so the form collapsed to a tiny width.
 *
 *   Issue 2: The form panel's AlignmentX was LEFT_ALIGNMENT (0.0f)
 *     while BoxLayout(Y_AXIS) aligns children to their AlignmentX.
 *     A mismatch between the form and other children caused the
 *     layout engine to not stretch it to full width.
 *
 *   FIX APPLIED:
 *   1. Removed setPreferredSize() from the add form entirely.
 *      We only keep setMaximumSize() so BoxLayout can still cap
 *      the height. Width is now determined naturally by the layout.
 *
 *   2. Changed AlignmentX on all panels inside vehicleListPanel
 *      to LEFT_ALIGNMENT consistently so BoxLayout stretches
 *      every child to the same full width.
 *
 *   3. Wrapped vehicleListPanel in a helper JPanel that uses
 *      BorderLayout — this forces the inner BoxLayout panel to
 *      stretch horizontally to fill the card width, which is the
 *      same technique used by the vehicle data rows.
 *
 * ============================================================
 */
public class ViewProfile extends JPanel {

    // =========================================================
    // FIELDS
    // =========================================================

    private final AppFrame                  app;
    private final CustomerProfileController profileController;
    private final VehicleSectionController  vehicleController;
    private final VehicleService            vehicleService;
    private final ProfilePicStorage         profilePicStorage;
    private final BackgroundImageStorage    backgroundStorage;

    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;

    private JPanel profileBanner;
    private JLabel profilePicLabel;

    private JLabel displayNameLabel;
    private JLabel displayEmailLabel;
    private JLabel displayRoleLabel;

    private JTextField editNameField;
    private JTextField editEmailField;

    private JPanel nameReadPanel;
    private JPanel nameEditPanel;
    private JPanel emailReadPanel;
    private JPanel emailEditPanel;

    private JButton btnEdit;
    private JButton btnSave;
    private JButton btnCancel;

    private JPanel vehicleListPanel;
    private JPanel vehicleAddPanel;
    private JPanel vehicleCard;

    private JComboBox<String> addTypeCombo;
    private int avatarColorIndex = 0;

    // =========================================================
    // COLOURS
    // =========================================================

    private static final Color BLUE        = new Color(80,  110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);
    private static final Color GREEN       = new Color(80,  190, 110);
    private static final Color RED         = new Color(210,  70,  70);
    private static final Color GREY_BTN    = new Color(150, 150, 165);
    private static final Color TEXT_DARK   = new Color(30,  35,  50);
    private static final Color TEXT_GREY   = new Color(130, 135, 155);
    private static final Color PAGE_BG     = new Color(245, 246, 250);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color CARD_BORDER = new Color(220, 222, 230);

    private static final Color[] AVATAR_COLORS = {
        new Color(80,  110, 230), new Color(220,  70,  70),
        new Color(80,  190, 110), new Color(230, 160,  40),
        new Color(160,  80, 230), new Color( 40, 180, 200),
        new Color(220,  80, 160), new Color(100, 100, 120),
    };

    // =========================================================
    // CONSTANTS
    // =========================================================

    /** Max vehicle rows shown before a "View All" link appears. */
    private static final int MAX_VISIBLE = 4;

    /**
     * ROW_H — height used for BOTH vehicle rows AND the add form.
     * One constant keeps everything exactly the same size.
     */
    private static final int ROW_H  = 64;
    private static final int EDIT_H = 68;

    /** Size of the icon canvas inside each vehicle row. */
    private static final int ICON_SIZE = 44;

    // =========================================================
    // UNICODE EMOJI ICONS
    // =========================================================

    /** Car emoji — shown for vehicle type "Car" */
    private static final String ICON_CAR   = "\uD83D\uDE97";

    /** Motorcycle emoji — shown for vehicle type "Motor" */
    private static final String ICON_MOTOR = "\uD83C\uDFCD";


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ViewProfile(AppFrame app,
                       VehicleSectionController vehicleController,
                       CustomerProfileController profileController,
                       VehicleService vehicleService,
                       ProfilePicStorage profilePicStorage,
                       BackgroundImageStorage backgroundStorage) {

        this.app               = app;
        this.vehicleController = vehicleController;
        this.profileController = profileController;
        this.vehicleService    = vehicleService;
        this.profilePicStorage = profilePicStorage;
        this.backgroundStorage = backgroundStorage;

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        JScrollPane scroll = new JScrollPane(buildPage());
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(PAGE_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Load vehicle data after the UI is fully built
        SwingUtilities.invokeLater(this::loadVehicles);
    }


    // =========================================================
    // PUBLIC METHODS
    // =========================================================

    /** Call after login or after saving profile edits. */
    public void refreshUser() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        avatarColorIndex = user.getProfilePicture();
        profileImage     = profilePicStorage.loadImage(user.getUserId());
        bannerImage      = backgroundStorage.loadImage(user.getUserId());

        if (displayNameLabel  != null) displayNameLabel.setText(user.getName());
        if (displayEmailLabel != null) displayEmailLabel.setText(user.getEmail());
        if (displayRoleLabel  != null) {
            String role = user.getRole();
            if (role != null && !role.isEmpty())
                role = Character.toUpperCase(role.charAt(0)) + role.substring(1).toLowerCase();
            displayRoleLabel.setText(role);
        }

        if (profileBanner   != null) profileBanner.repaint();
        if (profilePicLabel != null) profilePicLabel.repaint();
        if (btnEdit != null && !btnEdit.isVisible()) exitEditMode();
        loadVehicles();
    }

    /**
     * Called by the controller callback chain.
     * Rebuilds all vehicle rows with fresh data.
     * Each String[]: [0]id [1]type [2]plate [3]brand [4]year [5]colour
     */
    public void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;
        boolean addFormWasOpen = (vehicleAddPanel != null) && vehicleAddPanel.isVisible();
        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered yet."));
        } else {
            int toShow = Math.min(MAX_VISIBLE, vehicles.size());
            for (int i = 0; i < toShow; i++) {
                String[] v = vehicles.get(i);
                // FIX: set AlignmentX to LEFT on every vehicle row
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(row);
                if (i < toShow - 1) vehicleListPanel.add(Box.createVerticalStrut(6));
            }
        }

        if (vehicleAddPanel != null) {
            vehicleListPanel.add(Box.createVerticalStrut(6));
            // FIX: make sure the add form also has LEFT alignment to match rows
            vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            vehicleListPanel.add(vehicleAddPanel);
            vehicleAddPanel.setVisible(addFormWasOpen);
        }

        if (vehicles.size() > MAX_VISIBLE) {
            vehicleListPanel.add(Box.createVerticalStrut(4));
            JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            linkRow.setOpaque(false);
            linkRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            linkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            JButton viewAll = makeLinkButton("View All (" + vehicles.size() + ")", BLUE);
            final List<String[]> snap = vehicles;
            viewAll.addActionListener(e -> showViewAllDialog(snap));
            linkRow.add(viewAll);
            vehicleListPanel.add(linkRow);
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
        if (vehicleCard != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
    }


    // =========================================================
    // PRIVATE — load vehicles directly from VehicleService
    // =========================================================

    private void loadVehicles() {
        User user = app.getLoggedInUserObj();
        if (user == null || vehicleListPanel == null) return;
        List<String[]> list = vehicleService.getVehiclesByUserId(user.getUserId());
        rebuildVehicleList(list);
    }


    // =========================================================
    // PAGE LAYOUT
    // =========================================================

    private JPanel buildPage() {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(PAGE_BG);
        page.setBorder(new EmptyBorder(0, 0, 40, 0));
        page.add(buildBannerSection());
        page.add(Box.createVerticalStrut(20));
        page.add(buildTwoColumnSection());
        return page;
    }


    // =========================================================
    // BANNER SECTION
    // =========================================================

    private JPanel buildBannerSection() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHov = {false};
        boolean[] avatarHov = {false};

        profileBanner = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setPaint(new GradientPaint(0, 0, BANNER_BLUE,
                            getWidth(), getHeight(), new Color(55, 85, 205)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (bannerHov[0]) {
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    drawCameraIcon(g2, getWidth() / 2, getHeight() / 2 - 10, 26, Color.WHITE);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String hint = "Click to change";
                    g2.drawString(hint, getWidth() / 2 - fm.stringWidth(hint) / 2, getHeight() / 2 + 30);
                }
                g2.dispose();
            }
        };
        profileBanner.setOpaque(false);
        profileBanner.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profileBanner.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { bannerHov[0] = true;  profileBanner.repaint(); }
            @Override public void mouseExited (MouseEvent e) { bannerHov[0] = false; profileBanner.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { pickBannerImage(); }
        });

        profilePicLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih), ox = (iw - crop) / 2, oy = (ih - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size, ox, oy, ox + crop, oy + crop, null);
                    g2.setClip(null);
                } else {
                    drawDefaultAvatar(g2, size);
                }
                if (avatarHov[0]) {
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.setColor(new Color(0, 0, 0, 100));
                    g2.fillOval(0, 0, size, size);
                    g2.setClip(null);
                    drawCameraIcon(g2, size / 2, size / 2, 18, Color.WHITE);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4));
                g2.drawOval(2, 2, size - 4, size - 4);
                g2.dispose();
            }
        };
        profilePicLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        profilePicLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { avatarHov[0] = true;  profilePicLabel.repaint(); }
            @Override public void mouseExited (MouseEvent e) { avatarHov[0] = false; profilePicLabel.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { pickProfileImage(); }
        });

        hero.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent e) {
                profileBanner.setBounds(0, 0, hero.getWidth(), 170);
                profilePicLabel.setBounds(30, 90, 110, 110);
            }
        });
        profileBanner.setBounds(0, 0, 800, 170);
        profilePicLabel.setBounds(30, 90, 110, 110);

        hero.add(profilePicLabel);
        hero.add(profileBanner);
        hero.setComponentZOrder(profilePicLabel, 0);
        hero.setComponentZOrder(profileBanner,   1);
        return hero;
    }

    private void drawCameraIcon(Graphics2D g2, int cx, int cy, int size, Color c) {
        g2.setColor(c);
        g2.setStroke(new BasicStroke(size / 9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bw = size, bh = size * 7 / 10, bx = cx - bw / 2, by = cy - bh / 2;
        g2.drawRoundRect(bx, by, bw, bh, size / 5, size / 5);
        int lr = size * 22 / 100;
        g2.drawOval(cx - lr, cy - lr + size / 20, lr * 2, lr * 2);
        g2.drawRoundRect(bx + size / 6, by - size / 6, size / 4, size / 6, 2, 2);
    }

    private void drawDefaultAvatar(Graphics2D g2, int size) {
        g2.setColor(AVATAR_COLORS[avatarColorIndex % AVATAR_COLORS.length]);
        g2.fillOval(0, 0, size, size);
        String letter = "U";
        User user = app.getLoggedInUserObj();
        if (user != null && user.getName() != null && !user.getName().isEmpty())
            letter = String.valueOf(user.getName().charAt(0)).toUpperCase();
        g2.setFont(new Font("SansSerif", Font.BOLD, size / 3));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(letter,
                (size - fm.stringWidth(letter)) / 2,
                (size + fm.getAscent() - fm.getDescent()) / 2);
    }


    // =========================================================
    // TWO-COLUMN SECTION
    // =========================================================

    private JPanel buildTwoColumnSection() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setBackground(PAGE_BG);
        row.setBorder(new EmptyBorder(0, 24, 0, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0; c.weighty = 1.0; c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.NORTH;

        c.gridx = 0; c.weightx = 0.45; c.insets = new Insets(0, 0, 0, 14);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(buildPersonalInfoCard());
        row.add(left, c);

        c.gridx = 1; c.weightx = 0.55; c.insets = new Insets(0, 0, 0, 0);
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.add(buildVehicleCard());
        row.add(right, c);

        return row;
    }


    // =========================================================
    // PERSONAL INFORMATION CARD
    // =========================================================

    private JPanel buildPersonalInfoCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ── Title row ──────────────────────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel title = new JLabel("Personal Information");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        titleRow.add(title, BorderLayout.WEST);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        btnGroup.setOpaque(false);
        btnGroup.setPreferredSize(new Dimension(185, 30));

        btnEdit   = makeFilledButton("\u270E  Edit", BLUE,     Color.WHITE);
        btnSave   = makeFilledButton("Save",         GREEN,    Color.WHITE);
        btnCancel = makeFilledButton("Cancel",       GREY_BTN, Color.WHITE);

        btnEdit.setPreferredSize(new Dimension(82, 30));
        btnSave.setPreferredSize(new Dimension(70, 30));
        btnCancel.setPreferredSize(new Dimension(85, 30));

        btnSave.setVisible(false);
        btnCancel.setVisible(false);

        btnGroup.add(btnEdit);
        btnGroup.add(btnSave);
        btnGroup.add(btnCancel);
        titleRow.add(btnGroup, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(10));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(18));

        // ── User data ──────────────────────────────────────────
        User   user  = app.getLoggedInUserObj();
        String name  = (user != null && user.getName()  != null) ? user.getName()  : "";
        String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
        String role  = (user != null && user.getRole()  != null) ? user.getRole()  : "";
        if (!role.isEmpty())
            role = Character.toUpperCase(role.charAt(0)) + role.substring(1).toLowerCase();

        nameReadPanel    = makeReadRow("Username", name);
        displayNameLabel = getValueLabel(nameReadPanel);
        card.add(nameReadPanel);
        card.add(Box.createVerticalStrut(14));

        nameEditPanel = new JPanel(new BorderLayout(10, 0));
        nameEditPanel.setOpaque(false);
        nameEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel nameFieldLabel = makeFieldLabel("Username");
        nameFieldLabel.setPreferredSize(new Dimension(90, 18));

        editNameField = new JTextField(name);
        styleField(editNameField);
        editNameField.setPreferredSize(new Dimension(160, 30));

        nameEditPanel.add(nameFieldLabel, BorderLayout.WEST);
        nameEditPanel.add(editNameField,  BorderLayout.CENTER);
        nameEditPanel.setVisible(false);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

        emailReadPanel    = makeReadRow("Email", email);
        displayEmailLabel = getValueLabel(emailReadPanel);
        card.add(emailReadPanel);
        card.add(Box.createVerticalStrut(14));

        emailEditPanel = new JPanel(new BorderLayout(10, 0));
        emailEditPanel.setOpaque(false);
        emailEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel emailFieldLabel = makeFieldLabel("Email");
        emailFieldLabel.setPreferredSize(new Dimension(90, 18));

        editEmailField = new JTextField(email);
        styleField(editEmailField);
        editEmailField.setPreferredSize(new Dimension(160, 30));

        emailEditPanel.add(emailFieldLabel, BorderLayout.WEST);
        emailEditPanel.add(editEmailField,  BorderLayout.CENTER);
        emailEditPanel.setVisible(false);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));

        JPanel roleRow = makeReadRow("Role", role);
        displayRoleLabel = getValueLabel(roleRow);
        card.add(roleRow);
        card.add(Box.createVerticalStrut(4));

        btnEdit.addActionListener(e -> enterEditMode());
        btnCancel.addActionListener(e -> exitEditMode());
        btnSave.addActionListener(e -> saveProfileChanges());

        KeyAdapter enterSaves = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) saveProfileChanges();
            }
        };
        editNameField.addKeyListener(enterSaves);
        editEmailField.addKeyListener(enterSaves);

        return card;
    }

    private void enterEditMode() {
        editNameField.setText(profileController.getCurrentName());
        editEmailField.setText(profileController.getCurrentEmail());
        nameReadPanel.setVisible(false);  nameEditPanel.setVisible(true);
        emailReadPanel.setVisible(false); emailEditPanel.setVisible(true);
        btnEdit.setVisible(false); btnSave.setVisible(true); btnCancel.setVisible(true);
        editNameField.requestFocusInWindow();
    }

    private void exitEditMode() {
        if (editNameField  != null) editNameField.setText(profileController.getCurrentName());
        if (editEmailField != null) editEmailField.setText(profileController.getCurrentEmail());
        if (nameReadPanel  != null) nameReadPanel.setVisible(true);
        if (nameEditPanel  != null) nameEditPanel.setVisible(false);
        if (emailReadPanel != null) emailReadPanel.setVisible(true);
        if (emailEditPanel != null) emailEditPanel.setVisible(false);
        if (btnEdit   != null) btnEdit.setVisible(true);
        if (btnSave   != null) btnSave.setVisible(false);
        if (btnCancel != null) btnCancel.setVisible(false);
    }

    private void saveProfileChanges() {
        String newName  = editNameField.getText().trim();
        String newEmail = editEmailField.getText().trim();
        if (profileController.hasNoChanges(newName, newEmail)) {
            JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes",
                    JOptionPane.INFORMATION_MESSAGE);
            exitEditMode();
            return;
        }
        try {
            if (profileController.saveProfile(newName, newEmail)) {
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


    // =========================================================
    // MY VEHICLES CARD
    // =========================================================

    private JPanel buildVehicleCard() {
        vehicleCard = makeCard();
        vehicleCard.setLayout(new BoxLayout(vehicleCard, BoxLayout.Y_AXIS));
        vehicleCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // ── Title row with "+Add" button ──────────────────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);  // FIX: consistent alignment

        JLabel title = new JLabel("My Vehicles");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        titleRow.add(title, BorderLayout.WEST);

        JButton addBtn = makeFilledButton("+ Add", BLUE, Color.WHITE);
        addBtn.setPreferredSize(new Dimension(80, 30));
        titleRow.add(addBtn, BorderLayout.EAST);

        vehicleCard.add(titleRow);
        vehicleCard.add(Box.createVerticalStrut(8));

        JSeparator divider = makeDivider();
        vehicleCard.add(divider);
        vehicleCard.add(Box.createVerticalStrut(12));

        // ── FIX: Wrap vehicleListPanel in a BorderLayout panel ──────────────
        //
        // WHY THIS FIX WORKS (beginner-friendly explanation):
        //
        // BoxLayout (Y_AXIS) stacks panels top-to-bottom. It respects each
        // child's AlignmentX and preferred/maximum width. The problem is that
        // BoxLayout doesn't automatically stretch children to fill the full
        // available width — it sizes them based on their own preferred size.
        //
        // By wrapping vehicleListPanel in a "stretchWrapper" that uses
        // BorderLayout and placing vehicleListPanel in the CENTER, we let
        // BorderLayout handle the horizontal stretching. BorderLayout ALWAYS
        // fills the CENTER component to the full available width.
        //
        // This is the same reason the vehicle data rows looked correct — they
        // were already using BorderLayout internally, so they filled the width.
        // Now the add form (which lives inside vehicleListPanel) also benefits
        // from the same full-width stretching behaviour.

        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        vehicleListPanel.add(makeEmptyLabel("Loading vehicles..."));

        // The stretch wrapper: forces vehicleListPanel to fill full card width
        JPanel stretchWrapper = new JPanel(new BorderLayout());
        stretchWrapper.setOpaque(false);
        stretchWrapper.setAlignmentX(Component.LEFT_ALIGNMENT); // consistent with siblings
        stretchWrapper.add(vehicleListPanel, BorderLayout.CENTER);

        vehicleCard.add(stretchWrapper);

        // Build add form — this is now inside stretchWrapper → vehicleListPanel
        vehicleAddPanel = buildAddForm();
        vehicleAddPanel.setVisible(false);
        vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT); // FIX: match other rows

        addBtn.addActionListener(e -> {
            boolean open = !vehicleAddPanel.isVisible();
            vehicleAddPanel.setVisible(open);
            if (open) {
                clearFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
            }
            vehicleCard.revalidate();
            vehicleCard.repaint();
        });

        SwingUtilities.invokeLater(this::loadVehicles);
        return vehicleCard;
    }

    /**
     * Builds the inline add-vehicle form.
     *
     * FIX EXPLANATION (beginner-friendly):
     *
     * The original code had:
     *   form.setPreferredSize(new Dimension(0, ROW_H));
     *
     * Setting the preferred WIDTH to 0 is the main culprit. When BoxLayout
     * asks the form "how wide do you want to be?", the form answered "0 pixels".
     * BoxLayout then allocated only a tiny amount of space for it.
     *
     * The fix is simple:
     *   - Remove setPreferredSize() entirely. Let the form calculate its own
     *     natural width based on the fields inside it.
     *   - Keep setMaximumSize() only for height control (capping the height
     *     so the form doesn't grow too tall).
     *   - The width is now controlled by the parent stretchWrapper (BorderLayout
     *     CENTER), which fills the full available card width automatically.
     */
    private JPanel buildAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                new EmptyBorder(6, 10, 6, 10)));

        // FIX: Only set maximum HEIGHT — do NOT set preferred width to 0.
        // Previously: form.setPreferredSize(new Dimension(0, ROW_H));  ← WRONG
        // Now we only cap the height, width is determined by the layout parent.
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        // Note: we intentionally do NOT call setPreferredSize() here anymore.
        // The width will now naturally stretch to fill the card, just like the
        // vehicle data rows above the form.

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.gridy   = 0;
        g.weighty = 1.0;
        g.insets  = new Insets(0, 1, 0, 1);

        addTypeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        addTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JTextField plateF  = makeSmallField("");
        JTextField brandF  = makeSmallField("");
        JTextField yearF   = makeSmallField("");
        JTextField colourF = makeSmallField("");

        g.gridx = 0; g.weightx = 0;   g.ipadx = 28; fields.add(labelWrap("Type",          addTypeCombo), g);
        g.gridx = 1; g.weightx = 0;   g.ipadx = 40; fields.add(labelWrap("Car Plate",     plateF),       g);
        g.gridx = 2; g.weightx = 0.7; g.ipadx = 0;  fields.add(labelWrap("Brand / Model", brandF),       g);
        g.gridx = 3; g.weightx = 0;   g.ipadx = 26; fields.add(labelWrap("Year",          yearF),        g);
        g.gridx = 4; g.weightx = 0;   g.ipadx = 34; fields.add(labelWrap("Colour",        colourF),      g);
        form.add(fields, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btns.setOpaque(false);
        JButton saveBtn   = makeFilledButton("Save",   GREEN,    Color.WHITE);
        JButton cancelBtn = makeFilledButton("Cancel", GREY_BTN, Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(65, 30));
        cancelBtn.setPreferredSize(new Dimension(80, 30));
        btns.add(saveBtn);
        btns.add(cancelBtn);
        form.add(btns, BorderLayout.EAST);

        cancelBtn.addActionListener(e -> {
            vehicleAddPanel.setVisible(false);
            clearFields(vehicleAddPanel);
            if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
            if (vehicleCard  != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
        });

        Runnable doSave = () -> {
            String   type = (String) addTypeCombo.getSelectedItem();
            String[] data = { type, plateF.getText().trim(), brandF.getText().trim(),
                              yearF.getText().trim(), colourF.getText().trim() };
            boolean ok = vehicleController.handleAdd(data);
            if (ok) {
                clearFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
                vehicleAddPanel.setVisible(false);
                if (vehicleCard != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
                loadVehicles();
                JOptionPane.showMessageDialog(app, "Vehicle added successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        };
        saveBtn.addActionListener(e -> doSave.run());

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        plateF.addKeyListener(enter);
        brandF.addKeyListener(enter);
        yearF.addKeyListener(enter);
        colourF.addKeyListener(enter);

        boolean[] comboOpen = {false};
        addTypeCombo.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible  (javax.swing.event.PopupMenuEvent e) { comboOpen[0] = true;  }
            @Override public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) { comboOpen[0] = false; }
            @Override public void popupMenuCanceled           (javax.swing.event.PopupMenuEvent e) { comboOpen[0] = false; }
        });
        addTypeCombo.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !comboOpen[0]) doSave.run();
            }
        });

        return form;
    }


    // =========================================================
    // VEHICLE ROW
    // =========================================================

    private JPanel buildVehicleRow(String vehicleType, String plate,
                                   String brand, String year, String colour) {

        // ── DISPLAY CARD ──────────────────────────────────────────
        JPanel display = new JPanel(new BorderLayout(0, 0));
        display.setOpaque(false);
        display.setBorder(new EmptyBorder(10, 12, 10, 12));

        // ── VEHICLE ICON ──────────────────────────────────────────
        String iconText = "Motor".equalsIgnoreCase(vehicleType) ? ICON_MOTOR : ICON_CAR;
        JLabel iconLbl  = new JLabel(iconText, SwingConstants.CENTER);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 24));
        iconLbl.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMinimumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMaximumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        display.add(iconLbl, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel brandLbl = new JLabel(brand);
        brandLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLbl.setForeground(TEXT_DARK);
        brandLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLbl = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLbl.setForeground(TEXT_GREY);
        detailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(Box.createVerticalGlue());
        info.add(brandLbl);
        info.add(Box.createVerticalStrut(3));
        info.add(detailLbl);
        info.add(Box.createVerticalGlue());
        display.add(info, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        JButton editBtn   = makeFilledButton("Edit",   BLUE, Color.WHITE);
        JButton removeBtn = makeFilledButton("Remove", RED,  Color.WHITE);
        editBtn.setPreferredSize(new Dimension(70, 30));
        removeBtn.setPreferredSize(new Dimension(85, 30));
        actions.add(editBtn);
        actions.add(removeBtn);
        display.add(actions, BorderLayout.EAST);

        // ── EDIT CARD ─────────────────────────────────────────────
        JPanel editCard = buildEditCard(vehicleType, plate, brand, year, colour);

        // ── CARD LAYOUT SWITCHER ──────────────────────────────────
        CardLayout switcher    = new CardLayout();
        JPanel     switchPanel = new JPanel(switcher);
        switchPanel.setOpaque(false);
        switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        switchPanel.add(display,  "display");
        switchPanel.add(editCard, "edit");
        switcher.show(switchPanel, "display");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        // FIX: ensure wrapper also has LEFT alignment for BoxLayout consistency
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(switchPanel, BorderLayout.CENTER);

        JComboBox<String> eType   = (JComboBox<String>) editCard.getClientProperty("typeCombo");
        JTextField        ePlate  = (JTextField)        editCard.getClientProperty("plate");
        JTextField        eBrand  = (JTextField)        editCard.getClientProperty("brand");
        JTextField        eYear   = (JTextField)        editCard.getClientProperty("year");
        JTextField        eColour = (JTextField)        editCard.getClientProperty("colour");
        JButton           eSave   = (JButton)           editCard.getClientProperty("saveBtn");
        JButton           eCancel = (JButton)           editCard.getClientProperty("cancelBtn");

        editBtn.addActionListener(e -> {
            if (eType   != null) eType.setSelectedItem(vehicleType);
            if (ePlate  != null) ePlate.setText(plate);
            if (eBrand  != null) eBrand.setText(brand);
            if (eYear   != null) eYear.setText(year);
            if (eColour != null) eColour.setText(colour);
            switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            switcher.show(switchPanel, "edit");
            if (ePlate != null) ePlate.requestFocusInWindow();
            if (wrapper.getParent() != null) wrapper.getParent().revalidate();
        });

        if (eCancel != null) {
            eCancel.addActionListener(e -> {
                switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switchPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
            });
        }

        Runnable doSave = () -> {
            String nt = (eType   != null) ? (String) eType.getSelectedItem() : vehicleType;
            String np = (ePlate  != null) ? ePlate.getText().trim()  : plate;
            String nb = (eBrand  != null) ? eBrand.getText().trim()  : brand;
            String ny = (eYear   != null) ? eYear.getText().trim()   : year;
            String nc = (eColour != null) ? eColour.getText().trim() : colour;
            if (nt.equals(vehicleType) && np.equals(plate) && nb.equals(brand)
                    && ny.equals(year) && nc.equals(colour)) {
                JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes",
                        JOptionPane.INFORMATION_MESSAGE);
                if (eCancel != null) eCancel.doClick();
                return;
            }
            boolean ok = vehicleController.handleEdit(plate, new String[]{nt, np, nb, ny, nc});
            if (ok) {
                switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switchPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
                loadVehicles();
                JOptionPane.showMessageDialog(app, "Vehicle updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        if (eSave != null) eSave.addActionListener(e -> doSave.run());

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        if (ePlate  != null) ePlate.addKeyListener(enter);
        if (eBrand  != null) eBrand.addKeyListener(enter);
        if (eYear   != null) eYear.addKeyListener(enter);
        if (eColour != null) eColour.addKeyListener(enter);

        removeBtn.addActionListener(e -> {
            int ch = JOptionPane.showConfirmDialog(app,
                    "Remove " + brand + " (" + plate + ")?",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (ch == JOptionPane.YES_OPTION) {
                User    u   = app.getLoggedInUserObj();
                boolean del = (u != null) && vehicleService.deleteVehicle(u.getUserId(), plate);
                if (del) loadVehicles();
                else JOptionPane.showMessageDialog(app,
                        "Failed to remove vehicle. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        return wrapper;
    }

    private JPanel buildEditCard(String vehicleType, String plate,
                                 String brand, String year, String colour) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel fieldRow = new JPanel(new GridBagLayout());
        fieldRow.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill    = GridBagConstraints.BOTH;
        g.gridy   = 0;
        g.weighty = 1.0;
        g.insets  = new Insets(0, 1, 0, 1);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        typeCombo.setSelectedItem(vehicleType);
        typeCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JTextField plateF  = makeSmallField(plate);
        JTextField brandF  = makeSmallField(brand);
        JTextField yearF   = makeSmallField(year);
        JTextField colourF = makeSmallField(colour);

        g.gridx = 0; g.weightx = 0;   g.ipadx = 28; fieldRow.add(labelWrap("Type",   typeCombo), g);
        g.gridx = 1; g.weightx = 0;   g.ipadx = 40; fieldRow.add(labelWrap("Plate",  plateF),    g);
        g.gridx = 2; g.weightx = 0.7; g.ipadx = 0;  fieldRow.add(labelWrap("Brand",  brandF),    g);
        g.gridx = 3; g.weightx = 0;   g.ipadx = 26; fieldRow.add(labelWrap("Year",   yearF),     g);
        g.gridx = 4; g.weightx = 0;   g.ipadx = 34; fieldRow.add(labelWrap("Colour", colourF),   g);
        card.add(fieldRow, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridBagLayout());
        btns.setOpaque(false);
        GridBagConstraints bc = new GridBagConstraints();
        bc.gridy  = 0;
        bc.anchor = GridBagConstraints.CENTER;
        bc.insets = new Insets(0, 4, 0, 0);
        JButton saveBtn   = makeFilledButton("Save",   GREEN,    Color.WHITE);
        JButton cancelBtn = makeFilledButton("Cancel", GREY_BTN, Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(65, 30));
        cancelBtn.setPreferredSize(new Dimension(80, 30));
        bc.gridx = 0; btns.add(saveBtn,   bc);
        bc.gridx = 1; btns.add(cancelBtn, bc);
        card.add(btns, BorderLayout.EAST);

        card.putClientProperty("typeCombo", typeCombo);
        card.putClientProperty("plate",     plateF);
        card.putClientProperty("brand",     brandF);
        card.putClientProperty("year",      yearF);
        card.putClientProperty("colour",    colourF);
        card.putClientProperty("saveBtn",   saveBtn);
        card.putClientProperty("cancelBtn", cancelBtn);
        return card;
    }


    // =========================================================
    // VIEW ALL DIALOG
    // =========================================================

    private void showViewAllDialog(List<String[]> initial) {
        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "All My Vehicles", true);
        dlg.setSize(820, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 22, 12, 22));
        JLabel titleLbl = new JLabel("All My Vehicles");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(TEXT_DARK);
        header.add(titleLbl, BorderLayout.WEST);
        JLabel countLbl = new JLabel(initial.size() + " vehicles");
        countLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLbl.setForeground(TEXT_GREY);
        header.add(countLbl, BorderLayout.EAST);
        dlg.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(8, 18, 18, 18));

        Runnable[] refresh = {null};
        refresh[0] = () -> {
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
                    JPanel row = buildDialogRow(latest.get(i), dlg, refresh[0]);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(row);
                    if (i < latest.size() - 1) listPanel.add(Box.createVerticalStrut(6));
                }
            }
            listPanel.revalidate();
            listPanel.repaint();
            loadVehicles();
            if (latest.size() <= MAX_VISIBLE) dlg.dispose();
        };

        for (int i = 0; i < initial.size(); i++) {
            JPanel row = buildDialogRow(initial.get(i), dlg, refresh[0]);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(row);
            if (i < initial.size() - 1) listPanel.add(Box.createVerticalStrut(6));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        dlg.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton closeBtn = makeFilledButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(78, 32));
        closeBtn.addActionListener(e -> dlg.dispose());
        footer.add(closeBtn);
        dlg.add(footer, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private JPanel buildDialogRow(String[] v, JDialog dlg, Runnable onChanged) {
        String vType  = v[1];
        String plate  = v[2];
        String brand  = v[3];
        String year   = v[4];
        String colour = v[5];

        JPanel display = new JPanel(new BorderLayout(0, 0));
        display.setOpaque(false);
        display.setBorder(new EmptyBorder(10, 12, 10, 12));

        // ── VEHICLE ICON ──────────────────────────────────────────
        String iconText = "Motor".equalsIgnoreCase(vType) ? ICON_MOTOR : ICON_CAR;
        JLabel iconLbl  = new JLabel(iconText, SwingConstants.CENTER);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 24));
        iconLbl.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMinimumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMaximumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        display.add(iconLbl, BorderLayout.WEST);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        info.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel brandLbl = new JLabel(brand);
        brandLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLbl.setForeground(TEXT_DARK);
        brandLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLbl = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLbl.setForeground(TEXT_GREY);
        detailLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(Box.createVerticalGlue());
        info.add(brandLbl);
        info.add(Box.createVerticalStrut(3));
        info.add(detailLbl);
        info.add(Box.createVerticalGlue());
        display.add(info, BorderLayout.CENTER);

        JPanel acts = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        acts.setOpaque(false);
        JButton editBtn   = makeFilledButton("Edit",   BLUE, Color.WHITE);
        JButton removeBtn = makeFilledButton("Remove", RED,  Color.WHITE);
        editBtn.setPreferredSize(new Dimension(70, 30));
        removeBtn.setPreferredSize(new Dimension(85, 30));
        acts.add(editBtn);
        acts.add(removeBtn);
        display.add(acts, BorderLayout.EAST);

        JPanel     editCard   = buildEditCard(vType, plate, brand, year, colour);
        CardLayout switcher   = new CardLayout();
        JPanel     switchPanel = new JPanel(switcher);
        switchPanel.setOpaque(false);
        switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        switchPanel.add(display,  "display");
        switchPanel.add(editCard, "edit");
        switcher.show(switchPanel, "display");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(switchPanel, BorderLayout.CENTER);

        JComboBox<String> eType   = (JComboBox<String>) editCard.getClientProperty("typeCombo");
        JTextField        ePlate  = (JTextField)        editCard.getClientProperty("plate");
        JTextField        eBrand  = (JTextField)        editCard.getClientProperty("brand");
        JTextField        eYear   = (JTextField)        editCard.getClientProperty("year");
        JTextField        eColour = (JTextField)        editCard.getClientProperty("colour");
        JButton           eSave   = (JButton)           editCard.getClientProperty("saveBtn");
        JButton           eCancel = (JButton)           editCard.getClientProperty("cancelBtn");

        editBtn.addActionListener(e -> {
            if (eType   != null) eType.setSelectedItem(vType);
            if (ePlate  != null) ePlate.setText(plate);
            if (eBrand  != null) eBrand.setText(brand);
            if (eYear   != null) eYear.setText(year);
            if (eColour != null) eColour.setText(colour);
            switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_H));
            switcher.show(switchPanel, "edit");
            if (ePlate != null) ePlate.requestFocusInWindow();
            if (wrapper.getParent() != null) wrapper.getParent().revalidate();
        });

        if (eCancel != null) {
            eCancel.addActionListener(e -> {
                switchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));
                switcher.show(switchPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
            });
        }

        Runnable doSave = () -> {
            String nt = (eType   != null) ? (String) eType.getSelectedItem() : vType;
            String np = (ePlate  != null) ? ePlate.getText().trim()  : plate;
            String nb = (eBrand  != null) ? eBrand.getText().trim()  : brand;
            String ny = (eYear   != null) ? eYear.getText().trim()   : year;
            String nc = (eColour != null) ? eColour.getText().trim() : colour;
            if (nt.equals(vType) && np.equals(plate) && nb.equals(brand)
                    && ny.equals(year) && nc.equals(colour)) {
                JOptionPane.showMessageDialog(dlg, "No changes were made.", "No Changes",
                        JOptionPane.INFORMATION_MESSAGE);
                if (eCancel != null) eCancel.doClick();
                return;
            }
            if (vehicleController.handleEdit(plate, new String[]{nt, np, nb, ny, nc})) {
                JOptionPane.showMessageDialog(dlg, "Vehicle updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                onChanged.run();
            }
        };
        if (eSave != null) eSave.addActionListener(e -> doSave.run());

        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        if (ePlate  != null) ePlate.addKeyListener(enter);
        if (eBrand  != null) eBrand.addKeyListener(enter);
        if (eYear   != null) eYear.addKeyListener(enter);
        if (eColour != null) eColour.addKeyListener(enter);

        removeBtn.addActionListener(e -> {
            int ch = JOptionPane.showConfirmDialog(dlg,
                    "Remove " + brand + " (" + plate + ")?",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (ch == JOptionPane.YES_OPTION) {
                User u = app.getLoggedInUserObj();
                if (u != null && vehicleService.deleteVehicle(u.getUserId(), plate)) {
                    JOptionPane.showMessageDialog(dlg, brand + " removed.", "Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    onChanged.run();
                } else {
                    JOptionPane.showMessageDialog(dlg, "Failed to remove vehicle.", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return wrapper;
    }


    // =========================================================
    // IMAGE PICKERS
    // =========================================================

    private void pickProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Profile Picture", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage img = ImageIO.read(new File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read image.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!profilePicStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save picture.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            profileImage = img;
            if (profilePicLabel != null) profilePicLabel.repaint();
        } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void pickBannerImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Choose Banner Image", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage img = ImageIO.read(new File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read image.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!backgroundStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save image.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            bannerImage = img;
            if (profileBanner != null) profileBanner.repaint();
        } catch (IOException ex) { ex.printStackTrace(); }
    }


    // =========================================================
    // HELPERS
    // =========================================================

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        return card;
    }

    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    private JPanel makeReadRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setPreferredSize(new Dimension(90, 18));
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 13));
        val.setForeground(TEXT_DARK);
        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    private JLabel getValueLabel(JPanel readRow) {
        return (JLabel) ((BorderLayout) readRow.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
    }

    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setPreferredSize(new Dimension(90, 18));
        return lbl;
    }

    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 2),
                new EmptyBorder(2, 6, 2, 6)));
    }

    private JTextField makeSmallField(String value) {
        JTextField f = new JTextField(value);
        f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 1),
                new EmptyBorder(2, 3, 2, 3)));
        return f;
    }

    private JPanel labelWrap(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(0, 1));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(TEXT_GREY);
        p.add(lbl,   BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private JPanel labelWrap(String label, JComboBox<String> combo) {
        JPanel p = new JPanel(new BorderLayout(0, 1));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(TEXT_GREY);
        p.add(lbl,   BorderLayout.NORTH);
        p.add(combo, BorderLayout.CENTER);
        return p;
    }

    private JButton makeLinkButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 4, 0, 4));
        return btn;
    }

    private JButton makeFilledButton(String text, Color bg, Color fg) {
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
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel makeEmptyLabel(String msg) {
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void clearFields(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JTextField) ((JTextField) comp).setText("");
            else if (comp instanceof Container) clearFields((Container) comp);
        }
    }
}