import static spark.Spark.*;
import com.google.gson.Gson;
import java.io.*;
import java.util.*;

public class SpotifyServer {
    private static Map<String, User> users = new HashMap<>();
    private static MusicLibrary library = new MusicLibrary();
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        loadData();

        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            port(Integer.parseInt(portEnv));
        } else {
            port(4567);
        }
        
        staticFiles.externalLocation("frontend");

        after((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
        });

        // 1. Get Library
        get("/api/library", (req, res) -> {
            res.type("application/json");
            return gson.toJson(library.getLibrary());
        });

        // 2. Login
        post("/api/login", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username").toLowerCase();
            String password = body.get("password");
            User user = users.get(username);
            
            if (user != null && user.getPassword().equals(password)) {
                return gson.toJson(user);
            }
            res.status(401);
            return "{\"error\":\"Invalid credentials\"}";
        });

        // 3. Register
        post("/api/register", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");
            
            if (username == null || password == null) {
                res.status(400); return "{}";
            }
            
            String uname = username.toLowerCase();
            if (users.containsKey(uname)) {
                res.status(409);
                return "{\"error\":\"User already exists\"}";
            }
            
            User newUser = new User(username, password);
            users.put(uname, newUser);
            saveData();
            return gson.toJson(newUser);
        });

        // 4. Get User Playlists
        get("/api/playlists", (req, res) -> {
            res.type("application/json");
            String username = req.queryParams("username");
            if (username == null || !users.containsKey(username.toLowerCase())) {
                res.status(401); return "{}";
            }
            return gson.toJson(users.get(username.toLowerCase()).getAllPlaylists().values());
        });

        // 5. Create Playlist
        post("/api/playlists", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String playlistName = body.get("name");
            
            if (username == null || !users.containsKey(username.toLowerCase())) {
                res.status(401); return "{}";
            }
            User user = users.get(username.toLowerCase());
            user.createPlaylist(playlistName);
            saveData();
            return gson.toJson(user);
        });
        
        // 6. Add Song to Playlist
        post("/api/playlists/add", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String playlistName = body.get("playlistName");
            String title = body.get("songTitle");
            
            try {
                User user = users.get(username.toLowerCase());
                Playlist p = user.getPlaylist(playlistName);
                
                Song songToAdd = null;
                for(Song s : library.getLibrary()) {
                    if (s.getTitle().equals(title)) {
                        songToAdd = s;
                        break;
                    }
                }
                if (songToAdd != null) {
                    p.addSong(songToAdd);
                    saveData();
                    return gson.toJson(p);
                }
            } catch(Exception e) {
                 res.status(400); return "{\"error\":\"" + e.getMessage() + "\"}";
            }
            res.status(400); return "{\"error\":\"Song not found\"}";
        });

        System.out.println("Spotify Web Server started on http://localhost:4567");
    }

    @SuppressWarnings("unchecked")
    private static void loadData() {
        File file = new File("spotify_data.dat");
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                users = (Map<String, User>) ois.readObject();
            } catch (Exception e) {
                System.out.println("Error loading data: " + e.getMessage());
            }
        }
    }

    private static void saveData() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("spotify_data.dat"))) {
            oos.writeObject(users);
        } catch (Exception e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
}
