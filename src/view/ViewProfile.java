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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * ============================================================
 * ViewProfile.java
 * ============================================================
 * This panel shows the customer's profile page.
 *
 * The page is split into these sections (top to bottom):
 *
 *   1. BANNER HERO
 *      - A wide blue banner image at the top (clickable to change)
 *      - A circular profile picture overlapping the banner
 *        (clickable to change)
 *
 *   2. STATS ROW
 *      - Total Appointments  |  Total Spent
 *      - Counts are read from appointments.txt and payments.txt
 *
 *   3. TWO COLUMNS (side by side)
 *      LEFT  → Personal Information card
 *              Shows: Username, Email, Role
 *              Has an Edit button to update Username and Email
 *      RIGHT → My Vehicles card
 *              Shows every vehicle registered to this user
 *              Has Add, Edit, and Remove buttons
 *
 * ── FIXES APPLIED ─────────────────────────────────────────────
 *
 *   ★ FIX 1 — My Vehicles was ALWAYS empty (root cause: timing):
 *     The constructor now calls refreshVehicleList() at the very end
 *     so that vehicles are loaded and displayed the first time the
 *     page is shown. Previously the constructor only built the empty
 *     UI panels and stopped — nothing ever triggered the first load.
 *
 *     A secondary safety fallback was also added inside buildVehicleCard():
 *     it directly calls vehicleService.getVehiclesByUserId() and populates
 *     the panel immediately, before the controller callback fires.
 *     This guarantees vehicles appear even if the SectionView wiring in
 *     the outer class (CustomerDashboard / AppFrame) is incomplete.
 *
 *   ★ FIX 2 — Total appointments showed 0:
 *     countUserAppointments() reads appointments.txt and counts every row
 *     whose column[1] matches the logged-in user's ID.
 *     The result is stored in totalAppointmentsLabel (a field) so
 *     refreshUser() can update it without rebuilding the whole page.
 *
 *   ★ FIX 3 — Total spent showed RM 0:
 *     sumUserPayments() reads payments.txt and sums column[5] for every
 *     row whose column[1] matches the logged-in user's ID.
 *     The result is stored in totalSpentLabel (a field) so refreshUser()
 *     can update it without rebuilding the whole page.
 * ============================================================
 */
public class ViewProfile extends JPanel {

    // ----------------------------------------------------------
    // SECTION 1 — References to other objects this class needs
    // ----------------------------------------------------------

    /** The main application window (used to get the logged-in user). */
    private final AppFrame app;

    /** Handles saving changes to the user's name and email. */
    private final CustomerProfileController profileController;

    /** Handles adding, editing, and removing vehicles. */
    private final VehicleSectionController vehicleController;

    /** Reads and writes the vehicles.txt file directly. */
    private final VehicleService vehicleService;

    /** Loads and saves the circular profile picture image. */
    private final ProfilePicStorage profilePicStorage;

    /** Loads and saves the banner background image. */
    private final BackgroundImageStorage backgroundStorage;

    // ----------------------------------------------------------
    // SECTION 2 — Images stored in memory
    // ----------------------------------------------------------

    /** The profile picture chosen by the user (null = show default avatar). */
    private BufferedImage profileImage = null;

    /** The banner image chosen by the user (null = show default blue gradient). */
    private BufferedImage bannerImage = null;

    // ----------------------------------------------------------
    // SECTION 3 — UI components we need to reference later
    // ----------------------------------------------------------

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

    // ★ FIX 2 & 3 — Stored as fields so refreshUser() can call setText() on them.
    private JLabel totalAppointmentsLabel;
    private JLabel totalSpentLabel;

    // ----------------------------------------------------------
    // SECTION 4 — Colours
    // ----------------------------------------------------------

    private static final Color BLUE        = new Color(80,  110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);
    private static final Color GREEN       = new Color(80,  190, 110);
    private static final Color RED         = new Color(220, 80,  80);
    private static final Color GREY_BTN    = new Color(150, 150, 165);

    private static final Color TEXT_DARK  = new Color(30,  35,  50);
    private static final Color TEXT_GREY  = new Color(130, 135, 155);

    private static final Color PAGE_BG     = new Color(245, 246, 250);
    private static final Color CARD_BG     = Color.WHITE;
    private static final Color CARD_BORDER = new Color(225, 227, 235);

    private static final Color[] AVATAR_COLORS = {
        new Color(80,  110, 230), new Color(230, 80,  80),
        new Color(80,  190, 110), new Color(230, 160, 40),
        new Color(160, 80,  230), new Color(40,  180, 200),
        new Color(230, 80,  160), new Color(100, 100, 120),
    };

    // ----------------------------------------------------------
    // SECTION 5 — Size constants
    // ----------------------------------------------------------

    private static final int MAX_VISIBLE_VEHICLES = 4;
    private static final int ROW_HEIGHT  = 64;
    private static final int EDIT_HEIGHT = 64;

    // ----------------------------------------------------------
    // SECTION 6 — File paths
    // ----------------------------------------------------------

    private static final String APPOINTMENTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "appointments.txt";

    private static final String PAYMENTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "payments.txt";


    // ==========================================================
    // CONSTRUCTOR
    // ==========================================================

