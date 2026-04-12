package view;

import model.AccountService;
import model.User;
import model.VehicleService;
import util.PdfUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * ReceiptUtil — a shared utility class for displaying payment receipts.
 *
 * WHY A SEPARATE CLASS?
 *   Both PaymentHistoryPage (customer) and PaymentCollectionPanel (staff)
 *   need to show the same receipt popup. Instead of making showReceipt()
 *   public on one of those classes (which would expose sensitive financial
 *   logic), we extract it here into a dedicated utility class.
 *
 *   In the real world, receipt generation is often handled by a dedicated
 *   service or utility — keeping it separate from both the customer view
 *   and the staff view. Neither class needs to "own" the receipt logic;
 *   they simply ask ReceiptUtil to display it.
 *
 * HOW TO USE IT:
 *   From PaymentHistoryPage (customer side):
 *     ReceiptUtil.showReceipt(parentWindow, rawPaymentRow, vehicleService, accountService);
 *
 *   From PaymentCollectionPanel (staff side):
 *     ReceiptUtil.showReceipt(parentWindow, rawPaymentRow, vehicleService, accountService);
 *
 * Both calls look identical — the receipt popup is the same for everyone.
 *
 * WHY IS showReceipt() PACKAGE-PRIVATE (no "public")?
 *   It uses the default (package-private) access level intentionally.
 *   Only classes inside the "view" package can call it, so code in other
 *   packages (e.g. model, controller) cannot access receipt data.
 *   This is a simple but effective encapsulation boundary.
 */
class ReceiptUtil {

    // ── Design colours (kept private to this class) ────────────────
    private static final Color COLOR_TEXT   = new Color(30,  35,  50);
    private static final Color COLOR_MUTED  = new Color(110, 118, 140);
    private static final Color COLOR_BORDER = new Color(225, 228, 235);

    // ── Private constructor — no one should create a ReceiptUtil object.
    //    All methods are static; this class is a pure utility.
    private ReceiptUtil() {}

    // ═══════════════════════════════════════════════════════════════
    // showReceipt()
    //
    // Displays the payment receipt popup. This is the ONLY public-
    // facing method. Everything else in this class is private.
    //
    // Parameters:
    //   parent         — the parent window (used to centre the popup)
    //   payment        — String[9] read from payments.txt:
    //                    [0]paymentID  [1]customerID  [2]shID
    //                    [3]apptID     [4]vehicleID   [5]amount
    //                    [6]date       [7]method      [8]status
    //   vehicleService — looks up vehicle type and car plate
    //   accountService — looks up the customer's display name
    // ═══════════════════════════════════════════════════════════════
    static void showReceipt(Component parent, String[] payment,
                            VehicleService vehicleService,
                            AccountService accountService) {

        // ── Step 1: Extract all fields from the payment row ────────
        String paymentId     = payment[0].trim();
        String customerId    = payment[1].trim();
        String shId          = payment[2].trim();
        String appointmentId = payment[3].trim();
        String vehicleId     = payment[4].trim();
        String amount        = payment[5].trim();
        String date          = payment[6].trim();
        String method        = payment[7].trim();
        String status        = payment[8].trim();

        // ── Step 2: Resolve display values from services ───────────
        String customerName = resolveCustomerName(customerId, accountService);
        String vehicleLabel = vehicleService.getVehiclePlate(vehicleId);
        String amountDisplay;
        try {
            amountDisplay = String.format("RM %.2f", Double.parseDouble(amount));
        } catch (NumberFormatException e) {
            amountDisplay = "RM " + amount;
        }

        // ── Step 3: Build the receipt panel ───────────────────────
        JPanel receipt = buildReceiptPanel(
            paymentId, customerId, customerName, shId, appointmentId,
            vehicleLabel, date, method, status, amountDisplay
        );

        // ── Step 4: Show the popup dialog ─────────────────────────
        Window parentWindow = toWindow(parent);
        JOptionPane.showMessageDialog(
            parentWindow,
            receipt,
            "Receipt — " + paymentId,
            JOptionPane.PLAIN_MESSAGE
        );
    }

