package view;
 
import model.AccountService;
import model.BackgroundImageStorage;
import model.ProfilePicStorage;
import model.User;
 
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
 
 
public class TechnicianDashboard extends JPanel {
 
   
    private final AppFrame app;
 
    private CardLayout contentLayout;
    private JPanel     contentPanel;
    private String     activeNav = "Dashboard";
 
    private JLabel headerTitle;
    private JLabel profileLabel;
    private JLabel avatarLabel;
 
    private JTextField profileNameField;
    private JTextField profileEmailField;
    private JLabel     profileRoleLabel;

    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;
    private JPanel        profileBanner;
    private JLabel        profilePicLabel;
    private final ProfilePicStorage      profilePicStorage   = new ProfilePicStorage();
    private final BackgroundImageStorage backgroundStorage   = new BackgroundImageStorage();

    private TechDashboard  dashboardPanel;
    private TechAppointment appointmentPanel;
    private TechFeedback    feedbackPanel;

    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);

    private static final String[] NAV_ITEMS = {
            "Dashboard", "My Appointments", "My Feedbacks"
    };
    private static final String[] NAV_ICONS = {
            "\u2302", "\u2637", "\u2605"
    };
 
 
 
    private JMenuItem menuItem(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(UIConstants.FONT_SMALL);
        item.setForeground(UIConstants.TEXT_DARK);
        item.setBackground(Color.WHITE);
        item.setBorder(new EmptyBorder(8, 20, 8, 30));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return item;
    }
 
    private JButton navButton(String text, boolean active) {
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
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 14));
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(0, 20, 0, 20));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.putClientProperty("active", active);
        btn.setRolloverEnabled(true);
        return btn;
    }
 
    private void styleNavBtn(JButton btn, boolean active) {
        btn.putClientProperty("active", active);
        btn.setForeground(active ? Color.WHITE : UIConstants.TEXT_SIDEBAR);
        btn.setFont(new Font("SansSerif", active ? Font.BOLD : Font.PLAIN, 14));
        btn.repaint();
    }
 
    private void refreshProfileFields() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        if (profileNameField  != null) { profileNameField.setText(user.getName());  profileNameField.setForeground(Color.BLACK); }
        if (profileEmailField != null) { profileEmailField.setText(user.getEmail()); profileEmailField.setForeground(Color.BLACK); }
        if (profileRoleLabel  != null) {
            String r = user.getRole();
            profileRoleLabel.setText(r.substring(0, 1).toUpperCase() + r.substring(1));
        }
        profileImage = profilePicStorage.loadImage(user.getEmail());
        bannerImage  = backgroundStorage.loadImage(user.getEmail());
        if (profileBanner   != null) profileBanner.repaint();
        if (profilePicLabel != null) profilePicLabel.repaint();
    }
 
    private void handleProfileSave() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        String newName  = UIFactory.getFieldValue(profileNameField,  "Enter your name");
        String newEmail = UIFactory.getFieldValue(profileEmailField, "Enter your email");
        if (newName.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(app, "Name and email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        if (!newName.matches("[a-zA-Z ]{2,50}")) {
            JOptionPane.showMessageDialog(app, "Name must be 2-50 characters (letters only).", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            JOptionPane.showMessageDialog(app, "Please enter a valid email address.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        AccountService svc = app.getAccountService();
        if (!newEmail.equalsIgnoreCase(user.getEmail()) && svc.emailExists(newEmail)) {
            JOptionPane.showMessageDialog(app, "An account with this email already exists.", "Error", JOptionPane.ERROR_MESSAGE); return;
        }
        User updated = new User(user.getUserId(), newName, newEmail, user.getPassword(), user.getRole(), 0);
        if (svc.updateUser(user.getEmail(), updated)) {
            app.setLoggedInUser(newName);
            app.setLoggedInUserObj(updated);
            refreshUser();
            JOptionPane.showMessageDialog(app, "Profile updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(app, "Failed to save profile.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
 
    private void chooseProfileImage() {
        User user = app.getLoggedInUserObj();
        if (user == null) return;
        FileDialog fd = new FileDialog((Frame) SwingUtilities.getWindowAncestor(this), "Choose Profile Picture", FileDialog.LOAD);
        fd.setFile("*.jpg;*.jpeg;*.png;*.gif;*.bmp");
        fd.setVisible(true);
        if (fd.getFile() == null) return;
        try {
            BufferedImage image = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
            if (image == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (!profilePicStorage.saveImage(user.getEmail(), image)) {
                JOptionPane.showMessageDialog(app, "Failed to save profile picture.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            profileImage = image;
            if (profilePicLabel != null) profilePicLabel.repaint();
            if (avatarLabel     != null) avatarLabel.repaint();
        } catch (java.io.IOException ex) { ex.printStackTrace(); }
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
            if (image == null) {
                JOptionPane.showMessageDialog(app, "Could not read the selected image.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            if (!backgroundStorage.saveImage(user.getEmail(), image)) {
                JOptionPane.showMessageDialog(app, "Failed to save background image.", "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            bannerImage = image;
            if (profileBanner != null) profileBanner.repaint();
        } catch (java.io.IOException ex) { ex.printStackTrace(); }
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
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                if (profileImage != null) {
                    int imgW  = profileImage.getWidth();
                    int imgH  = profileImage.getHeight();
                    int crop  = Math.min(imgW, imgH);
                    int cropX = (imgW - crop) / 2;
                    int cropY = (imgH - crop) / 2;
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
 
    private JPanel buildProfileFormCard() {
        JPanel card = UIFactory.createCard();
        card.setBorder(new EmptyBorder(32, 50, 40, 50));
 
        JLabel sectionLabel = new JLabel("Profile Information");
        sectionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        sectionLabel.setForeground(UIConstants.TEXT_PRIMARY);
        sectionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(sectionLabel);
        card.add(Box.createVerticalStrut(20));
 
        JSeparator topSep = new JSeparator();
        topSep.setForeground(UIConstants.BORDER_DEFAULT);
        topSep.setMaximumSize(new Dimension(380, 1));
        topSep.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(topSep);
        card.add(Box.createVerticalStrut(22));
 
        card.add(UIFactory.createFieldLabel("Name")); card.add(Box.createVerticalStrut(6));
        profileNameField = UIFactory.createTextField("Enter your name"); card.add(profileNameField);
        card.add(Box.createVerticalStrut(16));
 
        card.add(UIFactory.createFieldLabel("Email")); card.add(Box.createVerticalStrut(6));
        profileEmailField = UIFactory.createTextField("Enter your email"); card.add(profileEmailField);
        card.add(Box.createVerticalStrut(16));
 
        card.add(UIFactory.createFieldLabel("Role")); card.add(Box.createVerticalStrut(6));
        profileRoleLabel = new JLabel("\u2014");
        profileRoleLabel.setFont(UIConstants.FONT_BODY);
        profileRoleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        profileRoleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        profileRoleLabel.setMaximumSize(new Dimension(380, 30));
        profileRoleLabel.setBorder(new EmptyBorder(8, 14, 8, 14));
        card.add(profileRoleLabel);
        card.add(Box.createVerticalStrut(28));
 
        JButton saveBtn = UIFactory.createPrimaryButton("Save Changes");
        saveBtn.addActionListener(e -> handleProfileSave());
        card.add(saveBtn);
 
        return card;
    }
 
    private JPanel buildProfilePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UIConstants.BG_CONTENT);
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setBackground(UIConstants.BG_CONTENT);
        inner.setBorder(new EmptyBorder(0, 0, 40, 0));
        inner.add(buildBannerHero());
        inner.add(Box.createVerticalStrut(24));
        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);
        center.add(buildProfileFormCard());
        inner.add(center);
        JScrollPane scroll = new JScrollPane(inner);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        page.add(scroll, BorderLayout.CENTER);
        return page;
    }
 
    private JPanel buildProfileArea() {
        JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 12));
        profileArea.setBackground(UIConstants.BG_HEADER);
 
        avatarLabel = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (profileImage != null) {
                    int iw = profileImage.getWidth(), ih = profileImage.getHeight();
                    int crop = Math.min(iw, ih);
                    int cx = (iw - crop) / 2, cy = (ih - crop) / 2;
                    g2.setClip(new Ellipse2D.Float(0, 0, 38, 38));
                    g2.drawImage(profileImage, 0, 0, 38, 38, cx, cy, cx + crop, cy + crop, null);
                    g2.setClip(null);
                } else {
                    g2.setColor(BRAND_BLUE);
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
 
        profileLabel = new JLabel("Technician");
        profileLabel.setFont(UIConstants.FONT_BODY_BOLD);
        profileLabel.setForeground(UIConstants.TEXT_PRIMARY);
        profileLabel.setBorder(new EmptyBorder(0, 10, 0, 6));
 
        JLabel arrow = new JLabel("\u25BE");
        arrow.setFont(new Font("SansSerif", Font.PLAIN, 12));
        arrow.setForeground(UIConstants.TEXT_MUTED);
 
        JPanel profileBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        profileBtn.setBackground(UIConstants.BG_HEADER);
        profileBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        profileBtn.add(avatarLabel); profileBtn.add(profileLabel); profileBtn.add(arrow);
 
        JPopupMenu menu = new JPopupMenu();
        menu.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 220, 225), 1), new EmptyBorder(6, 0, 6, 0)));
        menu.setBackground(Color.WHITE);
 
        JMenuItem viewProfile = menuItem("View Profile");
        viewProfile.addActionListener(e -> {
            activeNav = "";
            headerTitle.setText("My Profile");
            contentLayout.show(contentPanel, "Profile");
            refreshProfileFields();
        });
        JMenuItem logout = menuItem("Logout");
        logout.setForeground(UIConstants.TEXT_DANGER);
        logout.addActionListener(e -> { app.setLoggedInUser(""); app.setLoggedInUserObj(null); app.showPage(PageName.ONBOARDING); });
        menu.add(viewProfile); menu.add(logout);
 
        profileBtn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { menu.show(profileBtn, profileBtn.getWidth() - menu.getPreferredSize().width, profileBtn.getHeight()); }
        });
        menu.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) { if (!menu.getBounds().contains(e.getPoint())) menu.setVisible(false); }
        });
 
        profileArea.add(profileBtn);
        return profileArea;
    }
 
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.BG_HEADER);
        header.setPreferredSize(new Dimension(0, UIConstants.HEADER_HEIGHT));
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_HEADER),
                new EmptyBorder(0, 30, 0, 25)));
        headerTitle = new JLabel("Dashboard");
        headerTitle.setFont(UIConstants.FONT_BODY_BOLD);
        headerTitle.setForeground(UIConstants.TEXT_PRIMARY);
        header.add(headerTitle, BorderLayout.WEST);
        header.add(buildProfileArea(), BorderLayout.EAST);
        return header;
    }
 
 
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
        try { ImageIcon raw = new ImageIcon(getClass().getResource("/Image/apu-logo.png")); logoArea.add(new JLabel(new ImageIcon(raw.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)))); } catch (Exception ignored) {}
        JLabel brand = new JLabel("APU ASC Technician");
        brand.setFont(UIConstants.FONT_SIDEBAR); brand.setForeground(Color.WHITE);
        brand.setAlignmentX(Component.LEFT_ALIGNMENT); brand.setBorder(new EmptyBorder(8, 0, 0, 0));
        logoArea.add(brand); sidebar.add(logoArea);
 
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.SIDEBAR_DIVIDER); sep.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 1));
        sidebar.add(sep); sidebar.add(Box.createVerticalStrut(10));
 
        JLabel menuLabel = new JLabel("MENU");
        menuLabel.setFont(UIConstants.FONT_LABEL); menuLabel.setForeground(UIConstants.TEXT_NAV_LABEL);
        menuLabel.setBorder(new EmptyBorder(10, 24, 10, 20)); menuLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuLabel.setMaximumSize(new Dimension(UIConstants.SIDEBAR_WIDTH, 35)); sidebar.add(menuLabel);
 
        JButton[] btns = new JButton[NAV_ITEMS.length];
        for (int i = 0; i < NAV_ITEMS.length; i++) {
            final String name = NAV_ITEMS[i];
            btns[i] = navButton(NAV_ICONS[i] + "   " + name, name.equals(activeNav));
            btns[i].addActionListener(e -> {
                activeNav = name;
                for (int j = 0; j < btns.length; j++) styleNavBtn(btns[j], NAV_ITEMS[j].equals(activeNav));
                headerTitle.setText(name);
                contentLayout.show(contentPanel, name);
                // Refresh the panel when navigating to it
                if (name.equals("My Appointments") && appointmentPanel != null) appointmentPanel.refresh();
                if (name.equals("My Feedbacks")    && feedbackPanel    != null) feedbackPanel.refresh();
                if (name.equals("Dashboard")        && dashboardPanel   != null) dashboardPanel.refresh();
            });
            sidebar.add(btns[i]); sidebar.add(Box.createVerticalStrut(2));
        }
        sidebar.add(Box.createVerticalGlue());
        return sidebar;
    }
 
 
    public TechnicianDashboard(AppFrame app) {
        this.app = app;
        setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
 
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);
 
        contentLayout = new CardLayout();
        contentPanel  = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);
 
        // Create each panel — each is its own class
        dashboardPanel   = new TechDashboard(app);
        appointmentPanel = new TechAppointment(app);
        feedbackPanel    = new TechFeedback(app);
 
        // Add to CardLayout — name must match NAV_ITEMS exactly
        contentPanel.add(dashboardPanel,   "Dashboard");
        contentPanel.add(appointmentPanel, "My Appointments");
        contentPanel.add(feedbackPanel,    "My Feedbacks");
        contentPanel.add(buildProfilePage(), "Profile");
 
        rightSide.add(contentPanel, BorderLayout.CENTER);
        add(rightSide, BorderLayout.CENTER);
    }
 
    @Override
    public void addNotify() {
        super.addNotify();
        refreshUser();
    }
 
    public void refreshUser() {
        String name = app.getLoggedInUser();
        if (name == null || name.isEmpty()) name = "Technician";
        if (profileLabel != null) profileLabel.setText(name);
 
        User user = app.getLoggedInUserObj();
        if (user != null) {
            profileImage = profilePicStorage.loadImage(user.getEmail());
            bannerImage  = backgroundStorage.loadImage(user.getEmail());
            if (profileBanner   != null) profileBanner.repaint();
            if (profilePicLabel != null) profilePicLabel.repaint();
        }
        if (avatarLabel != null) avatarLabel.repaint();
 
        // Refresh all sub-panels
        if (dashboardPanel   != null) dashboardPanel.refresh();
        if (appointmentPanel != null) appointmentPanel.refresh();
        if (feedbackPanel    != null) feedbackPanel.refresh();
    }
}