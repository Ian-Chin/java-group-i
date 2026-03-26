package view;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

// Shows completed appointments so the customer can submit feedback.
// Each row has a "Feedback" button that opens a feedback form popup.
// Columns match the UML Appointment attributes.
//
// TODO: wire up file reading when friends complete appointments.txt and feedback.txt
//       appointments.txt format: appointmentID, serviceType, status, dateTime, duration, customerEmail
//       feedback.txt    format : feedbackID, appointmentID, vehicleCondition, workDone, recommendations, date
public class MyFeedbackPage extends JPanel {

    // Maximum total characters allowed across all three feedback fields
    private static final int MAX_CHARS = 1500;

    // Column names shown in the table header — based on UML Appointment attributes
    // The last column "" is for the Feedback button — no header text needed
    private static final String[] COLUMNS = {
            "Appointment ID", "Service Type", "Date / Time", "Duration", ""
    };

    // Text shown when there is no data yet
    private static final String NO_DATA_ICON = "\uD83D\uDCAC"; // 💬 icon
    private static final String NO_DATA_TEXT = "No completed appointments found.";
    private static final String NO_DATA_DESC = "Feedback can be submitted once your appointments are completed.";

    // Constructor — sets up the panel and builds the page
    public MyFeedbackPage() {
        setLayout(new BorderLayout());
        setBackground(UIConstants.BG_CONTENT);
        setBorder(new EmptyBorder(30, 30, 30, 30));
        build();
    }

    // Called every time the user clicks "My Feedback" in the sidebar
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

        // TODO: add rows from appointments.txt here (only rows where status = "Completed"
        //       and customerEmail matches the logged-in user)
        // Example of how to add a row:
        // model.addRow(new String[]{ "APT001", "Oil Change", "2025-03-01 10:00", "45 min", "Feedback" });