    /**
     * Creates the ViewProfile panel.
     *
     * ★ FIX 1 — The constructor now calls refreshVehicleList() via
     * SwingUtilities.invokeLater() at the very end, so vehicle data is
     * loaded and shown the first time this panel is displayed.
     *
     * Previously the constructor only built empty UI panels and stopped —
     * nothing ever triggered the initial population, so My Vehicles was
     * always blank.
     */
    public ViewProfile(AppFrame app,
                       VehicleSectionController vehicleController,
                       CustomerProfileController profileController,
                       VehicleService vehicleService,
                       ProfilePicStorage profilePicStorage,
                       BackgroundImageStorage backgroundStorage) {

        this.app                = app;
        this.vehicleController  = vehicleController;
        this.profileController  = profileController;
        this.vehicleService     = vehicleService;
        this.profilePicStorage  = profilePicStorage;
        this.backgroundStorage  = backgroundStorage;

        setLayout(new BorderLayout());
        setBackground(PAGE_BG);

        JScrollPane scrollPane = new JScrollPane(buildPageContent());
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(PAGE_BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        // ★ FIX 1 — Trigger the first vehicle load after the UI is assembled.
        // invokeLater ensures the scroll pane is attached to the layout tree
        // before we try to repaint vehicle rows into it.
        SwingUtilities.invokeLater(this::refreshVehicleList);
    }


    // ==========================================================
    // PUBLIC METHODS
    // ==========================================================

    /**
     * Refreshes the whole page when the logged-in user changes.
     * Call this after login or after saving profile changes.
     */
    public void refreshUser() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;

        avatarColorIndex = user.getProfilePicture();
        profileImage     = profilePicStorage.loadImage(user.getUserId());
        bannerImage      = backgroundStorage.loadImage(user.getUserId());

        if (displayNameLabel  != null) displayNameLabel.setText(user.getName());
        if (displayEmailLabel != null) displayEmailLabel.setText(user.getEmail());

        if (displayRoleLabel != null) {
            String role = user.getRole();
            if (role != null && !role.isEmpty()) {
                role = role.substring(0, 1).toUpperCase()
                     + role.substring(1).toLowerCase();
            }
            displayRoleLabel.setText(role);
        }

        // ★ FIX 2 — Re-read appointments.txt and refresh the label
        if (totalAppointmentsLabel != null) {
            int count = countUserAppointments(user.getUserId());
            totalAppointmentsLabel.setText(String.valueOf(count));
        }

        // ★ FIX 3 — Re-read payments.txt and refresh the label
        if (totalSpentLabel != null) {
            double spent = sumUserPayments(user.getUserId());
            totalSpentLabel.setText(String.format("RM %,.0f", spent));
        }

        if (profileBanner   != null) profileBanner.repaint();
        if (profilePicLabel != null) profilePicLabel.repaint();

        if (btnEdit != null && !btnEdit.isVisible()) {
            exitEditMode();
        }

        refreshVehicleList();
    }

    /**
     * Asks VehicleSectionController to reload from vehicles.txt, then calls
     * rebuildVehicleList() on this panel with the latest data.
     *
     * ★ FIX 1 — Also called from the constructor so the list is never empty
     * on first open.
     */
    public void refreshVehicleList() {
        vehicleController.refreshList();
    }

