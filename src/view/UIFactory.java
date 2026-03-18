package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class UIFactory {

    private UIFactory() {} // prevent instantiation

    // ─── Card panel (white rounded rectangle) ───────────────────

    public static JPanel createCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        return card;
    }

    // ─── Primary button (filled blue) ───────────────────────────

    public static JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovering = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? UIConstants.PRIMARY_HOVER : UIConstants.PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(UIConstants.BUTTON_SIZE);
        btn.setPreferredSize(UIConstants.BUTTON_SIZE);
        btn.setFont(UIConstants.FONT_BODY_BOLD);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── Outline button (white with border) ─────────────────────

    public static JButton createOutlineButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hovering = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? new Color(245, 245, 250) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(UIConstants.BORDER_OUTLINE);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(UIConstants.BUTTON_SIZE);
        btn.setPreferredSize(UIConstants.BUTTON_SIZE);
        btn.setFont(UIConstants.FONT_BODY);
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── Back button ────────────────────────────────────────────

    public static JButton createBackButton() {
        JButton btn = new JButton("\u2190  Back") {
            private boolean hovering = false;

            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) { hovering = true; repaint(); }
                    public void mouseExited(MouseEvent e)  { hovering = false; repaint(); }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovering ? new Color(235, 235, 240) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(200, 200, 200));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(UIConstants.FONT_SMALL);
        btn.setForeground(UIConstants.TEXT_DARK);
        btn.setPreferredSize(new Dimension(100, 35));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─── Field label ────────────────────────────────────────────

    public static JLabel createFieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(UIConstants.FONT_SMALL_BOLD);
        label.setForeground(UIConstants.TEXT_DARK);
        label.setMaximumSize(new Dimension(380, 20));
        return label;
    }

    // ─── Text field with placeholder ────────────────────────────

    public static JTextField createTextField(String placeholder) {
        JTextField field = new JTextField();
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(UIConstants.FIELD_SIZE);
        field.setPreferredSize(UIConstants.FIELD_SIZE);
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(createFieldBorder(false));
        field.setForeground(Color.GRAY);
        field.setText(placeholder);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                }
                field.setBorder(createFieldBorder(true));
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                }
                field.setBorder(createFieldBorder(false));
            }
        });
        return field;
    }

    // ─── Password field ─────────────────────────────────────────

    public static JPasswordField createPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setAlignmentX(Component.CENTER_ALIGNMENT);
        field.setMaximumSize(UIConstants.FIELD_SIZE);
        field.setPreferredSize(UIConstants.FIELD_SIZE);
        field.setFont(UIConstants.FONT_BODY);
        field.setBorder(createFieldBorder(false));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(createFieldBorder(true));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(createFieldBorder(false));
            }
        });
        return field;
    }

    // ─── Clickable link label ───────────────────────────────────

    public static JLabel createLink(String text) {
        JLabel link = new JLabel(text);
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        link.setFont(UIConstants.FONT_SMALL);
        link.setForeground(UIConstants.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                link.setText("<html><u>" + text + "</u></html>");
            }
            public void mouseExited(MouseEvent e) {
                link.setText(text);
            }
        });
        return link;
    }

    // ─── Helper: get real text from a placeholder field ─────────

    public static String getFieldValue(JTextField field, String placeholder) {
        String text = field.getText().trim();
        return text.equals(placeholder) ? "" : text;
    }

    // ─── Helper: create field border ────────────────────────────

    private static javax.swing.border.Border createFieldBorder(boolean focused) {
        Color borderColor = focused ? UIConstants.PRIMARY : UIConstants.BORDER_DEFAULT;
        int thickness = focused ? 2 : 1;
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, thickness),
                new EmptyBorder(8, 14, 8, 14)
        );
    }
}
