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
            + File.separator + "comments.txt";
    private static final String FEEDBACK_FILE  = "src" + File.separator + "TxtFile"
            + File.separator + "feedback.txt";

    public ViewFeedbackPanel() {
        setLayout(new GridLayout(2, 1, 0, 16));
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 36, 30, 36));

        add(buildCommentsSection("Customer Comments", "Customer comments on service and staff/technician", loadFile(COMMENTS_FILE)));
        add(buildFeedbackSection("Technician Feedback", "Technician feedback on customer vehicles", loadFile(FEEDBACK_FILE)));
    }

    private JPanel buildCommentsSection(String heading, String subDesc, List<String[]> rows) {
        String[] columns = {"No", "Comment ID", "Appointment ID", "Staff ID", "Technician ID", "Rating", "Comment", "Date"};
        JPanel section = createSectionBase(heading, subDesc);
        DefaultTableModel model = createModel(columns);
        
        int idx = 1;
        for (String[] row : rows) {
            if (row.length >= 9) {
                model.addRow(new String[]{
                    String.valueOf(idx++), row[0], row[2], row[4], row[5], row[6], row[7], row[8]
                });
            }
        }
        
        return finalizeSection(section, model, columns, true, rows.isEmpty());
    }

    private JPanel buildFeedbackSection(String heading, String subDesc, List<String[]> rows) {
        String[] columns = {"No", "Feedback ID", "Appointment ID", "Technician ID", "Condition", "Feedback", "Date"};
        JPanel section = createSectionBase(heading, subDesc);
        DefaultTableModel model = createModel(columns);
        
        int idx = 1;
        for (String[] row : rows) {
            if (row.length >= 8) {
                model.addRow(new String[]{
                    String.valueOf(idx++), row[0], row[2], row[4], row[5], row[6], row[7]
                });
            }
        }
        
        return finalizeSection(section, model, columns, false, rows.isEmpty());
    }

    private JPanel createSectionBase(String heading, String subDesc) {
        JPanel section = new JPanel(new BorderLayout(0, 10));
        section.setBackground(UIConstants.BG_CONTENT);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);

        JLabel title = new JLabel(heading);
        title.setFont(UIConstants.FONT_BODY_BOLD);
        title.setForeground(UIConstants.TEXT_PRIMARY);
        titlePanel.add(title);

        titlePanel.add(Box.createVerticalStrut(4));

        JLabel desc = new JLabel(subDesc);
        desc.setFont(UIConstants.FONT_SMALL);
        desc.setForeground(UIConstants.TEXT_MUTED);
        titlePanel.add(desc);

        section.add(titlePanel, BorderLayout.NORTH);
        return section;
    }

    private DefaultTableModel createModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JPanel finalizeSection(JPanel section, DefaultTableModel model, String[] columns, boolean isComments, boolean isEmpty) {
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
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

        DefaultTableCellRenderer centre = new DefaultTableCellRenderer();
        centre.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setCellRenderer(centre);

        if (isComments) {
            table.getColumnModel().getColumn(1).setPreferredWidth(100);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(90);
            table.getColumnModel().getColumn(4).setPreferredWidth(110);
            table.getColumnModel().getColumn(5).setPreferredWidth(80);
            table.getColumnModel().getColumn(5).setCellRenderer(new RatingRenderer());
            table.getColumnModel().getColumn(6).setPreferredWidth(600);
            table.getColumnModel().getColumn(7).setPreferredWidth(100);
        } else {
            table.getColumnModel().getColumn(1).setPreferredWidth(100);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(110);
            table.getColumnModel().getColumn(4).setPreferredWidth(150);
            table.getColumnModel().getColumn(5).setPreferredWidth(600);
            table.getColumnModel().getColumn(6).setPreferredWidth(100);
        }

        JScrollPane scroll = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
        scroll.setPreferredSize(new Dimension(0, 200));

        if (isEmpty) {
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