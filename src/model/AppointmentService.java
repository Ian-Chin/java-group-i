package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * AppointmentService reads/writes appointment data from appointments.txt.
 *
 * File format — each line has 7 values separated by commas:
 *   AppointmentID, CustomerID, TechnicianID, ServiceType, Status, DateTime, Duration(Hours)
 *
 * Example:
 *   AP1,C1,T1,Normal Service,Completed,2025-03-05 09:00,1
 */
public class AppointmentService {

    private static final String FILE_PATH = "src" + File.separator
            + "TxtFile" + File.separator + "appointments.txt";

    private static final int EXPECTED_COLUMNS = 7;

    public static class Appointment {
        private final String id;
        private final String customerEmail;
        private final String technicianEmail;
        private final String serviceType;
        private final String status;
        private final String dateTime;
        private final int durationHours;

        public Appointment(String id, String customerEmail, String technicianEmail,
                           String serviceType, String status, String dateTime, int durationHours) {
            this.id = id;
            this.customerEmail = customerEmail;
            this.technicianEmail = technicianEmail;
            this.serviceType = serviceType;
            this.status = status;
            this.dateTime = dateTime;
            this.durationHours = durationHours;
        }

        public String getId()              { return id; }
        public String getCustomerEmail()   { return customerEmail; }
        public String getTechnicianEmail() { return technicianEmail; }
        public String getServiceType()     { return serviceType; }
        public String getStatus()          { return status; }
        public String getDateTime()        { return dateTime; }
        public int    getDurationHours()   { return durationHours; }
    }

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
                        cols[0].trim(),
                        cols[1].trim(),
                        cols[2].trim(),
                        cols[3].trim(),
                        cols[4].trim(),
                        cols[5].trim(),
                        duration
                ));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

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
}
