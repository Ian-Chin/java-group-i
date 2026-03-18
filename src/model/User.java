package model;

public class User {
    private String name;
    private String email;
    private String password;

    public User(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
    }

    // Parse from a CSV line in accounts.txt
    public static User fromCsv(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split(",");
        if (parts.length != 3) return null;
        return new User(parts[0].trim(), parts[1].trim(), parts[2].trim());
    }

    // Convert to a CSV line for accounts.txt
    public String toCsv() {
        return name + "," + email + "," + password;
    }

    public boolean matchesCredentials(String email, String password) {
        return this.email.equalsIgnoreCase(email) && this.password.equals(password);
    }

    public String getName()  { return name; }
    public String getEmail() { return email; }
}
