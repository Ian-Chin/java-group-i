package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

// Shows the customer's payment history in a table.
// Columns match the UML Payment attributes.
// When data is ready, clicking a row will show an invoice popup.
//
// TODO: wire up file reading when friends complete payments.txt
//       File format: paymentID, appointmentID, amount, paymentDate, method
public class PaymentHistoryPage extends JPanel {

    // Column names shown in the table header — based on UML Payment attributes
    private static final String[] COLUMNS = {
            "Payment ID", "Appointment ID", "Amount (RM)", "Payment Date", "Method"
    };

    // Text shown when there is no data yet
    private static final String NO_DATA_ICON = "\uD83D\uDCB5"; // 💵 icon
    private static final String NO_DATA_TEXT = "No payment records found.";
    private static final String NO_DATA_DESC = "Your completed payment transactions will appear here.";

    // Constructor — sets up the panel and builds the page
    public PaymentHistoryPage() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 30, 30, 30));
        build();
    }

    // Called every time the user clicks "Payment History" in the sidebar
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

        // TODO: add rows from payments.txt here once the file is ready
        // Example of how to add a row:
        // model.addRow(new String[]{ "PAY001", "APT001", "150.00", "2025-03-01", "Cash" });

        if (model.getRowCount() == 0) {
            // No data — show the "no records" placeholder card
            add(ServiceHistoryPage.buildNoDataPanel(NO_DATA_ICON, NO_DATA_TEXT, NO_DATA_DESC),
                    BorderLayout.CENTER);
        } else {
            // Has data — show the table with a hint below it
            JTable table = TableHelper.buildTable(model);

            // TODO: uncomment this block once data is ready so clicking a row shows the invoice
            // table.addMouseListener(new java.awt.event.MouseAdapter() {
            //     @Override
            //     public void mouseClicked(java.awt.event.MouseEvent e) {
            //         int selectedRow = table.getSelectedRow();
            //         if (selectedRow >= 0) {
            //             showInvoice(rows.get(selectedRow));
            //         }
            //     }
            // });

            // Wrap the table in a scroll pane
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));

            // Small hint text below the table
            JLabel hint = new JLabel("  \uD83D\uDCA1 Click any row to view the full invoice.");
            hint.setFont(new Font("SansSerif", Font.ITALIC, 12));
            hint.setForeground(UIConstants.TEXT_MUTED);
            hint.setBorder(new EmptyBorder(8, 0, 0, 0));

            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(scroll, BorderLayout.CENTER);
            wrapper.add(hint,   BorderLayout.SOUTH);
            add(wrapper, BorderLayout.CENTER);
        }

        revalidate(); // re-layout the panel
        repaint();    // redraw the panel
    }

    // Shows the invoice popup for the selected payment row.
    // data = [paymentID, appointmentID, amount, paymentDate, method]
    // TODO: call this method from the row click listener once data is wired up
    private void showInvoice(String[] data) {

        // Build the invoice content panel
        JPanel invoice = new JPanel();
        invoice.setLayout(new BoxLayout(invoice, BoxLayout.Y_AXIS));
        invoice.setBackground(Color.WHITE);
        invoice.setBorder(new EmptyBorder(24, 30, 24, 30));
        invoice.setPreferredSize(new Dimension(400, 340));

        // ── Invoice header ────────────────────────────────────────
        JLabel shopName = new JLabel("APU Automotive Service Centre");
        shopName.setFont(new Font("SansSerif", Font.BOLD, 15));
        shopName.setForeground(new Color(80, 110, 230));
        shopName.setAlignmentX(Component.LEFT_ALIGNMENT);
        invoice.add(shopName);
        invoice.add(Box.createVerticalStrut(4));

        JLabel invoiceTitle = new JLabel("Official Payment Invoice");
        invoiceTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        invoiceTitle.setForeground(UIConstants.TEXT_MUTED);
        invoiceTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        invoice.add(invoiceTitle);
        invoice.add(Box.createVerticalStrut(16));
        invoice.add(makeDivider());
        invoice.add(Box.createVerticalStrut(14));

        // ── Invoice detail rows ───────────────────────────────────
        invoice.add(makeInvoiceRow("Payment ID",     data[0]));
        invoice.add(Box.createVerticalStrut(8));
        invoice.add(makeInvoiceRow("Appointment ID", data[1]));
        invoice.add(Box.createVerticalStrut(8));
        invoice.add(makeInvoiceRow("Payment Date",   data[3]));
        invoice.add(Box.createVerticalStrut(8));
        invoice.add(makeInvoiceRow("Method",         data[4]));
        invoice.add(Box.createVerticalStrut(14));
        invoice.add(makeDivider());
        invoice.add(Box.createVerticalStrut(14));

        // ── Total amount row (larger + green) ─────────────────────
        JPanel amountRow = new JPanel(new BorderLayout());
        amountRow.setOpaque(false);
        amountRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        amountRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel amountLabel = new JLabel("Total Amount");
        amountLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        amountLabel.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel amountValue = new JLabel("RM " + data[2]);
        amountValue.setFont(new Font("SansSerif", Font.BOLD, 14));
        amountValue.setForeground(new Color(80, 190, 110)); // green

        amountRow.add(amountLabel, BorderLayout.WEST);
        amountRow.add(amountValue, BorderLayout.EAST);
        invoice.add(amountRow);

        // Show the invoice in a popup dialog
        JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(this),
                invoice,
                "Invoice — " + data[0],
                JOptionPane.PLAIN_MESSAGE);
    }

    // Builds one label + value row inside the invoice (e.g. "Payment ID    PAY001")
    private JPanel makeInvoiceRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(130, 20)); // fixed width so values line up

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(UIConstants.TEXT_PRIMARY);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // Builds a thin horizontal divider line used inside the invoice
    private JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(UIConstants.BORDER_DEFAULT);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }
}
