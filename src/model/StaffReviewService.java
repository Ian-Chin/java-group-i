package model;

import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * comments.txt format — 9 fields separated by commas:
 *   [0] commentID
 *   [1] customerID
 *   [2] appointmentID
 *   [3] vehicleID
 *   [4] staffID
 *   [5] technicianID
 *   [6] rating
 *   [7] feedback
 *   [8] date
 */
public class StaffReviewService {

    // ── Where the data files live ─────────────────────────────────
    private static final String COMMENTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "comments.txt";

    private static final String ACCOUNTS_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "accounts.txt";

    private static final String VEHICLES_FILE =
            "src" + File.separator + "TxtFile" + File.separator + "vehicles.txt";

    // How many columns we expect in each line of comments.txt
    private static final int EXPECTED_COLUMNS = 9;

    // ─────────────────────────────────────────────────────────────
    // StaffReview — one object holds the data for ONE review row
    // ─────────────────────────────────────────────────────────────
    public static class StaffReview {
        public String commentId;       // e.g. "CM1"
        public String customerId;      // e.g. "C1"  — used to filter by customer
        public String appointmentId;   // e.g. "AP1"
        public String vehicleId;       // e.g. "V1"
        public String vehicleType;     // e.g. "Car"   — looked up from vehicles.txt
        public String carPlate;        // e.g. "WXY1234" — looked up from vehicles.txt
        public String staffId;         // e.g. "S1"
        public String staffName;       // e.g. "Ian Chin" — looked up from accounts.txt
        public String technicianId;    // e.g. "T1"
        public String technicianName;  // e.g. "Mike Tan" — looked up from accounts.txt
        public double rating;          // e.g. 4.0
        public String feedbackText;    // e.g. "Service was thorough and staff were helpful."
        public String date;            // e.g. "2026-03-05"
    }

