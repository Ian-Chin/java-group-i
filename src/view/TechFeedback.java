package view;
 
import model.AppointmentService;
import model.AppointmentService.Appointment;
import model.MyFeedbackService;
import model.User;
 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
 

public class TechFeedback extends JPanel {

    private static final Color COLOR_BG = new Color(245, 246, 250);
    private static final Color COLOR_CARD = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(225, 228, 235);
    private static final Color COLOR_TEXT = new Color(30,  35,  50);
    private static final Color COLOR_MUTED = new Color(110, 118, 140);
    private static final Color BLUE_ACCENT = new Color(80,  110, 230);
    private static final Color GREEN = new Color(40,  167, 69);
    private static final Color ORANGE = new Color(255, 165,  0);
    private static final Color GREY = new Color(108, 117, 125);
    private static final Color YELLOW_STAR = new Color(255, 193,  7);
 
    private static final Color BAR_COLOR_COUNT = YELLOW_STAR;
    private static final Color BAR_COLOR_BREAKDOWN = BLUE_ACCENT;
 
    
    private static final String[] CONDITION_LABELS = {
        "Unsatisfactory", "Poor", "Average", "Good", "Excellent"
    };

    private final AppFrame app;
    private final AppointmentService appointmentService = new AppointmentService();
    private final MyFeedbackService feedbackService = new MyFeedbackService();
 
    private JLabel totalCountLabel;
    private JLabel totalSubLabel;
    private JPanel barsPanel;
 
    private DefaultTableModel tableModel;
    private JTable table;
 
    private JTextField searchField;
    private JComboBox<String> conditionFilter;

    private List<Appointment> allMine = new ArrayList<>();

    private static final String[] COLUMNS = {
        "Appointment ID", "Service Type", "Date / Time",
        "Customer", "Vehicle", "Status", "Condition", "My Feedback"
    };
 
     public TechFeedback(AppFrame app) {
        this.app = app;
 
        setLayout(new BorderLayout());
        setBackground(COLOR_BG);
 
        JPanel pageContent = new JPanel(new BorderLayout());
        pageContent.setBackground(COLOR_BG);
        pageContent.setBorder(new EmptyBorder(24, 28, 28, 28));
 
        pageContent.add(buildSubtitleRow(), BorderLayout.NORTH);
        pageContent.add(buildDataPanel(), BorderLayout.CENTER);
 
        JScrollPane outerScroll = new JScrollPane(pageContent);
        outerScroll.setBorder(null);
        outerScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outerScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        outerScroll.getVerticalScrollBar().setUnitIncrement(16);
        outerScroll.getViewport().setBackground(COLOR_BG);
        add(outerScroll, BorderLayout.CENTER);
    }
 
