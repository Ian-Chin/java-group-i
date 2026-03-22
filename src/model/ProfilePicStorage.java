package model;

/**
 * Concrete service for storing and loading profile pictures.
 *
 * Java principles:
 *  - Inheritance  : extends ImageStorage
 *  - Polymorphism : overrides getStorageFolder() so saveImage/loadImage
 *                   automatically use src/ProfilePic/
 *  - Encapsulation: the folder path is private to this class;
 *                   callers just use saveImage(email, image)
 *
 * Storage location: java-group-i/src/ProfilePic/{email}.jpg
 */
public class ProfilePicStorage extends ImageStorage {

    private static final String FOLDER =
            "src" + java.io.File.separator + "ProfilePic";

    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}