        if (model.getRowCount() == 0) {
            // No data — show the "no records" placeholder card
            add(ServiceHistoryPage.buildNoDataPanel(NO_DATA_ICON, NO_DATA_TEXT, NO_DATA_DESC),
                    BorderLayout.CENTER);
        } else {
            // Has data — show the styled table with a Feedback button on each row
            JTable table = TableHelper.buildTable(model);

            // Fix the width of the last column (Feedback button)
            table.getColumn("").setMaxWidth(110);
            table.getColumn("").setMinWidth(110);

            // TODO: uncomment these lines once data is ready
            // These add the blue "Feedback" button to each row
            // table.getColumn("").setCellRenderer(new FeedbackButtonRenderer());
            // table.getColumn("").setCellEditor(new FeedbackButtonEditor(rows));

            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1));
            add(scroll, BorderLayout.CENTER);
        }

        revalidate(); // re-layout the panel
        repaint();    // redraw the panel
    }

    // Opens the feedback form popup for a specific appointment.
    // appointmentID — used to save the feedback linked to the correct appointment
    // serviceType   — shown in the form header so the user knows which service they are rating
    // dateTime      — shown in the form for reference
    //
    // TODO: call this method when the Feedback button is clicked on a table row
    void showFeedbackForm(String appointmentID, String serviceType, String dateTime) {

        // ── Build the feedback form panel ─────────────────────────
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS)); // stack items vertically
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(10, 10, 10, 10));
        form.setPreferredSize(new Dimension(480, 480));

        // Show appointment info at the top of the form (read-only)
        JLabel info = new JLabel("Appointment: " + appointmentID
                + "   |   Service: " + serviceType
                + "   |   Date: " + dateTime);
        info.setFont(new Font("SansSerif", Font.PLAIN, 12));
        info.setForeground(UIConstants.TEXT_MUTED);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(info);
        form.add(Box.createVerticalStrut(14));

        // ── Three text areas matching UML Feedback attributes ─────
        JTextArea vehicleConditionField = addTextArea("Vehicle Condition",  form);
        form.add(Box.createVerticalStrut(10));
        JTextArea workDoneField         = addTextArea("Work Done",          form);
        form.add(Box.createVerticalStrut(10));
        JTextArea recommendationsField  = addTextArea("Recommendations",    form);
        form.add(Box.createVerticalStrut(10));

        // ── Live character counter ────────────────────────────────
        // Shows the total characters typed across all three fields.
        // Turns red if the user exceeds the 1500 character limit.
        JLabel counter = new JLabel("Total characters: 0 / " + MAX_CHARS);
        counter.setFont(new Font("SansSerif", Font.PLAIN, 11));
        counter.setForeground(UIConstants.TEXT_MUTED);
        counter.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(counter);

        // This listener is called every time the user types or deletes text
        javax.swing.event.DocumentListener typingListener = new javax.swing.event.DocumentListener() {

            // Called when text is typed
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e)  { updateCounter(); }

            // Called when text is deleted
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e)  { updateCounter(); }

            // Called when text styling changes (not used here but required by the interface)
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }

            // Recalculates the total character count and updates the counter label
            void updateCounter() {
                int total = vehicleConditionField.getText().length()
                        + workDoneField.getText().length()
                        + recommendationsField.getText().length();

                counter.setText("Total characters: " + total + " / " + MAX_CHARS);

                // Turn the counter red if limit is exceeded
                counter.setForeground(total > MAX_CHARS
                        ? new Color(220, 60, 60)    // red = over limit
                        : UIConstants.TEXT_MUTED);  // grey = within limit
            }
        };

        // Attach the listener to all three text areas
        vehicleConditionField.getDocument().addDocumentListener(typingListener);
        workDoneField.getDocument().addDocumentListener(typingListener);
        recommendationsField.getDocument().addDocumentListener(typingListener);

        // ── Show the form in a Submit / Cancel dialog ─────────────
        // JOptionPane.OK_CANCEL_OPTION shows an "OK" (Submit) button and a "Cancel" button
        int userChoice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                new JScrollPane(form),                  // wrap in scroll in case form is tall
                "Submit Feedback — " + appointmentID,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        // Only process if the user clicked OK (Submit)
        if (userChoice == JOptionPane.OK_OPTION) {

            int total = vehicleConditionField.getText().length()
                    + workDoneField.getText().length()
                    + recommendationsField.getText().length();

            // Check: at least one field must have text
            if (total == 0) {
                JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Please fill in at least one field.",
                        "Empty Feedback",
                        JOptionPane.WARNING_MESSAGE);
                return; // stop here — do not submit
            }

            // Check: total text must not exceed 1500 characters
            if (total > MAX_CHARS) {
                JOptionPane.showMessageDialog(
                        SwingUtilities.getWindowAncestor(this),
                        "Feedback exceeds the maximum of " + MAX_CHARS + " characters.\n"
                        + "Please shorten your responses.",
                        "Too Long",
                        JOptionPane.WARNING_MESSAGE);
                return; // stop here — do not submit
            }

            // TODO: save the feedback to feedback.txt once file writing is wired up
            // saveFeedback(appointmentID,
            //              vehicleConditionField.getText().trim(),
            //              workDoneField.getText().trim(),
            //              recommendationsField.getText().trim());

            // Show success message
            JOptionPane.showMessageDialog(
                    SwingUtilities.getWindowAncestor(this),
                    "Your feedback has been submitted successfully.",
                    "Feedback Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
        }
        // If user clicked Cancel — the dialog just closes, nothing is saved
    }

    // Creates a labelled text area and adds it to the form panel.
    // label  = the field name shown above the text area (e.g. "Vehicle Condition")
    // parent = the form panel to add this field to
    // Returns the JTextArea so the caller can read what the user typed.
    private JTextArea addTextArea(String label, JPanel parent) {

        // Label above the text area
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(UIConstants.TEXT_PRIMARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        parent.add(lbl);
        parent.add(Box.createVerticalStrut(4)); // small gap between label and text area

        // The text area where the user types
        JTextArea area = new JTextArea(4, 40); // 4 rows tall, 40 columns wide
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setLineWrap(true);       // wrap long lines automatically
        area.setWrapStyleWord(true);  // wrap at word boundaries, not mid-word
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER_DEFAULT, 1),
                new EmptyBorder(6, 8, 6, 8)));

        // Wrap the text area in a scroll pane in case the user types a lot
        JScrollPane scroll = new JScrollPane(area);
        scroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); // max height

        parent.add(scroll);
        return area; // return so the caller can read the typed text
    }
}