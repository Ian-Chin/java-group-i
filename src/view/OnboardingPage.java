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
        card.setBorder(new EmptyBorder(20, 60, 50, 60));

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
        card.add(Box.createVerticalStrut(16));

        // Features bullet points
        JLabel featuresLabel = new JLabel(
                "<html><div style='text-align:center;'>"
                + "\u2022 Book and manage appointments<br>"
                + "\u2022 Track service history and status<br>"
                + "\u2022 Secure payment collection<br>"
                + "\u2022 Customer and vehicle management"
                + "</div></html>"
        );
        featuresLabel.setHorizontalAlignment(SwingConstants.CENTER);
        featuresLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        featuresLabel.setFont(UIConstants.FONT_SMALL);
        featuresLabel.setForeground(UIConstants.TEXT_MUTED);
        card.add(featuresLabel);
        card.add(Box.createVerticalStrut(30));

        // "Create an Account" button
        JButton createBtn = UIFactory.createPrimaryButton("Create an Account");
        createBtn.addActionListener(e -> app.showPage(PageName.CREATE_ACCOUNT));
        card.add(createBtn);
        card.add(Box.createVerticalStrut(12));

        // "Login" button
        JButton loginBtn = UIFactory.createOutlineButton("Login");
        loginBtn.addActionListener(e -> app.showPage(PageName.LOGIN));
        card.add(loginBtn);

        // Wrap card with pop-out logo
        add(UIFactory.createCardWithLogo(card, getClass()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        UIFactory.paintPageGradientBackground(g, getWidth(), getHeight());
    }
}
