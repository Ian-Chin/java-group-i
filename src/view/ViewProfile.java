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
 * CHANGES IN THIS VERSION:
 *
 *   My Vehicles section now:
 *   - Shows up to 7 vehicles without scrolling
 *   - If there are MORE than 7 vehicles, a vertical scroll bar
 *     appears ONLY inside the My Vehicles card
 *   - The scroll bar style matches the Upcoming Appointments
 *     section in CounterStaffDashboard.java
 *   - The panel sizes for Personal Info and My Vehicles do NOT change
 *   - The "View All" link is removed — scrolling handles overflow
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

    private JPanel vehicleListPanel;   // The inner panel that holds vehicle rows
    private JPanel vehicleAddPanel;    // The "add vehicle" form
    private JPanel vehicleCard;        // The outer card container

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

    /**
     * MAX_VISIBLE — how many vehicles are shown before a scroll bar appears.
     * Set to 6 — scroll bar only appears when there are 7 or more vehicles.
     */
    private static final int MAX_VISIBLE = 6;

    /** Height of each vehicle row and the add-vehicle form. */
    private static final int ROW_H  = 64;
    private static final int EDIT_H = 68;

    /** Size of the icon canvas inside each vehicle row. */
    private static final int ICON_SIZE = 50;

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

        // The outer scroll pane wraps the WHOLE page
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
     *
     * Each String[] in the list holds:
     *   [0] id  [1] type  [2] plate  [3] brand  [4] year  [5] colour
     *
     * HOW SCROLLING WORKS HERE:
     * - All vehicles are added to vehicleListPanel (no MAX_VISIBLE limit on display)
     * - vehicleListPanel lives inside a JScrollPane
     * - The JScrollPane only shows a scroll bar when the content is taller
     *   than the fixed height we set on the scroll pane
     * - MAX_VISIBLE (6) is used to calculate the preferred height of the
     *   scroll pane so that exactly 6 rows are visible before scrolling starts
     */
    public void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        // Remember if the add-form was open before we rebuild
        boolean addFormWasOpen = (vehicleAddPanel != null) && vehicleAddPanel.isVisible();

        // Clear all existing rows
        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            // Show a friendly message when there are no vehicles
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered yet."));
        } else {
            // Add ALL vehicles to the list — scrolling handles overflow
            for (int i = 0; i < vehicles.size(); i++) {
                String[] v = vehicles.get(i);

                // Build one row for this vehicle
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT); // keep rows aligned left
                vehicleListPanel.add(row);

                // Add a small gap between rows (but not after the last one)
                if (i < vehicles.size() - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(6));
                }
            }
        }

        // Re-add the "add vehicle" form below all the rows — ONLY when it is open.
        //
        // WHY: Even when vehicleAddPanel is hidden (setVisible(false)), Swing
        // still counts it and its gap strut as height inside vehicleListPanel.
        // This causes the content to appear taller than the viewport, which:
        //   1. Triggers a scroll bar even when there are only 6 vehicles
        //   2. Leaves a blank space at the bottom of the card
        //
        // FIX: Only physically add the form panel to the list when it is open.
        // When the form is closed, it is not in the layout at all, so Swing
        // measures no extra height and the scroll bar stays hidden.
        if (vehicleAddPanel != null && addFormWasOpen) {
            vehicleListPanel.add(Box.createVerticalStrut(6));
            vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            vehicleListPanel.add(vehicleAddPanel);
            vehicleAddPanel.setVisible(true);
        }

        // Tell Swing to re-draw the updated list
        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
        if (vehicleCard != null) {
            vehicleCard.revalidate();
            vehicleCard.repaint();
        }
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

        // weightx controls how much of the total width each column gets.
        // 0.19 = Personal Information takes 19% of the width (narrower)
        // 0.81 = My Vehicles takes 81% of the width (wider)
        c.gridx = 0; c.weightx = 0.19; c.insets = new Insets(0, 0, 0, 14);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(buildPersonalInfoCard());
        row.add(left, c);

        c.gridx = 1; c.weightx = 0.81; c.insets = new Insets(0, 0, 0, 0);
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

    /**
     * Builds the "My Vehicles" card.
     *
     * SCROLL BAR EXPLANATION (beginner-friendly):
     *
     * The vehicle list is placed inside a JScrollPane — the same approach
     * used for the Upcoming Appointments section in CounterStaffDashboard.java.
     *
     * How it works step by step:
     *
     * 1. vehicleListPanel — a BoxLayout(Y_AXIS) panel that holds all vehicle rows.
     *    All vehicles are added here; there is no hard cut-off on display count.
     *
     * 2. JScrollPane (vehicleScrollPane) — wraps vehicleListPanel.
     *    When the content inside is taller than the scroll pane's height,
     *    a vertical scroll bar automatically appears.
     *
     * 3. Preferred height of the scroll pane — we set it to exactly
     *    MAX_VISIBLE (7) rows tall. So the first 7 rows are visible without
     *    scrolling; any row beyond the 7th requires scrolling to reach.
     *
     * 4. The scroll pane has no border and a transparent background so
     *    it blends seamlessly inside the card — just like Upcoming Appointments.
     */
    private JPanel buildVehicleCard() {
        vehicleCard = makeCard();
        vehicleCard.setLayout(new BoxLayout(vehicleCard, BoxLayout.Y_AXIS));
        vehicleCard.setBorder(new EmptyBorder(18, 20, 18, 20));

        // ── Title row with "+Add" button ──────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

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

        // ── vehicleListPanel: holds all the rows ─────────────────
        //
        // We use ScrollableVehiclePanel — a private inner class at the
        // bottom of this file that extends JPanel and implements Scrollable.
        //
        // WHY: A plain JPanel inside a JScrollPane cannot tell Swing
        // "stretch my width to fill the viewport". Without this, Swing
        // may show a scroll bar even when fewer than MAX_VISIBLE rows
        // are present, because it cannot correctly compare content height
        // to viewport height.
        //
        // ScrollableVehiclePanel fixes this by implementing Scrollable:
        //   getScrollableTracksViewportWidth()  → true
        //     Always fill full width; no horizontal scroll bar ever.
        //   getScrollableTracksViewportHeight() → false
        //     Allow vertical scrolling only when content genuinely
        //     exceeds the fixed viewport height of MAX_VISIBLE rows.
        vehicleListPanel = new ScrollableVehiclePanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false); // transparent so card background shows through

        // Show a placeholder while real data is loading
        vehicleListPanel.add(makeEmptyLabel("Loading vehicles..."));

        // ── JScrollPane: wraps vehicleListPanel ───────────────────
        //
        // This is the key change: vehicleListPanel is placed inside a
        // JScrollPane so that when there are more than MAX_VISIBLE rows,
        // a vertical scroll bar appears automatically.
        //
        // We use the SAME settings as the Upcoming Appointments scroll pane
        // in CounterStaffDashboard.java:
        //   - VERTICAL_SCROLLBAR_AS_NEEDED  → scroll bar only appears when needed
        //   - HORIZONTAL_SCROLLBAR_NEVER    → no horizontal scroll bar
        //   - setBorder(null)               → no visible border around the scroll pane
        //   - setOpaque(false)              → transparent background
        //   - getViewport().setOpaque(false)→ transparent viewport
        //   - getVerticalScrollBar().setUnitIncrement(10) → smooth scrolling
        JScrollPane vehicleScrollPane = new JScrollPane(vehicleListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vehicleScrollPane.setBorder(null);
        vehicleScrollPane.setOpaque(false);
        vehicleScrollPane.getViewport().setOpaque(false);
        vehicleScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        // ── Set the scroll pane height to exactly MAX_VISIBLE rows ──
        //
        // WHY: We want exactly 6 rows visible before scrolling starts.
        // Each row is ROW_H (64px) tall, plus a 6px gap between rows.
        // Formula: (6 rows × 64px) + (5 gaps × 6px) = 384 + 30 = 414px
        //
        // setMaximumSize controls how tall the scroll pane can grow.
        // We also set it as the preferred size so BoxLayout respects it.
        int scrollPaneHeight = (MAX_VISIBLE * ROW_H) + ((MAX_VISIBLE - 1) * 6);
        vehicleScrollPane.setPreferredSize(new Dimension(0, scrollPaneHeight));
        vehicleScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, scrollPaneHeight));
        vehicleScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        vehicleCard.add(vehicleScrollPane);

        // ── Build the add-vehicle form ─────────────────────────────
        // vehicleAddPanel is added INSIDE vehicleListPanel (by rebuildVehicleList),
        // so it also appears inside the scroll pane below all the rows.
        vehicleAddPanel = buildAddForm();
        vehicleAddPanel.setVisible(false);
        vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Wire up the "+Add" button ──────────────────────────────
        //
        // Because we now only add vehicleAddPanel to vehicleListPanel when
        // it is open, the toggle button must physically add/remove it from
        // the layout rather than just calling setVisible().
        addBtn.addActionListener(e -> {
            boolean currentlyOpen = vehicleListPanel.isAncestorOf(vehicleAddPanel);
            if (currentlyOpen) {
                // Close: remove the form and its gap strut from the list
                vehicleListPanel.remove(vehicleAddPanel);
                // Remove the last strut that was added before the form
                int count = vehicleListPanel.getComponentCount();
                if (count > 0) {
                    java.awt.Component last = vehicleListPanel.getComponent(count - 1);
                    if (last instanceof javax.swing.Box.Filler) {
                        vehicleListPanel.remove(last);
                    }
                }
            } else {
                // Open: append the gap strut and form to the list
                clearFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
                vehicleListPanel.add(Box.createVerticalStrut(6));
                vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(vehicleAddPanel);
                vehicleAddPanel.setVisible(true);
            }
            vehicleListPanel.revalidate();
            vehicleListPanel.repaint();
            vehicleCard.revalidate();
            vehicleCard.repaint();
        });

        SwingUtilities.invokeLater(this::loadVehicles);
        return vehicleCard;
    }

    /**
     * Builds the inline add-vehicle form.
     *
     * This form is added inside vehicleListPanel (which lives inside the
     * JScrollPane), so it scrolls along with the vehicle rows.
     */
    private JPanel buildAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                new EmptyBorder(6, 10, 6, 10)));

        // Only cap the height; let width fill naturally from parent layout
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H));

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
            // Remove form and its preceding gap strut from the list entirely
            if (vehicleListPanel != null && vehicleListPanel.isAncestorOf(vehicleAddPanel)) {
                vehicleListPanel.remove(vehicleAddPanel);
                int count = vehicleListPanel.getComponentCount();
                if (count > 0) {
                    java.awt.Component last = vehicleListPanel.getComponent(count - 1);
                    if (last instanceof javax.swing.Box.Filler) {
                        vehicleListPanel.remove(last);
                    }
                }
                vehicleListPanel.revalidate();
                vehicleListPanel.repaint();
            }
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
                // Remove form and its preceding gap strut from the list
                if (vehicleListPanel != null && vehicleListPanel.isAncestorOf(vehicleAddPanel)) {
                    vehicleListPanel.remove(vehicleAddPanel);
                    int count = vehicleListPanel.getComponentCount();
                    if (count > 0) {
                        java.awt.Component last = vehicleListPanel.getComponent(count - 1);
                        if (last instanceof javax.swing.Box.Filler) {
                            vehicleListPanel.remove(last);
                        }
                    }
                }
                clearFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
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
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 30));
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
    // =========================================================
    // PRIVATE INNER CLASS — ScrollableVehiclePanel
    // =========================================================

    /**
     * A JPanel that implements javax.swing.Scrollable so that the
     * JScrollPane wrapping the vehicle list knows:
     *
     *   1. Always stretch width to fill the viewport (no horizontal scroll bar).
     *   2. Only show a vertical scroll bar when content height TRULY
     *      exceeds the viewport height (i.e. more than MAX_VISIBLE rows).
     *
     * WHY A SEPARATE CLASS:
     * Java does not allow "new JPanel() implements SomeInterface { ... }"
     * inside a method body — that is invalid syntax. The correct pattern
     * is to declare a named inner class and instantiate it with "new".
     */
    private static class ScrollableVehiclePanel extends JPanel
            implements javax.swing.Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            // Let the scroll pane use our natural preferred size
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            // Scroll one row height at a time when using the scroll bar arrows
            return ROW_H;
        }

        @Override
        public int getScrollableBlockIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            // Scroll three rows at a time on Page Up / Page Down
            return ROW_H * 3;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // TRUE → always stretch to fill the full viewport width.
            // This prevents a horizontal scroll bar and lets Swing
            // measure height correctly.
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // FALSE → do NOT stretch to fill viewport height.
            // This allows the scroll bar to appear when the content
            // (vehicle rows) is taller than the fixed viewport height.
            return false;
        }

        // ROW_H must be accessible here; we re-declare it as a constant.
        // It must match the ROW_H in the outer ViewProfile class.
        private static final int ROW_H = 64;
    }


}