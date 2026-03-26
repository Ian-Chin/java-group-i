package model;

/**
 * BackgroundImageStorage handles saving and loading background/banner images.
 * 
 * Example:
 *   BackgroundImageStorage storage = new BackgroundImageStorage();
 *   storage.saveImage("lin@gmail.com", image);
 *   // saves to: src/BackgroundImg/lin@gmail.com.jpg
 */
public class BackgroundImageStorage extends ImageStorage {

    // The folder where background images are stored (relative to the project root)
    private static final String FOLDER = "src" + java.io.File.separator + "BackgroundImg";

    /**
     * Tells ImageStorage which folder to use for background images.
     * This method overrides the abstract method in ImageStorage.
     */
    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}