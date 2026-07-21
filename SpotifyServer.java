import static spark.Spark.*;
import com.google.gson.Gson;
import java.sql.*;
import java.util.*;
import com.Admin.Admin;

public class SpotifyServer {
    private static Gson gson = new Gson();
    private static Admin adminHelper = new Admin();

    public static void main(String[] args) {
        // Trigger Database Connection & Table Initialization
        try {
            Connection trigger = DatabaseManager.getConnection();
            trigger.close();
        } catch (Exception e) {
            System.out.println("Warning: Database pre-load failed: " + e.getMessage());
        }

        String portEnv = System.getenv("PORT");
        if (portEnv != null) {
            port(Integer.parseInt(portEnv));
        } else {
            port(4567);
        }
        
        staticFiles.externalLocation("frontend");

        // CORS Headers
        after((req, res) -> {
            res.header("Access-Control-Allow-Origin", "*");
            res.header("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
            res.header("Access-Control-Allow-Headers", "Content-Type,Authorization");
        });

        // Options handler for preflight requests
        options("/*", (req, res) -> {
            String accessControlRequestHeaders = req.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                res.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = req.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                res.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            return "OK";
        });

        // 1. Get Library (Browse all songs)
        get("/api/library", (req, res) -> {
            res.type("application/json");
            List<Map<String, Object>> songs = new ArrayList<>();
            try (Connection conn = DatabaseManager.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT title, artist, duration, genere FROM song")) {
                while (rs.next()) {
                    songs.add(mapSong(
                        rs.getString("title"),
                        rs.getString("artist"),
                        rs.getDouble("duration"),
                        rs.getString("genere")
                    ));
                }
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
            return gson.toJson(songs);
        });

        // 2. User Login
        post("/api/login", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            if (username == null || password == null) {
                res.status(400);
                return "{\"error\":\"Username and password are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM user WHERE username = ? AND password = ?")) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> userMap = new HashMap<>();
                        userMap.put("username", rs.getString("username"));
                        userMap.put("email", rs.getString("email"));
                        userMap.put("mobileNo", rs.getString("mobile_no"));
                        userMap.put("location", rs.getString("location"));
                        userMap.put("playlists", getUserPlaylistsMap(rs.getString("username")));
                        return gson.toJson(userMap);
                    }
                }
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
            res.status(401);
            return "{\"error\":\"Invalid username or password\"}";
        });

        // 3. User Register
        post("/api/register", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");
            String email = body.get("email") != null ? body.get("email") : "";
            String mobileNo = body.get("mobileNo") != null ? body.get("mobileNo") : "";
            String location = body.get("location") != null ? body.get("location") : "";

            if (username == null || password == null) {
                res.status(400);
                return "{\"error\":\"Username and password are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                // Check if username exists
                try (PreparedStatement check = conn.prepareStatement("SELECT 1 FROM user WHERE username = ?")) {
                    check.setString(1, username);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            res.status(409);
                            return "{\"error\":\"Username already exists\"}";
                        }
                    }
                }
                // Insert new user
                try (PreparedStatement insert = conn.prepareStatement("INSERT INTO user VALUES(?, ?, ?, ?, ?)")) {
                    insert.setString(1, username);
                    insert.setString(2, email);
                    insert.setString(3, password);
                    insert.setString(4, mobileNo);
                    insert.setString(5, location);
                    insert.executeUpdate();
                }

                Map<String, Object> userMap = new HashMap<>();
                userMap.put("username", username);
                userMap.put("email", email);
                userMap.put("mobileNo", mobileNo);
                userMap.put("location", location);
                userMap.put("playlists", new HashMap<>());
                return gson.toJson(userMap);
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 4. Admin Authentication
        post("/api/admin/login", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String password = body.get("password");

            if (adminHelper.isAuthenticate(username, password)) {
                return "{\"status\":\"success\", \"role\":\"admin\"}";
            }
            res.status(401);
            return "{\"error\":\"Invalid Admin credentials\"}";
        });

        // 5. Admin Add Song
        post("/api/songs", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            String title = (String) body.get("title");
            String artist = (String) body.get("artist");
            Double duration = body.get("duration") != null ? Double.parseDouble(body.get("duration").toString()) : 0.0;
            String genre = (String) body.get("genre");

            if (title == null || artist == null) {
                res.status(400);
                return "{\"error\":\"Title and Artist are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("INSERT INTO song VALUES(?, ?, ?, ?)")) {
                pstmt.setString(1, title);
                pstmt.setString(2, artist);
                pstmt.setDouble(3, duration);
                pstmt.setString(4, genre);
                pstmt.executeUpdate();
                return gson.toJson(mapSong(title, artist, duration, genre));
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 6. Admin Update Song
        put("/api/songs", (req, res) -> {
            res.type("application/json");
            Map<String, Object> body = gson.fromJson(req.body(), Map.class);
            String title = (String) body.get("title");
            String artist = (String) body.get("artist");
            Double duration = body.get("duration") != null ? Double.parseDouble(body.get("duration").toString()) : null;
            String genre = (String) body.get("genre");

            if (title == null) {
                res.status(400);
                return "{\"error\":\"Title is required to update a song\"}";
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                // Fetch existing details
                String existingArtist = "";
                double existingDuration = 0.0;
                String existingGenre = "";
                try (PreparedStatement sel = conn.prepareStatement("SELECT * FROM song WHERE title = ?")) {
                    sel.setString(1, title);
                    try (ResultSet rs = sel.executeQuery()) {
                        if (rs.next()) {
                            existingArtist = rs.getString("artist");
                            existingDuration = rs.getDouble("duration");
                            existingGenre = rs.getString("genere");
                        } else {
                            res.status(404);
                            return "{\"error\":\"Song not found\"}";
                        }
                    }
                }

                // Update
                try (PreparedStatement upd = conn.prepareStatement(
                        "UPDATE song SET artist = ?, duration = ?, genere = ? WHERE title = ?")) {
                    upd.setString(1, artist != null ? artist : existingArtist);
                    upd.setDouble(2, duration != null ? duration : existingDuration);
                    upd.setString(3, genre != null ? genre : existingGenre);
                    upd.setString(4, title);
                    upd.executeUpdate();
                }

                return gson.toJson(mapSong(
                    title,
                    artist != null ? artist : existingArtist,
                    duration != null ? duration : existingDuration,
                    genre != null ? genre : existingGenre
                ));
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 7. Admin Delete Song
        delete("/api/songs/:title", (req, res) -> {
            res.type("application/json");
            String title = req.params(":title");
            try (Connection conn = DatabaseManager.getConnection()) {
                // Delete from playlist table first to avoid orphaned references
                try (PreparedStatement delP = conn.prepareStatement("DELETE FROM playlist WHERE song_title = ?")) {
                    delP.setString(1, title);
                    delP.executeUpdate();
                }
                // Delete from song table
                try (PreparedStatement delS = conn.prepareStatement("DELETE FROM song WHERE title = ?")) {
                    delS.setString(1, title);
                    int count = delS.executeUpdate();
                    if (count > 0) {
                        return "{\"status\":\"success\",\"message\":\"Song deleted successfully\"}";
                    }
                }
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
            res.status(404);
            return "{\"error\":\"Song not found\"}";
        });

        // 8. Get User Playlists
        get("/api/playlists", (req, res) -> {
            res.type("application/json");
            String username = req.queryParams("username");
            if (username == null) {
                res.status(400);
                return "{\"error\":\"Username parameter is required\"}";
            }
            return gson.toJson(getUserPlaylistsList(username));
        });

        // 9. Create Playlist (Inserts a placeholder row)
        post("/api/playlists", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String playlistName = body.get("name");

            if (username == null || playlistName == null) {
                res.status(400);
                return "{\"error\":\"Username and playlist name are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                // Insert placeholder row for empty playlist
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO playlist (playlist_name, username, song_title) VALUES (?, ?, '')")) {
                    pstmt.setString(1, playlistName);
                    pstmt.setString(2, username);
                    pstmt.executeUpdate();
                }
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("username", username);
                userMap.put("playlists", getUserPlaylistsMap(username));
                return gson.toJson(userMap);
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 10. Add Song to Playlist
        post("/api/playlists/add", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String playlistName = body.get("playlistName");
            String songTitle = body.get("songTitle");

            if (username == null || playlistName == null || songTitle == null) {
                res.status(400);
                return "{\"error\":\"Username, playlistName, and songTitle are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection()) {
                // Check if song exists
                boolean exists = false;
                try (PreparedStatement chk = conn.prepareStatement("SELECT 1 FROM song WHERE title = ?")) {
                    chk.setString(1, songTitle);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) exists = true;
                    }
                }
                if (!exists) {
                    res.status(404);
                    return "{\"error\":\"Song does not exist in library\"}";
                }

                // Add song to playlist
                try (PreparedStatement pstmt = conn.prepareStatement(
                        "INSERT INTO playlist (playlist_name, username, song_title) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, playlistName);
                    pstmt.setString(2, username);
                    pstmt.setString(3, songTitle);
                    pstmt.executeUpdate();
                }

                // Remove the empty placeholder if exists
                try (PreparedStatement clean = conn.prepareStatement(
                        "DELETE FROM playlist WHERE playlist_name = ? AND username = ? AND (song_title = '' OR song_title IS NULL)")) {
                    clean.setString(1, playlistName);
                    clean.setString(2, username);
                    clean.executeUpdate();
                }

                return "{\"status\":\"success\", \"message\":\"Song added to playlist\"}";
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 11. Remove Song from Playlist
        post("/api/playlists/remove", (req, res) -> {
            res.type("application/json");
            Map<String, String> body = gson.fromJson(req.body(), Map.class);
            String username = body.get("username");
            String playlistName = body.get("playlistName");
            String songTitle = body.get("songTitle");

            if (username == null || playlistName == null || songTitle == null) {
                res.status(400);
                return "{\"error\":\"Username, playlistName, and songTitle are required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM playlist WHERE playlist_name = ? AND username = ? AND song_title = ?")) {
                pstmt.setString(1, playlistName);
                pstmt.setString(2, username);
                pstmt.setString(3, songTitle);
                int count = pstmt.executeUpdate();
                
                // If playlist is now empty, add a placeholder row so it isn't completely deleted
                boolean hasSongs = false;
                try (PreparedStatement chk = conn.prepareStatement(
                        "SELECT 1 FROM playlist WHERE playlist_name = ? AND username = ?")) {
                    chk.setString(1, playlistName);
                    chk.setString(2, username);
                    try (ResultSet rs = chk.executeQuery()) {
                        if (rs.next()) hasSongs = true;
                    }
                }
                if (!hasSongs) {
                    try (PreparedStatement placeholder = conn.prepareStatement(
                            "INSERT INTO playlist VALUES (?, ?, '')")) {
                        placeholder.setString(1, playlistName);
                        placeholder.setString(2, username);
                        placeholder.executeUpdate();
                    }
                }

                if (count > 0) {
                    return "{\"status\":\"success\", \"message\":\"Song removed from playlist\"}";
                }
                res.status(404);
                return "{\"error\":\"Song not found in playlist\"}";
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        // 12. Delete Playlist Entirely
        delete("/api/playlists/:name", (req, res) -> {
            res.type("application/json");
            String playlistName = req.params(":name");
            String username = req.queryParams("username");

            if (username == null) {
                res.status(400);
                return "{\"error\":\"Username query parameter is required\"}";
            }

            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                        "DELETE FROM playlist WHERE playlist_name = ? AND username = ?")) {
                pstmt.setString(1, playlistName);
                pstmt.setString(2, username);
                int count = pstmt.executeUpdate();
                if (count > 0) {
                    return "{\"status\":\"success\", \"message\":\"Playlist deleted successfully\"}";
                }
                res.status(404);
                return "{\"error\":\"Playlist not found\"}";
            } catch (SQLException e) {
                res.status(500);
                return "{\"error\":\"" + e.getMessage() + "\"}";
            }
        });

        System.out.println("Spotify Web Server started on http://localhost:4567");
    }

    // Helper: Map table properties to a clean Song representation matching what the frontend expects
    private static Map<String, Object> mapSong(String title, String artist, double duration, String genre) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("artist", artist);
        
        // Album mapping
        if (title.equalsIgnoreCase("Blinding Lights")) {
            map.put("album", "After Hours");
        } else if (title.equalsIgnoreCase("Shape of You")) {
            map.put("album", "Divide");
        } else if (title.equalsIgnoreCase("Bohemian Rhapsody")) {
            map.put("album", "A Night at the Opera");
        } else if (title.equalsIgnoreCase("Stairway to Heaven")) {
            map.put("album", "Led Zeppelin IV");
        } else if (title.equalsIgnoreCase("Levitating")) {
            map.put("album", "Future Nostalgia");
        } else if (title.equalsIgnoreCase("Hotel California")) {
            map.put("album", "Hotel California");
        } else if (title.equalsIgnoreCase("Take Five")) {
            map.put("album", "Time Out");
        } else if (title.equalsIgnoreCase("So What")) {
            map.put("album", "Kind of Blue");
        } else if (title.equalsIgnoreCase("Cruel Summer")) {
            map.put("album", "Lover");
        } else if (title.equalsIgnoreCase("Smells Like Teen Spirit")) {
            map.put("album", "Nevermind");
        } else {
            map.put("album", "Single");
        }

        map.put("genre", genre);
        map.put("durationInSeconds", (int) duration);
        
        // Set filePath (if Blinding Lights, use song1.mp3, otherwise fallback to song1.mp3 since it's our only audio asset)
        map.put("filePath", "/music/song1.mp3");
        return map;
    }

    // Helper: Get user's playlists as a Map structure (for nested User responses)
    private static Map<String, Object> getUserPlaylistsMap(String username) {
        Map<String, Object> map = new HashMap<>();
        List<Map<String, Object>> playlists = getUserPlaylistsList(username);
        for (Map<String, Object> p : playlists) {
            map.put(((String) p.get("name")).toLowerCase(), p);
        }
        return map;
    }

    // Helper: Get user's playlists as a list
    private static List<Map<String, Object>> getUserPlaylistsList(String username) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, List<Map<String, Object>>> playlistGroups = new LinkedHashMap<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                "SELECT p.playlist_name, p.song_title, s.artist, s.duration, s.genere " +
                "FROM playlist p " +
                "LEFT JOIN song s ON p.song_title = s.title " +
                "WHERE p.username = ?")) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String pName = rs.getString("playlist_name");
                    String sTitle = rs.getString("song_title");
                    
                    if (!playlistGroups.containsKey(pName)) {
                        playlistGroups.put(pName, new ArrayList<>());
                    }
                    
                    // Add song if it's not a placeholder row
                    if (sTitle != null && !sTitle.trim().isEmpty()) {
                        playlistGroups.get(pName).add(mapSong(
                            sTitle,
                            rs.getString("artist") != null ? rs.getString("artist") : "Unknown Artist",
                            rs.getDouble("duration"),
                            rs.getString("genere") != null ? rs.getString("genere") : "Unknown Genre"
                        ));
                    }
                }
            }

            for (Map.Entry<String, List<Map<String, Object>>> entry : playlistGroups.entrySet()) {
                Map<String, Object> playlistMap = new HashMap<>();
                playlistMap.put("name", entry.getKey());
                playlistMap.put("songs", entry.getValue());
                list.add(playlistMap);
            }

        } catch (SQLException e) {
            System.out.println("Error fetching playlists for user " + username + ": " + e.getMessage());
        }

        return list;
    }
}
