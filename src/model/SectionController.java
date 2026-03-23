package model;

import java.util.List;

/**
 * SectionController is a generic interface that defines the four core actions
 * every dashboard section needs:
 *
 *   1. refreshList()   — reload and display the latest data
 *   2. handleAdd()     — validate and add a new record
 *   3. handleEdit()    — validate and update an existing record
 *   4. handleDelete()  — confirm and delete a record
 *
 * Why use an interface?
 * Every section (Vehicles, Service History, Payment History, Feedback)
 * does the same four things — just with different data and different
 * validation rules. By defining this interface once, each section
 * controller just implements it with its own specific logic.
 *
 * Java OOP principles used:
 *  - Abstraction  : defines WHAT each section can do, not HOW
 *  - Polymorphism : each section's handleAdd(), handleEdit(), handleDelete()
 *                   behaves differently but is called the same way
 *  - Inheritance  : all section controllers implement this interface,
 *                   so they can be treated as the same type
 *
 * How your teammates use this for a new section:
 *
 *   public class ServiceHistorySectionController implements SectionController {
 *       // Fill in all four methods with service-history-specific logic
 *   }
 *
 * Example of polymorphism in action:
 *   SectionController vehicles = new VehicleSectionController(...);
 *   SectionController history  = new ServiceHistorySectionController(...);
 *
 *   vehicles.handleAdd(fields);  // saves a vehicle
 *   history.handleAdd(fields);   // saves a service record
 *   // Same method call — different behaviour!
 */
public interface SectionController {

    /**
     * Reloads the data for this section and tells the UI to update the list.
     *
     * Example:
     *  - VehicleSectionController reads vehicles.txt and rebuilds the vehicle rows
     *  - ServiceHistorySectionController reads serviceHistory.txt and rebuilds rows
     */
    void refreshList();

    /**
     * Validates the given field values and adds a new record if valid.
     *
     * The fields array contains the values entered by the user in the Add form.
     * Each section controller decides which index maps to which field.
     *
     * Example for vehicles:
     *   fields[0] = plate
     *   fields[1] = brand
     *   fields[2] = year
     *   fields[3] = colour
     *
     * @param fields  the values typed by the user, in order
     * @return true if the record was added successfully, false if validation failed
     */
    boolean handleAdd(String[] fields);

    /**
     * Validates the given field values and updates an existing record in place.
     *
     * The id parameter identifies WHICH record to update.
     * For vehicles this is the plate number.
     * For other sections this could be an ID number.
     *
     * @param id      the unique identifier of the record to update
     * @param fields  the new values entered by the user
     * @return true if the record was updated successfully, false otherwise
     */
    boolean handleEdit(String id, String[] fields);

    /**
     * Shows a confirmation dialog then deletes the record with the given id.
     *
     * @param id  the unique identifier of the record to delete
     */
    void handleDelete(String id);

    /**
     * Validates the given field values for this section.
     *
     * Each section has its own rules:
     *  - Vehicles       : plate must have letters + numbers, colour = letters only
     *  - Service History: date must be valid, service type must be letters only
     *  - Payment History: amount must be a number, status must be paid/unpaid
     *
     * @param fields  the values to validate
     * @return an error message string if invalid, or null if all fields are valid
     */
    String validateFields(String[] fields);
}
