package model;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * AppointmentService reads and manages appointment data from appointments.txt.
 *
 * File format — each line has 6 values separated by commas:
 *   AppointmentID , UserID , Type , Status , DateTime , Duration
 *
 * Example:
 *   AP1,C1,Full Service,Completed,2025-03-05 09:00,2 hrs
 */
public class AppointmentService {

    // Path to the appointments data file
    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "appointments.txt";

    // Each line must have exactly 6 columns
    private static final int EXPECTED_COLUMNS = 6;

    // The date+time format used in the file: "2025-03-05 09:00"
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Returns all appointments belonging to the given userID.
     *
     * Each appointment is returned as a String array:
     *   [0] appointmentID  e.g. "AP1"
     *   [1] type           e.g. "Full Service"
     *   [2] status         e.g. "Completed" / "In Progress" / "Pending"
     *   [3] dateTime       e.g. "2025-03-05 09:00"
     *   [4] duration       e.g. "2 hrs"
     */
    public List<String[]> getAppointmentsByUserId(String userId) {
        List<String[]> result = new ArrayList<>();

        File file = new File(FILE_PATH);
        if (!file.exists()) return result;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Skip blank lines and comment lines starting with #
                if (line.isBlank() || line.trim().startsWith("#")) continue;

                String[] columns = line.split(",", EXPECTED_COLUMNS);

                if (columns.length != EXPECTED_COLUMNS) continue;

                // Column index 1 is the userID
                String userIdInFile = columns[1].trim();
                if (userIdInFile.equalsIgnoreCase(userId)) {
                    String[] appointment = new String[]{
                            columns[0].trim(), // appointmentID
                            columns[2].trim(), // type
                            columns[3].trim(), // status
                            columns[4].trim(), // dateTime
                            columns[5].trim()  // duration
                    };
                    result.add(appointment);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    /**
     * Returns only "Pending" appointments for the given userID.
     * These are shown in the Upcoming Appointments section.
     *
     * "Pending" means the appointment has not started yet.
     */
    public List<String[]> getPendingAppointments(String userId) {
        List<String[]> result = new ArrayList<>();
        for (String[] appt : getAppointmentsByUserId(userId)) {
            // appt[2] is the status
            if (appt[2].equalsIgnoreCase("Pending")) {
                result.add(appt);
            }
        }
        return result;
    }

    /**
     * Returns appointments that need payment for the given userID.
     *
     * These are shown in the Pending Payment section.
     * An appointment needs payment if:
     *   - Status is "Completed" AND it has no payment record yet, OR
     *   - Status is "In Progress" AND the appointment date+time has already passed
     *     (based on current Malaysia time)
     *
     * @param userId           the customer's user ID
     * @param paidAppointmentIds  set of appointment IDs that already have a Paid payment
     */
    public List<String[]> getUnpaidAppointments(String userId,
                                                 java.util.Set<String> paidAppointmentIds) {
        List<String[]> result = new ArrayList<>();

        // Get current Malaysia date and time
        // Malaysia is UTC+8
        LocalDateTime nowMalaysia = LocalDateTime.now(
                java.time.ZoneId.of("Asia/Kuala_Lumpur"));

        for (String[] appt : getAppointmentsByUserId(userId)) {
            String apptId  = appt[0]; // appointment ID
            String status  = appt[2]; // Completed / In Progress / Pending
            String dateStr = appt[3]; // "2025-03-05 09:00"

            // Skip if already paid
            if (paidAppointmentIds.contains(apptId)) continue;

            // Include if Completed (service done but not paid)
            if (status.equalsIgnoreCase("Completed")) {
                result.add(appt);
                continue;
            }

            // Include if In Progress AND the appointment time has already passed
            if (status.equalsIgnoreCase("In Progress")) {
                try {
                    LocalDateTime apptTime = LocalDateTime.parse(dateStr, FORMATTER);
                    // If the appointment time is in the past, payment is due
                    if (apptTime.isBefore(nowMalaysia)) {
                        result.add(appt);
                    }
                } catch (Exception e) {
                    // If we can't parse the date, skip it
                    System.err.println("AppointmentService: could not parse date: " + dateStr);
                }
            }
        }

        return result;
    }
}