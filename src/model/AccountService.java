package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AccountService {

    private static final String FILE_PATH = "src" + File.separator + "TxtFile"
            + File.separator + "accounts.txt";

    // ─── Auth ────────────────────────────────────────────────────

    public User authenticate(String email, String password) {
        for (User user : loadAll()) {
            if (user.matchesCredentials(email, password)) return user;
        }
        return null;
    }

    // ─── Queries ─────────────────────────────────────────────────

    public boolean emailExists(String email) {
        return loadAll().stream().anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    /** Returns every user in the file. */
    public List<User> getAllUsers() {
        return loadAll();
    }

    /** Returns only users whose role matches (case-insensitive). */
    public List<User> getUsersByRole(String role) {
        return loadAll().stream()
                .filter(u -> u.getRole().equalsIgnoreCase(role))
                .collect(Collectors.toList());
    }

    // ─── Mutations ───────────────────────────────────────────────

    public boolean register(User user) {
        File file = new File(FILE_PATH);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, true))) {
                writer.write(user.toCsv());
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

    /** Removes the user with the given email. Returns false if not found. */
    public boolean deleteUser(String email) {
        List<User> users = loadAll();
        boolean removed = users.removeIf(u -> u.getEmail().equalsIgnoreCase(email));
        return removed && saveAll(users);
    }

    // ─── File I/O (private — encapsulated) ──────────────────────

    private List<User> loadAll() {
        List<User> users = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return users;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                User user = User.fromCsv(line);
                if (user != null) users.add(user);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return users;
    }

    private boolean saveAll(List<User> users) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_PATH, false))) {
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
}