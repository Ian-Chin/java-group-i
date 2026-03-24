package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class LoginPage extends JPanel {

    private final AppFrame app;
    private final AccountService accountService;
    private JButton loginBtn; // keylistener: enter/login btn

    public LoginPage(AppFrame app) {
        this.app = app;
        this.accountService = app.getAccountService();
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_PAGE);

        // Back button at top-left
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 15));
        topBar.setBackground(UIConstants.BG_PAGE);
        JButton backBtn = UIFactory.createBackButton();
        backBtn.addActionListener(e -> app.showPage(PageName.ONBOARDING));
        topBar.add(backBtn);
        add(topBar, BorderLayout.NORTH);

        // Main card
        JPanel card = UIFactory.createCard();
        card.setBorder(BorderFactory.createCompoundBorder(
                new DropShadowBorder(),
                new EmptyBorder(40, 50, 40, 50)
        ));

        // Logo
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(80, 80, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logoLabel);
        card.add(Box.createVerticalStrut(20));

        // Title
        JLabel titleLabel = new JLabel("Login");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UIConstants.FONT_HEADING_3);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Welcome back to APU ASC");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(UIConstants.FONT_BODY);
        subtitleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(30));

        // Email field
        card.add(UIFactory.createFieldLabel("Email"));
        card.add(Box.createVerticalStrut(6));
        JTextField emailField = UIFactory.createTextField("Enter your email");
        card.add(emailField);
        card.add(Box.createVerticalStrut(16));

        // Password field
        card.add(UIFactory.createFieldLabel("Password"));
        card.add(Box.createVerticalStrut(6));
        JPasswordField passwordField = UIFactory.createPasswordField();
        card.add(UIFactory.createPasswordFieldPanel(passwordField));
        card.add(Box.createVerticalStrut(28));

        // Login button
        loginBtn = UIFactory.createPrimaryButton("Login");
        loginBtn.addActionListener(e -> handleLogin(emailField, passwordField));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(16));

        // Create account link
        JLabel registerLink = UIFactory.createLink("Don't have an account? Create one");
        registerLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                app.showPage(PageName.CREATE_ACCOUNT);
            }
        });
        card.add(registerLink);

        // Wrapper to center the card
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIConstants.BG_PAGE);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);
    }
	    @Override
	    public void addNotify() {
	        super.addNotify();
	        SwingUtilities.getRootPane(loginBtn).setDefaultButton(loginBtn);
	    }

    private void handleLogin(JTextField emailField, JPasswordField passwordField) {
        String email = UIFactory.getFieldValue(emailField, "Enter your email");
        String password = new String(passwordField.getPassword()).trim();

        if (email.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(app, "Please fill in all fields.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        User user = accountService.authenticate(email, password);
        if (user != null) {
            // Clear fields and redirect to dashboard
            emailField.setText("Enter your email");
            emailField.setForeground(Color.GRAY);
            passwordField.setText("");
            app.setLoggedInUser(user.getName());
            app.setLoggedInUserObj(user);
            
            // Redirect based on role
            if(user.getRole().equals("customer")) {
            	app.showPage(PageName.CUSTOMER_DASHBOARD);
            }
            else if(user.getRole().equals("staff")) {
            	app.showPage(PageName.COUNTERSTAFF_DASHBOARD);
            }
            else {
            	app.showPage(PageName.DASHBOARD);
            }
        } else {
            JOptionPane.showMessageDialog(app, "Invalid email or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
