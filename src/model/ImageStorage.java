package model;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Abstract base class for storing and loading user images.
 *
 * Images are saved to:
 *   {user.dir}/src/ProfilePic/{email}.jpg
 *   {user.dir}/src/BackgroundImg/{email}.jpg
 *
 * {user.dir} is the working directory when the app is launched —
 * in Eclipse/IntelliJ this is the project root (java-group-i/).
 * Files persist on disk after the app closes and reload on next login.
 *
 * Java principles:
 *  - Abstraction  : defines WHAT image storage does, not HOW
 *  - Inheritance  : ProfilePicStorage and BackgroundImageStorage extend this
 *  - Polymorphism : saveImage/loadImage use getStorageFolder() which is
 *                   overridden per subclass
 *  - Encapsulation: path building and JPEG conversion are private here;
 *                   callers only use saveImage() and loadImage()
 */
public abstract class ImageStorage {

    /**
     * Subfolder under the project root where images are stored.
     * Subclasses return "src/ProfilePic" or "src/BackgroundImg".
     */
    protected abstract String getStorageFolder();

    /** File extension — default jpg. */
    protected String getFileExtension() {
        return "jpg";
    }

    /**
     * Saves the image for the given email to disk.
     * If an image already exists for this user it is overwritten.
     * The folder is created automatically if it does not exist.
     *
     * @param email logged-in user's email (becomes the filename)
     * @param image the image to save
     * @return true on success, false on failure
     */
    public boolean saveImage(String email, BufferedImage image) {
        if (email == null || email.isBlank() || image == null) return false;
        try {
            File folder = getFolder();
            if (!folder.exists()) folder.mkdirs();

            File out = new File(folder, sanitise(email) + "." + getFileExtension());
            boolean ok = ImageIO.write(convertToRgb(image), getFileExtension(), out);
            System.out.println("[ImageStorage] saveImage → " + out.getAbsolutePath()
                    + "  success=" + ok);
            return ok;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Loads the saved image for the given email.
     * Returns null if no image has been saved yet.
     *
     * @param email logged-in user's email
     * @return BufferedImage or null
     */
    public BufferedImage loadImage(String email) {
        if (email == null || email.isBlank()) return null;
        File file = getFile(email);
        System.out.println("[ImageStorage] loadImage → " + file.getAbsolutePath()
                + "  exists=" + file.exists());
        if (!file.exists()) return null;
        try {
            return ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Returns true if a saved image exists for this email. */
    public boolean hasImage(String email) {
        return email != null && !email.isBlank() && getFile(email).exists();
    }

    /** Deletes the saved image for this email. */
    public boolean deleteImage(String email) {
        if (email == null || email.isBlank()) return false;
        File f = getFile(email);
        return f.exists() && f.delete();
    }

    // ── Private helpers ──────────────────────────────────────────

    /** Returns the absolute folder File for this storage type. */
    private File getFolder() {
        // System.getProperty("user.dir") = project root in Eclipse/IntelliJ
        return new File(System.getProperty("user.dir")
                + File.separator + getStorageFolder());
    }

    /** Returns the absolute File for a specific user's image. */
    private File getFile(String email) {
        return new File(getFolder(), sanitise(email) + "." + getFileExtension());
    }

    /**
     * Converts any BufferedImage to TYPE_INT_RGB.
     * Required so JPEG writing never fails due to alpha channel (e.g. PNG).
     */
    private BufferedImage convertToRgb(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_RGB) return src;
        BufferedImage rgb = new BufferedImage(
                src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = rgb.createGraphics();
        g2.drawImage(src, 0, 0, null);
        g2.dispose();
        return rgb;
    }

    /**
     * Makes an email safe to use as a filename.
     * Keeps letters, digits, @, dots, underscores, hyphens.
     */
    private String sanitise(String email) {
        return email.trim().replaceAll("[^a-zA-Z0-9@._\\-]", "_");
    }
}
