package model;

/**
 * BackgroundImageStorage handles saving and loading background/banner images.
 *
 * This class extends ImageStorage, which means it inherits all the
 * save, load, and delete methods from ImageStorage for free.
 * The only thing this class needs to do is tell ImageStorage
 * WHICH folder to use — "src/BackgroundImg".
 *
 * Java OOP principles used:
 *  - Inheritance  : extends ImageStorage, gets saveImage() and loadImage() for free
 *  - Polymorphism : overrides getStorageFolder() so all inherited methods
 *                   automatically use the BackgroundImg folder
 *  - Encapsulation: the folder path is stored as a private constant here;
 *                   other classes never need to know the path, they just
 *                   call saveImage(email, image) or loadImage(email)
 *
 * Storage location: java-group-i/src/BackgroundImg/{email}.jpg
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
