package view;

import javax.swing.border.AbstractBorder;
import java.awt.*;

public class DropShadowBorder extends AbstractBorder {

    private static final int SHADOW_SIZE = 8;
    private static final Color SHADOW_COLOR = new Color(0, 0, 0, 6);

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (int i = SHADOW_SIZE; i > 0; i--) {
            g2.setColor(SHADOW_COLOR);
            g2.fillRoundRect(x + i, y + i, width - i * 2, height - i * 2, 30, 30);
        }
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE);
    }
}