    private JPanel buildSubtitleRow() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 18, 0));
 
        JLabel subtitle = new JLabel(
                "Write feedback for your assigned appointments. " +
                "Each entry is saved and visible to the admin.");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(COLOR_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(subtitle);
        return header;
    }
 
 
    private JPanel buildDataPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
 
        JPanel topRow = buildTopCardsRow();
        topRow.setBorder(new EmptyBorder(0, 0, 18, 0));
        panel.add(topRow, BorderLayout.NORTH);
        panel.add(buildTableCard(), BorderLayout.CENTER);
        return panel;
    }
 

    private JPanel buildTopCardsRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 16, 0));
        row.setOpaque(false);
        row.add(buildCountCard());
        row.add(buildBreakdownCard());
        return row;
    }
 
    private JPanel buildCountCard() {
        JPanel card = makeRoundedCardWithLeftBar(BAR_COLOR_COUNT);
 
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
 
        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.setOpaque(false);
 
        totalCountLabel = new JLabel("0");
        totalCountLabel.setFont(new Font("SansSerif", Font.BOLD, 52));
        totalCountLabel.setForeground(BLUE_ACCENT);
        totalCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
 
        totalSubLabel = new JLabel("feedbacks submitted");
        totalSubLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        totalSubLabel.setForeground(COLOR_MUTED);
        totalSubLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
        inner.add(totalCountLabel);
        inner.add(Box.createVerticalStrut(4));
        inner.add(Box.createVerticalStrut(6));
        inner.add(totalSubLabel);
 
        content.add(inner);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
 

    private JPanel buildBreakdownCard() {
        JPanel card = makeRoundedCardWithLeftBar(BAR_COLOR_BREAKDOWN);
 
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
 
        JLabel heading = new JLabel("Condition Breakdown");
        heading.setFont(new Font("SansSerif", Font.BOLD, 14));
        heading.setForeground(COLOR_TEXT);
        heading.setBorder(new EmptyBorder(0, 0, 12, 0));
        content.add(heading, BorderLayout.NORTH);
 
        barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setOpaque(false);
 
       
        for (int i = 4; i >= 0; i--) {
            barsPanel.add(buildOneBarRow(CONDITION_LABELS[i], 0, 1));
            barsPanel.add(Box.createVerticalStrut(6));
        }
 
        content.add(barsPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }
 

    private JPanel buildOneBarRow(String label, int count, int max) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
 
        JLabel labelLbl = new JLabel(label);
        labelLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        labelLbl.setForeground(COLOR_TEXT);
        labelLbl.setPreferredSize(new Dimension(100, 16));
        row.add(labelLbl, BorderLayout.WEST);
 
        JPanel track = new JPanel(new BorderLayout());
        track.setBackground(new Color(230, 230, 235));
        track.setBorder(new EmptyBorder(4, 0, 4, 0));
        JPanel fill = new JPanel();
        fill.setBackground(BLUE_ACCENT);
        double ratio = (max == 0) ? 0.0 : (double) count / max;
        fill.setPreferredSize(new Dimension((int) (180 * ratio), 8));
        track.add(fill, BorderLayout.WEST);
        row.add(track, BorderLayout.CENTER);
 
        JLabel countLbl = new JLabel(String.valueOf(count));
        countLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        countLbl.setForeground(COLOR_MUTED);
        countLbl.setPreferredSize(new Dimension(20, 16));
        row.add(countLbl, BorderLayout.EAST);
 
        return row;
    }
 

    private JPanel buildTableCard() {
        JPanel card = makeRoundedCard();
        card.setLayout(new BorderLayout());
 

        card.add(buildControlsRow(), BorderLayout.NORTH);
 
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
 
        table = new JTable(tableModel);
        table.setRowHeight(44);
        table.setFont(new Font("SansSerif", Font.PLAIN, 13));
        table.setForeground(COLOR_TEXT);
        table.setGridColor(new Color(240, 240, 245));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(235, 240, 255));
        table.setFillsViewportHeight(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        table.getTableHeader().setBackground(new Color(250, 250, 253));
        table.getTableHeader().setForeground(COLOR_MUTED);
        table.getTableHeader().setPreferredSize(new Dimension(0, 44));
        table.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 238)));
 
     
        DefaultTableCellRenderer centreRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(
                    JTable t, Object value, boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, value, sel, foc, row, col);
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("SansSerif", Font.PLAIN, 13));
                if (!sel) setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                return this;
            }
        };
        for (int i = 0; i < COLUMNS.length - 1; i++)  
            table.getColumnModel().getColumn(i).setCellRenderer(centreRenderer);
 
        table.getColumnModel().getColumn(5).setCellRenderer(
            (t, value, sel, foc, row, col) -> {
                String status = value != null ? value.toString() : "";
                JLabel badge = new JLabel(status, SwingConstants.CENTER);
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                switch (status) {
                    case "Completed": badge.setForeground(GREEN);  break;
                    case "In Progress": badge.setForeground(ORANGE); break;
                    default: badge.setForeground(GREY);   break;
                }
                JPanel wrap = new JPanel(new GridBagLayout());
                wrap.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                wrap.add(badge);
                return wrap;
            }
        );
 
 
        table.getColumnModel().getColumn(6).setCellRenderer(
            (t, value, sel, foc, row, col) -> {
                String condition = value != null ? value.toString() : "";
                JLabel badge = new JLabel(condition.isEmpty() ? "—" : condition,
                        SwingConstants.CENTER);
                badge.setFont(new Font("SansSerif", Font.BOLD, 11));
                badge.setOpaque(true);
                Color condColor = conditionColor(condition);
                badge.setForeground(condColor);
                JPanel wrap = new JPanel(new GridBagLayout());
                wrap.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                wrap.add(badge);
                return wrap;
            }
        );
 

        table.getColumnModel().getColumn(7).setCellRenderer(
            (t, value, sel, foc, row, col) -> {
                String text = value != null ? value.toString() : "";
                JPanel wrap = new JPanel(new BorderLayout(8, 0));
                wrap.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 253));
                wrap.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (text.isEmpty()) {
                    JButton btn = makeOutlineButton("Add Feedback");
                    wrap.add(btn, BorderLayout.WEST);
                } else {
                    JLabel lbl = new JLabel(text);
                    lbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
                    lbl.setForeground(COLOR_TEXT);
                    JButton btn = makeOutlineButton("Edit");
                    btn.setPreferredSize(new Dimension(55, 26));
                    wrap.add(lbl,  BorderLayout.CENTER);
                    wrap.add(btn,  BorderLayout.EAST);
                }
                return wrap;
            }
        );
 
  
        table.getColumnModel().getColumn(7).setCellEditor(
            new DefaultCellEditor(new JCheckBox()) {
                @Override public Component getTableCellEditorComponent(
                        JTable t, Object value, boolean sel, int row, int col) {
                    fireEditingStopped();
                    String apptId = (String) tableModel.getValueAt(row, 0);
                    String existingCondition = (String) tableModel.getValueAt(row, 6);
                    String existingText = (String) tableModel.getValueAt(row, 7);
                    showFeedbackDialog(apptId, existingCondition, existingText, row);
                    return new JLabel();
                }
                @Override public Object getCellEditorValue() { return ""; }
            }
        );

        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0) {
                    String apptId = (String) tableModel.getValueAt(row, 0);
                    String existingCondition = (String) tableModel.getValueAt(row, 6);
                    String existingText = (String) tableModel.getValueAt(row, 7);
                    showFeedbackDialog(apptId, existingCondition, existingText, row);
                }
            }
        });
 
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(110);
        cm.getColumn(2).setPreferredWidth(130);
        cm.getColumn(3).setPreferredWidth(100);
        cm.getColumn(4).setPreferredWidth(80);
        cm.getColumn(5).setPreferredWidth(90);
        cm.getColumn(6).setPreferredWidth(100);
        cm.getColumn(7).setPreferredWidth(300);
 
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(0, 420));
        card.add(scroll, BorderLayout.CENTER);
 
  
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(235, 235, 240)),
                new EmptyBorder(10, 18, 10, 18)));
        JLabel hint = new JLabel(
                "Click any row or the feedback column to add/edit your feedback.");
        hint.setFont(new Font("SansSerif", Font.PLAIN, 12));
        hint.setForeground(COLOR_MUTED);
        footer.add(hint, BorderLayout.WEST);
        card.add(footer, BorderLayout.SOUTH);
 
        return card;
    }
 

    private JPanel buildControlsRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(0, 0, 8, 0));
 
        searchField = new JTextField(16);
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(180, 28));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(2, 8, 2, 8)));
        searchField.setToolTipText("Search by any column");
        searchField.getDocument().addDocumentListener(
            new javax.swing.event.DocumentListener() {
                public void insertUpdate(javax.swing.event.DocumentEvent e){ applyFilters(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e){ applyFilters(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e){ applyFilters(); }
            });
 
        String[] conditionOptions = {
            "All Conditions", "Excellent", "Good", "Average", "Poor", "Unsatisfactory"
        };
        conditionFilter = new JComboBox<>(conditionOptions);
        conditionFilter.setFont(new Font("SansSerif", Font.PLAIN, 13));
        conditionFilter.setPreferredSize(new Dimension(180, 28));
        conditionFilter.setBackground(Color.WHITE);
        conditionFilter.setToolTipText("Filter by condition");
        conditionFilter.addActionListener(e -> applyFilters());
 
        row.add(searchField);
        row.add(conditionFilter);
        return row;
    }
 

    private void showFeedbackDialog(String apptId, String existingCondition,
                                    String existingText, int tableRow) {
 

        String status = (String) tableModel.getValueAt(tableRow, 5);
        if (!"Completed".equalsIgnoreCase(status)) {
            JOptionPane.showMessageDialog(app,
                    "You can only submit feedback for Completed appointments.",
                    "Not Available", JOptionPane.WARNING_MESSAGE);
            return;
        }
 
        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Feedback for " + apptId, true);
        dialog.setSize(520, 420);
        dialog.setResizable(false);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());
 
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBackground(Color.WHITE);
        form.setBorder(new EmptyBorder(24, 32, 24, 32));
 

        JLabel titleLbl = new JLabel("Feedback for Appointment " + apptId);
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(COLOR_TEXT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(titleLbl);
        form.add(Box.createVerticalStrut(18));
 

        JLabel ratingLbl = new JLabel("Service Condition:");
        ratingLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        ratingLbl.setForeground(COLOR_MUTED);
        ratingLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(ratingLbl);
        form.add(Box.createVerticalStrut(8));
 
  
        int[] selectedStars = { conditionToStars(existingCondition) };  // mutable via array
        JLabel[] starBtns   = new JLabel[5];
        JLabel conditionDesc = new JLabel(
                selectedStars[0] > 0
                        ? CONDITION_LABELS[selectedStars[0] - 1]
                        : "Select a star rating");
        conditionDesc.setFont(new Font("SansSerif", Font.PLAIN, 12));
        conditionDesc.setForeground(COLOR_MUTED);
        conditionDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        JPanel starRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        starRow.setOpaque(false);
        starRow.setAlignmentX(Component.LEFT_ALIGNMENT);
 
        for (int i = 0; i < 5; i++) {
            final int starValue = i + 1;
            JLabel star = new JLabel(i < selectedStars[0] ? "\u2605" : "\u2606");
            star.setFont(new Font("SansSerif", Font.PLAIN, 28));
            star.setForeground(YELLOW_STAR);
            star.setCursor(new Cursor(Cursor.HAND_CURSOR));
            starBtns[i] = star;
 
            star.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    selectedStars[0] = starValue;
               
                    for (int j = 0; j < 5; j++) {
                        starBtns[j].setText(j < starValue ? "\u2605" : "\u2606");
                    }
                    conditionDesc.setText(CONDITION_LABELS[starValue - 1]);
                }
                @Override public void mouseEntered(MouseEvent e) {
                 
                    for (int j = 0; j < 5; j++) {
                        starBtns[j].setText(j < starValue ? "\u2605" : "\u2606");
                    }
                }
                @Override public void mouseExited(MouseEvent e) {
                 
                    for (int j = 0; j < 5; j++) {
                        starBtns[j].setText(j < selectedStars[0] ? "\u2605" : "\u2606");
                    }
                }
            });
            starRow.add(star);
        }
 
        form.add(starRow);
        form.add(Box.createVerticalStrut(4));
        form.add(conditionDesc);
        form.add(Box.createVerticalStrut(16));
 
      
        JLabel textLbl = new JLabel("Your feedback:");
        textLbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        textLbl.setForeground(COLOR_MUTED);
        textLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        form.add(textLbl);
        form.add(Box.createVerticalStrut(8));
 
        JTextArea area = new JTextArea(existingText != null ? existingText : "");
        area.setFont(new Font("SansSerif", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setRows(5);
        area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(8, 10, 8, 10)));
        area.setAlignmentX(Component.LEFT_ALIGNMENT);
        area.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        form.add(area);
        form.add(Box.createVerticalStrut(20));
 
       
        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
 
        JButton cancelBtn = makeDialogButton("Cancel",
                Color.WHITE, COLOR_TEXT, COLOR_BORDER);
        JButton saveBtn   = makeDialogButton("Save Feedback",
                BLUE_ACCENT, Color.WHITE, BLUE_ACCENT);
 
        cancelBtn.addActionListener(e -> dialog.dispose());
 
        saveBtn.addActionListener(e -> {
            
            if (selectedStars[0] == 0) {
                JOptionPane.showMessageDialog(dialog,
                        "Please select a star rating before saving.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String text = area.getText().trim();
            if (text.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                        "Please enter your feedback text before saving.",
                        "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
 
            String condition = CONDITION_LABELS[selectedStars[0] - 1]; 
 
       
            boolean saved = saveFeedbackToFile(apptId, condition, text, tableRow);
 
            if (saved) {
               
                tableModel.setValueAt(condition, tableRow, 6);
                tableModel.setValueAt(text,      tableRow, 7);
                dialog.dispose();
 
          
                refreshSummaryCards();
 
                JOptionPane.showMessageDialog(app,
                        "Feedback saved successfully!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(dialog,
                        "Failed to save feedback. Please try again.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
 
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);
        form.add(btnRow);
 
        dialog.add(form, BorderLayout.CENTER);
        dialog.setVisible(true);
    }
 

    private boolean saveFeedbackToFile(String apptId, String condition,
                                       String feedbackText, int tableRow) {
        User techUser = app.getLoggedInUserObj();
        if (techUser == null) return false;
 
     
        Appointment target = null;
        for (Appointment a : allMine) {
            if (a.getId().equalsIgnoreCase(apptId)) { target = a; break; }
        }
        if (target == null) return false;
 
        String customerId   = target.getCustomerId();
        String vehicleId    = target.getVehicleId();
        String technicianId = techUser.getUserId();
        String serviceType = target.getServiceType();
        String date         = LocalDate.now().toString();
        String cleanText    = feedbackText.replace(",", ";"); 
 
      
        String feedbackFile = "src" + File.separator + "TxtFile"
                            + File.separator + "feedback.txt";
        File file = new File(feedbackFile);
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
 
        List<String> lines = new ArrayList<>();
        int  existingLineIndex = -1;
        int  highestId = 0;
 
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                int idx = 0;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                    if (!line.isBlank() && !line.startsWith("#")) {
                        String[] parts = line.split(",", 8);
                    
                        if (parts.length >= 1 && parts[0].trim().matches("FB\\d+")) {
                            int n = Integer.parseInt(parts[0].trim().substring(2));
                            if (n > highestId) highestId = n;
                        }
                
                        if (parts.length >= 5
                                && parts[2].trim().equalsIgnoreCase(apptId)
                                && parts[4].trim().equalsIgnoreCase(technicianId)) {
                            existingLineIndex = idx;
                        }
                    }
                    idx++;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
 
        if (existingLineIndex >= 0) {
          
            String[] oldParts = lines.get(existingLineIndex).split(",", 8);
            String updatedLine = oldParts[0].trim() + ","  
                    + oldParts[1].trim() + ","             
                    + apptId + ","
                    + vehicleId + ","
                    + technicianId + ","
                    + condition + ","
                    + serviceType + ","
                    + cleanText+ ","
                    + date;
            lines.set(existingLineIndex, updatedLine);
        } else {
       
            String newId = "FB" + (highestId + 1);
            String newLine = newId + ","
                    + customerId + ","
                    + apptId + ","
                    + vehicleId + ","
                    + technicianId + ","
                    + condition + ","
                    + serviceType + ","
                    + cleanText+ ","
                    + date;
            lines.add(newLine);
        }
 

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String l : lines) {
                bw.write(l);
                bw.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
 

    public void refresh() {
        allMine.clear();
        User user = app.getLoggedInUserObj();
        if (user == null) {
            tableModel.setRowCount(0);
            return;
        }
 
        for (Appointment a : appointmentService.getAll()) {
            if (a.getTechnicianEmail().equalsIgnoreCase(user.getUserId()))
                allMine.add(a);
        }
 
    
        if (searchField != null) searchField.setText("");
        if (conditionFilter != null) conditionFilter.setSelectedIndex(0);
 
        applyFilters();
        refreshSummaryCards();
    }
 

    private void applyFilters() {
        String query    = searchField != null ? searchField.getText().trim().toLowerCase() : "";
        String condSel  = conditionFilter != null
                ? (String) conditionFilter.getSelectedItem() : "All Conditions";
 
        tableModel.setRowCount(0);
 
        User user = app.getLoggedInUserObj();
 
        for (Appointment a : allMine) {
            String custName  = resolveName(a.getCustomerEmail());
            String condition = loadConditionFromFile(a.getId(),
                    user != null ? user.getUserId() : "");
            String fbText    = loadFeedbackTextFromFile(a.getId(),
                    user != null ? user.getUserId() : "");
            String vehicle   = a.getVehicleId();
 
        
            if (!"All Conditions".equals(condSel)
                    && !condSel.equalsIgnoreCase(condition)) continue;
 
         
            if (!query.isEmpty()) {
                boolean match = a.getId().toLowerCase().contains(query)
                        || a.getServiceType().toLowerCase().contains(query)
                        || a.getDateTime().toLowerCase().contains(query)
                        || custName.toLowerCase().contains(query)
                        || vehicle.toLowerCase().contains(query)
                        || a.getStatus().toLowerCase().contains(query)
                        || condition.toLowerCase().contains(query)
                        || fbText.toLowerCase().contains(query);
                if (!match) continue;
            }
 
            tableModel.addRow(new Object[]{
                a.getId(),
                a.getServiceType(),
                a.getDateTime(),
                custName,
                vehicle,
                a.getStatus(),
                condition,
                fbText
            });
        }
    }
 

    private void refreshSummaryCards() {
        if (totalCountLabel == null || barsPanel == null) return;
 
        User user = app.getLoggedInUserObj();
        if (user == null) return;
 

        int total = 0;
        int[] condCounts = new int[5]; 
 
        for (Appointment a : allMine) {
            String cond = loadConditionFromFile(a.getId(), user.getUserId());
            if (!cond.isEmpty()) {
                total++;
                int rank = conditionToStars(cond) - 1; 
                if (rank >= 0 && rank < 5) condCounts[rank]++;
            }
        }
 
        totalCountLabel.setText(String.valueOf(total));
        String word = total == 1 ? "feedback submitted" : "feedbacks submitted";
        totalSubLabel.setText(word);
 
      
        barsPanel.removeAll();
        int max = Math.max(total, 1);
        for (int i = 4; i >= 0; i--) {  
            barsPanel.add(buildOneBarRow(CONDITION_LABELS[i], condCounts[i], max));
            barsPanel.add(Box.createVerticalStrut(6));
        }
        barsPanel.revalidate();
        barsPanel.repaint();
    }
 


    private String loadConditionFromFile(String apptId, String techId) {
        return readFeedbackField(apptId, techId, 5);
    }
 
    private String loadFeedbackTextFromFile(String apptId, String techId) {
        return readFeedbackField(apptId, techId, 6); 
    }
 
    private String readFeedbackField(String apptId, String techId, int fieldIndex) {
        String feedbackFile = "src" + File.separator + "TxtFile"
                            + File.separator + "feedback.txt";
        File file = new File(feedbackFile);
        if (!file.exists()) return "";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(",", 8);
                if (parts.length >= 7
                        && parts[2].trim().equalsIgnoreCase(apptId)
                        && parts[4].trim().equalsIgnoreCase(techId)) {
                    return fieldIndex < parts.length ? parts[fieldIndex].trim() : "";
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return "";
    }
 

    private int conditionToStars(String condition) {
        if (condition == null) return 0;
        switch (condition.trim().toLowerCase()) {
            case "excellent": return 5;
            case "good": return 4;
            case "average": return 3;
            case "poor": return 2;
            case "unsatisfactory": return 1;
            default: return 0;
        }
    }
 

    private Color conditionColor(String condition) {
        if (condition == null || condition.isEmpty()) return COLOR_MUTED;
        switch (condition.toLowerCase()) {
            case "excellent": return new Color(40, 167, 69);
            case "good": return new Color(23, 162, 184);
            case "average": return new Color(255, 193, 7);
            case "poor": return new Color(255, 140, 0);
            case "unsatisfactory": return new Color(220, 53, 69);
            default: return COLOR_MUTED;
        }
    }
 
    private String resolveName(String id) {
        for (User u : app.getAccountService().getAllUsers()) {
            if (u.getUserId() != null && u.getUserId().equalsIgnoreCase(id)) return u.getName();
            if (u.getEmail().equalsIgnoreCase(id)) return u.getName();
        }
        return id;
    }
 

    private JPanel makeRoundedCardWithLeftBar(Color barColor) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(barColor);
                g2.fillRoundRect(0, 0, 10, getHeight(), 14, 14);
                g2.setColor(COLOR_CARD);
                g2.fillRect(5, 0, 10, getHeight());
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 22, 18, 20));
        return card;
    }
 

    private JPanel makeRoundedCard() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 20, 18, 20));
        return card;
    }
 
   
    private JButton makeOutlineButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? BLUE_ACCENT : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BLUE_ACCENT);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 11));
        btn.setForeground(BLUE_ACCENT);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(120, 28));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setForeground(Color.WHITE); }
            public void mouseExited(MouseEvent e) { btn.setForeground(BLUE_ACCENT); }
        });
        return btn;
    }

    private JButton makeDialogButton(String text, Color bg, Color fg, Color border) {
        JButton btn = new JButton(text) {
            private boolean hov = false;
            { addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hov = true;  repaint(); }
                public void mouseExited(MouseEvent e) { hov = false; repaint(); }
            }); }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(border);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 36));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
 