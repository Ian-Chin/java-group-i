package model;

/**
 * Immutable representation of one row in vehicles.txt.
 * Replaces the bare {@code String[]} that VehicleService used to hand out so
 * UI code can request fields by name instead of by index.
 */
public class Vehicle {

    private final String id;
    private final String userId;
    private final String type;
    private final String plate;
    private final String brand;
    private final String year;
    private final String colour;

    public Vehicle(String id, String userId, String type, String plate,
                   String brand, String year, String colour) {
        this.id     = id;
        this.userId = userId;
        this.type   = type;
        this.plate  = plate;
        this.brand  = brand;
        this.year   = year;
        this.colour = colour;
    }

    public String getId()     { return id; }
    public String getUserId() { return userId; }
    public String getType()   { return type; }
    public String getPlate()  { return plate; }
    public String getBrand()  { return brand; }
    public String getYear()   { return year; }
    public String getColour() { return colour; }

    /** Short label for dropdowns / list rows: e.g. "WXY 1234 · Car (Toyota)". */
    public String displayLabel() {
        return plate + " \u00B7 " + type + " (" + brand + ")";
    }
}