    // ─────────────────────────────────────────────────────────────
    // buildReceiptPanel() — PRIVATE
    //
    // Constructs the visual receipt panel shown inside the popup.
    // This is private so no outside code can grab the panel directly
    // and embed it somewhere else — it must go through showReceipt().
    // ─────────────────────────────────────────────────────────────
    private static JPanel buildReceiptPanel(
            String paymentId, String customerId, String customerName,
            String shId, String appointmentId, String vehicleLabel,
            String date, String method, String status, String amountDisplay) {

        JPanel receipt = new JPanel();
        receipt.setLayout(new BoxLayout(receipt, BoxLayout.Y_AXIS));
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(new EmptyBorder(28, 32, 28, 32));
        receipt.setPreferredSize(new Dimension(460, 660));

        // ── Logo ───────────────────────────────────────────────────
        try {
            BufferedImage logoImg = ImageIO.read(new File(
                "src" + File.separator + "Image" + File.separator + "apu-logo.png"));
            if (logoImg != null) {
                int logoWidth  = 120;
                int logoHeight = (int) ((double) logoImg.getHeight() / logoImg.getWidth() * logoWidth);
                Image scaled   = logoImg.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);
                JLabel logoLabel = new JLabel(new ImageIcon(scaled));
                logoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
                receipt.add(logoLabel);
                receipt.add(Box.createVerticalStrut(10));
            }
        } catch (Exception ignored) {}

        // ── Shop name and receipt subtitle ────────────────────────
        JLabel shopName = new JLabel("APU Automotive Service Centre");
        shopName.setFont(new Font("SansSerif", Font.BOLD, 16));
        shopName.setForeground(new Color(80, 110, 230));
        shopName.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(shopName);
        receipt.add(Box.createVerticalStrut(4));

        JLabel receiptTitle = new JLabel("Official Payment Receipt");
        receiptTitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        receiptTitle.setForeground(COLOR_MUTED);
        receiptTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(receiptTitle);
        receipt.add(Box.createVerticalStrut(18));

        receipt.add(makeDivider());
        receipt.add(Box.createVerticalStrut(16));

        // ── Detail rows ────────────────────────────────────────────
        receipt.add(makeRow("Receipt No.",        paymentId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Customer",           customerName + " (" + customerId + ")"));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Service History ID", shId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Appointment ID",     appointmentId));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Vehicle",            vehicleLabel));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Payment Date",       date));
        receipt.add(Box.createVerticalStrut(10));
        receipt.add(makeRow("Payment Method",     method));
        receipt.add(Box.createVerticalStrut(10));

        // ── Status badge ───────────────────────────────────────────
        receipt.add(makeStatusRow(status));
        receipt.add(Box.createVerticalStrut(16));

        receipt.add(makeDivider());
        receipt.add(Box.createVerticalStrut(16));

        // ── Total amount ───────────────────────────────────────────
        JPanel totalRow = new JPanel(new BorderLayout());
        totalRow.setOpaque(false);
        totalRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        totalRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel totalLabel = new JLabel("Total Amount");
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setForeground(COLOR_TEXT);
        JLabel totalValue = new JLabel(amountDisplay);
        totalValue.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalValue.setForeground(new Color(34, 139, 80));
        totalRow.add(totalLabel, BorderLayout.WEST);
        totalRow.add(totalValue, BorderLayout.EAST);
        receipt.add(totalRow);
        receipt.add(Box.createVerticalStrut(20));

        // ── Footer ─────────────────────────────────────────────────
        JLabel footer = new JLabel("Thank you for your patronage!");
        footer.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footer.setForeground(COLOR_MUTED);
        footer.setAlignmentX(Component.LEFT_ALIGNMENT);
        receipt.add(footer);
        receipt.add(Box.createVerticalStrut(18));

        // ── Export PDF button ──────────────────────────────────────
        JButton exportPdfBtn = buildExportButton(receipt, paymentId);
        receipt.add(exportPdfBtn);

