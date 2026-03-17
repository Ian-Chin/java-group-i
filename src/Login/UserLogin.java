package Login;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

public class UserLogin extends JFrame {

    public UserLogin() {
        setTitle("Onboarding");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(new Color(240, 240, 245));

        // Main panel
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(40, 50, 40, 50));

        
        card.setBorder(BorderFactory.createCompoundBorder(
            new DropShadowBorder(),
            new EmptyBorder(40, 50, 40, 50)
        ));

        // logo: apu logo
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/Image/apu-logo.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(140, 140, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Spacer
        card.add(logoLabel);
        card.add(Box.createVerticalStrut(50));

        // Onboarding title
        JLabel titleLabel = new JLabel("Welcome to APU ASC!");
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 30));
        titleLabel.setForeground(new Color(30, 30, 30));

        card.add(titleLabel);
        card.add(Box.createVerticalStrut(30));

        // Feature text labels
        String[] features = {"APU Automotive Service Centre"};
        for (String feature : features) {
            JLabel featureLabel = new JLabel(feature);
            featureLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            featureLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
            featureLabel.setForeground(new Color(100, 100, 100));
            card.add(featureLabel);
            card.add(Box.createVerticalStrut(8));
        }

        card.add(Box.createVerticalStrut(30));

        // "Create an Account" button (filled blue)
        JButton createBtn = new JButton("Create an Account") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(100, 130, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        createBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        createBtn.setMaximumSize(new Dimension(380, 45));
        createBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        createBtn.setForeground(Color.WHITE);
        createBtn.setContentAreaFilled(false);
        createBtn.setBorderPainted(false);
        createBtn.setFocusPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Navigate to registration!"));

        card.add(createBtn);
        card.add(Box.createVerticalStrut(12));

        // "Login" button (outlined)
        JButton loginBtn = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                g2.setColor(new Color(180, 180, 180));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 40, 40);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(380, 45));
        loginBtn.setFont(new Font("SansSerif", Font.PLAIN, 14));
        loginBtn.setForeground(new Color(50, 50, 50));
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Navigate to login!")); // Button redirecting

        card.add(loginBtn);

        // Wrapper to center the card
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(new Color(240, 240, 245));
        wrapper.add(card);

        add(wrapper);
        setVisible(true);
    }

    // Simple drop shadow border
    static class DropShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 5; i > 0; i--) {
                g2.setColor(new Color(0, 0, 0, 10));
                g2.fillRoundRect(x + i, y + i, width - i * 2, height - i * 2, 30, 30);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(5, 5, 5, 5);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(UserLogin::new);
    }
}