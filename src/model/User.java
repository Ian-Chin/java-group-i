package model;

public class User extends BaseUser {
    private final String password;
    private final String role;
    private int profilePicture; // index into built-in avatar set (0-7)

    public User(String name, String email, String password, String role) {
        this(name, email, password, role, 0);
    }

    public User(String name, String email, String password, String role, int profilePicture) {
        super(name, email);
        this.password = password;
        this.role = role;
        this.profilePicture = profilePicture;
    }

    @Override
    public String getRole() { return role; }

    public String getPassword() { return password; }

    public int getProfilePicture() { return profilePicture; }

    public void setProfilePicture(int profilePicture) { this.profilePicture = profilePicture; }

    //Check email and pw
    public boolean matchesCredentials(String email, String password) {
        return getEmail().equalsIgnoreCase(email) && this.password.equals(password);
    }

    //save user data in csv line: name,email,password,role,profilePic
    @Override
    public String toCsv() {
        return getName() + "," + getEmail() + "," + password + "," + role + "," + profilePicture;
    }

    public static User fromCsv(String line) {
        if (line == null || line.isBlank()) return null;
        String[] parts = line.split(",");
        if (parts.length == 4) {
            return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), 0);
        }
        if (parts.length == 5) {
            int pic = 0;
            try { pic = Integer.parseInt(parts[4].trim()); } catch (NumberFormatException ignored) {}
            return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), pic);
        }
        return null;
    }
}