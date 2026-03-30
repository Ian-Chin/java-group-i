package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * AppointmentService reads/writes appointment data from appointments.txt.
 *
 * File format — each line has 8 values:
 *   AppointmentID, CustomerID, VehicleID, TechnicianID,
 *   ServiceType, Status, DateTime, Duration(Hours)
 *
 * Example:
 *   AP4,C3,V4,T3,Major Service,Completed,2026-03-26 13:00,3
 *
 * NEW METHOD ADDED:
 *   deletePendingByVehicleId(vehicleId) — when a vehicle is deleted,
 *   this removes ALL appointments for that vehicle whose status is
 *   "Pending". Appointments that are "Completed" are left alone
 *   because the customer still owes payment for those services.
 */
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

    // ── Read all appointments from the file ───────────────────────
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
    // deletePendingByVehicleId()  ← NEW METHOD
    // ═══════════════════════════════════════════════════════════════
    /**
     * When a vehicle is deleted, this method removes ALL appointments
     * for that vehicle whose status is "Pending".
     *
     * WHY only "Pending"?
     *   - "Pending" means the service has NOT happened yet.
     *     There is nothing to pay, so deleting these is safe and correct.
     *   - "Completed" means the service already happened and the customer
     *     owes payment. These must stay in appointments.txt so they
     *     still appear in the Pending Payment card.
     *   - "In Progress" is treated the same as "Pending" — the appointment
     *     was scheduled but removing the vehicle cancels it.
     *
     * This method is called automatically by VehicleService.deleteVehicle()
     * so the caller does not need to remember to call it separately.
     *
     * @param vehicleId  the ID of the vehicle being deleted, e.g. "V4"
     * @return true if the file was rewritten successfully (even if 0 rows removed)
     */
    public boolean deletePendingByVehicleId(String vehicleId) {
        File file = new File(FILE_PATH);
        if (!file.exists()) return false;

        // Read every line and decide which ones to keep
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

                // RULE: Remove only if BOTH conditions are true:
                //   1. The vehicleID matches the deleted vehicle
                //   2. The status is "Pending" or "In Progress"
                //      (these are future/active appointments — not yet done)
                boolean isThisVehicle  = lineVehicleId.equalsIgnoreCase(vehicleId);
                boolean isNotYetDone   = lineStatus.equalsIgnoreCase("Pending")
                                      || lineStatus.equalsIgnoreCase("In Progress");

                if (isThisVehicle && isNotYetDone) {
                    // DROP this line — appointment is cancelled because vehicle is deleted
                    System.out.println("[deletePendingByVehicleId] Removed appointment: "
                            + cols[0].trim() + " (vehicleId=" + vehicleId
                            + ", status=" + lineStatus + ")");
                } else {
                    // KEEP this line — either a different vehicle, or a Completed appointment
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
    // AUTO-COMPLETE EXPIRED APPOINTMENTS
    // ═══════════════════════════════════════════════════════════════
    /**
     * Scans all appointments. For any that are "Pending" or "In Progress"
     * and whose end time has already passed, this method:
     *   1. Changes status to "Completed" in appointments.txt
     *   2. Adds a new record in serviceHistory.txt
     */
    public void autoCompleteExpiredAppointments(ServiceHistoryService serviceHistoryService) {
        LocalDateTime nowMalaysia = LocalDateTime.now(
                java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        List<Appointment> allAppointments = getAll();
        boolean anyChanges = false;

        for (Appointment appt : allAppointments) {
            String status = appt.getStatus();
            boolean isActive = status.equalsIgnoreCase("Pending")
                            || status.equalsIgnoreCase("In Progress");
            if (!isActive) continue;

            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(appt.getDateTime(), DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                System.out.println("[AutoComplete] Bad date format: " + appt.getDateTime());
                continue;
            }

            LocalDateTime endTime = startTime.plusHours(appt.getDurationHours());

            if (endTime.isBefore(nowMalaysia)) {
                appt.setStatus("Completed");
                anyChanges = true;

                if (!serviceHistoryService.appointmentAlreadyRecorded(appt.getId())) {
                    String serviceDate = appt.getDateTime().contains(" ")
                            ? appt.getDateTime().split(" ")[0]
                            : appt.getDateTime();

                    serviceHistoryService.addRecord(
                            appt.getCustomerId(),
                            appt.getId(),
                            appt.getVehicleId(),
                            "NULL",
                            appt.getTechnicianId(),
                            serviceDate
                    );

                    System.out.println("[AutoComplete] Marked " + appt.getId() + " as Completed.");
                }
            }
        }

        if (anyChanges) {
            saveAll(allAppointments);
        }
    }

    // Rewrites appointments.txt with the updated list
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

    // ═══════════════════════════════════════════════════════════════
    // getPendingAppointments — for the Upcoming Appointments card
    // ═══════════════════════════════════════════════════════════════
    /**
     * Returns upcoming appointments for a customer.
     * "In Progress" = always shown.
     * "Pending"     = only shown if start time is in the future.
     *
     * Returned array has 7 elements:
     *   [0] appointmentID  [1] vehicleID  [2] technicianID
     *   [3] serviceType    [4] status     [5] dateTime     [6] duration
     */
    public List<String[]> getPendingAppointments(String userId) {
        List<String[]> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        for (Appointment a : getAll()) {
            if (!a.getCustomerId().equalsIgnoreCase(userId)) continue;

            String status = a.getStatus();
            if (status.equalsIgnoreCase("In Progress")) {
                result.add(toRow(a));
            } else if (status.equalsIgnoreCase("Pending")) {
                try {
                    LocalDateTime startTime = LocalDateTime.parse(a.getDateTime(), DATE_TIME_FORMAT);
                    if (startTime.isAfter(now)) {
                        result.add(toRow(a));
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("[getPending] Bad date: " + a.getDateTime());
                }
            }
        }

        result.sort((rowA, rowB) -> rowA[5].compareTo(rowB[5]));
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // getUnpaidAppointments — for the Pending Payment card
    // ═══════════════════════════════════════════════════════════════
    /**
     * Returns completed but unpaid appointments for a customer.
     *
     * Returned array has 7 elements:
     *   [0] appointmentID  [1] vehicleID  [2] technicianID
     *   [3] serviceType    [4] status     [5] dateTime     [6] duration
     */
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
}