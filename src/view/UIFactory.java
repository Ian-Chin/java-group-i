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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                g2.setColor(new Color(210, 210, 220, 120));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 24, 24);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        return card;
    }

    // ─── Card with logo popping out from top ───────────────────

    private static final int LOGO_SIZE = 250;
    private static final int LOGO_OVERLAP = 180;

    public static JPanel createCardWithLogo(JPanel card, Class<?> resourceClass) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        ImageIcon originalIcon = new ImageIcon(resourceClass.getResource("/Image/apu-logo.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(LOGO_SIZE, LOGO_SIZE, Image.SCALE_SMOOTH);
        JLabel logoLabel = new JLabel(new ImageIcon(scaledImage));
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        logoLabel.setVerticalAlignment(SwingConstants.CENTER);
        logoLabel.setPreferredSize(new Dimension(LOGO_SIZE, LOGO_SIZE));
        logoLabel.setMaximumSize(new Dimension(LOGO_SIZE, LOGO_SIZE));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add top padding to card so content sits below the logo overlap
        EmptyBorder existing = null;
        if (card.getBorder() instanceof EmptyBorder) {
            existing = (EmptyBorder) card.getBorder();
        }
        int topPad = LOGO_OVERLAP + 10;
        if (existing != null) {
            Insets i = existing.getBorderInsets();
            card.setBorder(new EmptyBorder(topPad + i.top, i.left, i.bottom, i.right));
        } else {
            card.setBorder(new EmptyBorder(topPad, 50, 40, 50));
        }

        wrapper.add(logoLabel);
        wrapper.add(Box.createVerticalStrut(-LOGO_OVERLAP));
        wrapper.add(card);

        return wrapper;
    }

    // ─── Stack the APU logo above an existing card ─────────────

    public static JPanel addLogoAbove(JComponent card, Class<?> resourceClass, int logoSize) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        ImageIcon icon = new ImageIcon(resourceClass.getResource("/Image/apu-logo.png"));
        Image scaled = icon.getImage().getScaledInstance(logoSize, logoSize, Image.SCALE_SMOOTH);
        JLabel logo = new JLabel(new ImageIcon(scaled));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setHorizontalAlignment(SwingConstants.CENTER);
        logo.setPreferredSize(new Dimension(logoSize, logoSize));
        logo.setMaximumSize(new Dimension(logoSize, logoSize));

        card.setAlignmentX(Component.CENTER_ALIGNMENT);

        wrapper.add(logo);
        wrapper.add(Box.createVerticalStrut(12));
        wrapper.add(card);
        return wrapper;
    }

    // ─── Form card with image embedded inside one half ─────────
    //
    // Returns a single white rounded card whose left or right half is
    // filled by the given image (clipped to match the card's rounded
    // corners on that side) and whose other half hosts the supplied
    // vertical form column.

    public static JPanel createSplitFormCard(JPanel formColumn, String imageResource,
                                             Class<?> resourceClass, boolean imageOnLeft,
                                             int imageWidth, int cardHeight) {
        ImageIcon icon = new ImageIcon(resourceClass.getResource(imageResource));
        final Image img = icon.getImage();
        final int ARC = 24;

        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                int w = getWidth(), h = getHeight();
                java.awt.geom.Area clip = new java.awt.geom.Area(new Rectangle(0, 0, w, h));
                java.awt.geom.Area rounded = new java.awt.geom.Area(
                        new java.awt.geom.RoundRectangle2D.Float(
                                imageOnLeft ? 0 : -ARC, 0,
                                w + ARC, h, ARC * 2, ARC * 2));
                clip.intersect(rounded);
                g2.setClip(clip);

                int iw = img.getWidth(null);
                int ih = img.getHeight(null);
                if (iw > 0 && ih > 0) {
                    double scale = Math.max((double) w / iw, (double) h / ih);
                    int drawW = (int) Math.ceil(iw * scale);
                    int drawH = (int) Math.ceil(ih * scale);
                    int x = (w - drawW) / 2;
                    int y = (h - drawH) / 2;
                    g2.drawImage(img, x, y, drawW, drawH, null);
                }
                g2.dispose();
            }
        };
        imagePanel.setOpaque(false);
        Dimension imgDim = new Dimension(imageWidth, cardHeight);
        imagePanel.setPreferredSize(imgDim);
        imagePanel.setMinimumSize(imgDim);
        imagePanel.setMaximumSize(imgDim);

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
                g2.setColor(new Color(210, 210, 220, 120));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
                g2.dispose();
            }
        };
        card.setOpaque(false);

        formColumn.setOpaque(false);

        if (imageOnLeft) {
            card.add(imagePanel, BorderLayout.WEST);
            card.add(formColumn, BorderLayout.CENTER);
        } else {
            card.add(formColumn, BorderLayout.CENTER);
            card.add(imagePanel, BorderLayout.EAST);
        }
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

    // ─── Password field wrapped with show/hide eye toggle ──────

    public static JPanel createPasswordFieldPanel(JPasswordField field) {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = field.hasFocus();
                // Fill white background
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                // Draw border
                Color borderColor = focused ? UIConstants.PRIMARY : UIConstants.BORDER_DEFAULT;
                float thickness = focused ? 2f : 1.2f;
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(thickness));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
                g2.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.setMaximumSize(UIConstants.FIELD_SIZE);
        panel.setPreferredSize(UIConstants.FIELD_SIZE);

        // Strip the border from the field itself — the panel draws it
        field.setBorder(new EmptyBorder(8, 14, 8, 0));
        field.setOpaque(false);
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // Eye toggle label
        JLabel eye = new JLabel("\u25CE");
        eye.setFont(new Font("SansSerif", Font.PLAIN, 18));
        eye.setForeground(UIConstants.TEXT_MUTED);
        eye.setOpaque(false);
        eye.setCursor(new Cursor(Cursor.HAND_CURSOR));
        eye.setToolTipText("Show / hide password");
        eye.setBorder(new EmptyBorder(0, 6, 0, 12));
        eye.setPreferredSize(new Dimension(36, 44));
        eye.setHorizontalAlignment(SwingConstants.CENTER);

        final boolean[] visible = {false};
        eye.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                visible[0] = !visible[0];
                field.setEchoChar(visible[0] ? (char) 0 : '\u2022');
                eye.setText(visible[0] ? "\u25C9" : "\u25CE");
                eye.setForeground(visible[0] ? UIConstants.PRIMARY : UIConstants.TEXT_MUTED);
            }
        });

        // Repaint panel border on focus change
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { panel.repaint(); }
            @Override public void focusLost(FocusEvent e)   { panel.repaint(); }
        });

        panel.add(field, BorderLayout.CENTER);
        panel.add(eye, BorderLayout.EAST);
        return panel;
    }

    // ─── Clickable link label ───────────────────────────────────

    public static JLabel createLink(String text) {
        Font baseFont = UIConstants.FONT_SMALL;
        @SuppressWarnings("unchecked")
        java.util.Map<java.awt.font.TextAttribute, Object> attrs =
                (java.util.Map<java.awt.font.TextAttribute, Object>) baseFont.getAttributes();
        attrs.put(java.awt.font.TextAttribute.UNDERLINE, java.awt.font.TextAttribute.UNDERLINE_ON);
        Font underlineFont = baseFont.deriveFont(attrs);

        JLabel link = new JLabel(text);
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        link.setFont(baseFont);
        link.setForeground(UIConstants.PRIMARY);
        link.setCursor(new Cursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                link.setFont(underlineFont);
            }
            public void mouseExited(MouseEvent e) {
                link.setFont(baseFont);
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

    // ─── Shared gradient background for auth pages ───────────────

    public static void paintPageGradientBackground(Graphics g, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Large gradient circle anchored at bottom center
        int radius = (int) (Math.max(w, h) * 0.9);
        int cx = w / 2 - radius / 2;
        int cy = h - radius / 3;
        RadialGradientPaint gradient = new RadialGradientPaint(
                new Point(w / 2, h),
                radius,
                new float[]{0f, 0.5f, 1f},
                new Color[]{
                        new Color(80, 110, 230, 40),
                        new Color(80, 110, 230, 15),
                        new Color(80, 110, 230, 0)
                }
        );
        g2.setPaint(gradient);
        g2.fillOval(cx, cy, radius, radius);

        // Smaller accent circle offset to the right
        int r2 = (int) (Math.max(w, h) * 0.45);
        RadialGradientPaint gradient2 = new RadialGradientPaint(
                new Point(w - w / 4, h),
                r2,
                new float[]{0f, 0.6f, 1f},
                new Color[]{
                        new Color(160, 80, 230, 25),
                        new Color(160, 80, 230, 8),
                        new Color(160, 80, 230, 0)
                }
        );
        g2.setPaint(gradient2);
        g2.fillOval(w - w / 4 - r2 / 2, h - r2 / 3, r2, r2);

        g2.dispose();
    }

    // ─── Logo label for inline use inside a form column ─────────

    public static JLabel createInlineLogo(Class<?> resourceClass, int size) {
        ImageIcon icon = new ImageIcon(resourceClass.getResource("/Image/apu-logo.png"));
        Image scaled = icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
        JLabel label = new JLabel(new ImageIcon(scaled));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setPreferredSize(new Dimension(size, size));
        label.setMaximumSize(new Dimension(size, size));
        return label;
    }
}
