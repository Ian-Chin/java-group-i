package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * MyFeedbackService
 *
 * Reads feedback data from feedback.txt and looks up names and vehicle
 * info from accounts.txt and vehicles.txt.
 *
 * feedback.txt format — 9 fields separated by commas:
 *   [0] feedbackID
 *   [1] customerID
 *   [2] appointmentID
 *   [3] vehicleID
 *   [4] technicianID
 *   [5] condition        e.g. "Good", "Excellent", "Average"
 *   [6] feedbackText     e.g. "Full service completed - engine oil changed..."
 *   [7] date             e.g. "2026-03-05"
 *
 * Example line:
 *   FB1,C1,AP1,V1,T1,Good,Full service completed - engine oil changed.,2026-03-05
 *
 * appointments.txt is also read so we can look up the service type
 * that belongs to a given appointmentID (used for the "Feedback" button table).
 */
public class MyFeedbackService {

    // ── File paths ────────────────────────────────────────────────
    private static final String FEEDBACK_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "feedback.txt";

    private static final String ACCOUNTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "accounts.txt";

    private static final String VEHICLES_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "vehicles.txt";

    private static final String APPOINTMENTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "appointments.txt";

    // How many columns we expect per line in feedback.txt
    private static final int EXPECTED_COLUMNS = 8;

    // ─────────────────────────────────────────────────────────────
    // MyFeedback — holds the data for ONE feedback row
    // ─────────────────────────────────────────────────────────────
    public static class MyFeedback {
        public String feedbackId;       // e.g. "FB1"
        public String customerId;       // e.g. "C1"
        public String appointmentId;    // e.g. "AP1"
        public String vehicleId;        // e.g. "V1"
        public String vehicleType;      // e.g. "Car"    — looked up from vehicles.txt
        public String carPlate;         // e.g. "WXY1234" — looked up from vehicles.txt
        public String technicianId;     // e.g. "T1"
        public String technicianName;   // e.g. "Mike Tan" — looked up from accounts.txt
        public String condition;        // e.g. "Good"
        public String feedbackText;     // e.g. "Full service completed..."
        public String date;             // e.g. "2026-03-05"
    }

    // ─────────────────────────────────────────────────────────────
    // AppointmentRow — lightweight object for the "pending feedback"
    // table — shows completed appointments the customer has NOT yet
    // submitted feedback for.
    // ─────────────────────────────────────────────────────────────
    public static class AppointmentRow {
        public String appointmentId;   // e.g. "AP1"
        public String serviceType;     // e.g. "Oil Change"
        public String dateTime;        // e.g. "2026-03-05 10:00"
        public String duration;        // e.g. "1"
        public String vehicleId;       // e.g. "V1"
        public String technicianId;    // e.g. "T1"
    }

    // ═════════════════════════════════════════════════════════════
    // PUBLIC — READ FEEDBACK
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // getFeedbackByCustomer()
    //
    // Returns all feedback rows that belong to the given customer.
    // Used by MyFeedbackPage to populate the history table.
    // ─────────────────────────────────────────────────────────────
    public List<MyFeedback> getFeedbackByCustomer(String customerId) {

        List<MyFeedback> result = new ArrayList<>();

        if (customerId == null || customerId.trim().isEmpty()) {
            return result;
        }

        File file = new File(FEEDBACK_FILE);
        System.out.println("[MyFeedbackService] Looking for: " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("[MyFeedbackService] feedback.txt not found!");
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip blank lines and comment lines starting with #
                if (line.isEmpty() || line.startsWith("#")) continue;

                MyFeedback fb = parseFeedbackLine(line, lineNumber);
                if (fb == null) continue;

                // Only keep rows for THIS customer
                if (fb.customerId.equalsIgnoreCase(customerId)) {
                    result.add(fb);
                }
            }

        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error reading feedback.txt: " + e.getMessage());
        }

