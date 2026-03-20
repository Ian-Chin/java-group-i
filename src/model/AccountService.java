package model;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class AccountService {

    private static final String FILE_PATH = "src" + File.separator + "TxtFile"
            + File.separator + "accounts.txt";

    public User authenticate(String email, String password) {
        for (User user : loadAll()) {
            if (user.matchesCredentials(email, password)) {
                return user;
            }
        }
        return null;
    }

    public boolean emailExists(String email) {
        for (User user : loadAll()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }

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
        if (!found) return false;
        return saveAll(users);
    }

    private boolean saveAll(List<User> users) {
        File file = new File(FILE_PATH);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, false))) {
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
}
