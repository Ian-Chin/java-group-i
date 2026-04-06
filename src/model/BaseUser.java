package model;


public abstract class BaseUser {
    private final String userId;
    private final String name;
    private final String email;

    protected BaseUser(String userId, String name, String email) {
        this.userId = userId;
        this.name   = name;
        this.email  = email;
    }

    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }

    public abstract String getRole();
    public abstract String toCsv();
}