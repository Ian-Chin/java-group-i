package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates service tier prices AND the named service catalogue.
 *
 * prices.txt format  (line 1): normalPrice,majorPrice
 * services.txt format (one line per service): serviceName,tier
 *   tier = "Normal Service" or "Major Service"
 *
 * OOP: Encapsulation — all file I/O is private; callers use clean getters/setters.
 */
public class PriceConfig {

    private static final String PRICES_FILE   = "src" + File.separator + "TxtFile"
            + File.separator + "prices.txt";
    private static final String SERVICES_FILE = "src" + File.separator + "TxtFile"
            + File.separator + "services.txt";

    public static final String TIER_NORMAL = "Normal Service";
    public static final String TIER_MAJOR  = "Major Service";

    // Default catalogue shown when services.txt doesn't exist yet
    private static final String[][] DEFAULTS = {
        {"Oil & Filter Change",       TIER_NORMAL},
        {"Tyre Rotation & Balance",   TIER_NORMAL},
        {"Brake Inspection",          TIER_NORMAL},
        {"Air Filter Replacement",    TIER_NORMAL},
        {"Full Engine Overhaul",      TIER_MAJOR},
        {"Transmission Service",      TIER_MAJOR},
        {"Suspension & Steering",     TIER_MAJOR},
        {"Air-Conditioning Service",  TIER_MAJOR},
    };

    // ─── State ────────────────────────────────────────────────────

    private double normalPrice;
    private double majorPrice;

    /** Each entry: { serviceName, tier } */
    private List<String[]> services = new ArrayList<>();

    // ─── Constructor ─────────────────────────────────────────────

    public PriceConfig() {
        loadPrices();
        loadServices();
    }

    // ─── Tier price getters / setters ────────────────────────────

    public double getNormalPrice() { return normalPrice; }
    public double getMajorPrice()  { return majorPrice;  }
    public void setNormalPrice(double p) { this.normalPrice = p; }
    public void setMajorPrice(double p)  { this.majorPrice  = p; }

    /** Price for a named service — looks up its tier then returns tier price. */
    public double getPriceFor(String serviceName) {
        for (String[] s : services) {
            if (s[0].equalsIgnoreCase(serviceName))
                return TIER_MAJOR.equals(s[1]) ? majorPrice : normalPrice;
        }
        return normalPrice; // fallback
    }

    // ─── Service catalogue getters / setters ─────────────────────

    /** Returns a copy of the service catalogue: each entry is { name, tier }. */
    public List<String[]> getServices() {
        List<String[]> copy = new ArrayList<>();
        for (String[] s : services) copy.add(new String[]{s[0], s[1]});
        return copy;
    }

    public void setServices(List<String[]> updated) {
        this.services = new ArrayList<>(updated);
    }

    public void addService(String name, String tier) {
        services.add(new String[]{name, tier});
    }

    public void removeService(int index) {
        if (index >= 0 && index < services.size()) services.remove(index);
    }

    // ─── Persistence ─────────────────────────────────────────────

    public boolean savePrices() {
        try {
            ensureDir(PRICES_FILE);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(PRICES_FILE, false))) {
                w.write(normalPrice + "," + majorPrice);
                w.newLine();
            }
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
    }

    public boolean saveServices() {
        try {
            ensureDir(SERVICES_FILE);
            try (BufferedWriter w = new BufferedWriter(new FileWriter(SERVICES_FILE, false))) {
                for (String[] s : services) {
                    w.write(s[0].replace(",", ";") + "," + s[1]);
                    w.newLine();
                }
            }
            return true;
        } catch (IOException e) { e.printStackTrace(); return false; }
    }

    // ─── Private loaders ─────────────────────────────────────────

    private void loadPrices() {
        normalPrice = 50.0;
        majorPrice  = 200.0;
        File file = new File(PRICES_FILE);
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line = r.readLine();
            if (line != null) {
                String[] p = line.split(",");
                if (p.length == 2) {
                    normalPrice = Double.parseDouble(p[0].trim());
                    majorPrice  = Double.parseDouble(p[1].trim());
                }
            }
        } catch (IOException | NumberFormatException e) { e.printStackTrace(); }
    }

    private void loadServices() {
        services.clear();
        File file = new File(SERVICES_FILE);
        if (!file.exists()) {
            // Seed with defaults on first run
            for (String[] d : DEFAULTS) services.add(new String[]{d[0], d[1]});
            return;
        }
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank()) continue;
                int idx = line.lastIndexOf(",");
                if (idx < 0) continue;
                String name = line.substring(0, idx).trim().replace(";", ",");
                String tier = line.substring(idx + 1).trim();
                services.add(new String[]{name, tier});
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void ensureDir(String path) {
        File parent = new File(path).getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
    }
}