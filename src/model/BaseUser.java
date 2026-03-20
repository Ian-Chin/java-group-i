package model;

//base class for all user types
public abstract class BaseUser {
	private final String name;
    private final String email;

    //stores name & email for any user type
    protected BaseUser(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName()  { return name; }
    public String getEmail() { return email; }
    
    //return role of user and save in Accounts.txt
    public abstract String getRole();
    public abstract String toCsv();
}