        return receipt;
    }

    // ─────────────────────────────────────────────────────────────
    // buildExportButton() — PRIVATE
    // Creates the "Export PDF" button and wires up its click action.
    // Kept private so only buildReceiptPanel() can create it.
    // ─────────────────────────────────────────────────────────────
    private static JButton buildExportButton(JPanel receipt, String paymentId) {
        JButton btn = new JButton("Export PDF");
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(80, 110, 230));
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(140, 36));

        btn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Receipt as PDF");
            chooser.setSelectedFile(new File("Receipt_" + paymentId + ".pdf"));

            // Use the button's own parent window to anchor the file chooser
            Window parentWindow = SwingUtilities.getWindowAncestor(btn);
            if (chooser.showSaveDialog(parentWindow) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.toLowerCase().endsWith(".pdf")) path += ".pdf";
                try {
                    btn.setVisible(false);
                    receipt.revalidate();
                    receipt.repaint();
                    exportToPdf(receipt, path);
                    btn.setVisible(true);
                    receipt.revalidate();
                    receipt.repaint();
                    if (Desktop.isDesktopSupported()) Desktop.getDesktop().open(new File(path));
                    JOptionPane.showMessageDialog(parentWindow,
                        "Receipt exported!\n" + path,
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    btn.setVisible(true);
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(parentWindow,
                        "Error exporting PDF: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        return btn;
    }

    // ─────────────────────────────────────────────────────────────
    // makeRow() — PRIVATE
    // One "Label : Bold Value" row inside the receipt.
    // ─────────────────────────────────────────────────────────────
    private static JPanel makeRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(160, 22));

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 13));
        val.setForeground(COLOR_TEXT);

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.CENTER);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // makeStatusRow() — PRIVATE
    // The status row shows a coloured badge instead of plain text.
    // ─────────────────────────────────────────────────────────────
    private static JPanel makeStatusRow(String status) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Status");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setForeground(COLOR_MUTED);
        lbl.setPreferredSize(new Dimension(160, 22));

        JLabel badge = new JLabel(status);
        badge.setFont(new Font("SansSerif", Font.BOLD, 11));
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(3, 10, 3, 10));

        // Pick badge colour based on status value
        switch (status.toLowerCase()) {
            case "paid":
                badge.setBackground(new Color(220, 248, 232));
                badge.setForeground(new Color(34, 139, 80));
                break;
            case "pending":
                badge.setBackground(new Color(255, 243, 220));
                badge.setForeground(new Color(180, 110, 20));
                break;
            default:
                badge.setBackground(new Color(235, 236, 240));
                badge.setForeground(COLOR_MUTED);
        }

        row.add(lbl,   BorderLayout.WEST);
        row.add(badge, BorderLayout.CENTER);
        return row;
    }

    // ─────────────────────────────────────────────────────────────
    // makeDivider() — PRIVATE
    // A thin horizontal line used to separate sections.
    // ─────────────────────────────────────────────────────────────
    private static JSeparator makeDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(COLOR_BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    // ─────────────────────────────────────────────────────────────
    // exportToPdf() — PRIVATE
    // Renders the receipt panel to a PDF file using PdfUtil.
    // ─────────────────────────────────────────────────────────────
    private static void exportToPdf(JPanel panel, String path) throws Exception {
        int w = panel.getWidth(), h = panel.getHeight();
        if (w == 0 || h == 0) {
            w = panel.getPreferredSize().width;
            h = panel.getPreferredSize().height;
            panel.setSize(w, h);
            panel.doLayout();
        }
        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = image.createGraphics();
        panel.paint(g2);
        g2.dispose();
        try {
            PdfUtil.writeImageAsPdf(image, new File(path));
        } catch (IOException ioe) {
            throw ioe;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // resolveCustomerName() — PRIVATE
    // Looks up a customer's name from AccountService by their ID.
    // Falls back to the raw ID if no match is found.
    // ─────────────────────────────────────────────────────────────
    private static String resolveCustomerName(String customerId,
                                               AccountService accountService) {
        for (User u : accountService.getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(customerId)) {
                return u.getName();
            }
        }
        return customerId;
    }

    // ─────────────────────────────────────────────────────────────
    // toWindow() — PRIVATE
    // Safely converts a Component to its parent Window, which is
    // needed for anchoring dialogs and the file chooser.
    // ─────────────────────────────────────────────────────────────
    private static Window toWindow(Component component) {
        if (component instanceof Window) return (Window) component;
        return SwingUtilities.getWindowAncestor(component);
    }
}