package model;

/**
 * Concrete service for storing and loading background/banner images.
 *
 * Java principles:
 *  - Inheritance  : extends ImageStorage
 *  - Polymorphism : overrides getStorageFolder() so saveImage/loadImage
 *                   automatically use src/BackgroundImg/
 *  - Encapsulation: the folder path is private to this class;
 *                   callers just use saveImage(email, image)
 *
 * Storage location: java-group-i/src/BackgroundImg/{email}.jpg
 */
public class BackgroundImageStorage extends ImageStorage {

    private static final String FOLDER =
            "src" + java.io.File.separator + "BackgroundImg";

    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}

