package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class OnboardingPage extends JPanel {

    public OnboardingPage(AppFrame app) {
        setLayout(new GridBagLayout());
        setBackground(UIConstants.BG_PAGE);

        // Main card
        JPanel card = UIFactory.createCard();
        card.setBorder(BorderFactory.createCompoundBorder(
                new DropShadowBorder(),
                new EmptyBorder(50, 60, 50, 60)
        ));

        // Logo
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(120, 120, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(logoLabel);
        card.add(Box.createVerticalStrut(30));

        // Title
        JLabel titleLabel = new JLabel("Welcome to APU ASC!");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(UIConstants.FONT_HEADING_2);
        titleLabel.setForeground(UIConstants.TEXT_PRIMARY);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(10));

        // Subtitle
        JLabel subtitleLabel = new JLabel("APU Automotive Service Centre");
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitleLabel.setFont(UIConstants.FONT_BODY);
        subtitleLabel.setForeground(UIConstants.TEXT_SECONDARY);
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(40));

        // "Create an Account" button
        JButton createBtn = UIFactory.createPrimaryButton("Create an Account");
        createBtn.addActionListener(e -> app.showPage(PageName.CREATE_ACCOUNT));
        card.add(createBtn);
        card.add(Box.createVerticalStrut(12));

        // "Login" button
        JButton loginBtn = UIFactory.createOutlineButton("Login");
        loginBtn.addActionListener(e -> app.showPage(PageName.LOGIN));
        card.add(loginBtn);

        add(card);
    }
}
