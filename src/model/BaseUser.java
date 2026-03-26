package model;

// Base class for all user types
public abstract class BaseUser {
    private final String userId; // stored here so ALL user types have a user ID
    private final String name;
    private final String email;

    // Stores userId, name and email for any user type
    protected BaseUser(String userId, String name, String email) {
        this.userId = userId;
        this.name   = name;
        this.email  = email;
    }

    public String getUserId() { return userId; } // all user types can call getUserId()
    public String getName()   { return name; }
    public String getEmail()  { return email; }

    // Return role of user and save in accounts.txt
    public abstract String getRole();
    public abstract String toCsv();
}