package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

// A shared helper class used by all four section pages.
// Only has one job: build a nicely styled table.
// Other pages call TableHelper.buildTable() instead of styling their own tables.
public class TableHelper {

    // Builds and returns a styled read-only table.
    // model = the data to show (column names + rows)
    // The table has:
    //   - Bold blue header row
    //   - Alternating white / light-grey row colours
    //   - Rows cannot be edited by the user
    public static JTable buildTable(DefaultTableModel model) {

        JTable table = new JTable(model);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setRowHeight(38);             // height of each row
        table.setShowGrid(false);           // hide grid lines between cells
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true); // stretch the table to fill the scroll pane
        table.setSelectionBackground(new Color(80, 110, 230, 60)); // light blue when a row is selected
        table.setSelectionForeground(UIConstants.TEXT_PRIMARY);

        // ── Style the column header row ───────────────────────────
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setBackground(new Color(235, 238, 248)); // light blue-grey background
        header.setForeground(UIConstants.TEXT_PRIMARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(80, 110, 230))); // blue bottom line
        header.setReorderingAllowed(false); // prevent user from dragging columns around
        header.setPreferredSize(new Dimension(0, 42)); // header row height

        // ── Alternating row colours ───────────────────────────────
        // This renderer is called every time a cell needs to be drawn on screen.
        // Even rows (0, 2, 4...) = white background
        // Odd  rows (1, 3, 5...) = very light grey background
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {

                // Run the default drawing logic first
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);

                // Only apply alternating colour when the row is NOT selected
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                }

                // Add left and right padding inside each cell
                setBorder(new EmptyBorder(0, 14, 0, 14));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                return this;
            }
        });

        return table;
    }
}
