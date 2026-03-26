package model;

public class CustomerProfileController extends ProfileController {

    public interface AppFrameAccessor {
        User   getLoggedInUserObj();
        String getLoggedInUser();
        void   setLoggedInUser(String name);
        void   setLoggedInUserObj(User user);
    }

    private final AppFrameAccessor appAccessor;

    public CustomerProfileController(AccountService accountService,
                                     AppFrameAccessor appAccessor) {
        super(accountService);
        this.appAccessor = appAccessor;
    }

    @Override
    public User getCurrentUser() {
        return appAccessor.getLoggedInUserObj();
    }

    @Override
    public boolean saveProfile(String newName, String newEmail) {
        newName  = newName.trim();
        newEmail = newEmail.trim();

        String validationError = validate(newName, newEmail);
        if (validationError != null) {
            throw new IllegalArgumentException(validationError);
        }

        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        String originalEmail = currentUser.getEmail();

        // IMPORTANT: keep the same userId when building the updated User object
        User updatedUser = new User(
                currentUser.getUserId(),   // keep original userId e.g. "C3"
                newName,
                newEmail,
                currentUser.getPassword(),
                currentUser.getRole(),
                currentUser.getProfilePicture()
        );

        boolean saveSuccessful = accountService.updateUser(originalEmail, updatedUser);

        if (saveSuccessful) {
            appAccessor.setLoggedInUser(newName);
            appAccessor.setLoggedInUserObj(updatedUser);

            // If the email changed, rename the image files
            // NOTE: image files are now named by userId (e.g. C3.jpg), NOT email,
            // so we do NOT need to rename them when email changes anymore.
            // The renameImageFile calls are removed on purpose.
        }

        return saveSuccessful;
    }

    @Override
    public boolean deleteAccount() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        String filePath = "src" + java.io.File.separator
                + "TxtFile" + java.io.File.separator + "accounts.txt";
        java.io.File accountsFile = new java.io.File(filePath);

        if (!accountsFile.exists()) return false;

        java.util.List<String> linesToKeep = new java.util.ArrayList<>();
        boolean accountWasFound = false;

        try (java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.FileReader(accountsFile))) {
            String line;

            while ((line = reader.readLine()) != null) {
                // Keep comment lines (lines starting with #)
                if (line.trim().startsWith("#")) {
                    linesToKeep.add(line);
                    continue;
                }

                if (line.isBlank()) continue;

                // NEW format: userID, name, email, password, role, profilePic
                // So email is now at index 2 (not index 1)
                String[] columns = line.split(",", 6);

                boolean isThisAccount = columns.length >= 3
                        && columns[2].trim().equalsIgnoreCase(currentUser.getEmail());

                if (isThisAccount) {
                    accountWasFound = true;
                    // Do NOT add this line — this is how we "delete" the account
                } else {
                    linesToKeep.add(line); // keep all other accounts
                }
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        if (!accountWasFound) return false;

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(accountsFile, false))) {
            for (String lineToWrite : linesToKeep) {
                writer.write(lineToWrite);
                writer.newLine();
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            return false;
        }

        appAccessor.setLoggedInUser("");
        appAccessor.setLoggedInUserObj(null);

        return true;
    }

    public String getCurrentName() {
        User user = getCurrentUser();
        return user != null ? user.getName() : "";
    }

    public String getCurrentEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : "";
    }

    public String getCurrentRole() {
        User user = getCurrentUser();
        if (user == null) return "";
        String role = user.getRole();
        return role.substring(0, 1).toUpperCase() + role.substring(1);
    }
}