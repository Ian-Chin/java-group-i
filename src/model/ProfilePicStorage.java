package model;

/**
 * ProfilePicStorage handles saving and loading profile pictures.
 *
 * This class extends ImageStorage, which means it inherits all the
 * save, load, and delete methods from ImageStorage for free.
 * The only thing this class needs to do is tell ImageStorage
 * WHICH folder to use — "src/ProfilePic".
 *
 * Java OOP principles used:
 *  - Inheritance  : extends ImageStorage, gets saveImage() and loadImage() for free
 *  - Polymorphism : overrides getStorageFolder() so all inherited methods
 *                   automatically use the ProfilePic folder
 *  - Encapsulation: the folder path is stored as a private constant here;
 *                   other classes never need to know the path, they just
 *                   call saveImage(email, image) or loadImage(email)
 *
 * Storage location: java-group-i/src/ProfilePic/{email}.jpg
 *
 * Example:
 *   ProfilePicStorage storage = new ProfilePicStorage();
 *   storage.saveImage("lin@gmail.com", image);
 *   // saves to: src/ProfilePic/lin@gmail.com.jpg
 */
public class ProfilePicStorage extends ImageStorage {

    // The folder where profile pictures are stored (relative to the project root)
    private static final String FOLDER = "src" + java.io.File.separator + "ProfilePic";

    /**
     * Tells ImageStorage which folder to use for profile pictures.
     * This method overrides the abstract method in ImageStorage.
     */
    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}
