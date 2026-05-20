package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AppointmentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "appointments.txt";

    private static final int EXPECTED_COLUMNS = 8;

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Inner class — one appointment row ─────────────────────────
    public static class Appointment {
        private final String id;
        private final String customerId;
        private final String vehicleId;
        private final String technicianId;
        private final String serviceType;
        private String status;
        private final String dateTime;
        private final int durationHours;

        public Appointment(String id, String customerId, String vehicleId,
                           String technicianId, String serviceType,
                           String status, String dateTime, int durationHours) {
            this.id            = id;
            this.customerId    = customerId;
            this.vehicleId     = vehicleId;
            this.technicianId  = technicianId;
            this.serviceType   = serviceType;
            this.status        = status;
            this.dateTime      = dateTime;
            this.durationHours = durationHours;
        }

        public String getId()             { return id; }
        public String getCustomerId()     { return customerId; }
        public String getVehicleId()      { return vehicleId; }
        public String getTechnicianId()   { return technicianId; }
        public String getServiceType()    { return serviceType; }
        public String getStatus()         { return status; }
        public void   setStatus(String s) { this.status = s; }
        public String getDateTime()       { return dateTime; }
        public int    getDurationHours()  { return durationHours; }

        // Keep old getter names so existing code still compiles
        public String getCustomerEmail()   { return customerId; }
        public String getTechnicianEmail() { return technicianId; }
    }

    // ── Read all appointments from the appointments.txt ───────────────────────
    public List<Appointment> getAll() {
        List<Appointment> list = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return list;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                String[] cols = line.split(",", EXPECTED_COLUMNS);
                if (cols.length != EXPECTED_COLUMNS) continue;

                int duration;
                try { duration = Integer.parseInt(cols[7].trim()); }
                catch (NumberFormatException e) { duration = 1; }

                list.add(new Appointment(
                        cols[0].trim(), // [0] appointmentID
                        cols[1].trim(), // [1] customerID
                        cols[2].trim(), // [2] vehicleID
                        cols[3].trim(), // [3] technicianID
                        cols[4].trim(), // [4] serviceType
                        cols[5].trim(), // [5] status
                        cols[6].trim(), // [6] dateTime
                        duration        // [7] durationHours
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Add a new appointment to the file ─────────────────────────
    public boolean add(Appointment appt) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.newLine();
            writer.write(String.join(",",
                    appt.getId(),
                    appt.getCustomerId(),
                    appt.getVehicleId(),
                    appt.getTechnicianId(),
                    appt.getServiceType(),
                    appt.getStatus(),
                    appt.getDateTime(),
                    String.valueOf(appt.getDurationHours())
            ));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ── Delete an appointment by ID ───────────────────────────────
    public boolean delete(String id) {
        List<String> lines = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        boolean found = false;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank() && !line.trim().startsWith("#")) {
                    String[] cols = line.split(",", 2);
                    if (cols[0].trim().equals(id)) { found = true; continue; }
                }
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!found) return false;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (int i = 0; i < lines.size(); i++) {
                if (i > 0) writer.newLine();
                writer.write(lines.get(i));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════
    // deletePendingByVehicleId()
    // ═══════════════════════════════════════════════════════════════
    public boolean deletePendingByVehicleId(String vehicleId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        List<String> linesToKeep = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {

                // Always keep blank lines and comment lines unchanged
                if (line.isBlank() || line.trim().startsWith("#")) {
                    linesToKeep.add(line);
                    continue;
                }

                String[] cols = line.split(",", EXPECTED_COLUMNS);

                // If the line does not have the right number of columns,
                // keep it as-is to avoid corrupting the file
                if (cols.length != EXPECTED_COLUMNS) {
                    linesToKeep.add(line);
                    continue;
                }

                // cols[2] = vehicleID   cols[5] = status
                String lineVehicleId = cols[2].trim();
                String lineStatus    = cols[5].trim();

                boolean isThisVehicle = lineVehicleId.equalsIgnoreCase(vehicleId);
                boolean isNotYetDone  = lineStatus.equalsIgnoreCase("Pending")
                                     || lineStatus.equalsIgnoreCase("In Progress");

                if (isThisVehicle && isNotYetDone) {
                    // DROP this line — appointment is cancelled because vehicle is deleted
                    System.out.println("[deletePendingByVehicleId] Removed appointment: "
                            + cols[0].trim() + " (vehicleId=" + vehicleId
                            + ", status=" + lineStatus + ")");
                } else {
                    linesToKeep.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        // Rewrite appointments.txt with only the kept lines
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
            for (int i = 0; i < linesToKeep.size(); i++) {
                writer.write(linesToKeep.get(i));
                if (i < linesToKeep.size() - 1) {
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    // ── Generate the next appointment ID ──────────────────────────
    public String nextId() {
        int max = 0;
        for (Appointment a : getAll()) {
            String num = a.getId().replaceAll("[^0-9]", "");
            try {
                int n = Integer.parseInt(num);
                if (n > max) max = n;
            } catch (NumberFormatException ignored) {}
        }
        return "AP" + (max + 1);
    }

    // ═══════════════════════════════════════════════════════════════
    // getPendingAppointments — for the Upcoming Appointments card
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getPendingAppointments(String userId) {
        List<String[]> result = new ArrayList<>();

        // Get the current date and time in Malaysia timezone
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        for (Appointment a : getAll()) {

            // Step 1: Skip appointments that don't belong to this customer
            if (!a.getCustomerId().equalsIgnoreCase(userId)) continue;

            String status = a.getStatus();

            // Step 2: We only care about "Pending" or "In Progress" appointments.
            //         "Completed" appointments go to the Payment section, not here.
            if (!status.equalsIgnoreCase("Pending")
                    && !status.equalsIgnoreCase("In Progress")) {
                continue;
            }

            // Step 3: Try to read the appointment's start date and time
            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(a.getDateTime(), DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                // If the date is badly formatted, skip this appointment safely
                System.out.println("[getPending] Bad date format, skipping: " + a.getDateTime());
                continue;
            }

            // Step 4: Calculate when the appointment ENDS
            //         end time = start time + how many hours the service takes
            LocalDateTime endTime = startTime.plusHours(a.getDurationHours());

            // Step 5: Only show the appointment if it has NOT ended yet.
            //         endTime.isAfter(now) means the appointment is still upcoming.
            //         If endTime is before now, the appointment has passed — hide it.
            if (endTime.isAfter(now)) {
                result.add(toRow(a));
            }
        }

        // Step 6: Sort so the soonest appointment appears at the top
        result.sort((rowA, rowB) -> rowA[5].compareTo(rowB[5]));

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // getUnpaidAppointments — for the Pending Payment card
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getUnpaidAppointments(String userId, Set<String> paidIds) {
        List<String[]> result = new ArrayList<>();

        for (Appointment a : getAll()) {
            if (!a.getCustomerId().equalsIgnoreCase(userId)) continue;

            boolean isCompleted = a.getStatus().equalsIgnoreCase("Completed");
            boolean notPaid     = !paidIds.contains(a.getId());

            if (isCompleted && notPaid) {
                result.add(toRow(a));
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // getAllAppointmentsForUser — for dashboard chart totals
    // ═══════════════════════════════════════════════════════════════
    public List<String[]> getAllAppointmentsForUser(String userId) {
        List<String[]> result = new ArrayList<>();
        for (Appointment a : getAll()) {
            if (a.getCustomerId().equalsIgnoreCase(userId)) {
                result.add(toRow(a));
            }
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // toRow() — converts an Appointment to a String[] for the UI
    // ─────────────────────────────────────────────────────────────
    private String[] toRow(Appointment a) {
        return new String[]{
            a.getId(),                           // [0] appointmentID
            a.getVehicleId(),                    // [1] vehicleID
            a.getTechnicianId(),                 // [2] technicianID
            a.getServiceType(),                  // [3] serviceType
            a.getStatus(),                       // [4] status
            a.getDateTime(),                     // [5] dateTime
            String.valueOf(a.getDurationHours()) // [6] duration
        };
    }

    // ─────────────────────────────────────────────────────────────
    // update() — replaces editable fields of an existing appointment
    // (status and vehicleId are preserved)
    // ─────────────────────────────────────────────────────────────
    public boolean update(String id, String customerId, String technicianId,
                          String serviceType, String dateTime, int durationHours) {
        List<Appointment> all = getAll();
        boolean found = false;
        for (int i = 0; i < all.size(); i++) {
            Appointment a = all.get(i);
            if (a.getId().equalsIgnoreCase(id)) {
                Appointment replacement = new Appointment(
                        a.getId(),
                        customerId,
                        a.getVehicleId(),
                        technicianId,
                        serviceType,
                        a.getStatus(),
                        dateTime,
                        durationHours);
                all.set(i, replacement);
                found = true;
                break;
            }
        }
        return found && saveAll(all);
    }

    // ─────────────────────────────────────────────────────────────
    // updateStatus() — used by Technician Dashboard
    // ─────────────────────────────────────────────────────────────
    public boolean updateStatus(String id, String newStatus) {
        List<Appointment> all = getAll();
        boolean found = false;
        for (Appointment a : all) {
            if (a.getId().equalsIgnoreCase(id)) {
                a.setStatus(newStatus);
                found = true;
                break;
            }
        }
        return found && saveAll(all);
    }

    // ─────────────────────────────────────────────────────────────
    // Feedback and Comment methods
    // ─────────────────────────────────────────────────────────────
    public boolean saveFeedback(String appointmentId, String feedbackText) {
        return saveEntry(appointmentId, "feedback", feedbackText);
    }

    public String getFeedback(String appointmentId) {
        return getEntry(appointmentId, "feedback");
    }

    public boolean saveComment(String appointmentId, String commentText) {
        return saveEntry(appointmentId, "comment", commentText);
    }

    public String getComment(String appointmentId) {
        return getEntry(appointmentId, "comment");
    }

    private static final String FEEDBACK_FILE = "src" + java.io.File.separator
            + "TxtFile" + java.io.File.separator + "feedback.txt";

    private List<String> loadFeedbackLines() {
        List<String> lines = new java.util.ArrayList<>();
        java.io.File file = new java.io.File(FEEDBACK_FILE);
        if (!file.exists()) return lines;
        try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.isBlank() && !line.startsWith("#")) lines.add(line);
            }
        } catch (java.io.IOException e) { e.printStackTrace(); }
        return lines;
    }

    private boolean saveEntry(String appointmentId, String type, String text) {
        List<String> lines = loadFeedbackLines();
        String prefix = appointmentId + "," + type + ",";
        boolean found = false;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                lines.set(i, prefix + text);
                found = true;
                break;
            }
        }
        if (!found) lines.add(prefix + text);
        java.io.File file = new java.io.File(FEEDBACK_FILE);
        try {
            java.io.File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (java.io.BufferedWriter bw = new java.io.BufferedWriter(
                    new java.io.FileWriter(file, false))) {
                for (String l : lines) { bw.write(l); bw.newLine(); }
            }
            return true;
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getEntry(String appointmentId, String type) {
        String prefix = appointmentId + "," + type + ",";
        for (String line : loadFeedbackLines()) {
            if (line.startsWith(prefix)) return line.substring(prefix.length());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────
    // saveAll() — rewrites appointments.txt with an updated list
    // ─────────────────────────────────────────────────────────────
    private boolean saveAll(List<Appointment> list) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (Appointment a : list) {
                writer.write(a.getId()           + ","
                           + a.getCustomerId()   + ","
                           + a.getVehicleId()    + ","
                           + a.getTechnicianId() + ","
                           + a.getServiceType()  + ","
                           + a.getStatus()       + ","
                           + a.getDateTime()     + ","
                           + a.getDurationHours());
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}