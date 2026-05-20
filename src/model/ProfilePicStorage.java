package model;

/**
 * ProfilePicStorage handles saving and loading profile pictures.
 */
public class ProfilePicStorage extends ImageStorage {

    private static final String FOLDER = "src" + java.io.File.separator + "ProfilePic";

    @Override
    protected String getStorageFolder() {
        return FOLDER;
    }
}
