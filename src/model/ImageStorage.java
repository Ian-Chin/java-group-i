package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public abstract class ImageStorage {

    /**
     * Each subclass must override this method to return the folder where its images are stored.
     *
     * Example:
     *   ProfilePicStorage      returns "src/ProfilePic"
     *   BackgroundImageStorage returns "src/BackgroundImg"
     */
    protected abstract String getStorageFolder();

    protected String getFileExtension() {
        return "jpg";
    }

    /**
     * Example: saveImage("C3", image)
     * saves to: java-group-i/src/ProfilePic/C3.jpg
     */
    public boolean saveImage(String userId, BufferedImage image) {

        if (userId == null || userId.isBlank() || image == null) return false;

        try {
            // Get the folder where this type of image should be saved
            File folder = getFolder();

            if (!folder.exists()) {
                folder.mkdirs();
            }

            // Build the full file path e.g. src/ProfilePic/C3.jpg
            File outputFile = new File(folder, sanitise(userId) + "." + getFileExtension());

            // Write the image to disk (convert to RGB first to avoid JPEG errors with PNG)
            boolean success = ImageIO.write(convertToRgb(image), getFileExtension(), outputFile);

            System.out.println("[ImageStorage] Saved image to: " + outputFile.getAbsolutePath()
                    + "  success=" + success);

            return success;

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Example: loadImage("C3")
     * loads from: java-group-i/src/ProfilePic/C3.jpg
     */
    public BufferedImage loadImage(String userId) {

        if (userId == null || userId.isBlank()) return null;

        // Build the path to the image file
        File imageFile = getFile(userId);

        System.out.println("[ImageStorage] Loading image from: " + imageFile.getAbsolutePath()
                + "  exists=" + imageFile.exists());

        if (!imageFile.exists()) return null;

        try {
            return ImageIO.read(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns true if the image file exists, false if not.
     */
    public boolean hasImage(String userId) {
        if (userId == null || userId.isBlank()) return false;
        return getFile(userId).exists();
    }

    /**
     * Deletes the saved image for the given user ID.
     * Returns true if the file was found and deleted, false otherwise.
     */
    public boolean deleteImage(String userId) {
        if (userId == null || userId.isBlank()) return false;
        File imageFile = getFile(userId);
        if (imageFile.exists()) {
            return imageFile.delete();
        }
        return false;
    }

    /**
     * Example result: C:\Users\User\git\java-group-i\src\ProfilePic
     */
    private File getFolder() {
        // user.dir = the project root folder when running from Eclipse or IntelliJ
        String projectRoot = System.getProperty("user.dir");
        return new File(projectRoot + File.separator + getStorageFolder());
    }

    /**
     * Example: getFile("C3") returns: C:\...\java-group-i\src\ProfilePic\C3.jpg
     */
    private File getFile(String userId) {
        return new File(getFolder(), sanitise(userId) + "." + getFileExtension());
    }

    /**
     * Converts an image to standard RGB format before saving as JPEG.
     *
     * Why is this needed?
     * PNG images can have a transparent background (called "alpha channel")
     * which JPEG does not support. Without this conversion, saving a PNG as JPEG would cause an error.
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
     * Makes a user ID safe to use as a filename.
     * Replaces any characters not allowed in filenames with an underscore "_".
     *
     * Example: "C3"  → stays "C3"   (letters and digits are allowed)
     * 
     * Example: "bad/name" → becomes "bad_name" (slash is replaced)
     *
     * Allowed characters: letters, digits, @, dot, underscore, hyphen
     */
    private String sanitise(String userId) {
        return userId.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");
    }
}