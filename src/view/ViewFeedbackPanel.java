package view;

import view.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays all customer comments / technician feedback for the admin.
 * Data is read from two text files:
 *   - customer_comments.txt   : commentID,appointmentID,rating,comment,staffID,techID
 *   - technician_feedback.txt : feedbackID,appointmentID,vehicleCondition,workDone,recommendations,date
 *
 * OOP highlights:
 *  - Encapsulation : file paths and parsing hidden inside private methods
 *  - Inheritance   : extends JPanel
 *  - Abstraction   : table building abstracted into buildTable()
 *  - Polymorphism  : loadComments() and loadFeedback() both return List<String[]>
 *                    and are displayed by the same generic renderTable() method
 */
public class ViewFeedbackPanel extends JPanel {

    private static final String COMMENTS_FILE  = "src" + File.separator + "TxtFile"
            + File.separator + "customer_comments.txt";
    private static final String FEEDBACK_FILE  = "src" + File.separator + "TxtFile"
            + File.separator + "technician_feedback.txt";

    public ViewFeedbackPanel() {
        setLayout(new BorderLayout(0, 16));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        add(buildSection("Customer Comments",
                new String[]{"#", "Appt ID", "Rating", "Comment", "Staff ID", "Tech ID"},
                loadFile(COMMENTS_FILE)), BorderLayout.NORTH);

        add(buildSection("Technician Feedback",
                new String[]{"#", "Appt ID", "Vehicle Condition", "Work Done", "Recommendations", "Date"},
                loadFile(FEEDBACK_FILE)), BorderLayout.CENTER);
    }

    // ─── Section builder (reused for both tables) ────────────────

    private JPanel buildSection(String heading, String[] columns, List<String[]> rows) {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(UIConstants.BG_CONTENT);

        JLabel title = new JLabel(heading);
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        section.add(title, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        int idx = 1;
        for (String[] row : rows) {
            // Build a row: # + up to 5 data columns (pad/truncate to match columns length-1)
            String[] display = new String[columns.length];
            display[0] = String.valueOf(idx++);
            for (int i = 1; i < columns.length; i++) {
                display[i] = (i - 1 < row.length) ? row[i - 1].trim() : "";
            }
            model.addRow(display);
        }

        JTable table = new JTable(model);
        table.setRowHeight(36);
        table.setFont(UIConstants.FONT_SMALL);
        table.setForeground(UIConstants.TEXT_DARK);
        table.setGridColor(new Color(235, 235, 240));
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.setSelectionBackground(new Color(230, 235, 255));
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(new Color(248, 248, 252));
        table.getTableHeader().setForeground(UIConstants.TEXT_MUTED);
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER_DEFAULT));

        // Centre the # column
        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(30);
        table.getColumnModel().getColumn(0).setCellRenderer(centre);

        // Colour the rating column for comments (column index 2)
        if (heading.contains("Comments")) {
            table.getColumnModel().getColumn(2).setCellRenderer(new RatingRenderer());
        }

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        scroll.setPreferredSize(new Dimension(0, 200));

        // Empty state message
        if (rows.isEmpty()) {
            JLabel empty = new JLabel("No records found.", SwingConstants.CENTER);
            empty.setFont(UIConstants.FONT_SMALL);
            empty.setForeground(UIConstants.TEXT_MUTED);
            section.add(empty, BorderLayout.CENTER);
        } else {
            section.add(scroll, BorderLayout.CENTER);
        }

        return section;
    }

    // ─── File loading (private — encapsulated) ───────────────────

    private List<String[]> loadFile(String path) {
        List<String[]> rows = new ArrayList<>();
        File file = new File(path);
        if (!file.exists()) return rows;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) rows.add(line.split(",", -1));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return rows;
    }

    // ─── Rating colour renderer ──────────────────────────────────

    /** Colours rating cells: green ≥ 4, amber = 3, red ≤ 2 */
    private static class RatingRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, value, sel, focus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            try {
                int rating = Integer.parseInt(value.toString().trim());
                if      (rating >= 4) setForeground(new Color(34, 139, 34));
                else if (rating == 3) setForeground(new Color(200, 130, 0));
                else                  setForeground(UIConstants.TEXT_DANGER);
            } catch (NumberFormatException ignored) {
                setForeground(UIConstants.TEXT_DARK);
            }
            return this;
        }
    }
}