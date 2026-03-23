package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * ImageStorage is an abstract class that handles saving and loading
 * images (profile pictures and background images) to/from the project folder.
 *
 * "Abstract" means this class cannot be used directly — you must create
 * a subclass (like ProfilePicStorage or BackgroundImageStorage) that
 * tells this class WHICH folder to save into.
 *
 * Java OOP principles used:
 *  - Abstraction  : This class defines WHAT to do (save, load, delete images)
 *                   but leaves the folder location to the subclass
 *  - Inheritance  : ProfilePicStorage and BackgroundImageStorage both extend
 *                   this class and inherit all its methods for free
 *  - Polymorphism : saveImage() and loadImage() work differently for each
 *                   subclass because getStorageFolder() is overridden
 *  - Encapsulation: The path-building and image-conversion logic is kept
 *                   private here — callers only need saveImage() and loadImage()
 */
public abstract class ImageStorage {

    /**
     * Each subclass must override this method to return the folder
     * where its images are stored.
     *
     * Example:
     *   ProfilePicStorage      returns "src/ProfilePic"
     *   BackgroundImageStorage returns "src/BackgroundImg"
     */
    protected abstract String getStorageFolder();

    /**
     * Returns the file type used when saving images.
     * Default is "jpg". Subclasses can override this if needed.
     */
    protected String getFileExtension() {
        return "jpg";
    }

    /**
     * Saves the given image to disk using the user's email as the filename.
     *
     * Example: saveImage("lin@gmail.com", image)
     * saves to: java-group-i/src/ProfilePic/lin@gmail.com.jpg
     *
     * If a file already exists for this email, it is overwritten.
     * The folder is created automatically if it does not exist yet.
     *
     * @param email  the logged-in user's email address (used as the filename)
     * @param image  the image to save
     * @return true if saved successfully, false if something went wrong
     */
    public boolean saveImage(String email, BufferedImage image) {
        // Do not save if email or image is missing
        if (email == null || email.isBlank() || image == null) return false;

        try {
            // Get the folder where this type of image should be saved
            File folder = getFolder();

            // Create the folder if it does not exist yet
            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Build the full file path e.g. src/ProfilePic/lin@gmail.com.jpg
            File outputFile = new File(folder, sanitise(email) + "." + getFileExtension());

            // Write the image to disk (convert to RGB first to avoid JPEG errors with PNG)
            boolean success = ImageIO.write(convertToRgb(image), getFileExtension(), outputFile);

            // Print to console so you can see exactly where the file was saved
            System.out.println("[ImageStorage] Saved image to: " + outputFile.getAbsolutePath()
                    + "  success=" + success);

            return success;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the saved image for the given email from disk.
     * Returns null if no image has been saved yet for this user.
     *
     * Example: loadImage("lin@gmail.com")
     * loads from: java-group-i/src/ProfilePic/lin@gmail.com.jpg
     *
     * @param email  the logged-in user's email address
     * @return the loaded image, or null if no image was found
     */
    public BufferedImage loadImage(String email) {
        // Return null if email is missing
        if (email == null || email.isBlank()) return null;

        // Build the path to the image file
        File imageFile = getFile(email);

        // Print to console so you can see whether the file was found
        System.out.println("[ImageStorage] Loading image from: " + imageFile.getAbsolutePath()
                + "  exists=" + imageFile.exists());

        // Return null if the file does not exist (user has not set an image yet)
        if (!imageFile.exists()) return null;

        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if an image has been saved for the given email.
     * Returns true if the image file exists, false if not.
     */
    public boolean hasImage(String email) {
        if (email == null || email.isBlank()) return false;
        return getFile(email).exists();
    }

    /**
     * Deletes the saved image for the given email.
     * Returns true if the file was found and deleted, false otherwise.
     */
    public boolean deleteImage(String email) {
        if (email == null || email.isBlank()) return false;
        File imageFile = getFile(email);
        if (imageFile.exists()) {
            return imageFile.delete();
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE HELPER METHODS
    // These are internal — other classes never need to call them.
    // ═══════════════════════════════════════════════════════════

    /**
     * Returns the storage folder as a File object.
     * Uses the project root folder (user.dir) as the starting point.
     *
     * Example result: C:\Users\User\git\java-group-i\src\ProfilePic
     */
    private File getFolder() {
        // user.dir = the project root folder when running from Eclipse or IntelliJ
        String projectRoot = System.getProperty("user.dir");
        return new File(projectRoot + File.separator + getStorageFolder());
    }

    /**
     * Returns the full File path for a specific user's image.
     *
     * Example: getFile("lin@gmail.com")
     * returns: C:\...\java-group-i\src\ProfilePic\lin@gmail.com.jpg
     */
    private File getFile(String email) {
        return new File(getFolder(), sanitise(email) + "." + getFileExtension());
    }

    /**
     * Converts an image to standard RGB format before saving as JPEG.
     *
     * Why is this needed?
     * PNG images can have a transparent background (called "alpha channel")
     * which JPEG does not support. Without this conversion, saving a PNG
     * as JPEG would cause an error.
     *
     * If the image is already RGB (no transparency), it is returned unchanged.
     */
    private BufferedImage convertToRgb(BufferedImage sourceImage) {
        // If already RGB format, no conversion needed
        if (sourceImage.getType() == BufferedImage.TYPE_INT_RGB) {
            return sourceImage;
        }

        // Create a new blank RGB image with the same width and height
        BufferedImage rgbImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_RGB);

        // Draw the original image onto the blank RGB canvas
        Graphics2D canvas = rgbImage.createGraphics();
        canvas.drawImage(sourceImage, 0, 0, null);
        canvas.dispose();

        return rgbImage;
    }

    /**
     * Makes an email address safe to use as a filename.
     * Replaces any characters not allowed in filenames with an underscore "_".
     *
     * Example: "lin@gmail.com" → stays "lin@gmail.com"  (all characters are allowed)
     *          "bad/name"      → becomes "bad_name"      (slash is replaced)
     *
     * Allowed characters: letters, digits, @, dot, underscore, hyphen
     */
    private String sanitise(String email) {
        return email.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");
    }
}
