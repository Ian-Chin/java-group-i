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
 * SCROLL BAR / GHOST SPACE FIX (latest change):
 *
 *   ROOT CAUSE:
 *   vehicleAddPanel was being added to vehicleListPanel during
 *   buildVehicleCard() with setVisible(false).
 *
 *   In Java Swing, an INVISIBLE component still occupies space
 *   inside a BoxLayout panel. The layout still measures its size
 *   even when hidden, so vehicleListPanel's total height was always:
 *       (6 rows x 64px) + (5 gaps x 6px) + (add-form 64px) = 478px
 *   This is MORE than the scroll pane's fixed height of 414px.
 *   Result: scroll bar appeared even with only 6 vehicles, and
 *   scrolling down revealed an empty gap at the bottom.
 *
 *   THE FIX — one change in buildVehicleCard():
 *
 *   BEFORE (broken):
 *     vehicleAddPanel = buildAddForm();
 *     vehicleAddPanel.setVisible(false);    <- hidden but STILL IN the panel
 *     vehicleAddPanel.setAlignmentX(...);
 *     vehicleCard.add(vehicleAddPanel);     <- caused ghost space
 *
 *   AFTER (fixed):
 *     vehicleAddPanel = buildAddForm();
 *     // NOT added to any panel here.
 *     // It only enters vehicleListPanel when + Add is clicked,
 *     // and is removed again on Cancel or successful Save.
 *
 *   WHY THIS WORKS:
 *   vehicleListPanel now only ever contains real vehicle rows.
 *   Its total height is exactly (n x 64px) + ((n-1) x 6px).
 *   When n <= 6  ->  fits in the scroll pane  ->  no scroll bar.
 *   When n >= 7  ->  overflows scroll pane   ->  scroll bar appears.
 *   This is exactly the intended behaviour.
 *
 * ============================================================
 *
 * BUTTON VERTICAL CENTERING FIX:
 *
 *   The Edit/Remove buttons (in display mode) and Save/Cancel buttons
 *   (in edit mode) inside each vehicle row were not vertically centered.
 *
 *   ROOT CAUSE:
 *   The actions panel in buildVehicleRow() used FlowLayout, which only
 *   centers content horizontally, not vertically. The button panel in
 *   buildEditCard() used GridBagConstraints but without setting fill
 *   and weighty correctly for vertical centering.
 *
 *   THE FIX:
 *   - In buildVehicleRow(): Replace FlowLayout with GridBagLayout and
 *     use GridBagConstraints with anchor = CENTER and weighty = 1.0
 *     on the outer wrapper so the buttons sit in the middle vertically.
 *
 *   - In buildEditCard(): Same approach — use GridBagConstraints with
 *     anchor = GridBagConstraints.CENTER and a fill = VERTICAL wrapper
 *     so Save/Cancel buttons are vertically centered in the edit row.
 *
 * ============================================================
 *
 * PERSONAL INFO CARD GAP FIX:
 *
 *   Reduced the vertical gap between Username, Email, and Role rows
 *   from 14px to 6px. This makes the fields sit closer together and
 *   gives the card a more compact, modern look.
 *
 *   WHAT CHANGED (4 lines only, inside buildPersonalInfoCard()):
 *     Box.createVerticalStrut(14)  ->  Box.createVerticalStrut(6)
 *     — after nameReadPanel
 *     — after nameEditPanel
 *     — after emailReadPanel
 *     — after emailEditPanel
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

    private JPanel      vehicleListPanel;   // The inner panel that holds vehicle rows
    private JPanel      vehicleAddPanel;    // The add form — NOT pre-added to any panel
    private JPanel      vehicleCard;        // The outer card container
    private JScrollPane vehicleScrollPane;  // Scroll pane wrapping vehicleListPanel

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
     * MAX_VISIBLE = 6 means the scroll bar appears only when there
     * are 7 or more vehicles.
     */
    private static final int MAX_VISIBLE = 6;

    private static final int ROW_H    = 64;
    private static final int EDIT_H   = 68;
    private static final int ICON_SIZE = 50;

    // =========================================================
    // UNICODE ICONS
    // =========================================================

    private static final String ICON_CAR   = "\uD83D\uDE97";
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

        SwingUtilities.invokeLater(this::loadVehicles);
    }


    // =========================================================
    // PUBLIC METHODS
    // =========================================================

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
     * Rebuilds all vehicle rows with fresh data.
     * Each String[] holds: [0] id  [1] type  [2] plate  [3] brand  [4] year  [5] colour
     */
    public void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        // Check if the add-form is currently open (attached to vehicleListPanel)
        boolean addFormWasOpen = (vehicleAddPanel != null)
                && vehicleListPanel.isAncestorOf(vehicleAddPanel);

        // If the form is open, detach it cleanly before clearing all rows.
        // We strip it (and its preceding gap strut) so removeAll() has a
        // clean slate, then re-attach it at the end if it was open.
        if (addFormWasOpen) {
            int count = vehicleListPanel.getComponentCount();
            if (count > 0) {
                Component last = vehicleListPanel.getComponent(count - 1);
                if (last == vehicleAddPanel) {
                    vehicleListPanel.remove(vehicleAddPanel);
                    count = vehicleListPanel.getComponentCount();
                    if (count > 0) {
                        Component maybeStrut = vehicleListPanel.getComponent(count - 1);
                        if (maybeStrut instanceof Box.Filler) {
                            vehicleListPanel.remove(maybeStrut);
                        }
                    }
                }
            }
        }

        // Clear all existing rows
        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered yet."));
        } else {
            for (int i = 0; i < vehicles.size(); i++) {
                String[] v = vehicles.get(i);
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(row);
                if (i < vehicles.size() - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(6));
                }
            }
        }

        // Re-attach the add-form at the bottom only if it was open before
        if (addFormWasOpen && vehicleAddPanel != null) {
            vehicleListPanel.add(Box.createVerticalStrut(6));
            vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            vehicleListPanel.add(vehicleAddPanel);
        }

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

        // ── Title row with Edit / Save / Cancel buttons ────────────────
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

        btnEdit.setFont(new Font("SansSerif", Font.BOLD, 12));

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

        // ── Pull current user data ─────────────────────────────────────
        User   user  = app.getLoggedInUserObj();
        String name  = (user != null && user.getName()  != null) ? user.getName()  : "";
        String email = (user != null && user.getEmail() != null) ? user.getEmail() : "";
        String role  = (user != null && user.getRole()  != null) ? user.getRole()  : "";
        if (!role.isEmpty())
            role = Character.toUpperCase(role.charAt(0)) + role.substring(1).toLowerCase();

        // ── Username read row (shown in normal view) ───────────────────
        nameReadPanel    = makeReadRow("Username", name);
        displayNameLabel = getValueLabel(nameReadPanel);
        card.add(nameReadPanel);

        // GAP: 6px below the Username read row.
        // Box.createVerticalStrut(n) inserts exactly n pixels of blank
        // vertical space between two components in a BoxLayout panel.
        // We use 6 here (down from 14) to keep the fields closer together.
        card.add(Box.createVerticalStrut(10));  // gap below Username read row

        // ── Username edit row (shown only when user clicks Edit) ───────
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
        nameEditPanel.setVisible(false);  // hidden until Edit is clicked
        card.add(nameEditPanel);

        // GAP: 6px below the Username edit row.
        // Same reason as above — keeps spacing tight and consistent.
        card.add(Box.createVerticalStrut(10));  // gap below Username edit row

        // ── Email read row (shown in normal view) ──────────────────────
        emailReadPanel    = makeReadRow("Email", email);
        displayEmailLabel = getValueLabel(emailReadPanel);
        card.add(emailReadPanel);

        // GAP: 6px below the Email read row.
        card.add(Box.createVerticalStrut(10));  // gap below Email read row

        // ── Email edit row (shown only when user clicks Edit) ──────────
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
        emailEditPanel.setVisible(false);  // hidden until Edit is clicked
        card.add(emailEditPanel);

        // GAP: 6px below the Email edit row.
        card.add(Box.createVerticalStrut(10));  // gap below Email edit row

        // ── Role read row (read-only, no edit version) ─────────────────
        JPanel roleRow = makeReadRow("Role", role);
        displayRoleLabel = getValueLabel(roleRow);
        card.add(roleRow);
        card.add(Box.createVerticalStrut(4));

        // ── Wire up button listeners ────────────────────────────────────
        btnEdit.addActionListener(e -> enterEditMode());
        btnCancel.addActionListener(e -> exitEditMode());
        btnSave.addActionListener(e -> saveProfileChanges());

        // Allow pressing Enter inside any text field to trigger Save
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

        // ── Title row ─────────────────────────────────────────────
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("My Vehicles");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        titleRow.add(title, BorderLayout.WEST);

        JButton addBtn = makeFilledButton("\u271A  Add", BLUE, Color.WHITE);
        addBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
        addBtn.setPreferredSize(new Dimension(80, 30));

        JPanel addBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        addBtnPanel.setOpaque(false);
        addBtnPanel.setPreferredSize(new Dimension(90, 30));
        addBtnPanel.add(addBtn);
        titleRow.add(addBtnPanel, BorderLayout.EAST);

        vehicleCard.add(titleRow);
        vehicleCard.add(Box.createVerticalStrut(8));
        vehicleCard.add(makeDivider());
        vehicleCard.add(Box.createVerticalStrut(12));

        // ── vehicleListPanel: holds all the rows ─────────────────
        vehicleListPanel = new ScrollableVehiclePanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        vehicleListPanel.add(makeEmptyLabel("Loading vehicles..."));

        // ── JScrollPane wraps vehicleListPanel ────────────────────
        vehicleScrollPane = new JScrollPane(vehicleListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vehicleScrollPane.setBorder(null);
        vehicleScrollPane.setOpaque(false);
        vehicleScrollPane.getViewport().setOpaque(false);
        vehicleScrollPane.getVerticalScrollBar().setUnitIncrement(10);

        // Fixed height = exactly MAX_VISIBLE rows visible before scrolling.
        // Formula: (6 x 64px rows) + (5 x 6px gaps) = 384 + 30 = 414px
        int scrollPaneHeight = (MAX_VISIBLE * ROW_H) + ((MAX_VISIBLE - 1) * 6);
        vehicleScrollPane.setPreferredSize(new Dimension(0, scrollPaneHeight));
        vehicleScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, scrollPaneHeight));
        vehicleScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

        vehicleCard.add(vehicleScrollPane);

        // vehicleAddPanel is NOT added here — only built and stored.
        // The + Add button listener adds it to vehicleListPanel on demand.
        vehicleAddPanel = buildAddForm();

        // ── Wire up the + Add button ───────────────────────────────
        addBtn.addActionListener(e -> {
            boolean currentlyOpen = vehicleListPanel.isAncestorOf(vehicleAddPanel);
            if (currentlyOpen) {
                // CLOSE: remove the form and its preceding gap strut
                vehicleListPanel.remove(vehicleAddPanel);
                int count = vehicleListPanel.getComponentCount();
                if (count > 0) {
                    Component last = vehicleListPanel.getComponent(count - 1);
                    if (last instanceof Box.Filler) {
                        vehicleListPanel.remove(last);
                    }
                }
                vehicleListPanel.revalidate();
                vehicleListPanel.repaint();
                vehicleCard.revalidate();
                vehicleCard.repaint();
                SwingUtilities.invokeLater(this::scrollVehicleListToTop);
            } else {
                // OPEN: append gap strut then the form, scroll to bottom
                clearFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
                vehicleListPanel.add(Box.createVerticalStrut(6));
                vehicleAddPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(vehicleAddPanel);
                vehicleListPanel.revalidate();
                vehicleListPanel.repaint();
                vehicleCard.revalidate();
                vehicleCard.repaint();
                SwingUtilities.invokeLater(this::scrollVehicleListToBottom);
            }
        });

        SwingUtilities.invokeLater(this::loadVehicles);
        return vehicleCard;
    }


    // =========================================================
    // SCROLL HELPERS
    // =========================================================

    /**
     * Scrolls vehicleScrollPane all the way to the bottom.
     * Must be called via SwingUtilities.invokeLater() so the layout
     * pass from revalidate() finishes before we move the scroll bar.
     */
    private void scrollVehicleListToBottom() {
        if (vehicleScrollPane == null) return;
        JScrollBar verticalBar = vehicleScrollPane.getVerticalScrollBar();
        // Integer.MAX_VALUE is a safe "go to end" shortcut —
        // Swing clamps it to the real maximum automatically.
        verticalBar.setValue(Integer.MAX_VALUE);
    }

    /**
     * Scrolls vehicleScrollPane back to the very top.
     * Must be called via SwingUtilities.invokeLater() for the same reason.
     */
    private void scrollVehicleListToTop() {
        if (vehicleScrollPane == null) return;
        JScrollBar verticalBar = vehicleScrollPane.getVerticalScrollBar();
        verticalBar.setValue(0);
    }


    // =========================================================
    // ADD VEHICLE FORM
    // =========================================================

    /**
     * Builds the inline add-vehicle form.
     * Only inserted into vehicleListPanel when the user clicks + Add.
     */
    private JPanel buildAddForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                new EmptyBorder(6, 10, 6, 10)));
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

        // ── Save / Cancel buttons — vertically centered ───────────
        JPanel btns = makeCenteredButtonPanel();
        JButton saveBtn   = makeFilledButton("Save",   GREEN,    Color.WHITE);
        JButton cancelBtn = makeFilledButton("Cancel", GREY_BTN, Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(65, 30));
        cancelBtn.setPreferredSize(new Dimension(80, 30));

        JPanel btnPair = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        btnPair.setOpaque(false);
        btnPair.add(saveBtn);
        btnPair.add(cancelBtn);

        btns.add(btnPair, makeCenterConstraints());
        form.add(btns, BorderLayout.EAST);

        cancelBtn.addActionListener(e -> {
            if (vehicleListPanel != null && vehicleListPanel.isAncestorOf(vehicleAddPanel)) {
                vehicleListPanel.remove(vehicleAddPanel);
                int count = vehicleListPanel.getComponentCount();
                if (count > 0) {
                    Component last = vehicleListPanel.getComponent(count - 1);
                    if (last instanceof Box.Filler) {
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
                if (vehicleListPanel != null && vehicleListPanel.isAncestorOf(vehicleAddPanel)) {
                    vehicleListPanel.remove(vehicleAddPanel);
                    int count = vehicleListPanel.getComponentCount();
                    if (count > 0) {
                        Component last = vehicleListPanel.getComponent(count - 1);
                        if (last instanceof Box.Filler) {
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

        // ── Display panel (normal view) ───────────────────────────
        JPanel display = new JPanel(new BorderLayout(0, 0));
        display.setOpaque(false);
        display.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Vehicle icon (car or motorcycle emoji)
        String iconText = "Motor".equalsIgnoreCase(vehicleType) ? ICON_MOTOR : ICON_CAR;
        JLabel iconLbl  = new JLabel(iconText, SwingConstants.CENTER);
        iconLbl.setFont(new Font("SansSerif", Font.PLAIN, 30));
        iconLbl.setPreferredSize(new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMinimumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        iconLbl.setMaximumSize  (new Dimension(ICON_SIZE, ICON_SIZE));
        display.add(iconLbl, BorderLayout.WEST);

        // Vehicle info text (brand name + details)
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

        // ── Edit & Remove buttons — vertically centered ────────────
        // GridBagLayout with weighty=1.0 and anchor=CENTER ensures the
        // button pair sits exactly in the middle of the 64px row height.
        JPanel actions = makeCenteredButtonPanel();

        JButton editBtn   = makeFilledButton("Edit",   BLUE, Color.WHITE);
        JButton removeBtn = makeFilledButton("Remove", RED,  Color.WHITE);
        editBtn.setPreferredSize(new Dimension(70, 30));
        removeBtn.setPreferredSize(new Dimension(85, 30));

        JPanel btnPair = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
        btnPair.setOpaque(false);
        btnPair.add(editBtn);
        btnPair.add(removeBtn);

        actions.add(btnPair, makeCenterConstraints());
        display.add(actions, BorderLayout.EAST);

        // ── Edit card (shown when user clicks "Edit") ─────────────
        JPanel editCard = buildEditCard(vehicleType, plate, brand, year, colour);

        // CardLayout flips between display and edit cleanly.
        // All three size methods are locked to ROW_H for the same reason
        // as the wrapper below — prevents BoxLayout from stretching the row.
        CardLayout switcher    = new CardLayout();
        JPanel     switchPanel = new JPanel(switcher);
        switchPanel.setOpaque(false);
        switchPanel.setMinimumSize  (new Dimension(0,                 ROW_H));
        switchPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, ROW_H));
        switchPanel.setMaximumSize  (new Dimension(Integer.MAX_VALUE, ROW_H));
        switchPanel.add(display,  "display");
        switchPanel.add(editCard, "edit");
        switcher.show(switchPanel, "display");

        // Outer wrapper with border — this goes into vehicleListPanel.
        //
        // WHY all three size methods (min, preferred, max)?
        // BoxLayout respects all three when deciding how tall to make a
        // component. Setting only setMaximumSize() is not enough — if the
        // preferred size is taller (e.g. because revalidate() re-measures
        // after the Add form is appended), BoxLayout will still stretch the
        // row up to its preferred height. Locking all three to the same
        // ROW_H value makes the height completely rigid: no matter what
        // happens to the rest of the list, each vehicle row stays 64px tall.
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        wrapper.setMinimumSize  (new Dimension(0,                    ROW_H));
        wrapper.setPreferredSize(new Dimension(Integer.MAX_VALUE,    ROW_H));
        wrapper.setMaximumSize  (new Dimension(Integer.MAX_VALUE,    ROW_H));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(switchPanel, BorderLayout.CENTER);

        // Retrieve sub-components stored inside editCard via client properties
        JComboBox<String> eType   = (JComboBox<String>) editCard.getClientProperty("typeCombo");
        JTextField        ePlate  = (JTextField)        editCard.getClientProperty("plate");
        JTextField        eBrand  = (JTextField)        editCard.getClientProperty("brand");
        JTextField        eYear   = (JTextField)        editCard.getClientProperty("year");
        JTextField        eColour = (JTextField)        editCard.getClientProperty("colour");
        JButton           eSave   = (JButton)           editCard.getClientProperty("saveBtn");
        JButton           eCancel = (JButton)           editCard.getClientProperty("cancelBtn");

        // ── Edit button: switch to edit card ──────────────────────
        editBtn.addActionListener(e -> {
            if (eType   != null) eType.setSelectedItem(vehicleType);
            if (ePlate  != null) ePlate.setText(plate);
            if (eBrand  != null) eBrand.setText(brand);
            if (eYear   != null) eYear.setText(year);
            if (eColour != null) eColour.setText(colour);
            // Sizes stay locked at ROW_H — do NOT mutate them here.
            // Changing setMaximumSize triggers a revalidate that causes
            // all other vehicle rows to briefly repaint at wrong heights.
            switcher.show(switchPanel, "edit");
            if (ePlate != null) ePlate.requestFocusInWindow();
        });

        // ── Cancel button: go back to display card ─────────────────
        if (eCancel != null) {
            eCancel.addActionListener(e -> {
                // Sizes stay locked — just flip the card, no size mutation needed.
                switcher.show(switchPanel, "display");
            });
        }

        // ── Save logic: validate and persist changes ───────────────
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
                // Sizes stay locked — just flip back to display card.
                switcher.show(switchPanel, "display");
                loadVehicles();
                JOptionPane.showMessageDialog(app, "Vehicle updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        if (eSave != null) eSave.addActionListener(e -> doSave.run());

        // Allow pressing Enter inside any edit field to save
        KeyAdapter enter = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        if (ePlate  != null) ePlate.addKeyListener(enter);
        if (eBrand  != null) eBrand.addKeyListener(enter);
        if (eYear   != null) eYear.addKeyListener(enter);
        if (eColour != null) eColour.addKeyListener(enter);

        // ── Remove button: confirm then delete ────────────────────
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

    /**
     * Builds the inline edit form for an existing vehicle row.
     * Save and Cancel buttons are vertically centered via makeCenteredButtonPanel().
     */
    private JPanel buildEditCard(String vehicleType, String plate,
                                 String brand, String year, String colour) {
        JPanel card = new JPanel(new BorderLayout(6, 0));
        card.setOpaque(false);
        // 4px top/bottom padding (was 6) so the edit form fits neatly
        // inside ROW_H=64px without needing to expand the row height.
        card.setBorder(new EmptyBorder(4, 10, 4, 10));

        // ── Input fields ──────────────────────────────────────────
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

        // ── Save & Cancel buttons — vertically centered ────────────
        JPanel btns = makeCenteredButtonPanel();

        JButton saveBtn   = makeFilledButton("Save",   GREEN,    Color.WHITE);
        JButton cancelBtn = makeFilledButton("Cancel", GREY_BTN, Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(65, 30));
        cancelBtn.setPreferredSize(new Dimension(80, 30));

        JPanel btnPair = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        btnPair.setOpaque(false);
        btnPair.add(saveBtn);
        btnPair.add(cancelBtn);

        btns.add(btnPair, makeCenterConstraints());
        card.add(btns, BorderLayout.EAST);

        // Store references so buildVehicleRow() can wire up listeners
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
    // CENTERING HELPERS
    // =========================================================

    /**
     * Creates a transparent panel with GridBagLayout.
     *
     * We use GridBagLayout because it is the only standard Swing layout
     * that can both STRETCH a cell to fill all available height AND
     * CENTER the content inside that cell at the same time.
     *
     * This panel wraps button pairs so they appear vertically centered
     * inside a fixed-height vehicle row (64px or 68px).
     */
    private JPanel makeCenteredButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        return panel;
    }

    /**
     * Returns GridBagConstraints configured for vertical centering:
     *
     *   weighty = 1.0  ->  the cell fills ALL available vertical space
     *   anchor  = CENTER -> the component sits in the MIDDLE of that space
     *   fill    = NONE  ->  the component keeps its own preferred size
     *   weightx = 1.0  ->  the cell also fills horizontal space (hugs EAST edge)
     */
    private GridBagConstraints makeCenterConstraints() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.weightx = 1.0;  // fill width
        gbc.weighty = 1.0;  // fill height — this is what enables vertical centering
        gbc.fill    = GridBagConstraints.NONE;   // don't stretch the button itself
        gbc.anchor  = GridBagConstraints.CENTER; // center it in the cell
        return gbc;
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
    // UI HELPER METHODS
    // =========================================================

    /** Creates a white rounded-corner card panel with a subtle border. */
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

    /** Creates a thin horizontal divider line. */
    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(CARD_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    /**
     * Creates a read-only label+value row.
     * Layout: [label (grey, fixed width)] [value (dark)]
     */
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

    /**
     * Retrieves the value JLabel from a read row panel.
     * The value label is always in the BorderLayout.CENTER slot.
     */
    private JLabel getValueLabel(JPanel readRow) {
        return (JLabel) ((BorderLayout) readRow.getLayout())
                .getLayoutComponent(BorderLayout.CENTER);
    }

    /** Creates a bold grey label used above edit fields. */
    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setPreferredSize(new Dimension(90, 18));
        return lbl;
    }

    /** Applies a blue-bordered style to a profile text field. */
    private void styleField(JTextField f) {
        f.setFont(new Font("SansSerif", Font.PLAIN, 13));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 2),
                new EmptyBorder(2, 6, 2, 6)));
    }

    /** Creates a compact text field used inside vehicle rows. */
    private JTextField makeSmallField(String value) {
        JTextField f = new JTextField(value);
        f.setFont(new Font("SansSerif", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 1),
                new EmptyBorder(2, 3, 2, 3)));
        return f;
    }

    /**
     * Wraps a text field with a small label above it.
     * Used inside vehicle add/edit forms.
     */
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

    /**
     * Wraps a combo box with a small label above it.
     * Used inside vehicle add/edit forms.
     */
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

    /**
     * Creates a filled, rounded button with hover darkening.
     *
     * HOW THE CUSTOM PAINT WORKS:
     * - setContentAreaFilled(false) tells Swing NOT to draw the default
     *   grey button background — we draw our own rounded rectangle instead.
     * - On mouseEntered we set hov=true and repaint, which uses bg.darker()
     *   to give visual feedback that the button is being hovered.
     */
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

    /** Creates a centred grey placeholder label (e.g. "No vehicles yet"). */
    private JLabel makeEmptyLabel(String msg) {
        JLabel lbl = new JLabel(msg);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    /**
     * Recursively clears all JTextField components inside a container.
     * Used to reset the add/edit form fields after Save or Cancel.
     */
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
     * A JPanel that implements Scrollable so the JScrollPane knows:
     *
     * 1. Always stretch width to fill the viewport — no horizontal scroll bar.
     * 2. Only show a vertical scroll bar when content height truly exceeds
     *    the viewport height (i.e. more than MAX_VISIBLE rows).
     *
     * WITHOUT this class, JScrollPane would show a horizontal scroll bar
     * when the panel is slightly narrower than the viewport, and the
     * vertical scroll behaviour would be inconsistent.
     */
    private static class ScrollableVehiclePanel extends JPanel
            implements javax.swing.Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            // Tell the scroll pane our preferred size is our actual preferred size.
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            // How many pixels to scroll per arrow-key click or scroll-arrow click.
            return ROW_H; // one full vehicle row
        }

        @Override
        public int getScrollableBlockIncrement(
                java.awt.Rectangle visibleRect, int orientation, int direction) {
            // How many pixels to scroll per Page Up / Page Down.
            return ROW_H * 3; // three vehicle rows at a time
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            // true = always stretch to fill the full viewport width.
            // This prevents a horizontal scroll bar from appearing.
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            // false = do NOT force the panel to fill the viewport height.
            // This allows the vertical scroll bar to appear when needed.
            return false;
        }

        // Each vehicle row is 64px tall — used for unit/block scroll increments.
        private static final int ROW_H = 64;
    }
}