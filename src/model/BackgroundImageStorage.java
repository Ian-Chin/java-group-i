package model;

/**
 * BackgroundImageStorage handles saving and loading background/banner images.
 */
public class BackgroundImageStorage extends ImageStorage {

    private static final String FOLDER = "src" + java.io.File.separator + "BackgroundImg";

    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}
