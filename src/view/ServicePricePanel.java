package view;

import model.PriceConfig;
import view.UIConstants;
import view.UIFactory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Lets the admin view and update the normal / major service prices.
 *
 * OOP highlights:
 *  - Encapsulation : PriceConfig hides file I/O behind setters/save()
 *  - Inheritance   : extends JPanel
 *  - Abstraction   : UIFactory hides widget construction details
 */
public class ServicePricePanel extends JPanel {

    private final PriceConfig priceConfig;

    private JTextField normalField;
    private JTextField majorField;

    public ServicePricePanel() {
        this.priceConfig = new PriceConfig();
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(40, 40, 40, 40));

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UIConstants.BG_CONTENT);
        center.add(buildCard());

        add(center, BorderLayout.CENTER);
    }

    private JPanel buildCard() {
        JPanel card = UIFactory.createCard();
        card.setBorder(new EmptyBorder(40, 50, 40, 50));
        card.setPreferredSize(new Dimension(460, 380));

        // Title
        JLabel title = new JLabel("Service Pricing");
        title.setFont(UIConstants.FONT_HEADING_4);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(title);

        JLabel subtitle = new JLabel("Set the prices charged per service type");
        subtitle.setFont(UIConstants.FONT_SMALL);
        subtitle.setForeground(UIConstants.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(subtitle);
        card.add(Box.createVerticalStrut(30));

        // Normal service price
        card.add(buildPriceRow("\u25CB  Normal Service  (1 hour)", String.valueOf(priceConfig.getNormalPrice()), false));
        card.add(Box.createVerticalStrut(6));
        normalField = buildPriceField(String.valueOf(priceConfig.getNormalPrice()));
        card.add(normalField);
        card.add(Box.createVerticalStrut(20));

        // Major service price
        card.add(buildPriceRow("\u25CF  Major Service   (3 hours)", String.valueOf(priceConfig.getMajorPrice()), false));
        card.add(Box.createVerticalStrut(6));
        majorField = buildPriceField(String.valueOf(priceConfig.getMajorPrice()));
        card.add(majorField);
        card.add(Box.createVerticalStrut(30));

        // Save button
        JButton saveBtn = UIFactory.createPrimaryButton("Save Prices");
        saveBtn.addActionListener(e -> handleSave());
        card.add(saveBtn);

        return card;
    }

    private JLabel buildPriceRow(String label, String current, boolean showCurrent) {
        JLabel lbl = UIFactory.createFieldLabel(label + (showCurrent ? "  (current: RM " + current + ")" : ""));
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        return lbl;
    }

    private JTextField buildPriceField(String value) {
        JTextField f = UIFactory.createTextField("e.g. 80.00");
        f.setText(value);
        f.setForeground(Color.BLACK);
        return f;
    }

    private void handleSave() {
        String normalText = UIFactory.getFieldValue(normalField, "e.g. 80.00");
        String majorText  = UIFactory.getFieldValue(majorField,  "e.g. 80.00");

        double normal, major;
        try {
            normal = Double.parseDouble(normalText);
            major  = Double.parseDouble(majorText);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Prices must be valid numbers (e.g. 80.00).",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (normal <= 0 || major <= 0) {
            JOptionPane.showMessageDialog(this, "Prices must be greater than zero.",
                    "Invalid Input", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (major <= normal) {
            JOptionPane.showMessageDialog(this, "Major service price should be higher than normal service price.",
                    "Invalid Input", JOptionPane.WARNING_MESSAGE);
            return;
        }

        priceConfig.setNormalPrice(normal);
        priceConfig.setMajorPrice(major);

        if (priceConfig.save()) {
            JOptionPane.showMessageDialog(this,
                    "Prices saved!\n  Normal: RM " + normal + "\n  Major:  RM " + major,
                    "Saved", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Failed to save prices. Please try again.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}