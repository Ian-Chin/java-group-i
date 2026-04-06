package view;

import model.AccountService;
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.ProfilePicStorage;
import model.BackgroundImageStorage;
import model.User;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.ArrayList;

public class TechnicianDashboard extends JPanel {
	private final AppFrame app;
	
	private CardLayout contentLayout;
	private JPanel contentPanel;
	private String activeNav = "Dashboard";
	
//	gui design
	
	private JLabel headerTitle;
	private JLabel profileLabel;
	private JLabel avatarLabel;
	
	private JTextField profileNameField;
	private JTextField profileEmailField;
	private JLabel     profileRoleLabel;
    private JLabel     profileAvatarDisplay;
    private JPanel     avatarSelectionPanel;
    private JPanel feedbackListPanel;
    
    private int selectedAvatarIndex = 0;
    
//  profile pics and banner  
    private BufferedImage profileImage = null;
    private BufferedImage bannerImage  = null;
    private JPanel profileBanner;
    private JLabel profilePicLabel;
   
    private final ProfilePicStorage      profilePicStorage = new ProfilePicStorage();
    private final BackgroundImageStorage backgroundStorage = new BackgroundImageStorage();
    
//    appointment panel
    private JPanel appointmentsListPanel;
    private final AppointmentService appointmentService = new AppointmentService();
    
    private static final Color BRAND_BLUE  = new Color(80, 110, 230);
    private static final Color BANNER_BLUE = new Color(100, 130, 240);
    
    private static final Color[] AVATAR_COLORS = {
            new Color(80, 110, 230), new Color(230, 80, 80),  new Color(80, 190, 110),
            new Color(230, 160, 40), new Color(160, 80, 230), new Color(40, 180, 200),
            new Color(230, 80, 160), new Color(100, 100, 120),
    };
    private static final String[] AVATAR_ICONS = {
            "\u263A", "\u2605", "\u2665", "\u2666",
            "\u263C", "\u2708", "\u266B", "\u2618"
    };
            
// sidebar navigation
            private static final String[] NAV_ITEMS = {
            		"Dashboard", "My Appointments", "My Feedbacks"
            };
            
            private static final String[] NAV_ICONS = {
            		"\u2302", "\u2637", "\u2605"
            };
            
