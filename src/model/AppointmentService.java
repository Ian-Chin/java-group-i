package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AppointmentService {

    private static final String FILE_PATH = "src" + File.separator + "TxtFile"
            + File.separator + "appointments.txt";

    public static class Appointment {
        private final String id;
        private final String customerEmail;
        private final String technicianEmail;
        private final String serviceType;   // "Normal Service" or "Major Service"
        private String status;               // "Pending", "In Progress", "Completed"
        private final String dateTime;       // yyyy-MM-dd HH:mm
        private final int durationHours;     // 1 or 3

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
        public void   setStatus(String s)  { this.status = s; }
        public String getDateTime()        { return dateTime; }
        public int    getDurationHours()   { return durationHours; }

        public String toCsv() {
            return id + "," + customerEmail + "," + technicianEmail + ","
                    + serviceType + "," + status + "," + dateTime + "," + durationHours;
        }

        public static Appointment fromCsv(String line) {
            if (line == null || line.isBlank()) return null;
            String[] p = line.split(",");
            if (p.length < 7) return null;
            try {
                int dur = Integer.parseInt(p[6].trim());
                return new Appointment(p[0].trim(), p[1].trim(), p[2].trim(),
                        p[3].trim(), p[4].trim(), p[5].trim(), dur);
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    public List<Appointment> getAll() {
        return loadAll();
    }

    public boolean add(Appointment appt) {
        File file = new File(FILE_PATH);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file, true))) {
                w.write(appt.toCsv());
                w.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean delete(String id) {
        List<Appointment> all = loadAll();
        boolean removed = all.removeIf(a -> a.getId().equals(id));
        return removed && saveAll(all);
    }

    public boolean update(Appointment updated) {
        List<Appointment> all = loadAll();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(updated.getId())) {
                all.set(i, updated);
                return saveAll(all);
            }
        }
        return false;
    }

    public String nextId() {
        List<Appointment> all = loadAll();
        int max = 0;
        for (Appointment a : all) {
            try {
                int n = Integer.parseInt(a.getId().replace("APT", ""));
                if (n > max) max = n;
            } catch (NumberFormatException ignored) {}
        }
        return "APT" + String.format("%04d", max + 1);
    }

    private List<Appointment> loadAll() {
        List<Appointment> list = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return list;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                Appointment a = Appointment.fromCsv(line);
                if (a != null) list.add(a);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    private boolean saveAll(List<Appointment> list) {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (Appointment a : list) {
                w.write(a.toCsv());
                w.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
