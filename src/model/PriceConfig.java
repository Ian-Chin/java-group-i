package model;

import java.io.*;

/**
 * Encapsulates reading and writing service prices to a text file.
 * Format: normalPrice,majorPrice   (e.g.  50.0,150.0)
 */
public class PriceConfig {

    private static final String FILE_PATH = "src" + File.separator + "TxtFile"
            + File.separator + "prices.txt";

    private double normalPrice;
    private double majorPrice;

    // ─── Constructor: load from file (or use defaults) ───────────

    public PriceConfig() {
        load();
    }

    // ─── Getters / Setters ───────────────────────────────────────

    public double getNormalPrice() { return normalPrice; }
    public double getMajorPrice()  { return majorPrice;  }

    public void setNormalPrice(double price) { this.normalPrice = price; }
    public void setMajorPrice(double price)  { this.majorPrice  = price; }

    // ─── Persistence ─────────────────────────────────────────────

    public boolean save() {
        File file = new File(FILE_PATH);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter w = new BufferedWriter(new FileWriter(file, false))) {
                w.write(normalPrice + "," + majorPrice);
                w.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void load() {
        // Default prices
        normalPrice = 50.0;
        majorPrice  = 150.0;
        File file = new File(FILE_PATH);
        if (!file.exists()) return;
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            String line = r.readLine();
            if (line != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    normalPrice = Double.parseDouble(parts[0].trim());
                    majorPrice  = Double.parseDouble(parts[1].trim());
                }
            }
        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
        }
    }
}