    // ─────────────────────────────────────────────────────────────
    // getReviewsByCustomer()
    // ─────────────────────────────────────────────────────────────
    public List<StaffReview> getReviewsByCustomer(String customerId) {

        List<StaffReview> result = new ArrayList<>();

        // Safety check — do nothing if no customer ID is given
        if (customerId == null || customerId.trim().isEmpty()) {
            System.out.println("[StaffReviewService] No customer ID given — returning empty list.");
            return result;
        }

        // Check if comments.txt exists
        File file = new File(COMMENTS_FILE);
        System.out.println("[StaffReviewService] Looking for: " + file.getAbsolutePath());

        if (!file.exists()) {
            System.out.println("[StaffReviewService] ERROR — comments.txt not found!");
            return result;
        }

        // Read comments.txt line by line
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // Skip blank lines and comment/header lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Try to parse this line into a StaffReview object
                StaffReview review = parseLine(line, lineNumber);

                // If parsing failed, skip this line
                if (review == null) {
                    continue;
                }

                // Only keep reviews that belong to THIS customer
                if (review.customerId.equalsIgnoreCase(customerId)) {
                    result.add(review);
                    System.out.println("[StaffReviewService] Matched: " + review.commentId
                            + " for customer " + review.customerId);
                }
            }

        } catch (IOException e) {
            System.out.println("[StaffReviewService] Error reading comments.txt: " + e.getMessage());
        }

        System.out.println("[StaffReviewService] Total reviews found for "
                + customerId + ": " + result.size());

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // getAllReviews()
    // ─────────────────────────────────────────────────────────────
    public List<StaffReview> getAllReviews() {

        List<StaffReview> result = new ArrayList<>();

        File file = new File(COMMENTS_FILE);
        if (!file.exists()) {
            System.out.println("[StaffReviewService] comments.txt not found at: "
                    + file.getAbsolutePath());
            return result;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                StaffReview review = parseLine(line, lineNumber);
                if (review != null) {
                    result.add(review);
                }
            }
        } catch (IOException e) {
            System.out.println("[StaffReviewService] Error reading comments.txt: " + e.getMessage());
        }

        return result;
    }

    // ─────────────────────────────────────────────────────────────
    // parseLine() — private helper
    //
    // Takes ONE line of text from comments.txt and turns it into
    // a StaffReview object. Returns null if the line is invalid.
    //
    // Expected format (9 comma-separated values):
    //   [0] commentID
    //   [1] customerID
    //   [2] appointmentID
    //   [3] vehicleID
    //   [4] staffID
    //   [5] technicianID
    //   [6] rating
    //   [7] feedbackText
    //   [8] date
    // ─────────────────────────────────────────────────────────────
    private StaffReview parseLine(String line, int lineNumber) {

        // Split by comma. Using EXPECTED_COLUMNS as the limit means
        // the last field (feedback) can safely contain commas.
        String[] parts = line.split(",", EXPECTED_COLUMNS);

        // We need exactly 9 fields
        if (parts.length < EXPECTED_COLUMNS) {
            System.out.println("[StaffReviewService] Line " + lineNumber
                    + " skipped — expected " + EXPECTED_COLUMNS + " fields, got " + parts.length
                    + ": [" + line + "]");
            return null;
        }

        StaffReview review = new StaffReview();

        // Fill in the fields by position (matches new comments.txt layout)
        review.commentId     = parts[0].trim(); // e.g. "CM1"
        review.customerId    = parts[1].trim(); // e.g. "C1"
        review.appointmentId = parts[2].trim(); // e.g. "AP1"
        review.vehicleId     = parts[3].trim(); // e.g. "V1"
        review.staffId       = parts[4].trim(); // e.g. "S1"
        review.technicianId  = parts[5].trim(); // e.g. "T1"
        // parts[6] = rating (parsed separately below)
        review.feedbackText  = parts[7].trim(); // e.g. "Service was thorough..."
        review.date          = parts[8].trim(); // e.g. "2026-03-05"

        // Parse the rating number — if it's not a valid number, skip this line
        try {
            review.rating = Double.parseDouble(parts[6].trim());
        } catch (NumberFormatException e) {
            System.out.println("[StaffReviewService] Line " + lineNumber
                    + " skipped — bad rating value: [" + parts[6].trim() + "]");
            return null;
        }

        // Look up human-readable names from accounts.txt
        review.staffName      = lookUpName(review.staffId);
        review.technicianName = lookUpName(review.technicianId);

        // Look up vehicle type and car plate from vehicles.txt
        String[] vehicleInfo  = lookUpVehicle(review.vehicleId);
        review.vehicleType    = vehicleInfo[0]; // e.g. "Car"
        review.carPlate       = vehicleInfo[1]; // e.g. "WXY1234"

        return review;
    }

    // ─────────────────────────────────────────────────────────────
    // lookUpName() — private helper
    // ─────────────────────────────────────────────────────────────
    private String lookUpName(String userId) {

        if (userId == null || userId.isEmpty()) {
            return "Unknown";
        }

        File file = new File(ACCOUNTS_FILE);

        if (!file.exists()) {
            System.out.println("[StaffReviewService] accounts.txt not found — using ID as name: " + userId);
            return userId;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(",", -1);

                // [0] = userID, [1] = name
                if (parts.length >= 2
                        && parts[0].trim().equalsIgnoreCase(userId)) {
                    return parts[1].trim();
                }
            }
        } catch (IOException e) {
            System.out.println("[StaffReviewService] Error reading accounts.txt: " + e.getMessage());
        }

        System.out.println("[StaffReviewService] Name not found for ID: " + userId + " — using ID as fallback");
        return userId;
    }

    // ─────────────────────────────────────────────────────────────
    // lookUpVehicle() — private helper
    // ─────────────────────────────────────────────────────────────
    private String[] lookUpVehicle(String vehicleId) {

        String[] fallback = { "Unknown", "Unknown" };

        if (vehicleId == null || vehicleId.isEmpty()) {
            return fallback;
        }

        File file = new File(VEHICLES_FILE);

        if (!file.exists()) {
            System.out.println("[StaffReviewService] vehicles.txt not found — using fallback for: " + vehicleId);
            return fallback;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                // vehicles.txt has 7 columns
                String[] parts = line.split(",", 7);

                // [0] = vehicleID, [2] = vehicleType, [3] = plate
                if (parts.length >= 4
                        && parts[0].trim().equalsIgnoreCase(vehicleId)) {
                    return new String[]{
                            parts[2].trim(), // vehicleType e.g. "Car"
                            parts[3].trim()  // plate       e.g. "WXY1234"
                    };
                }
            }
        } catch (IOException e) {
            System.out.println("[StaffReviewService] Error reading vehicles.txt: " + e.getMessage());
        }

        System.out.println("[StaffReviewService] Vehicle not found for ID: " + vehicleId + " — using fallback");
        return fallback;
    }

    // ─────────────────────────────────────────────────────────────
    // calculateAverageRating()
    // ─────────────────────────────────────────────────────────────
    public double calculateAverageRating(List<StaffReview> reviews) {
        if (reviews.isEmpty()) return 0.0;

        double total = 0.0;
        for (StaffReview review : reviews) {
            total += review.rating;
        }

        return total / reviews.size();
    }

    // ─────────────────────────────────────────────────────────────
    // countByStarLevel()
    //
    // Counts how many reviews fall into each star band:
    //   5★ = rating >= 4.5
    //   4★ = rating >= 3.5 and < 4.5
    //   3★ = rating >= 2.5 and < 3.5
    //   2★ = rating >= 1.5 and < 2.5
    //   1★ = rating <  1.5
    // ─────────────────────────────────────────────────────────────
    public int countByStarLevel(List<StaffReview> reviews, int starLevel) {
        int count = 0;

        for (StaffReview review : reviews) {
            double r = review.rating;
            boolean matches;

            if      (starLevel == 5) matches = (r >= 4.5);
            else if (starLevel == 4) matches = (r >= 3.5 && r < 4.5);
            else if (starLevel == 3) matches = (r >= 2.5 && r < 3.5);
            else if (starLevel == 2) matches = (r >= 1.5 && r < 2.5);
            else                     matches = (r < 1.5); // 1 star

            if (matches) count++;
        }

        return count;
    }

    // ─────────────────────────────────────────────────────────────
    // buildStarString()
    //
    // Converts a numeric rating to a string of ★ / ½ / ☆ symbols.
    // e.g. 4.4 → "★★★★½"
    // ─────────────────────────────────────────────────────────────
    public String buildStarString(double rating) {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if      (rating >= i)       sb.append("\u2605"); // ★ full
            else if (rating >= i - 0.5) sb.append("\u00BD"); // ½ half
            else                        sb.append("\u2606"); // ☆ empty
        }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // filterReviews()
    //
    // Returns a filtered sub-list of the given reviews based on:
    //   keyword    — case-insensitive match against all text fields
    //   ratingIndex — 0 = All, 1 = 5★, 2 = 4★, 3 = 3★, 4 = 2★, 5 = 1★
    // ─────────────────────────────────────────────────────────────
    public List<StaffReview> filterReviews(List<StaffReview> reviews,
                                           String keyword,
                                           int ratingIndex) {

        // Normalise keyword for case-insensitive search
        String kw = (keyword == null) ? "" : keyword.trim().toLowerCase();

        List<StaffReview> filtered = new ArrayList<>();

        for (StaffReview review : reviews) {

            // Does the keyword appear in any column?
            boolean keywordMatch = kw.isEmpty()
                    || review.commentId     .toLowerCase().contains(kw)
                    || review.staffName     .toLowerCase().contains(kw)
                    || review.technicianName.toLowerCase().contains(kw)
                    || review.appointmentId .toLowerCase().contains(kw)
                    || review.vehicleType   .toLowerCase().contains(kw)
                    || review.carPlate      .toLowerCase().contains(kw)
                    || review.feedbackText  .toLowerCase().contains(kw)
                    || review.date          .toLowerCase().contains(kw)
                    || String.valueOf(review.rating).contains(kw);

            // Does the rating fall in the selected band?
            boolean ratingMatch;
            switch (ratingIndex) {
                case 1:  ratingMatch = (review.rating >= 4.5);                        break;
                case 2:  ratingMatch = (review.rating >= 3.5 && review.rating < 4.5); break;
                case 3:  ratingMatch = (review.rating >= 2.5 && review.rating < 3.5); break;
                case 4:  ratingMatch = (review.rating >= 1.5 && review.rating < 2.5); break;
                case 5:  ratingMatch = (review.rating  < 1.5);                        break;
                default: ratingMatch = true; // "All Ratings"
            }

            if (keywordMatch && ratingMatch) {
                filtered.add(review);
            }
        }

        return filtered;
    }

    // ─────────────────────────────────────────────────────────────
    // sortReviews()
    //
    // Sorts the given list IN PLACE by the specified column index
    // (matching the column order used in StaffReviewPage's table):
    //
    //   0 = Comment ID      5 = Car Plate
    //   1 = Staff Name      6 = Rating
    //   2 = Technician Name 7 = Feedback
    //   3 = Appointment ID  8 = Date
    //   4 = Vehicle Type
    //
    // Pass sortColumnIndex = -1 to skip sorting entirely.
    // ─────────────────────────────────────────────────────────────
    public void sortReviews(List<StaffReview> list,
                            int sortColumnIndex,
                            boolean ascending) {

        if (sortColumnIndex < 0) return; // no sort requested

        Comparator<StaffReview> comparator;

        switch (sortColumnIndex) {
            case 0:  comparator = Comparator.comparing(r -> r.commentId);      break;
            case 1:  comparator = Comparator.comparing(r -> r.staffName);      break;
            case 2:  comparator = Comparator.comparing(r -> r.technicianName); break;
            case 3:  comparator = Comparator.comparing(r -> r.appointmentId);  break;
            case 4:  comparator = Comparator.comparing(r -> r.vehicleType);    break;
            case 5:  comparator = Comparator.comparing(r -> r.carPlate);       break;
            case 6:  comparator = Comparator.comparingDouble(r -> r.rating);   break;
            case 7:  comparator = Comparator.comparing(r -> r.feedbackText);   break;
            case 8:  comparator = Comparator.comparing(r -> r.date);           break;
            default: comparator = Comparator.comparing(r -> r.commentId);      break;
        }

        if (!ascending) {
            comparator = comparator.reversed();
        }

        list.sort(comparator);
    }
}