        System.out.println("[MyFeedbackService] Feedback found for " + customerId + ": " + result.size());
        return result;
    }

    // ═════════════════════════════════════════════════════════════
    // PUBLIC — WRITE FEEDBACK
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // saveFeedback()
    //
    // Appends a new feedback row to feedback.txt.
    // Returns true if saved successfully, false otherwise.
    //
    // Parameters:
    //   customerId    — e.g. "C1"
    //   appointmentId — e.g. "AP1"
    //   vehicleId     — e.g. "V1"
    //   technicianId  — e.g. "T1"
    //   condition     — e.g. "Good", "Excellent", "Average"
    //   feedbackText  — the customer's written feedback
    //   date          — e.g. "2026-03-05"
    // ─────────────────────────────────────────────────────────────
    public boolean saveFeedback(String customerId, String appointmentId,
                                String vehicleId, String technicianId,
                                String condition, String feedbackText, String date) {

        File file = new File(FEEDBACK_FILE);

        // Create the parent directory if it does not exist yet
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        String newId = generateNextFeedbackId();

        // Clean feedback text — replace any commas inside it with semicolons
        // so we don't accidentally break the CSV format
        String cleanText = feedbackText.replace(",", ";");

        String newLine = newId + ","
                + customerId.trim() + ","
                + appointmentId.trim() + ","
                + vehicleId.trim() + ","
                + technicianId.trim() + ","
                + condition.trim() + ","
                + cleanText.trim() + ","
                + date.trim();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
            writer.write(newLine);
            writer.newLine();
            System.out.println("[MyFeedbackService] Saved: " + newLine);
            return true;
        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error writing feedback.txt: " + e.getMessage());
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // hasFeedbackForAppointment()
    //
    // Returns true if feedback.txt already has a row for this
    // appointmentId + customerId combination.
    // Used to hide the Feedback button for appointments already reviewed.
    // ─────────────────────────────────────────────────────────────
    public boolean hasFeedbackForAppointment(String customerId, String appointmentId) {
        File file = new File(FEEDBACK_FILE);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split into at least 3 parts to read customerId and appointmentId
                String[] parts = line.split(",", EXPECTED_COLUMNS);
                if (parts.length >= 3) {
                    boolean customerMatch     = parts[1].trim().equalsIgnoreCase(customerId);
                    boolean appointmentMatch  = parts[2].trim().equalsIgnoreCase(appointmentId);
                    if (customerMatch && appointmentMatch) return true;
                }
            }
        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error in hasFeedbackForAppointment: " + e.getMessage());
        }

        return false;
    }

    // ═════════════════════════════════════════════════════════════
    // PUBLIC — READ COMPLETED APPOINTMENTS (for the feedback button table)
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // getCompletedAppointmentsWithoutFeedback()
    //
    // Reads appointments.txt and returns completed appointments for
    // this customer that do NOT yet have a feedback entry.
    //
    // appointments.txt format (8 columns):
    //   [0] appointmentID  [1] customerID  [2] vehicleID  [3] technicianID
    //   [4] serviceType    [5] status      [6] dateTime   [7] duration
    // ─────────────────────────────────────────────────────────────
    public List<AppointmentRow> getCompletedAppointmentsWithoutFeedback(String customerId) {

        List<AppointmentRow> result = new ArrayList<>();

        if (customerId == null || customerId.trim().isEmpty()) {
            return result;
        }

        File file = new File(APPOINTMENTS_FILE);
        if (!file.exists()) {
            System.out.println("[MyFeedbackService] appointments.txt not found.");
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // Split into 8 columns
                String[] cols = line.split(",", 8);
                if (cols.length < 8) continue;

                String apptId       = cols[0].trim();
                String apptCustId   = cols[1].trim();
                String vehicleId    = cols[2].trim();
                String techId       = cols[3].trim();
                String serviceType  = cols[4].trim();
                String status       = cols[5].trim();
                String dateTime     = cols[6].trim();
                String duration     = cols[7].trim();

                // Only include Completed appointments for THIS customer
                // that do not already have feedback submitted
                boolean isThisCustomer  = apptCustId.equalsIgnoreCase(customerId);
                boolean isCompleted     = status.equalsIgnoreCase("Completed");
                boolean noFeedbackYet   = !hasFeedbackForAppointment(customerId, apptId);

                if (isThisCustomer && isCompleted && noFeedbackYet) {
                    AppointmentRow row = new AppointmentRow();
                    row.appointmentId = apptId;
                    row.serviceType   = serviceType;
                    row.dateTime      = dateTime;
                    row.duration      = duration;
                    row.vehicleId     = vehicleId;
                    row.technicianId  = techId;
                    result.add(row);
                }
            }
        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error reading appointments.txt: " + e.getMessage());
        }

        System.out.println("[MyFeedbackService] Pending feedback appointments for "
                + customerId + ": " + result.size());
        return result;
    }

    // ═════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═════════════════════════════════════════════════════════════

    // ─────────────────────────────────────────────────────────────
    // parseFeedbackLine()
    //
    // Parses one line from feedback.txt into a MyFeedback object.
    // Returns null if the line is badly formatted.
    //
    // Expected format (8 fields):
    //   [0] feedbackID  [1] customerID  [2] appointmentID  [3] vehicleID
    //   [4] technicianID  [5] condition  [6] feedbackText  [7] date
    // ─────────────────────────────────────────────────────────────
    private MyFeedback parseFeedbackLine(String line, int lineNumber) {

        // Split by comma, limit to EXPECTED_COLUMNS so feedback text (field 6)
        // can safely contain commas (they were replaced with semicolons on save,
        // but we support commas here too as a safety net via the limit).
        String[] parts = line.split(",", EXPECTED_COLUMNS);

        if (parts.length < EXPECTED_COLUMNS) {
            System.out.println("[MyFeedbackService] Line " + lineNumber
                    + " skipped — expected " + EXPECTED_COLUMNS
                    + " fields, got " + parts.length + ": [" + line + "]");
            return null;
        }

        MyFeedback fb = new MyFeedback();
        fb.feedbackId    = parts[0].trim();
        fb.customerId    = parts[1].trim();
        fb.appointmentId = parts[2].trim();
        fb.vehicleId     = parts[3].trim();
        fb.technicianId  = parts[4].trim();
        fb.condition     = parts[5].trim();
        fb.feedbackText  = parts[6].trim();
        fb.date          = parts[7].trim();

        // Look up human-readable names and vehicle info
        fb.technicianName = lookUpName(fb.technicianId);
        String[] vInfo    = lookUpVehicle(fb.vehicleId);
        fb.vehicleType    = vInfo[0];
        fb.carPlate       = vInfo[1];

        return fb;
    }

    // ─────────────────────────────────────────────────────────────
    // lookUpName()
    //
    // Finds a user's name by their ID in accounts.txt.
    // Returns the ID itself as a fallback if not found.
    //
    // accounts.txt format:
    //   [0] userID, [1] name, [2] email, [3] password, [4] role, [5] profilePic
    // ─────────────────────────────────────────────────────────────
    private String lookUpName(String userId) {
        if (userId == null || userId.isEmpty()) return "Unknown";

        File file = new File(ACCOUNTS_FILE);
        if (!file.exists()) return userId;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",", -1);
                if (parts.length >= 2 && parts[0].trim().equalsIgnoreCase(userId)) {
                    return parts[1].trim();
                }
            }
        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error reading accounts.txt: " + e.getMessage());
        }

        return userId; // fallback
    }

    // ─────────────────────────────────────────────────────────────
    // lookUpVehicle()
    //
    // Finds a vehicle's type and plate by vehicleID in vehicles.txt.
    // Returns ["Unknown", "Unknown"] if not found.
    //
    // vehicles.txt format:
    //   [0] vehicleID, [1] userID, [2] vehicleType, [3] plate,
    //   [4] brand, [5] year, [6] colour
    // ─────────────────────────────────────────────────────────────
    private String[] lookUpVehicle(String vehicleId) {
        String[] fallback = { "Unknown", "Unknown" };
        if (vehicleId == null || vehicleId.isEmpty()) return fallback;

        File file = new File(VEHICLES_FILE);
        if (!file.exists()) return fallback;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] parts = line.split(",", 7);
                if (parts.length >= 4 && parts[0].trim().equalsIgnoreCase(vehicleId)) {
                    return new String[]{
                            parts[2].trim(), // vehicleType e.g. "Car"
                            parts[3].trim()  // plate       e.g. "WXY1234"
                    };
                }
            }
        } catch (IOException e) {
            System.out.println("[MyFeedbackService] Error reading vehicles.txt: " + e.getMessage());
        }

        return fallback;
    }

    // ─────────────────────────────────────────────────────────────
    // generateNextFeedbackId()
    //
    // Reads feedback.txt, finds the highest existing FB number,
    // and returns the next ID e.g. "FB7" if "FB6" already exists.
    // ─────────────────────────────────────────────────────────────
    private String generateNextFeedbackId() {
        int highest = 0;
        File file = new File(FEEDBACK_FILE);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;
                    String[] parts = line.split(",", 2);
                    if (parts.length >= 1) {
                        String id = parts[0].trim();
                        // FB IDs start with "FB" followed by a number
                        if (id.matches("FB\\d+")) {
                            int n = Integer.parseInt(id.substring(2));
                            if (n > highest) highest = n;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("[MyFeedbackService] Error generating next ID: " + e.getMessage());
            }
        }

        return "FB" + (highest + 1);
    }
}