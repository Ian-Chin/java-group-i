package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

// Shows the customer's service history in a table.
// Columns match the UML ServiceHistory attributes.
//
// TODO: wire up file reading when friends complete serviceHistory.txt
//       File format: historyID, appointmentID, paymentID, technicianID, date
public class ServiceHistoryPage extends JPanel {

    // Column names shown in the table header — based on UML ServiceHistory
    private static final String[] COLUMNS = {
            "History ID", "Appointment ID", "Payment ID", "Technician ID", "Date"
    };

    // Text shown when there is no data yet
    private static final String NO_DATA_ICON = "\uD83D\uDD04"; // 🔄 icon
    private static final String NO_DATA_TEXT = "No service history records found.";
    private static final String NO_DATA_DESC = "Your completed service records will appear here.";

    // Constructor — sets up the panel and builds the page
    public ServiceHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 30, 30, 30));
        build();
    }

    // Called every time the user clicks "Service History" in the sidebar
    public void refresh() {
        build();
    }

    // Builds the page content — shows the table or a "no data" card
    private void build() {
        removeAll(); // clear anything shown before

        // Create an empty table model with the correct column names
        // isCellEditable = false means the user cannot type inside the table
        DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false; // read-only table
            }
        };

        // TODO: add rows from serviceHistory.txt here once the file is ready
        // Example of how to add a row:
        // model.addRow(new String[]{ "SH001", "APT001", "PAY001", "T001", "2025-03-01" });

        if (model.getRowCount() == 0) {
            // No data — show the "no records" placeholder card
            add(buildNoDataPanel(NO_DATA_ICON, NO_DATA_TEXT, NO_DATA_DESC), BorderLayout.CENTER);
        } else {
            // Has data — show the styled table
            add(buildTablePanel(model), BorderLayout.CENTER);
        }

        revalidate(); // re-layout the panel
        repaint();    // redraw the panel
    }

    // ═══════════════════════════════════════════════════════════════
    // SHARED HELPERS — used by ServiceHistoryPage, PaymentHistoryPage,
    //                  StaffReviewPage, and MyFeedbackPage
    // ═══════════════════════════════════════════════════════════════

    // Builds the centred "no data" placeholder card.
    // Looks the same as the placeholder pages (icon + title + description).
    // icon        = emoji shown at the top (e.g. "🔄")
    // title       = bold text (e.g. "No service history records found.")
    // description = smaller grey text below the title
    static JPanel buildNoDataPanel(String icon, String title, String description) {

        // GridBagLayout centres the card both horizontally and vertically
        JPanel centreWrapper = new JPanel(new GridBagLayout());
        centreWrapper.setBackground(UIConstants.BG_CONTENT);

        // The white rounded card
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                // Draw a white rounded rectangle as the card background
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS)); // stack items vertically
        card.setBorder(new EmptyBorder(50, 60, 50, 60));

        // Icon label (large emoji at the top)
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("SansSerif", Font.PLAIN, 40));
        iconLabel.setForeground(UIConstants.TEXT_SIDEBAR);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);
        card.add(Box.createVerticalStrut(16)); // gap

        // Title label (bold)
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(UIConstants.TEXT_DARK);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(8)); // gap

        // Description label (smaller, grey)
        JLabel descLabel = new JLabel(description);
        descLabel.setFont(UIConstants.FONT_BODY);
        descLabel.setForeground(UIConstants.TEXT_MUTED);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(descLabel);

        centreWrapper.add(card);
        return centreWrapper;
    }

    // Wraps a table model in a styled scroll pane and returns it.
    // Used when there IS data to show.
    static JScrollPane buildTablePanel(DefaultTableModel model) {
        JTable table = TableHelper.buildTable(model); // apply the shared table styling
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        return scroll;
    }
}
