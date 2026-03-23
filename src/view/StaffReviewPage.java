package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

// Shows reviews/comments from technicians given to the customer.
// Columns match the UML Comments (Technician → Customer) attributes.
//
// TODO: wire up file reading when friends complete comments.txt
//       File format: commentID, appointmentID, rating, comment, staffID, technicianID
public class StaffReviewPage extends JPanel {

    // Column names shown in the table header — based on UML Comments attributes
    private static final String[] COLUMNS = {
            "Comment ID", "Appointment ID", "Rating (1-5)", "Comment", "Staff ID", "Technician ID"
    };

    // Text shown when there is no data yet
    private static final String NO_DATA_ICON = "\u2605"; // ★ star icon
    private static final String NO_DATA_TEXT = "No staff review records found.";
    private static final String NO_DATA_DESC = "Reviews and comments from technicians will appear here.";

    // Constructor — sets up the panel and builds the page
    public StaffReviewPage() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 30, 30, 30));
        build();
    }

    // Called every time the user clicks "Staff Review" in the sidebar
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

        // TODO: add rows from comments.txt here once the file is ready
        // Example of how to add a row:
        // model.addRow(new String[]{ "CMT001", "APT001", "4.5", "Great service!", "ST001", "T001" });

        if (model.getRowCount() == 0) {
            // No data — show the "no records" placeholder card
            add(ServiceHistoryPage.buildNoDataPanel(NO_DATA_ICON, NO_DATA_TEXT, NO_DATA_DESC),
                    BorderLayout.CENTER);
        } else {
            // Has data — show the styled table
            JTable table = TableHelper.buildTable(model);

            // Make the Comment column wider so text is not clipped
            table.getColumn("Comment").setPreferredWidth(260);

            // Make the Rating column narrower — it only shows a short number
            table.getColumn("Rating (1-5)").setMaxWidth(100);

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
            add(scroll, BorderLayout.CENTER);
        }

        revalidate(); // re-layout the panel
        repaint();    // redraw the panel
    }
}
