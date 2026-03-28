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
 * File format — each line has 7 values:
 *   AppointmentID, CustomerID, TechnicianID, ServiceType, Status, DateTime, Duration(Hours)
 *
 * Example:
 *   AP4,C3,T3,Major Service,In Progress,2026-03-26 13:00,3
 *
 * KEY FEATURE — Auto-complete:
 *   When autoCompleteExpiredAppointments() is called on login, it scans every
 *   appointment that is "In Progress" or "Pending".
 *   If the end time (start time + duration hours) has already passed the
 *   current Malaysia clock time, it marks that appointment as "Completed"
 *   in appointments.txt and writes a new record in serviceHistory.txt.
 *   This means the appointment will automatically move from "Upcoming" to
 *   "Pending Payment" so the customer can pay for it.
 */
public class AppointmentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "appointments.txt";

    private static final int EXPECTED_COLUMNS = 7;

    // The date-time format used in appointments.txt: "2026-03-05 09:00"
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ── Inner class — one appointment row ─────────────────────────
    public static class Appointment {
        private final String id;
        private final String customerEmail;    // stores customerID e.g. "C3"
        private final String technicianEmail;  // stores technicianID e.g. "T1"
        private final String serviceType;
        private String status;
        private final String dateTime;
        private final int durationHours;

        public Appointment(String id, String customerEmail, String technicianEmail,
                           String serviceType, String status,
                           String dateTime, int durationHours) {
            this.id             = id;
            this.customerEmail  = customerEmail;
            this.technicianEmail = technicianEmail;
            this.serviceType    = serviceType;
            this.status         = status;
            this.dateTime       = dateTime;
            this.durationHours  = durationHours;
        }

        public String getId()              { return id; }
        public String getCustomerEmail()   { return customerEmail; }
        public String getTechnicianEmail() { return technicianEmail; }
        public String getServiceType()     { return serviceType; }
        public String getStatus()          { return status; }
        public void   setStatus(String s)  { this.status = s; }
        public String getDateTime()        { return dateTime; }
        public int    getDurationHours()   { return durationHours; }
    }

    // ── Read all appointments ─────────────────────────────────────
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
                try { duration = Integer.parseInt(cols[6].trim()); }
                catch (NumberFormatException e) { duration = 1; }

                list.add(new Appointment(
                        cols[0].trim(), // appointmentID
                        cols[1].trim(), // customerID
                        cols[2].trim(), // technicianID
                        cols[3].trim(), // serviceType
                        cols[4].trim(), // status
                        cols[5].trim(), // dateTime
                        duration        // durationHours
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ── Add a new appointment ─────────────────────────────────────
    public boolean add(Appointment appt) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            writer.newLine();
            writer.write(String.join(",",
                    appt.getId(),
                    appt.getCustomerEmail(),
                    appt.getTechnicianEmail(),
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

    // ── Generate the next appointment ID ─────────────────────────
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
     * Scans ALL appointments and auto-marks any expired ones as "Completed".
     *
     * An appointment is "expired" (i.e., the service should be done) when:
     *   current Malaysia time  >  appointment start time + duration hours
     *
     * Example:
     *   AP4: start = 2026-03-26 13:00, duration = 3 hours
     *   End time = 2026-03-26 16:00
     *   If current time is 2026-03-27 (after end time), mark as Completed.
     *
     * For each expired appointment, we:
     *   1. Change its status to "Completed" in appointments.txt
     *   2. Add a new record to serviceHistory.txt (with PaymentID = "NULL")
     *
     * This method is called once when the customer logs in.
     * It is safe to call multiple times — it skips already-Completed appointments.
     *
     * @param serviceHistoryService  used to write new rows to serviceHistory.txt
     */
    public void autoCompleteExpiredAppointments(ServiceHistoryService serviceHistoryService) {
        // Get current Malaysia time (UTC+8)
        // LocalDateTime.now() uses the system clock, which on any real device is
        // already in local time. For a PC set to Malaysia time, this is correct.
        LocalDateTime nowMalaysia = LocalDateTime.now(
                java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        // Read ALL appointments from the file
        List<Appointment> allAppointments = getAll();

        // Track whether we actually changed anything (so we only rewrite if needed)
        boolean anyChanges = false;

        for (Appointment appt : allAppointments) {

            // Only check appointments that are still "active"
            // (Pending = booked, In Progress = currently at garage)
            String status = appt.getStatus();
            boolean isActive = status.equalsIgnoreCase("Pending")
                            || status.equalsIgnoreCase("In Progress");

            if (!isActive) continue; // skip Completed ones — already done

            // Parse the appointment's start date-time, e.g. "2026-03-26 13:00"
            LocalDateTime startTime;
            try {
                startTime = LocalDateTime.parse(appt.getDateTime(), DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                // If the date format is invalid, skip this appointment safely
                System.out.println("[AutoComplete] Bad date format: " + appt.getDateTime());
                continue;
            }

            // Calculate when the appointment ENDS
            // e.g. start = 2026-03-26 13:00, duration = 3 hours → end = 2026-03-26 16:00
            LocalDateTime endTime = startTime.plusHours(appt.getDurationHours());

            // Is the end time already in the past?
            // isBefore(nowMalaysia) = true if end time < current time
            if (endTime.isBefore(nowMalaysia)) {

                // The appointment has ended — mark it as Completed
                appt.setStatus("Completed");
                anyChanges = true;

                // Write a new serviceHistory record (PaymentID = "NULL" until customer pays)
                // Only add if this appointment isn't already in serviceHistory.txt
                if (!serviceHistoryService.appointmentAlreadyRecorded(appt.getId())) {

                    // Extract just the date part from dateTime, e.g. "2026-03-26 13:00" → "2026-03-26"
                    String serviceDate = appt.getDateTime().contains(" ")
                            ? appt.getDateTime().split(" ")[0]  // "2026-03-26"
                            : appt.getDateTime();

                    serviceHistoryService.addRecord(
                            appt.getCustomerEmail(),    // customerID e.g. "C3"
                            appt.getId(),               // appointmentID e.g. "AP4"
                            "NULL",                     // no payment yet
                            appt.getTechnicianEmail(),  // technicianID e.g. "T3"
                            serviceDate                 // "2026-03-26"
                    );

                    System.out.println("[AutoComplete] Marked " + appt.getId() + " as Completed.");
                }
            }
        }

        // If any appointments changed status, rewrite the whole file
        if (anyChanges) {
            saveAll(allAppointments);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // saveAll() — rewrites appointments.txt with the given list.
    // Used by autoCompleteExpiredAppointments() after making changes.
    // ─────────────────────────────────────────────────────────────
    private boolean saveAll(List<Appointment> list) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (int i = 0; i < list.size(); i++) {
                Appointment a = list.get(i);
                // Write each appointment as one CSV line
                writer.write(a.getId() + ","
                           + a.getCustomerEmail() + ","
                           + a.getTechnicianEmail() + ","
                           + a.getServiceType() + ","
                           + a.getStatus() + ","
                           + a.getDateTime() + ","
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
    // getPendingAppointments — Upcoming Appointments card
    // ═══════════════════════════════════════════════════════════════
    /**
     * Returns "upcoming" appointments for a given customer.
     *
     * "UPCOMING" definition:
     *   - "In Progress" → ALWAYS shown (service is currently happening).
     *     Even if the date is slightly in the past, the job is still active.
     *   - "Pending" → only shown if the start date is in the FUTURE.
     *     A pending appointment that is already past is handled by auto-complete.
     *
     * Results are sorted earliest-first.
     *
     * Returned array per row (6 elements):
     *   [0] appointmentID  [1] technicianID  [2] serviceType
     *   [3] status         [4] dateTime      [5] duration (String)
     *
     * @param userId  customer ID e.g. "C3"
     */
    public List<String[]> getPendingAppointments(String userId) {
        List<String[]> result = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        for (Appointment a : getAll()) {

            // Only include appointments belonging to this customer
            if (!a.getCustomerEmail().equalsIgnoreCase(userId)) continue;

            String status = a.getStatus();

            if (status.equalsIgnoreCase("In Progress")) {
                // Always show In Progress appointments
                result.add(toRow(a));

            } else if (status.equalsIgnoreCase("Pending")) {
                // Only show Pending if the start time is in the future
                try {
                    LocalDateTime startTime = LocalDateTime.parse(a.getDateTime(), DATE_TIME_FORMAT);
                    if (startTime.isAfter(now)) {
                        result.add(toRow(a));
                    }
                } catch (DateTimeParseException e) {
                    System.out.println("[getPending] Bad date: " + a.getDateTime());
                }
            }
            // Completed appointments are NOT upcoming — skip them
        }

        // Sort by date-time (earliest first)
        // Works because "yyyy-MM-dd HH:mm" sorts alphabetically = chronologically
        result.sort((rowA, rowB) -> rowA[4].compareTo(rowB[4]));

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // getUnpaidAppointments — Pending Payment card
    // ═══════════════════════════════════════════════════════════════
    /**
     * Returns appointments that are Completed but not yet paid.
     *
     * "PENDING PAYMENT" definition:
     *   - Status is "Completed" (service fully done)
     *   - The appointment ID is NOT in paidIds (not yet paid)
     *
     * Returned array per row (6 elements — same layout as getPendingAppointments):
     *   [0] appointmentID  [1] technicianID  [2] serviceType
     *   [3] status         [4] dateTime      [5] duration (String)
     *
     * @param userId   customer ID e.g. "C3"
     * @param paidIds  set of appointment IDs already paid (from PaymentService)
     */
    public List<String[]> getUnpaidAppointments(String userId, Set<String> paidIds) {
        List<String[]> result = new ArrayList<>();

        for (Appointment a : getAll()) {

            // Must belong to this customer
            if (!a.getCustomerEmail().equalsIgnoreCase(userId)) continue;

            // Must be Completed and not yet paid
            boolean isCompleted = a.getStatus().equalsIgnoreCase("Completed");
            boolean notPaid     = !paidIds.contains(a.getId());

            if (isCompleted && notPaid) {
                result.add(toRow(a));
            }
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // toRow() — helper: converts an Appointment object to a String[]
    // that matches what CustomerDashboard expects.
    // ─────────────────────────────────────────────────────────────
    private String[] toRow(Appointment a) {
        return new String[]{
            a.getId(),                           // [0] appointmentID
            a.getTechnicianEmail(),              // [1] technicianID e.g. "T3"
            a.getServiceType(),                  // [2] serviceType
            a.getStatus(),                       // [3] status
            a.getDateTime(),                     // [4] dateTime
            String.valueOf(a.getDurationHours()) // [5] duration as String
        };
    }
}