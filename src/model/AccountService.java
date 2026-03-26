package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccountService {
    private static final String FILE_PATH = "src" + File.separator + "TxtFile"
            + File.separator + "accounts.txt";

    // ─── Auth ─────────────────────────────────────────────────────
    public User authenticate(String email, String password) {
        for (User user : loadAll()) {
            if (user.matchesCredentials(email, password)) return user;
        }
        return null;
    }

    // ─── Queries ──────────────────────────────────────────────────
    public boolean emailExists(String email) {
        return loadAll().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public List<User> getAllUsers() {
        return loadAll();
    }

    public List<User> getUsersByRole(String role) {
        return loadAll().stream()
                .filter(u -> u.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    // ─── Mutations ────────────────────────────────────────────────
    public boolean register(User user) {
        // Auto-generate a customer ID like C5, C6, etc.
        String newId = generateNextCustomerId();

        // Create a new User that includes the generated ID
        User userWithId = new User(newId, user.getName(), user.getEmail(),
                                   user.getPassword(), user.getRole(), user.getProfilePicture());

        File file = new File(FILE_PATH);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(userWithId.toCsv());
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateUser(String originalEmail, User updatedUser) {
        List<User> users = loadAll();
        boolean found = false;
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getEmail().equalsIgnoreCase(originalEmail)) {
                users.set(i, updatedUser);
                found = true;
                break;
            }
        }
        return found && saveAll(users);
    }

    public boolean deleteUser(String email) {
        List<User> users = loadAll();
        boolean removed = users.removeIf(u -> u.getEmail().equalsIgnoreCase(email));
        return removed && saveAll(users);
    }

    // ─── File I/O (private) ───────────────────────────────────────
    private List<User> loadAll() {
        List<User> users = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return users;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = User.fromCsv(line);
                if (user != null) users.add(user); // fromCsv already skips # lines
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    private boolean saveAll(List<User> users) {
        // Read the original file first to keep comment lines (lines starting with #)
        List<String> commentLines = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.trim().startsWith("#")) {
                        commentLines.add(line); // save comment lines so they are not lost
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Write comments first, then all user data
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
            for (String comment : commentLines) {
                writer.write(comment);
                writer.newLine();
            }
            for (User user : users) {
                writer.write(user.toCsv());
                writer.newLine();
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    // Generates the next customer ID like C1, C2, C3 ...
    private String generateNextCustomerId() {
        List<User> allUsers = loadAll();
        int maxNumber = 0;

        for (User user : allUsers) {
            String id = user.getUserId();
            // Customer IDs start with "C"
            if (id != null && id.startsWith("C")) {
                try {
                    int number = Integer.parseInt(id.substring(1)); // get the number after "C"
                    if (number > maxNumber) {
                        maxNumber = number;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        return "C" + (maxNumber + 1); // e.g. if max is C4, return C5
    }
}