            private static final Color GREEN  = new Color(40,  167, 69);
            private static final Color ORANGE = new Color(255, 165,  0);
            private static final Color GREY   = new Color(108, 117, 125);
            
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
     
   
   private JButton actionButton(String text, Color bg, Color fg) {
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
       btn.setPreferredSize(new Dimension(130, 34));
       btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
       return btn;
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

   
   private void updateAvatarSelection() {
       if (profileAvatarDisplay != null) { profileAvatarDisplay.setText(AVATAR_ICONS[selectedAvatarIndex]); profileAvatarDisplay.repaint(); }
       if (avatarSelectionPanel != null) {
           Component[] avatars = avatarSelectionPanel.getComponents();
           for (int i = 0; i < avatars.length; i++) {
               if (avatars[i] instanceof JLabel) {
                   ((JLabel) avatars[i]).setBorder(i == selectedAvatarIndex
                           ? BorderFactory.createLineBorder(UIConstants.PRIMARY, 3)
                           : BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
               }
           }
       }
       if (avatarLabel != null) avatarLabel.repaint();
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
       selectedAvatarIndex = user.getProfilePicture();
       profileImage = profilePicStorage.loadImage(user.getEmail());
       bannerImage  = backgroundStorage.loadImage(user.getEmail());
       if (profileBanner   != null) profileBanner.repaint();
       if (profilePicLabel != null) profilePicLabel.repaint();
       updateAvatarSelection();
   }
   
   private void handleProfileSave() {
       User user = app.getLoggedInUserObj();
       if (user == null) return;
       String newName  = UIFactory.getFieldValue(profileNameField,  "Enter your name");
       String newEmail = UIFactory.getFieldValue(profileEmailField, "Enter your email");
       if (newName.isEmpty() || newEmail.isEmpty()) { JOptionPane.showMessageDialog(app, "Name and email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE); return; }
       if (!newName.matches("[a-zA-Z ]{2,50}")) { JOptionPane.showMessageDialog(app, "Name must be 2-50 letters only.", "Error", JOptionPane.ERROR_MESSAGE); return; }
       if (!newEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) { JOptionPane.showMessageDialog(app, "Please enter a valid email.", "Error", JOptionPane.ERROR_MESSAGE); return; }
       AccountService svc = app.getAccountService();
       if (!newEmail.equalsIgnoreCase(user.getEmail()) && svc.emailExists(newEmail)) { JOptionPane.showMessageDialog(app, "Email already exists.", "Error", JOptionPane.ERROR_MESSAGE); return; }
       User updated = new User(user.getUserId(), newName, newEmail, user.getPassword(), user.getRole(), selectedAvatarIndex);
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
           BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
           if (img == null) { JOptionPane.showMessageDialog(app, "Could not read image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
           if (!profilePicStorage.saveImage(user.getEmail(), img)) { JOptionPane.showMessageDialog(app, "Failed to save picture.", "Error", JOptionPane.ERROR_MESSAGE); return; }
           profileImage = img;
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
           BufferedImage img = ImageIO.read(new java.io.File(fd.getDirectory(), fd.getFile()));
           if (img == null) { JOptionPane.showMessageDialog(app, "Could not read image.", "Error", JOptionPane.ERROR_MESSAGE); return; }
           if (!backgroundStorage.saveImage(user.getEmail(), img)) { JOptionPane.showMessageDialog(app, "Failed to save background.", "Error", JOptionPane.ERROR_MESSAGE); return; }
           bannerImage = img;
           if (profileBanner != null) profileBanner.repaint();
       } catch (java.io.IOException ex) { ex.printStackTrace(); }
   }
   
   private String resolveName(String id) {
       for (User u : app.getAccountService().getAllUsers()) {
           if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id)) return u.getName();
           if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
       }
       return id;
   }
   
   private List<Appointment> getMyAppointments() {
	    User user = app.getLoggedInUserObj();
	    List<Appointment> mine = new ArrayList<>();
	    if (user == null) return mine;
	    for (Appointment a : appointmentService.getAll()) {
	        if (a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) {
	            mine.add(a);
	        }
	    }
	    return mine;
	}
   
   private JPanel buildSummaryCard(String status, Color accent, String icon) {
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
       card.setBorder(new EmptyBorder(24, 24, 24, 24));
       JLabel iconLabel = new JLabel(icon);
       iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 28));
       iconLabel.setForeground(accent);
       iconLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
       card.add(iconLabel);
       card.add(Box.createVerticalStrut(12));
       User user = app.getLoggedInUserObj();
       long count = 0;
       if (user != null) {
           count = appointmentService.getAll().stream()
                   .filter(a -> a.getTechnicianEmail().equalsIgnoreCase(user.getUserId()))
                   .filter(a -> a.getStatus().equalsIgnoreCase(status))
                   .count();
       }
       JLabel countLabel = new JLabel(String.valueOf(count));
       countLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
       countLabel.setForeground(UIConstants.TEXT_PRIMARY);
       countLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
       card.add(countLabel);
       card.add(Box.createVerticalStrut(4));
       JLabel statusLabel = new JLabel(status);
       statusLabel.setFont(UIConstants.FONT_SMALL_BOLD);
       statusLabel.setForeground(UIConstants.TEXT_MUTED);
       statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
       card.add(statusLabel);
       return card;
   }
   
   private void refreshFeedbacksList() {
       if (feedbackListPanel == null) return;
       feedbackListPanel.removeAll();

       List<Appointment> mine = getMyAppointments();

       if (mine.isEmpty()) {
           JLabel empty = new JLabel("No appointments to provide feedback for.");
           empty.setFont(UIConstants.FONT_BODY);
           empty.setForeground(UIConstants.TEXT_MUTED);
           empty.setAlignmentX(Component.CENTER_ALIGNMENT);
           empty.setBorder(new EmptyBorder(60, 0, 0, 0));
           feedbackListPanel.add(empty);
       } else {
           for (Appointment appt : mine) {
               feedbackListPanel.add(buildFeedbackCard(appt));
               feedbackListPanel.add(Box.createVerticalStrut(14));
           }
       }
       feedbackListPanel.revalidate();
       feedbackListPanel.repaint();
   }
       
       private JPanel buildFeedbackCard(Appointment appt) {
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
           card.setBorder(new EmptyBorder(20, 24, 20, 24));
           card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
           card.setAlignmentX(Component.LEFT_ALIGNMENT);
    
           // ── Top row: ID + status ───────────────────────────────────
           JPanel topRow = new JPanel(new BorderLayout());
           topRow.setOpaque(false);
           topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
    
           JLabel idLabel = new JLabel(
                   appt.getId() + "  ·  " + appt.getServiceType()
                   + "  ·  " + appt.getDateTime());
           idLabel.setFont(UIConstants.FONT_SMALL_BOLD);
           idLabel.setForeground(UIConstants.TEXT_MUTED);
    
           JLabel statusBadge = new JLabel(appt.getStatus());
           statusBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
           switch (appt.getStatus()) {
               case "Completed":   statusBadge.setForeground(GREEN);  break;
               case "In Progress": statusBadge.setForeground(ORANGE); break;
               default:            statusBadge.setForeground(GREY);   break;
           }
           topRow.add(idLabel, BorderLayout.WEST);
           topRow.add(statusBadge, BorderLayout.EAST);
           card.add(topRow);
           card.add(Box.createVerticalStrut(8));
    
           // ── Customer name ──────────────────────────────────────────
           JLabel custLabel = new JLabel("Customer: " + resolveName(appt.getCustomerEmail()));
           custLabel.setFont(UIConstants.FONT_BODY);
           custLabel.setForeground(UIConstants.TEXT_PRIMARY);
           custLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
           card.add(custLabel);
           card.add(Box.createVerticalStrut(12));
    
           // ── Divider ────────────────────────────────────────────────
           JSeparator sep = new JSeparator();
           sep.setForeground(UIConstants.BORDER_DEFAULT);
           sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
           card.add(sep);
           card.add(Box.createVerticalStrut(12));
    
           // ── Feedback text area ─────────────────────────────────────
           JLabel feedbackTitle = new JLabel("My Feedback:");
           feedbackTitle.setFont(UIConstants.FONT_SMALL_BOLD);
           feedbackTitle.setForeground(UIConstants.TEXT_MUTED);
           feedbackTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
           card.add(feedbackTitle);
           card.add(Box.createVerticalStrut(6));
    
           // Load existing feedback from feedback.txt (null if none yet)
           String existingFeedback = appointmentService.getFeedback(appt.getId());
           JTextArea feedbackArea = new JTextArea(existingFeedback != null ? existingFeedback : "");
           feedbackArea.setFont(UIConstants.FONT_BODY);
           feedbackArea.setForeground(UIConstants.TEXT_DARK);
           feedbackArea.setBackground(Color.WHITE);
           feedbackArea.setLineWrap(true);
           feedbackArea.setWrapStyleWord(true);
           feedbackArea.setBorder(BorderFactory.createCompoundBorder(
                   BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                   new EmptyBorder(8, 10, 8, 10)));
           feedbackArea.setRows(3);
           feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
           feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
           card.add(feedbackArea);
           card.add(Box.createVerticalStrut(12));
    
           // ── Save Feedback button ───────────────────────────────────
           JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
           btnRow.setOpaque(false);
           btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
           btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
    
           JButton saveFeedbackBtn = actionButton("Save Feedback", new Color(80, 110, 230), Color.WHITE);
           saveFeedbackBtn.addActionListener(e -> {
               String fb = feedbackArea.getText().trim();
               if (fb.isEmpty()) {
                   JOptionPane.showMessageDialog(app,
                           "Please enter feedback before saving.",
                           "Validation", JOptionPane.WARNING_MESSAGE);
                   return;
               }
               // saveFeedback() writes to feedback.txt
               if (appointmentService.saveFeedback(appt.getId(), fb)) {
                   JOptionPane.showMessageDialog(app,
                           "Feedback saved successfully!",
                           "Success", JOptionPane.INFORMATION_MESSAGE);
               } else {
                   JOptionPane.showMessageDialog(app,
                           "Failed to save feedback.", "Error",
                           JOptionPane.ERROR_MESSAGE);
               }
           });
    
           btnRow.add(saveFeedbackBtn);
           card.add(btnRow);
           return card;
       }
  
       private void refreshFeedbackList() {
           if (feedbackListPanel == null) return;
           feedbackListPanel.removeAll();
    
           List<Appointment> mine = getMyAppointments();
    
           if (mine.isEmpty()) {
               JLabel empty = new JLabel("No appointments to provide feedback for.");
               empty.setFont(UIConstants.FONT_BODY);
               empty.setForeground(UIConstants.TEXT_MUTED);
               empty.setAlignmentX(Component.CENTER_ALIGNMENT);
               empty.setBorder(new EmptyBorder(60, 0, 0, 0));
               feedbackListPanel.add(empty);
           } else {
               for (Appointment appt : mine) {
                   feedbackListPanel.add(buildFeedbackCard(appt));
                   feedbackListPanel.add(Box.createVerticalStrut(14));
               }
           }
           feedbackListPanel.revalidate();
           feedbackListPanel.repaint();
       }
   
   private void refreshAppointmentsList() {
       if (appointmentsListPanel == null) return;
       appointmentsListPanel.removeAll();
       User user = app.getLoggedInUserObj();
       if (user == null) { appointmentsListPanel.revalidate(); appointmentsListPanel.repaint(); return; }
       List<Appointment> all = appointmentService.getAll();
       List<Appointment> mine = new java.util.ArrayList<>();
       for (Appointment a : all) {
           if (a.getTechnicianEmail().equalsIgnoreCase(user.getUserId())) mine.add(a);
       }
       if (mine.isEmpty()) {
           JLabel empty = new JLabel("No appointments assigned to you yet.");
           empty.setFont(UIConstants.FONT_BODY);
           empty.setForeground(UIConstants.TEXT_MUTED);
           empty.setAlignmentX(Component.CENTER_ALIGNMENT);
           empty.setBorder(new EmptyBorder(60, 0, 0, 0));
           appointmentsListPanel.add(empty);
       } else {
           for (Appointment appt : mine) {
               appointmentsListPanel.add(buildAppointmentCard(appt));
               appointmentsListPanel.add(Box.createVerticalStrut(14));
           }
       }
       appointmentsListPanel.revalidate();
       appointmentsListPanel.repaint();
   }
   
   private JPanel buildAppointmentCard(Appointment appt) {
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
       card.setBorder(new EmptyBorder(20, 24, 20, 24));
       card.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
       card.setAlignmentX(Component.LEFT_ALIGNMENT);

       JPanel topRow = new JPanel(new BorderLayout());
       topRow.setOpaque(false);
       topRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
       JLabel idLabel = new JLabel(appt.getId() + "  ·  " + appt.getServiceType() + "  ·  " + appt.getDateTime() + "  ·  " + appt.getDurationHours() + "h");
       idLabel.setFont(UIConstants.FONT_SMALL_BOLD);
       idLabel.setForeground(UIConstants.TEXT_MUTED);
       JLabel statusBadge = new JLabel(appt.getStatus());
       statusBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
       switch (appt.getStatus()) {
           case "Completed":   statusBadge.setForeground(GREEN);  break;
           case "In Progress": statusBadge.setForeground(ORANGE); break;
           default:            statusBadge.setForeground(GREY);   break;
       }
       topRow.add(idLabel, BorderLayout.WEST);
       topRow.add(statusBadge, BorderLayout.EAST);
       card.add(topRow);
       card.add(Box.createVerticalStrut(8));

       JLabel custLabel = new JLabel("Customer: " + resolveName(appt.getCustomerEmail()) + "  (" + appt.getCustomerEmail() + ")");
       custLabel.setFont(UIConstants.FONT_BODY);
       custLabel.setForeground(UIConstants.TEXT_PRIMARY);
       custLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
       card.add(custLabel);
       card.add(Box.createVerticalStrut(10));

       String comment = appointmentService.getComment(appt.getId());
       if (comment != null && !comment.isBlank()) {
           JLabel commentTitle = new JLabel("Customer Comment:");
           commentTitle.setFont(UIConstants.FONT_SMALL_BOLD);
           commentTitle.setForeground(UIConstants.TEXT_MUTED);
           commentTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
           card.add(commentTitle);
           card.add(Box.createVerticalStrut(4));
           JTextArea commentArea = new JTextArea(comment);
           commentArea.setFont(UIConstants.FONT_BODY);
           commentArea.setForeground(UIConstants.TEXT_SECONDARY);
           commentArea.setBackground(new Color(245, 246, 250));
           commentArea.setEditable(false);
           commentArea.setLineWrap(true);
           commentArea.setWrapStyleWord(true);
           commentArea.setBorder(new EmptyBorder(8, 10, 8, 10));
           commentArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
           commentArea.setAlignmentX(Component.LEFT_ALIGNMENT);
           card.add(commentArea);
           card.add(Box.createVerticalStrut(10));
       }

       JSeparator sep = new JSeparator();
       sep.setForeground(UIConstants.BORDER_DEFAULT);
       sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
       card.add(sep);
       card.add(Box.createVerticalStrut(12));

       JLabel feedbackTitle = new JLabel("My Feedback:");
       feedbackTitle.setFont(UIConstants.FONT_SMALL_BOLD);
       feedbackTitle.setForeground(UIConstants.TEXT_MUTED);
       feedbackTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
       card.add(feedbackTitle);
       card.add(Box.createVerticalStrut(6));

       String existingFeedback = appointmentService.getFeedback(appt.getId());
       JTextArea feedbackArea = new JTextArea(existingFeedback != null ? existingFeedback : "");
       feedbackArea.setFont(UIConstants.FONT_BODY);
       feedbackArea.setForeground(UIConstants.TEXT_DARK);
       feedbackArea.setBackground(Color.WHITE);
       feedbackArea.setLineWrap(true);
       feedbackArea.setWrapStyleWord(true);
       feedbackArea.setBorder(BorderFactory.createCompoundBorder(
               BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
               new EmptyBorder(8, 10, 8, 10)));
       feedbackArea.setRows(3);
       feedbackArea.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
       feedbackArea.setAlignmentX(Component.LEFT_ALIGNMENT);
       if (appt.getStatus().equals("Completed")) { feedbackArea.setEditable(false); feedbackArea.setBackground(new Color(245, 246, 250)); }
       card.add(feedbackArea);
       card.add(Box.createVerticalStrut(12));

       JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
       btnRow.setOpaque(false);
       btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
       btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);

       if (!appt.getStatus().equals("Completed")) {
           JButton saveFeedbackBtn = actionButton("Save Feedback", new Color(80, 110, 230), Color.WHITE);
           saveFeedbackBtn.addActionListener(e -> {
               String fb = feedbackArea.getText().trim();
               if (fb.isEmpty()) { JOptionPane.showMessageDialog(app, "Please enter feedback before saving.", "Validation", JOptionPane.WARNING_MESSAGE); return; }
               if (appointmentService.saveFeedback(appt.getId(), fb)) {
                   JOptionPane.showMessageDialog(app, "Feedback saved!", "Success", JOptionPane.INFORMATION_MESSAGE);
               } else {
                   JOptionPane.showMessageDialog(app, "Failed to save feedback.", "Error", JOptionPane.ERROR_MESSAGE);
               }
           });
           JButton completeBtn = actionButton("Mark Completed", GREEN, Color.WHITE);
           completeBtn.addActionListener(e -> {
               int confirm = JOptionPane.showConfirmDialog(app, "Mark appointment " + appt.getId() + " as Completed?", "Confirm", JOptionPane.YES_NO_OPTION);
               if (confirm == JOptionPane.YES_OPTION) {
                   if (appointmentService.updateStatus(appt.getId(), "Completed")) {
                       JOptionPane.showMessageDialog(app, "Appointment " + appt.getId() + " marked Completed.", "Success", JOptionPane.INFORMATION_MESSAGE);
                       refreshAppointmentsList();
                   } else {
                       JOptionPane.showMessageDialog(app, "Failed to update status.", "Error", JOptionPane.ERROR_MESSAGE);
                   }
               }
           });
           btnRow.add(saveFeedbackBtn);
           btnRow.add(completeBtn);
       } else {
           JLabel doneLabel = new JLabel("\u2714 Completed");
           doneLabel.setFont(UIConstants.FONT_SMALL_BOLD);
           doneLabel.setForeground(GREEN);
           btnRow.add(doneLabel);
       }
       card.add(btnRow);
       return card;
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
       profileAvatarDisplay = new JLabel(AVATAR_ICONS[0]) {
           @Override protected void paintComponent(Graphics g) {
               Graphics2D g2 = (Graphics2D) g.create();
               g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               g2.setColor(AVATAR_COLORS[selectedAvatarIndex]);
               g2.fillOval(0, 0, 60, 60);
               g2.dispose();
               super.paintComponent(g);
           }
       };
       profileAvatarDisplay.setFont(new Font("SansSerif", Font.PLAIN, 28));
       profileAvatarDisplay.setForeground(Color.WHITE);
       profileAvatarDisplay.setHorizontalAlignment(SwingConstants.CENTER);
       profileAvatarDisplay.setVerticalAlignment(SwingConstants.CENTER);
       profileAvatarDisplay.setPreferredSize(new Dimension(60, 60));
       profileAvatarDisplay.setMaximumSize(new Dimension(60, 60));
       profileAvatarDisplay.setAlignmentX(Component.CENTER_ALIGNMENT);
       card.add(profileAvatarDisplay);
       card.add(Box.createVerticalStrut(12));
       avatarSelectionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
       avatarSelectionPanel.setOpaque(false);
       avatarSelectionPanel.setMaximumSize(new Dimension(460, 60));
       for (int i = 0; i < AVATAR_COLORS.length; i++) {
           final int idx = i;
           JLabel av = new JLabel(AVATAR_ICONS[i]) {
               @Override protected void paintComponent(Graphics g) {
                   Graphics2D g2 = (Graphics2D) g.create();
                   g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                   g2.setColor(AVATAR_COLORS[idx]);
                   g2.fillOval(2, 2, 42, 42);
                   g2.dispose();
                   super.paintComponent(g);
               }
           };
           av.setFont(new Font("SansSerif", Font.PLAIN, 20));
           av.setForeground(Color.WHITE);
           av.setHorizontalAlignment(SwingConstants.CENTER);
           av.setVerticalAlignment(SwingConstants.CENTER);
           av.setPreferredSize(new Dimension(46, 46));
           av.setCursor(new Cursor(Cursor.HAND_CURSOR));
           av.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
           av.addMouseListener(new MouseAdapter() {
               @Override public void mouseClicked(MouseEvent e) { selectedAvatarIndex = idx; updateAvatarSelection(); }
           });
           avatarSelectionPanel.add(av);
       }
       card.add(avatarSelectionPanel);
       card.add(Box.createVerticalStrut(6));
       JLabel avatarNote = new JLabel("This icon appears in the header when no photo is set.");
       avatarNote.setFont(UIConstants.FONT_SMALL);
       avatarNote.setForeground(UIConstants.TEXT_MUTED);
       avatarNote.setAlignmentX(Component.CENTER_ALIGNMENT);
       card.add(avatarNote);
       card.add(Box.createVerticalStrut(24));
       JSeparator midSep = new JSeparator();
       midSep.setForeground(UIConstants.BORDER_DEFAULT);
       midSep.setMaximumSize(new Dimension(380, 1));
       midSep.setAlignmentX(Component.CENTER_ALIGNMENT);
       card.add(midSep);
       card.add(Box.createVerticalStrut(22));
       card.add(UIFactory.createFieldLabel("Name")); card.add(Box.createVerticalStrut(6));
       profileNameField = UIFactory.createTextField("Enter your name"); card.add(profileNameField);
       card.add(Box.createVerticalStrut(16));
       card.add(UIFactory.createFieldLabel("Email")); card.add(Box.createVerticalStrut(6));
       profileEmailField = UIFactory.createTextField("Enter your email"); card.add(profileEmailField);
       card.add(Box.createVerticalStrut(16));
       card.add(UIFactory.createFieldLabel("Role")); card.add(Box.createVerticalStrut(6));
       profileRoleLabel = new JLabel("Technician");
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
               if (bannerImage != null) { g2.drawImage(bannerImage, 0, 0, getWidth(), getHeight(), null); }
               else { GradientPaint gp = new GradientPaint(0, 0, BANNER_BLUE, getWidth(), getHeight(), new Color(60, 90, 210)); g2.setPaint(gp); g2.fillRect(0, 0, getWidth(), getHeight()); }
               if (bannerHovered[0]) { g2.setColor(new Color(0, 0, 0, 110)); g2.fillRect(0, 0, getWidth(), getHeight()); int cx = getWidth() / 2, cy = getHeight() / 2 - 10; drawCameraIcon(g2, cx, cy, 28, Color.WHITE); g2.setColor(Color.WHITE); g2.setFont(new Font("SansSerif", Font.BOLD, 13)); FontMetrics fm = g2.getFontMetrics(); String msg = "Click to change"; g2.drawString(msg, cx - fm.stringWidth(msg) / 2, cy + 44); }
               g2.dispose();
           }
       };
       profileBanner.setOpaque(false); profileBanner.setLayout(null); profileBanner.setCursor(new Cursor(Cursor.HAND_CURSOR));
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
               if (profileImage != null) { int iw = profileImage.getWidth(), ih = profileImage.getHeight(); int crop = Math.min(iw, ih); int cx = (iw - crop) / 2, cy = (ih - crop) / 2; g2.setClip(new Ellipse2D.Float(0, 0, size, size)); g2.drawImage(profileImage, 0, 0, size, size, cx, cy, cx + crop, cy + crop, null); g2.setClip(null); }
               else { drawDefaultAvatar(g2, size); }
               if (avatarHovered[0]) { g2.setClip(new Ellipse2D.Float(0, 0, size, size)); g2.setColor(new Color(0, 0, 0, 110)); g2.fillOval(0, 0, size, size); g2.setClip(null); drawCameraIcon(g2, size / 2, size / 2, 20, Color.WHITE); }
               g2.setColor(Color.WHITE); g2.setStroke(new BasicStroke(4)); g2.drawOval(2, 2, size - 4, size - 4);
               g2.dispose();
           }
       };
       profilePicLabel.setPreferredSize(new Dimension(110, 110)); profilePicLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
       profilePicLabel.addMouseListener(new MouseAdapter() {
           @Override public void mouseEntered(MouseEvent e) { avatarHovered[0] = true;  profilePicLabel.repaint(); }
           @Override public void mouseExited (MouseEvent e) { avatarHovered[0] = false; profilePicLabel.repaint(); }
           @Override public void mouseClicked(MouseEvent e) { chooseProfileImage(); }
       });
       hero.addComponentListener(new java.awt.event.ComponentAdapter() {
           @Override public void componentResized(java.awt.event.ComponentEvent e) { profileBanner.setBounds(0, 0, hero.getWidth(), 170); profilePicLabel.setBounds(30, 90, 110, 110); }
       });
       profileBanner.setBounds(0, 0, 800, 170); profilePicLabel.setBounds(30, 90, 110, 110);
       hero.add(profileBanner); hero.add(profilePicLabel);
       hero.setComponentZOrder(profilePicLabel, 0); hero.setComponentZOrder(profileBanner, 1);
       return hero;
   }
   
   private JPanel buildProfileArea() {
       JPanel profileArea = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 12));
       profileArea.setBackground(UIConstants.BG_HEADER);
       avatarLabel = new JLabel(AVATAR_ICONS[selectedAvatarIndex]) {
           @Override protected void paintComponent(Graphics g) {
               Graphics2D g2 = (Graphics2D) g.create();
               g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
               if (profileImage != null) { int iw = profileImage.getWidth(), ih = profileImage.getHeight(); int crop = Math.min(iw, ih); int cx = (iw - crop) / 2, cy = (ih - crop) / 2; g2.setClip(new Ellipse2D.Float(0, 0, 38, 38)); g2.drawImage(profileImage, 0, 0, 38, 38, cx, cy, cx + crop, cy + crop, null); g2.setClip(null); }
               else { g2.setColor(AVATAR_COLORS[selectedAvatarIndex]); g2.fillOval(0, 0, 38, 38); g2.dispose(); super.paintComponent(g); return; }
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
       viewProfile.addActionListener(e -> { activeNav = "Profile"; headerTitle.setText("Profile"); contentLayout.show(contentPanel, "Profile"); refreshProfileFields(); });
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
   
//   Main page builders
   
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
               if (name.equals("My Appointments")) refreshAppointmentsList();
               if (name.equals("My Feedbacks")) refreshFeedbackList();
           });
           sidebar.add(btns[i]); sidebar.add(Box.createVerticalStrut(2));
       }
       sidebar.add(Box.createVerticalGlue());
       return sidebar;
   }

   private JPanel buildDashboardPage() {
       JPanel page = new JPanel(new BorderLayout());
       page.setBackground(UIConstants.BG_CONTENT);
       page.setBorder(new EmptyBorder(40, 40, 40, 40));
       JLabel welcome = new JLabel("Welcome back, Technician");
       welcome.setFont(new Font("SansSerif", Font.BOLD, 28));
       welcome.setForeground(UIConstants.TEXT_PRIMARY);
       JLabel sub = new JLabel("Check your assigned appointments, update status, and leave feedback.");
       sub.setFont(UIConstants.FONT_BODY); sub.setForeground(UIConstants.TEXT_MUTED);
       sub.setBorder(new EmptyBorder(8, 0, 0, 0));
       JPanel top = new JPanel();
       top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
       top.setBackground(UIConstants.BG_CONTENT);
       top.add(welcome); top.add(sub);
       page.add(top, BorderLayout.NORTH);
       JPanel cards = new JPanel(new GridLayout(1, 3, 16, 0));
       cards.setBackground(UIConstants.BG_CONTENT);
       cards.setBorder(new EmptyBorder(30, 0, 0, 0));
       cards.add(buildSummaryCard("Pending",     GREY,   "\u23F3"));
       cards.add(buildSummaryCard("In Progress", ORANGE, "\u26A1"));
       cards.add(buildSummaryCard("Completed",   GREEN,  "\u2714"));
       page.add(cards, BorderLayout.CENTER);
       return page;
   }

   private JPanel buildAppointmentsPage() {
       JPanel page = new JPanel(new BorderLayout());
       page.setBackground(UIConstants.BG_CONTENT);
       page.setBorder(new EmptyBorder(30, 36, 30, 36));
       JLabel title = new JLabel("My Appointments");
       title.setFont(new Font("SansSerif", Font.BOLD, 22));
       title.setForeground(UIConstants.TEXT_PRIMARY);
       title.setBorder(new EmptyBorder(0, 0, 20, 0));
       page.add(title, BorderLayout.NORTH);
       appointmentsListPanel = new JPanel();
       appointmentsListPanel.setLayout(new BoxLayout(appointmentsListPanel, BoxLayout.Y_AXIS));
       appointmentsListPanel.setBackground(UIConstants.BG_CONTENT);
       JScrollPane scroll = new JScrollPane(appointmentsListPanel);
       scroll.setBorder(null);
       scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
       scroll.getVerticalScrollBar().setUnitIncrement(16);
       scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
       page.add(scroll, BorderLayout.CENTER);
       return page;
   }

   private JPanel buildFeedbackPage() {
       JPanel page = new JPanel(new BorderLayout());
       page.setBackground(UIConstants.BG_CONTENT);
       page.setBorder(new EmptyBorder(30, 36, 30, 36));

       JLabel title = new JLabel("My Feedbacks");
       title.setFont(new Font("SansSerif", Font.BOLD, 22));
       title.setForeground(UIConstants.TEXT_PRIMARY);

       JLabel subtitle = new JLabel("Write or update your feedback for each assigned appointment.");
       subtitle.setFont(UIConstants.FONT_BODY);
       subtitle.setForeground(UIConstants.TEXT_MUTED);
       subtitle.setBorder(new EmptyBorder(4, 0, 20, 0));

       JPanel titlePanel = new JPanel();
       titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
       titlePanel.setBackground(UIConstants.BG_CONTENT);
       titlePanel.add(title); titlePanel.add(subtitle);
       page.add(titlePanel, BorderLayout.NORTH);

       feedbackListPanel = new JPanel();
       feedbackListPanel.setLayout(new BoxLayout(feedbackListPanel, BoxLayout.Y_AXIS));
       feedbackListPanel.setBackground(UIConstants.BG_CONTENT);

       JScrollPane scroll = new JScrollPane(feedbackListPanel);
       scroll.setBorder(null);
       scroll.getViewport().setBackground(UIConstants.BG_CONTENT);
       scroll.getVerticalScrollBar().setUnitIncrement(16);
       scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
       page.add(scroll, BorderLayout.CENTER);
       return page;
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
   
    
//    Constuctor
    public TechnicianDashboard(AppFrame app) {
    	this.app = app;
    	setLayout(new BorderLayout());
        add(buildSidebar(), BorderLayout.WEST);
 
        JPanel rightSide = new JPanel(new BorderLayout());
        rightSide.setBackground(UIConstants.BG_CONTENT);
        rightSide.add(buildHeader(), BorderLayout.NORTH);
        
        contentLayout = new CardLayout();
        contentPanel = new JPanel(contentLayout);
        contentPanel.setBackground(UIConstants.BG_CONTENT);
        
        contentPanel.add(buildDashboardPage(), "Dashboard");
        contentPanel.add(buildAppointmentsPage(), "My Appointment");
        contentPanel.add(buildFeedbackPage(),"My Feedbacks");
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
    	if(name==null || name.isEmpty())name = "Technician";
    	
    	if(profileLabel != null) profileLabel.setText(name);
    	
    	User user = app.getLoggedInUserObj();
    	
    	if(user != null) {
    		selectedAvatarIndex = user.getProfilePicture();
    		profileImage = profilePicStorage.loadImage(user.getEmail());
            bannerImage  = backgroundStorage.loadImage(user.getEmail());
            if (profileBanner   != null) profileBanner.repaint();
            if (profilePicLabel != null) profilePicLabel.repaint();
        }
        if (avatarLabel != null) avatarLabel.repaint();
        refreshAppointmentsList();
        refreshFeedbackList();
    	}
    }
    	

