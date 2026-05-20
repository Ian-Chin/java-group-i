package model;

import java.util.List;

/**
 * SectionController is a generic interface that defines the four core actions every dashboard section needs:
 */
public interface SectionController {

    /**
     * Example:
     *  - VehicleSectionController reads vehicles.txt and rebuilds the vehicle rows
     *  - ServiceHistorySectionController reads serviceHistory.txt and rebuilds rows
     */
    void refreshList();

    /**
     * The fields array contains the values entered by the user in the Add form.
     * Each section controller decides which index maps to which field.
     *
     * Example for vehicles:
     *   fields[0] = plate
     *   fields[1] = brand
     *   fields[2] = year
     *   fields[3] = colour
     */
    boolean handleAdd(String[] fields);

    /**
     * The id parameter identifies WHICH record to update.
     * For vehicles this is the plate number.
     * For other sections this could be an ID number.
     */
    boolean handleEdit(String id, String[] fields);

    void handleDelete(String id);

    /**
     * Each section has its own rules:
     *  - Vehicles       : plate must have letters + numbers, colour = letters only
     *  - Service History: date must be valid, service type must be letters only
     *  - Payment History: amount must be a number, status must be paid/unpaid
     */
    String validateFields(String[] fields);
}
