package view;

import model.AccountService;
import model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class CreateAccountPage extends JPanel {

    private final AppFrame app;
    private final AccountService accountService;

    public CreateAccountPage(AppFrame app) {
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
        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UIConstants.FONT_HEADING_3);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(6));

        // Subtitle
        JLabel subtitleLabel = new JLabel("Join APU Automotive Service Centre");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(UIConstants.FONT_BODY);
        subtitleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(30));

        // Name field
        card.add(UIFactory.createFieldLabel("Name"));
        card.add(Box.createVerticalStrut(6));
        JTextField nameField = UIFactory.createTextField("Enter your full name");
        card.add(nameField);
        card.add(Box.createVerticalStrut(16));

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
        card.add(passwordField);
        card.add(Box.createVerticalStrut(28));

        // Register button
        JButton registerBtn = UIFactory.createPrimaryButton("Register");
        registerBtn.addActionListener(e -> handleRegister(nameField, emailField, passwordField));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(16));

        // Login link
        JLabel loginLink = UIFactory.createLink("Already have an account? Login");
        loginLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                app.showPage(PageName.LOGIN);
            }
        });
        card.add(loginLink);

        // Wrapper to center the card
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(UIConstants.BG_PAGE);
        wrapper.add(card);
        add(wrapper, BorderLayout.CENTER);
    }

    private void handleRegister(JTextField nameField, JTextField emailField, JPasswordField passwordField) {
        String name = UIFactory.getFieldValue(nameField, "Enter your full name");
        String email = UIFactory.getFieldValue(emailField, "Enter your email");
        String password = new String(passwordField.getPassword()).trim();

        // Validate name
        if (!name.matches("[a-zA-Z ]{2,50}")) {
            showError("Name must be 2-50 characters and contain only letters.");
            return;
        }

        // Validate email
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showError("Please enter a valid email address.");
            return;
        }

        // Validate password
        if (password.length() < 8 || !password.matches(".*[a-zA-Z].*") || !password.matches(".*\\d.*")) {
            showError("Password must be at least 8 characters with letters and numbers.");
            return;
        }

        // Check duplicate email
        if (accountService.emailExists(email)) {
            showError("An account with this email already exists.");
            return;
        }

        // Register
        User newUser = new User(name, email, password, "customer");
        if (accountService.register(newUser)) {
            JOptionPane.showMessageDialog(app, "Account created successfully!", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearFields(nameField, emailField, passwordField);
            app.showPage(PageName.ONBOARDING);
        } else {
            showError("Failed to save account. Please try again.");
        }
    }

    private void clearFields(JTextField nameField, JTextField emailField, JPasswordField passwordField) {
        nameField.setText("Enter your full name");
        nameField.setForeground(Color.GRAY);
        emailField.setText("Enter your email");
        emailField.setForeground(Color.GRAY);
        passwordField.setText("");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(app, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
