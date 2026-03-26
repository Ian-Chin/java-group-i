package model;

public class User extends BaseUser {
    // NOTE: userId is NOT declared here — it lives in BaseUser
    // Declaring it here again would create a duplicate hidden field, which causes bugs
    private final String password;
    private final String role;
    private int profilePicture;

    // Constructor WITHOUT userId (used when registering new customers)
    public User(String name, String email, String password, String role) {
        this(null, name, email, password, role, 0);
    }

    // Constructor WITHOUT userId but WITH profilePicture
    public User(String name, String email, String password, String role, int profilePicture) {
        this(null, name, email, password, role, profilePicture);
    }

    // Full constructor WITH userId (used when loading from file)
    public User(String userId, String name, String email, String password, String role, int profilePicture) {
        super(userId, name, email); // FIXED: pass userId to BaseUser so it is stored there
        this.password       = password;
        this.role           = role;
        this.profilePicture = profilePicture;
    }

    @Override
    public String getRole() { return role; }

    // NOTE: getUserId() is NOT declared here — it is inherited from BaseUser for free
    // You call user.getUserId() and it works because BaseUser already has it

    public String getPassword() { return password; }

    public int getProfilePicture() { return profilePicture; }

    public void setProfilePicture(int profilePicture) { this.profilePicture = profilePicture; }

    // Check email and password
    public boolean matchesCredentials(String email, String password) {
        return getEmail().equalsIgnoreCase(email) && this.password.equals(password);
    }

    // Save user data as CSV line: userID,name,email,password,role,profilePic
    @Override
    public String toCsv() {
        // getUserId() is inherited from BaseUser — no need to use a local field
        String id = (getUserId() != null) ? getUserId() : "";
        return id + "," + getName() + "," + getEmail() + "," + password + "," + role + "," + profilePicture;
    }

    // Read a user from a CSV line
    // Supports old format (4 or 5 columns) and new format (6 columns), skips comment lines
    public static User fromCsv(String line) {
        // Skip blank lines and comment lines starting with #
        if (line == null || line.isBlank() || line.trim().startsWith("#")) return null;

        String[] parts = line.split(",");

        // NEW format: userID, name, email, password, role, profilePicIndex  (6 parts)
        if (parts.length == 6) {
            String userId   = parts[0].trim();
            String name     = parts[1].trim();
            String email    = parts[2].trim();
            String password = parts[3].trim();
            String role     = parts[4].trim();
            int pic = 0;
            try { pic = Integer.parseInt(parts[5].trim()); } catch (NumberFormatException ignored) {}
            return new User(userId, name, email, password, role, pic);
        }

        // OLD format without profilePic: name, email, password, role  (4 parts)
        if (parts.length == 4) {
            return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), 0);
        }

        // OLD format with profilePic: name, email, password, role, profilePic  (5 parts)
        if (parts.length == 5) {
            int pic = 0;
            try { pic = Integer.parseInt(parts[4].trim()); } catch (NumberFormatException ignored) {}
            return new User(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(), pic);
        }

        // Unrecognised format — skip the line
        return null;
    }
}