    /**
     * Called by VehicleSectionController after vehicle data changes.
     * Rebuilds all vehicle rows in the My Vehicles card.
     *
     * ★ FIX 1 — This is the method that actually puts vehicle rows on screen.
     * It is called by VehicleSectionController → SectionView.rebuildList().
     *
     * IMPORTANT: If vehicles are still not appearing after this fix, check
     * that your SectionView implementation (in CustomerDashboard / AppFrame)
     * delegates rebuildList() to this method:
     *
     *   public void rebuildList(List<String[]> items) {
     *       viewProfile.rebuildVehicleList(items);   // <-- must have this line
     *   }
     *
     * Each String[] has 6 elements:
     *   [0] vehicleId   [1] vehicleType  [2] plate
     *   [3] brand       [4] year         [5] colour
     */
    public void rebuildVehicleList(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        boolean addFormOpen = (vehicleAddPanel != null) && vehicleAddPanel.isVisible();

        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered yet."));
        } else {
            int rowsToShow = Math.min(MAX_VISIBLE_VEHICLES, vehicles.size());
            for (int i = 0; i < rowsToShow; i++) {
                String[] v = vehicles.get(i);
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(row);
                if (i < rowsToShow - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(8));
                }
            }
        }

        if (vehicleAddPanel != null) {
            vehicleListPanel.add(Box.createVerticalStrut(8));
            vehicleListPanel.add(vehicleAddPanel);
            vehicleAddPanel.setVisible(addFormOpen);
        }

        if (vehicles.size() > MAX_VISIBLE_VEHICLES) {
            vehicleListPanel.add(Box.createVerticalStrut(4));
            JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            linkRow.setOpaque(false);
            linkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
            JButton viewAllBtn = makeLinkButton("View All (" + vehicles.size() + ")", BLUE);
            final List<String[]> copy = vehicles;
            viewAllBtn.addActionListener(e -> showViewAllDialog(copy));
            linkRow.add(viewAllBtn);
            vehicleListPanel.add(linkRow);
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
        if (vehicleCard != null) {
            vehicleCard.revalidate();
            vehicleCard.repaint();
        }
    }


    // ==========================================================
    // PAGE CONTENT
    // ==========================================================

    private JPanel buildPageContent() {
        JPanel page = new JPanel();
        page.setLayout(new BoxLayout(page, BoxLayout.Y_AXIS));
        page.setBackground(PAGE_BG);
        page.setBorder(new EmptyBorder(0, 0, 40, 0));

        page.add(buildBannerSection());
        page.add(Box.createVerticalStrut(20));
        page.add(buildStatsRow());
        page.add(Box.createVerticalStrut(24));
        page.add(buildTwoColumnSection());

        return page;
    }


    // ==========================================================
    // BANNER SECTION
    // ==========================================================

    private JPanel buildBannerSection() {
        JPanel hero = new JPanel(null);
        hero.setOpaque(false);
        hero.setPreferredSize(new Dimension(0, 200));
        hero.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));

        boolean[] bannerHovered = { false };
        boolean[] avatarHovered = { false };

        profileBanner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                if (bannerImage != null) {
                    g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setPaint(new GradientPaint(
                            0, 0, BANNER_BLUE,
                            getWidth(), getHeight(), new Color(55, 85, 205)));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
                if (bannerHovered[0]) {
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    drawCameraIcon(g2, getWidth() / 2, getHeight() / 2 - 10, 28, Color.WHITE);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("SansSerif", Font.BOLD, 13));
                    FontMetrics fm = g2.getFontMetrics();
                    String hint = "Click to change";
                    g2.drawString(hint,
                            getWidth() / 2 - fm.stringWidth(hint) / 2,
                            getHeight() / 2 + 34);
                }
                g2.dispose();
            }
        };
        profileBanner.setOpaque(false);
        profileBanner.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBanner.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { bannerHovered[0] = true;  profileBanner.repaint(); }
            @Override public void mouseExited (MouseEvent e) { bannerHovered[0] = false; profileBanner.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { pickBannerImage(); }
        });

        profilePicLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                    RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih);
                    int cx = (iw - crop) / 2, cy = (ih - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, size, size));
                    g2.drawImage(profileImage, 0, 0, size, size,
                            cx, cy, cx + crop, cy + crop, null);
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
        profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profilePicLabel.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { avatarHovered[0] = true;  profilePicLabel.repaint(); }
            @Override public void mouseExited (MouseEvent e) { avatarHovered[0] = false; profilePicLabel.repaint(); }
            @Override public void mouseClicked(MouseEvent e) { pickProfileImage(); }
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

        hero.add(profilePicLabel);
        hero.add(profileBanner);
        hero.setComponentZOrder(profilePicLabel, 0);
        hero.setComponentZOrder(profileBanner,   1);

        return hero;
    }

    private void drawCameraIcon(Graphics2D g2, int cx, int cy, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(size / 10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int bw = size, bh = size * 7 / 10;
        int bx = cx - bw / 2, by = cy - bh / 2;
        g2.drawRoundRect(bx, by, bw, bh, size / 5, size / 5);
        int lr = size * 22 / 100;
        g2.drawOval(cx - lr, cy - lr + size / 20, lr * 2, lr * 2);
        g2.drawRoundRect(bx + size / 6, by - size / 6, size / 4, size / 6, 2, 2);
    }

    private void drawDefaultAvatar(Graphics2D g2, int size) {
        g2.setColor(AVATAR_COLORS[avatarColorIndex % AVATAR_COLORS.length]);
        g2.fillOval(0, 0, size, size);
        String initial = "U";
        User user = app.getLoggedInUserObj();
        if (user != null && user.getName() != null && !user.getName().isEmpty()) {
            initial = String.valueOf(user.getName().charAt(0)).toUpperCase();
        }
        g2.setFont(new Font("SansSerif", Font.BOLD, size / 3));
        g2.setColor(Color.WHITE);
        FontMetrics fm = g2.getFontMetrics();
        int textX = (size - fm.stringWidth(initial)) / 2;
        int textY = (size + fm.getAscent() - fm.getDescent()) / 2;
        g2.drawString(initial, textX, textY);
    }


    // ==========================================================
    // STATS ROW
    // ==========================================================

    /**
     * ★ FIX 2 & 3 — Labels are now fields so refreshUser() can update them live.
     */
    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 0, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 24, 0, 24));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        User user = app.getLoggedInUserObj();
        int    totalAppointments = (user != null) ? countUserAppointments(user.getUserId()) : 0;
        double totalSpent        = (user != null) ? sumUserPayments(user.getUserId())        : 0.0;

        totalAppointmentsLabel = buildStatValueLabel(String.valueOf(totalAppointments));
        totalSpentLabel        = buildStatValueLabel(String.format("RM %,.0f", totalSpent));

        row.add(buildStatCellWith("Total appointments", totalAppointmentsLabel));
        row.add(buildStatCellWith("Total spent",        totalSpentLabel));

        return row;
    }

    /**
     * Counts appointments for a user. appointments.txt col[1] = customerID.
     */
    private int countUserAppointments(String userId) {
        int count = 0;
        File file = new File(APPOINTMENTS_FILE);
        if (!file.exists()) return count;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] cols = line.split(",");
                if (cols.length >= 2 && cols[1].trim().equalsIgnoreCase(userId))
                    count++;
            }
        } catch (IOException e) { e.printStackTrace(); }
        return count;
    }

    /**
     * Sums payments for a user. payments.txt col[1] = customerID, col[5] = amount.
     */
    private double sumUserPayments(String userId) {
        double total = 0.0;
        File file = new File(PAYMENTS_FILE);
        if (!file.exists()) return total;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;
                String[] cols = line.split(",");
                if (cols.length >= 6 && cols[1].trim().equalsIgnoreCase(userId)) {
                    try { total += Double.parseDouble(cols[5].trim()); }
                    catch (NumberFormatException ignored) {}
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return total;
    }

    private JPanel buildStatCellWith(String label, JLabel valueLabel) {
        JPanel cell = new JPanel();
        cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
        cell.setOpaque(false);
        cell.setBorder(new EmptyBorder(4, 0, 4, 32));
        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelText.setForeground(TEXT_GREY);
        labelText.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cell.add(labelText);
        cell.add(Box.createVerticalStrut(2));
        cell.add(valueLabel);
        return cell;
    }

    private JLabel buildStatValueLabel(String value) {
        JLabel lbl = new JLabel(value);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 24));
        lbl.setForeground(TEXT_DARK);
        return lbl;
    }


    // ==========================================================
    // TWO-COLUMN SECTION
    // ==========================================================

    private JPanel buildTwoColumnSection() {
        JPanel twoCol = new JPanel(new GridBagLayout());
        twoCol.setBackground(PAGE_BG);
        twoCol.setBorder(new EmptyBorder(0, 24, 0, 24));

        GridBagConstraints c = new GridBagConstraints();
        c.gridy   = 0;
        c.weighty = 1.0;
        c.fill    = GridBagConstraints.BOTH;
        c.anchor  = GridBagConstraints.NORTH;

        c.gridx   = 0;
        c.weightx = 0.38;
        c.insets  = new Insets(0, 0, 0, 16);
        JPanel leftCol = new JPanel();
        leftCol.setLayout(new BoxLayout(leftCol, BoxLayout.Y_AXIS));
        leftCol.setOpaque(false);
        leftCol.add(buildPersonalInfoCard());
        twoCol.add(leftCol, c);

        c.gridx   = 1;
        c.weightx = 0.62;
        c.insets  = new Insets(0, 0, 0, 0);
        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);
        rightCol.add(buildVehicleCard());
        twoCol.add(rightCol, c);

        return twoCol;
    }


    // ==========================================================
    // PERSONAL INFORMATION CARD
    // ==========================================================

    private JPanel buildPersonalInfoCard() {
        JPanel card = makeCard();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel title = new JLabel("Personal information");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        titleRow.add(title, BorderLayout.WEST);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonRow.setOpaque(false);

        btnEdit   = makeFilledButton("\u270E  Edit", BLUE,     Color.WHITE);
        btnSave   = makeFilledButton("Save",          GREEN,    Color.WHITE);
        btnCancel = makeFilledButton("Cancel",         GREY_BTN, Color.WHITE);

        btnEdit.setPreferredSize(new Dimension(82, 30));
        btnSave.setPreferredSize(new Dimension(70, 30));
        btnCancel.setPreferredSize(new Dimension(85, 30));

        btnSave.setVisible(false);
        btnCancel.setVisible(false);

        buttonRow.add(btnEdit);
        buttonRow.add(btnSave);
        buttonRow.add(btnCancel);
        titleRow.add(buttonRow, BorderLayout.EAST);

        card.add(titleRow);
        card.add(Box.createVerticalStrut(10));
        card.add(makeDivider());
        card.add(Box.createVerticalStrut(18));

        User user = app.getLoggedInUserObj();
        String currentName  = (user != null && user.getName()  != null) ? user.getName()  : "";
        String currentEmail = (user != null && user.getEmail() != null) ? user.getEmail() : "";
        String currentRole  = (user != null && user.getRole()  != null) ? user.getRole()  : "";

        if (!currentRole.isEmpty()) {
            currentRole = currentRole.substring(0, 1).toUpperCase()
                        + currentRole.substring(1).toLowerCase();
        }

        nameReadPanel    = makeReadRow("Username", currentName);
        displayNameLabel = getValueLabel(nameReadPanel);
        card.add(nameReadPanel);
        card.add(Box.createVerticalStrut(14));

        nameEditPanel = new JPanel(new BorderLayout(10, 0));
        nameEditPanel.setOpaque(false);
        nameEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        nameEditPanel.add(makeFieldLabel("Username"), BorderLayout.WEST);
        editNameField = new JTextField(currentName);
        styleTextField(editNameField);
        nameEditPanel.add(editNameField, BorderLayout.CENTER);
        nameEditPanel.setVisible(false);
        card.add(nameEditPanel);
        card.add(Box.createVerticalStrut(14));

        emailReadPanel    = makeReadRow("Email", currentEmail);
        displayEmailLabel = getValueLabel(emailReadPanel);
        card.add(emailReadPanel);
        card.add(Box.createVerticalStrut(14));

        emailEditPanel = new JPanel(new BorderLayout(10, 0));
        emailEditPanel.setOpaque(false);
        emailEditPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        emailEditPanel.add(makeFieldLabel("Email"), BorderLayout.WEST);
        editEmailField = new JTextField(currentEmail);
        styleTextField(editEmailField);
        emailEditPanel.add(editEmailField, BorderLayout.CENTER);
        emailEditPanel.setVisible(false);
        card.add(emailEditPanel);
        card.add(Box.createVerticalStrut(14));

        JPanel roleRow = makeReadRow("Role", currentRole);
        displayRoleLabel = getValueLabel(roleRow);
        card.add(roleRow);
        card.add(Box.createVerticalStrut(4));

        btnEdit.addActionListener(e -> enterEditMode());
        btnCancel.addActionListener(e -> exitEditMode());
        btnSave.addActionListener(e -> saveProfileChanges());

        KeyAdapter enterToSave = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) saveProfileChanges();
            }
        };
        editNameField.addKeyListener(enterToSave);
        editEmailField.addKeyListener(enterToSave);

        return card;
    }

    private void enterEditMode() {
        editNameField.setText(profileController.getCurrentName());
        editEmailField.setText(profileController.getCurrentEmail());
        nameReadPanel.setVisible(false);
        nameEditPanel.setVisible(true);
        emailReadPanel.setVisible(false);
        emailEditPanel.setVisible(true);
        btnEdit.setVisible(false);
        btnSave.setVisible(true);
        btnCancel.setVisible(true);
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
            boolean saved = profileController.saveProfile(newName, newEmail);
            if (saved) {
                exitEditMode();
                refreshUser();
                JOptionPane.showMessageDialog(app, "Profile updated successfully!", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(app, "Failed to save. Please try again.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(app, ex.getMessage(), "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        }
    }


    // ==========================================================
    // MY VEHICLES CARD
    // ==========================================================

    /**
     * Builds the My Vehicles card.
     *
     * ★ FIX 1 — DIRECT FALLBACK LOAD:
     * After building the empty vehicleListPanel, this method calls
     * populateVehicleListDirectly() to load and display vehicles from
     * vehicles.txt immediately — without waiting for the controller callback.
     *
     * This guarantees vehicles appear on screen even if the SectionView
     * wiring in CustomerDashboard / AppFrame is incomplete.
     *
     * The controller callback (rebuildVehicleList) will overwrite this data
     * when it fires — no double-display because the content is identical.
     */
    private JPanel buildVehicleCard() {
        vehicleCard = makeCard();
        vehicleCard.setLayout(new BoxLayout(vehicleCard, BoxLayout.Y_AXIS));
        vehicleCard.setBorder(new EmptyBorder(20, 24, 20, 24));

        // ---- Title row ----
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        titleRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JLabel title = new JLabel("My vehicles");
        title.setFont(new Font("SansSerif", Font.BOLD, 15));
        title.setForeground(TEXT_DARK);
        titleRow.add(title, BorderLayout.WEST);

        JButton addVehicleBtn = makeFilledButton("+ Add vehicle", BLUE, Color.WHITE);
        addVehicleBtn.setPreferredSize(new Dimension(120, 30));
        titleRow.add(addVehicleBtn, BorderLayout.EAST);

        vehicleCard.add(titleRow);
        vehicleCard.add(Box.createVerticalStrut(8));
        vehicleCard.add(makeDivider());
        vehicleCard.add(Box.createVerticalStrut(14));

        // ---- Vehicle list panel ----
        vehicleListPanel = new JPanel();
        vehicleListPanel.setLayout(new BoxLayout(vehicleListPanel, BoxLayout.Y_AXIS));
        vehicleListPanel.setOpaque(false);
        vehicleListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        vehicleListPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        vehicleCard.add(vehicleListPanel);

        // ---- Add form ----
        vehicleAddPanel = buildAddVehicleForm();
        vehicleAddPanel.setVisible(false);

        addVehicleBtn.addActionListener(e -> {
            boolean nowOpen = !vehicleAddPanel.isVisible();
            vehicleAddPanel.setVisible(nowOpen);
            if (nowOpen) {
                clearAllTextFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
            }
            vehicleCard.revalidate();
            vehicleCard.repaint();
        });

        // ★ FIX 1 — DIRECT FALLBACK LOAD ────────────────────────────────────
        // Read vehicles from file RIGHT NOW and put them on screen immediately.
        // This is the safety net — it works even if the controller callback
        // (rebuildVehicleList) is never triggered due to missing wiring.
        User currentUser = app.getLoggedInUserObj();
        if (currentUser != null) {
            List<String[]> initialVehicles =
                    vehicleService.getVehiclesByUserId(currentUser.getUserId());
            populateVehicleListDirectly(initialVehicles);
        }
        // ────────────────────────────────────────────────────────────────────

        return vehicleCard;
    }

    /**
     * ★ FIX 1 — Directly fills vehicleListPanel without going through the controller.
     *
     * Kept separate from rebuildVehicleList() because vehicleAddPanel is
     * built and assigned AFTER buildVehicleCard() returns, so calling
     * rebuildVehicleList() here would reference a null vehicleAddPanel.
     * This method omits the vehicleAddPanel block to avoid that null reference.
     */
    private void populateVehicleListDirectly(List<String[]> vehicles) {
        if (vehicleListPanel == null) return;

        vehicleListPanel.removeAll();

        if (vehicles.isEmpty()) {
            vehicleListPanel.add(makeEmptyLabel("No vehicles registered yet."));
        } else {
            int rowsToShow = Math.min(MAX_VISIBLE_VEHICLES, vehicles.size());
            for (int i = 0; i < rowsToShow; i++) {
                String[] v = vehicles.get(i);
                JPanel row = buildVehicleRow(v[1], v[2], v[3], v[4], v[5]);
                row.setAlignmentX(Component.LEFT_ALIGNMENT);
                vehicleListPanel.add(row);
                if (i < rowsToShow - 1) {
                    vehicleListPanel.add(Box.createVerticalStrut(8));
                }
            }
            if (vehicles.size() > MAX_VISIBLE_VEHICLES) {
                vehicleListPanel.add(Box.createVerticalStrut(4));
                JPanel linkRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
                linkRow.setOpaque(false);
                linkRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
                JButton viewAllBtn = makeLinkButton("View All (" + vehicles.size() + ")", BLUE);
                viewAllBtn.addActionListener(e -> showViewAllDialog(vehicles));
                linkRow.add(viewAllBtn);
                vehicleListPanel.add(linkRow);
            }
        }

        vehicleListPanel.revalidate();
        vehicleListPanel.repaint();
    }

    private JPanel buildAddVehicleForm() {
        JPanel form = new JPanel(new BorderLayout(6, 0));
        form.setOpaque(false);
        form.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, 1),
                new EmptyBorder(6, 8, 6, 8)));
        form.setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
        form.setPreferredSize(new Dimension(0, 64));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill    = GridBagConstraints.BOTH;
        gbc.gridy   = 0;
        gbc.weighty = 1.0;
        gbc.insets  = new Insets(0, 1, 0, 1);

        addTypeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        addTypeCombo.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JTextField addPlateField  = makeSmallTextField("");
        JTextField addBrandField  = makeSmallTextField("");
        JTextField addYearField   = makeSmallTextField("");
        JTextField addColourField = makeSmallTextField("");

        gbc.gridx = 0; gbc.weightx = 0;   gbc.ipadx = 28;
        fields.add(wrapWithLabel("Type",          addTypeCombo),  gbc);
        gbc.gridx = 1; gbc.weightx = 0;   gbc.ipadx = 40;
        fields.add(wrapWithLabel("Car Plate",     addPlateField), gbc);
        gbc.gridx = 2; gbc.weightx = 0.7; gbc.ipadx = 0;
        fields.add(wrapWithLabel("Brand / Model", addBrandField), gbc);
        gbc.gridx = 3; gbc.weightx = 0;   gbc.ipadx = 26;
        fields.add(wrapWithLabel("Year",          addYearField),  gbc);
        gbc.gridx = 4; gbc.weightx = 0;   gbc.ipadx = 34;
        fields.add(wrapWithLabel("Colour",        addColourField), gbc);

        form.add(fields, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        btnPanel.setOpaque(false);

        JButton saveBtn   = makeFilledButton("Save",   GREEN,    Color.WHITE);
        JButton cancelBtn = makeFilledButton("Cancel", GREY_BTN, Color.WHITE);
        saveBtn.setPreferredSize(new Dimension(65, 28));
        cancelBtn.setPreferredSize(new Dimension(80, 28));
        btnPanel.add(saveBtn);
        btnPanel.add(cancelBtn);
        form.add(btnPanel, BorderLayout.EAST);

        cancelBtn.addActionListener(e -> {
            vehicleAddPanel.setVisible(false);
            clearAllTextFields(vehicleAddPanel);
            if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
            if (vehicleCard  != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
        });

        Runnable doSave = () -> {
            String type = (String) addTypeCombo.getSelectedItem();
            String[] newVehicleData = {
                type,
                addPlateField.getText().trim(),
                addBrandField.getText().trim(),
                addYearField.getText().trim(),
                addColourField.getText().trim()
            };
            boolean success = vehicleController.handleAdd(newVehicleData);
            if (success) {
                clearAllTextFields(vehicleAddPanel);
                if (addTypeCombo != null) addTypeCombo.setSelectedIndex(0);
                vehicleAddPanel.setVisible(false);
                if (vehicleCard != null) { vehicleCard.revalidate(); vehicleCard.repaint(); }
                JOptionPane.showMessageDialog(app, "Vehicle added successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        saveBtn.addActionListener(e -> doSave.run());

        KeyAdapter enterKey = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        addPlateField.addKeyListener(enterKey);
        addBrandField.addKeyListener(enterKey);
        addYearField.addKeyListener(enterKey);
        addColourField.addKeyListener(enterKey);

        final boolean[] comboOpen = { false };
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


    // ==========================================================
    // VEHICLE ROW
    // ==========================================================

    private JPanel buildVehicleRow(String vehicleType, String plate,
                                   String brand, String year, String colour) {

        JPanel displayCard = new JPanel(new BorderLayout(0, 0));
        displayCard.setOpaque(false);
        displayCard.setBorder(new EmptyBorder(10, 12, 10, 8));

        JLabel iconLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawVehicleIcon(g2, vehicleType, getWidth(), getHeight());
                g2.dispose();
            }
        };
        iconLabel.setPreferredSize(new Dimension(40, 40));
        displayCard.add(iconLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel brandLabel = new JLabel(brand);
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLabel.setForeground(TEXT_DARK);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLabel = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLabel.setForeground(TEXT_GREY);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(brandLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(detailLabel);
        infoPanel.add(Box.createVerticalGlue());
        displayCard.add(infoPanel, BorderLayout.CENTER);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionButtons.setOpaque(false);
        JButton editLink   = makeLinkButton("Edit",   BLUE);
        JButton removeLink = makeLinkButton("Remove", RED);
        actionButtons.add(editLink);
        actionButtons.add(removeLink);
        displayCard.add(actionButtons, BorderLayout.EAST);

        JPanel editCard = buildVehicleEditCard(vehicleType, plate, brand, year, colour);

        CardLayout switcher   = new CardLayout();
        JPanel switcherPanel  = new JPanel(switcher);
        switcherPanel.setOpaque(false);
        switcherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        switcherPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        wrapper.add(switcherPanel, BorderLayout.CENTER);

        JComboBox<String> eTypeCombo = (JComboBox<String>) editCard.getClientProperty("typeCombo");
        JTextField ePlate  = (JTextField) editCard.getClientProperty("plate");
        JTextField eBrand  = (JTextField) editCard.getClientProperty("brand");
        JTextField eYear   = (JTextField) editCard.getClientProperty("year");
        JTextField eColour = (JTextField) editCard.getClientProperty("colour");
        JButton    eSave   = (JButton)    editCard.getClientProperty("saveBtn");
        JButton    eCancel = (JButton)    editCard.getClientProperty("cancelBtn");

        editLink.addActionListener(e -> {
            if (eTypeCombo != null) eTypeCombo.setSelectedItem(vehicleType);
            if (ePlate  != null) ePlate.setText(plate);
            if (eBrand  != null) eBrand.setText(brand);
            if (eYear   != null) eYear.setText(year);
            if (eColour != null) eColour.setText(colour);
            switcherPanel.setPreferredSize(new Dimension(0, EDIT_HEIGHT));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_HEIGHT));
            wrapper.setPreferredSize(new Dimension(0, EDIT_HEIGHT));
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_HEIGHT));
            switcher.show(switcherPanel, "edit");
            if (ePlate != null) ePlate.requestFocusInWindow();
            if (wrapper.getParent() != null) wrapper.getParent().revalidate();
        });

        if (eCancel != null) {
            eCancel.addActionListener(e -> {
                switcherPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                wrapper.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                switcher.show(switcherPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
            });
        }

        Runnable doEditSave = () -> {
            String newType   = (eTypeCombo != null) ? (String) eTypeCombo.getSelectedItem() : vehicleType;
            String newPlate  = (ePlate  != null) ? ePlate.getText().trim()  : plate;
            String newBrand  = (eBrand  != null) ? eBrand.getText().trim()  : brand;
            String newYear   = (eYear   != null) ? eYear.getText().trim()   : year;
            String newColour = (eColour != null) ? eColour.getText().trim() : colour;

            if (newType.equals(vehicleType) && newPlate.equals(plate)
                    && newBrand.equals(brand) && newYear.equals(year)
                    && newColour.equals(colour)) {
                JOptionPane.showMessageDialog(app, "No changes were made.", "No Changes",
                        JOptionPane.INFORMATION_MESSAGE);
                if (eCancel != null) eCancel.doClick();
                return;
            }

            boolean updated = vehicleController.handleEdit(
                    plate, new String[]{ newType, newPlate, newBrand, newYear, newColour });

            if (updated) {
                switcherPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                wrapper.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                switcher.show(switcherPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
                JOptionPane.showMessageDialog(app, "Vehicle updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        };
        if (eSave != null) eSave.addActionListener(e -> doEditSave.run());

        KeyAdapter enterSave = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doEditSave.run();
            }
        };
        if (ePlate  != null) ePlate.addKeyListener(enterSave);
        if (eBrand  != null) eBrand.addKeyListener(enterSave);
        if (eYear   != null) eYear.addKeyListener(enterSave);
        if (eColour != null) eColour.addKeyListener(enterSave);

        removeLink.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(app,
                    "Are you sure you want to remove " + brand + " (" + plate + ")?",
                    "Confirm Remove", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                User user = app.getLoggedInUserObj();
                boolean deleted = (user != null)
                        && vehicleService.deleteVehicle(user.getUserId(), plate);
                if (deleted) {
                    vehicleController.refreshList();
                } else {
                    JOptionPane.showMessageDialog(app,
                            "Failed to remove vehicle. Please try again.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return wrapper;
    }

    private JPanel buildVehicleEditCard(String vehicleType, String plate,
                                        String brand, String year, String colour) {
        JPanel editCard = new JPanel(new BorderLayout(6, 0));
        editCard.setOpaque(false);
        editCard.setBorder(new EmptyBorder(6, 10, 6, 10));

        JPanel editFields = new JPanel(new GridBagLayout());
        editFields.setOpaque(false);

        GridBagConstraints ec = new GridBagConstraints();
        ec.fill    = GridBagConstraints.BOTH;
        ec.gridy   = 0;
        ec.weighty = 1.0;
        ec.insets  = new Insets(0, 1, 0, 1);

        JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Car", "Motor"});
        typeCombo.setSelectedItem(vehicleType);
        typeCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));

        JTextField plateField  = makeSmallTextField(plate);
        JTextField brandField  = makeSmallTextField(brand);
        JTextField yearField   = makeSmallTextField(year);
        JTextField colourField = makeSmallTextField(colour);

        ec.gridx = 0; ec.weightx = 0;   ec.ipadx = 28;
        editFields.add(wrapWithLabel("Type",   typeCombo),   ec);
        ec.gridx = 1; ec.weightx = 0;   ec.ipadx = 40;
        editFields.add(wrapWithLabel("Plate",  plateField),  ec);
        ec.gridx = 2; ec.weightx = 0.7; ec.ipadx = 0;
        editFields.add(wrapWithLabel("Brand",  brandField),  ec);
        ec.gridx = 3; ec.weightx = 0;   ec.ipadx = 26;
        editFields.add(wrapWithLabel("Year",   yearField),   ec);
        ec.gridx = 4; ec.weightx = 0;   ec.ipadx = 34;
        editFields.add(wrapWithLabel("Colour", colourField), ec);
        editCard.add(editFields, BorderLayout.CENTER);

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
        editCard.add(btns, BorderLayout.EAST);

        editCard.putClientProperty("typeCombo", typeCombo);
        editCard.putClientProperty("plate",     plateField);
        editCard.putClientProperty("brand",     brandField);
        editCard.putClientProperty("year",      yearField);
        editCard.putClientProperty("colour",    colourField);
        editCard.putClientProperty("saveBtn",   saveBtn);
        editCard.putClientProperty("cancelBtn", cancelBtn);

        return editCard;
    }

    private void drawVehicleIcon(Graphics2D g2, String vehicleType, int width, int height) {
        int cx = width / 2, cy = height / 2;
        g2.setColor(new Color(80, 110, 230, 200));
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if ("Motor".equalsIgnoreCase(vehicleType)) {
            g2.drawOval(cx - 8,  cy - 5, 16, 10);
            g2.drawOval(cx + 7,  cy + 2,  8,  8);
            g2.drawOval(cx - 15, cy + 2,  8,  8);
            g2.drawLine(cx + 4, cy - 5, cx + 12, cy - 8);
        } else {
            g2.drawRoundRect(cx - 8,  cy - 9, 16,  8, 4, 4);
            g2.drawRoundRect(cx - 14, cy - 3, 28, 10, 3, 3);
            g2.fillOval(cx - 11, cy + 5, 7, 7);
            g2.fillOval(cx + 4,  cy + 5, 7, 7);
        }
    }


    // ==========================================================
    // VIEW ALL VEHICLES POPUP DIALOG
    // ==========================================================

    private void showViewAllDialog(List<String[]> initialVehicles) {
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "All My Vehicles", true);
        dialog.setSize(820, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
        dialog.setResizable(false);

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.WHITE);
        header.setBorder(new EmptyBorder(16, 22, 12, 22));
        JLabel titleLbl = new JLabel("All My Vehicles");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(TEXT_DARK);
        header.add(titleLbl, BorderLayout.WEST);
        JLabel countLbl = new JLabel(initialVehicles.size() + " vehicles");
        countLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLbl.setForeground(TEXT_GREY);
        header.add(countLbl, BorderLayout.EAST);
        dialog.add(header, BorderLayout.NORTH);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);
        listPanel.setBorder(new EmptyBorder(8, 18, 18, 18));

        Runnable[] refreshDialog = { null };
        refreshDialog[0] = () -> {
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
                    JPanel row = buildDialogVehicleRow(latest.get(i), dialog, refreshDialog[0]);
                    row.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(row);
                    if (i < latest.size() - 1)
                        listPanel.add(Box.createVerticalStrut(8));
                }
            }

            listPanel.revalidate();
            listPanel.repaint();
            vehicleController.refreshList();
            if (latest.size() <= MAX_VISIBLE_VEHICLES) dialog.dispose();
        };

        for (int i = 0; i < initialVehicles.size(); i++) {
            JPanel row = buildDialogVehicleRow(initialVehicles.get(i), dialog, refreshDialog[0]);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(row);
            if (i < initialVehicles.size() - 1)
                listPanel.add(Box.createVerticalStrut(8));
        }

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, CARD_BORDER));
        JButton closeBtn = makeFilledButton("Close", new Color(108, 117, 125), Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(78, 32));
        closeBtn.addActionListener(e -> dialog.dispose());
        footer.add(closeBtn);
        dialog.add(footer, BorderLayout.SOUTH);

        dialog.setVisible(true);
    }

    private JPanel buildDialogVehicleRow(String[] v, JDialog parentDialog, Runnable onChanged) {
        String vehicleType = v[1], plate = v[2], brand = v[3], year = v[4], colour = v[5];

        JPanel displayCard = new JPanel(new BorderLayout(0, 0));
        displayCard.setOpaque(false);
        displayCard.setBorder(new EmptyBorder(10, 12, 10, 8));

        JLabel iconLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                drawVehicleIcon(g2, vehicleType, getWidth(), getHeight());
                g2.dispose();
            }
        };
        iconLabel.setPreferredSize(new Dimension(40, 40));
        displayCard.add(iconLabel, BorderLayout.WEST);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);
        infoPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        JLabel brandLabel  = new JLabel(brand);
        brandLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        brandLabel.setForeground(TEXT_DARK);
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel detailLabel = new JLabel(plate + "  \u00B7  " + year + "  \u00B7  " + colour);
        detailLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        detailLabel.setForeground(TEXT_GREY);
        detailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(brandLabel);
        infoPanel.add(Box.createVerticalStrut(3));
        infoPanel.add(detailLabel);
        infoPanel.add(Box.createVerticalGlue());
        displayCard.add(infoPanel, BorderLayout.CENTER);

        JPanel actionButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actionButtons.setOpaque(false);
        JButton editLink   = makeLinkButton("Edit",   BLUE);
        JButton removeLink = makeLinkButton("Remove", RED);
        actionButtons.add(editLink);
        actionButtons.add(removeLink);
        displayCard.add(actionButtons, BorderLayout.EAST);

        JPanel editCard = buildVehicleEditCard(vehicleType, plate, brand, year, colour);

        CardLayout switcher   = new CardLayout();
        JPanel switcherPanel  = new JPanel(switcher);
        switcherPanel.setOpaque(false);
        switcherPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        switcherPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        switcherPanel.add(displayCard, "display");
        switcherPanel.add(editCard,    "edit");
        switcher.show(switcherPanel, "display");

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createLineBorder(CARD_BORDER, 1));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.setPreferredSize(new Dimension(0, ROW_HEIGHT));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        wrapper.add(switcherPanel, BorderLayout.CENTER);

        JComboBox<String> eType   = (JComboBox<String>) editCard.getClientProperty("typeCombo");
        JTextField ePlate  = (JTextField) editCard.getClientProperty("plate");
        JTextField eBrand  = (JTextField) editCard.getClientProperty("brand");
        JTextField eYear   = (JTextField) editCard.getClientProperty("year");
        JTextField eColour = (JTextField) editCard.getClientProperty("colour");
        JButton    eSave   = (JButton)    editCard.getClientProperty("saveBtn");
        JButton    eCancel = (JButton)    editCard.getClientProperty("cancelBtn");

        editLink.addActionListener(e -> {
            if (eType   != null) eType.setSelectedItem(vehicleType);
            if (ePlate  != null) ePlate.setText(plate);
            if (eBrand  != null) eBrand.setText(brand);
            if (eYear   != null) eYear.setText(year);
            if (eColour != null) eColour.setText(colour);
            switcherPanel.setPreferredSize(new Dimension(0, EDIT_HEIGHT));
            switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_HEIGHT));
            wrapper.setPreferredSize(new Dimension(0, EDIT_HEIGHT));
            wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, EDIT_HEIGHT));
            switcher.show(switcherPanel, "edit");
            if (ePlate != null) ePlate.requestFocusInWindow();
            if (wrapper.getParent() != null) wrapper.getParent().revalidate();
        });

        if (eCancel != null) {
            eCancel.addActionListener(e -> {
                switcherPanel.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                switcherPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                wrapper.setPreferredSize(new Dimension(0, ROW_HEIGHT));
                wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
                switcher.show(switcherPanel, "display");
                if (wrapper.getParent() != null) wrapper.getParent().revalidate();
            });
        }

        Runnable doSave = () -> {
            String nt = (eType   != null) ? (String) eType.getSelectedItem() : vehicleType;
            String np = (ePlate  != null) ? ePlate.getText().trim()  : plate;
            String nb = (eBrand  != null) ? eBrand.getText().trim()  : brand;
            String ny = (eYear   != null) ? eYear.getText().trim()   : year;
            String nc = (eColour != null) ? eColour.getText().trim() : colour;

            boolean noChange = nt.equals(vehicleType) && np.equals(plate)
                    && nb.equals(brand) && ny.equals(year) && nc.equals(colour);
            if (noChange) {
                JOptionPane.showMessageDialog(parentDialog, "No changes were made.", "No Changes",
                        JOptionPane.INFORMATION_MESSAGE);
                if (eCancel != null) eCancel.doClick();
                return;
            }

            boolean updated = vehicleController.handleEdit(plate, new String[]{ nt, np, nb, ny, nc });
            if (updated) {
                JOptionPane.showMessageDialog(parentDialog, "Vehicle updated successfully.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                onChanged.run();
            }
        };
        if (eSave != null) eSave.addActionListener(e -> doSave.run());

        KeyAdapter enterSave = new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doSave.run();
            }
        };
        if (ePlate  != null) ePlate.addKeyListener(enterSave);
        if (eBrand  != null) eBrand.addKeyListener(enterSave);
        if (eYear   != null) eYear.addKeyListener(enterSave);
        if (eColour != null) eColour.addKeyListener(enterSave);

        removeLink.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(parentDialog,
                    "Remove " + brand + " (" + plate + ")?", "Confirm Remove",
                    JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (choice == JOptionPane.YES_OPTION) {
                User user = app.getLoggedInUserObj();
                if (user != null && vehicleService.deleteVehicle(user.getUserId(), plate)) {
                    JOptionPane.showMessageDialog(parentDialog,
                            brand + " removed successfully.", "Removed",
                            JOptionPane.INFORMATION_MESSAGE);
                    onChanged.run();
                } else {
                    JOptionPane.showMessageDialog(parentDialog,
                            "Failed to remove vehicle.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return wrapper;
    }


    // ==========================================================
    // IMAGE PICKERS
    // ==========================================================

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
            BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!profilePicStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save the picture.",
                        "Error", JOptionPane.ERROR_MESSAGE);
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
            BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (img == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!backgroundStorage.saveImage(user.getUserId(), img)) {
                JOptionPane.showMessageDialog(app, "Failed to save the image.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            bannerImage = img;
            if (profileBanner != null) profileBanner.repaint();
        } catch (IOException ex) { ex.printStackTrace(); }
    }


    // ==========================================================
    // HELPER METHODS
    // ==========================================================

    private JPanel makeCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
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

    private JPanel makeReadRow(String fieldName, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        JLabel nameLabel = new JLabel(fieldName);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        nameLabel.setForeground(TEXT_GREY);
        nameLabel.setPreferredSize(new Dimension(90, 18));
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        valueLabel.setForeground(TEXT_DARK);
        row.add(nameLabel,  BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.CENTER);
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

    private void styleTextField(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 13));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 2),
                new EmptyBorder(2, 6, 2, 6)));
    }

    private JTextField makeSmallTextField(String value) {
        JTextField field = new JTextField(value);
        field.setFont(new Font("SansSerif", Font.PLAIN, 12));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BLUE, 1),
                new EmptyBorder(2, 3, 2, 3)));
        return field;
    }

    private JPanel wrapWithLabel(String label, JTextField field) {
        JPanel panel = new JPanel(new BorderLayout(0, 1));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(TEXT_GREY);
        panel.add(lbl,   BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    private JPanel wrapWithLabel(String label, JComboBox<String> combo) {
        JPanel panel = new JPanel(new BorderLayout(0, 1));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 9));
        lbl.setForeground(TEXT_GREY);
        panel.add(lbl,   BorderLayout.NORTH);
        panel.add(combo, BorderLayout.CENTER);
        return panel;
    }

    private JButton makeLinkButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btn.setForeground(color);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 4, 0, 4));
        return btn;
    }

    private JButton makeFilledButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovered = true;  repaint(); }
                    public void mouseExited (MouseEvent e) { hovered = false; repaint(); }
                });
            }
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? bg.darker() : bg);
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

    private JLabel makeEmptyLabel(String message) {
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(TEXT_GREY);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        return lbl;
    }

    private void clearAllTextFields(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JTextField) {
                ((JTextField) comp).setText("");
            } else if (comp instanceof Container) {
                clearAllTextFields((Container) comp);
            }
        }
    }
}