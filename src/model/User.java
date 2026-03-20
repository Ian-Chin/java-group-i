package model;

public class User extends BaseUser {
    private final String password;
    private final String role;

    public User(String name, String email, String password, String role) {
        super(name, email);
        this.password = password;
        this.role = role;
    }

    @Override
    public String getRole() { return "user"; }
    
    //Check email and pw
    public boolean matchesCredentials(String email, String password) {
        return getEmail().equalsIgnoreCase(email) && this.password.equals(password);
    }

    //save user data in csv line: name,email,password
    @Override
    public String toCsv() {
        return getName() + "," + getEmail() + "," + password + "," + role;
    }

    public static User fromCsv(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split(",");
        if (parts.length != 4) return null;
        return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim());
    }
}