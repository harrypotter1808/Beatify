import java.sql.*;

public class DatabaseManager {
    private static String dbUrl = "jdbc:mysql://localhost:3307/spotify?user=root&password=harry2004";
    private static boolean isMysql = true;

    static {
        // Check for Railway individual MySQL variables first (safest and most robust)
        String host = System.getenv("MYSQLHOST");
        String portEnv = System.getenv("MYSQLPORT");
        String user = System.getenv("MYSQLUSER");
        String password = System.getenv("MYSQLPASSWORD");
        String database = System.getenv("MYSQLDATABASE");

        if (host != null && portEnv != null && user != null && password != null && database != null) {
            dbUrl = "jdbc:mysql://" + host + ":" + portEnv + "/" + database + "?user=" + user + "&password=" + password;
            System.out.println("Constructed JDBC URL from Railway MySQL variables.");
        } else {
            // Check for general DATABASE_URL env var
            String envUrl = System.getenv("DATABASE_URL");
            if (envUrl != null && !envUrl.trim().isEmpty()) {
                if (envUrl.startsWith("mysql://")) {
                    // Convert mysql:// to jdbc:mysql:// and extract credentials if needed
                    // Simple prepend if it's already structured, or let it fall back
                    dbUrl = "jdbc:" + envUrl;
                } else {
                    dbUrl = envUrl;
                }
            }
        }

        // Attempt MySQL connection, otherwise fall back to SQLite
        if (dbUrl.startsWith("jdbc:mysql:")) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                try (Connection conn = DriverManager.getConnection(dbUrl)) {
                    System.out.println("Connected to MySQL Database successfully.");
                }
            } catch (Exception e) {
                System.out.println("MySQL database connection failed: " + e.getMessage());
                System.out.println("Falling back to SQLite database...");
                dbUrl = "jdbc:sqlite:spotify.db";
                isMysql = false;
                try {
                    Class.forName("org.xerial.sqlite.JDBC");
                } catch (Exception ex) {
                    System.out.println("SQLite Driver not found: " + ex.getMessage());
                }
            }
        } else {
            isMysql = false;
            try {
                Class.forName("org.xerial.sqlite.JDBC");
            } catch (Exception ex) {
                System.out.println("SQLite Driver not found: " + ex.getMessage());
            }
        }
        
        // Initialize tables
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Drop existing tables to resolve schema differences (e.g. 'genre' vs 'genere')
            try {
                stmt.execute("DROP TABLE IF EXISTS playlist");
                stmt.execute("DROP TABLE IF EXISTS song_file");
                stmt.execute("DROP TABLE IF EXISTS song");
                stmt.execute("DROP TABLE IF EXISTS user");
                System.out.println("Cleaned up old database tables.");
            } catch (SQLException e) {
                System.out.println("No existing tables to drop: " + e.getMessage());
            }

            // Create user table
            stmt.execute("CREATE TABLE IF NOT EXISTS user (" +
                    "username VARCHAR(255) PRIMARY KEY, " +
                    "email VARCHAR(255), " +
                    "password VARCHAR(255), " +
                    "mobile_no VARCHAR(255), " +
                    "location VARCHAR(255))");

            // Create song table (exactly 4 columns for CLI compatibility)
            stmt.execute("CREATE TABLE IF NOT EXISTS song (" +
                    "title VARCHAR(255) PRIMARY KEY, " +
                    "artist VARCHAR(255), " +
                    "duration DOUBLE, " +
                    "genere VARCHAR(255))");

            // Create song_file table for storing custom audio files/URLs
            stmt.execute("CREATE TABLE IF NOT EXISTS song_file (" +
                    "title VARCHAR(255) PRIMARY KEY, " +
                    "file_path VARCHAR(1000))");

            // Create playlist table
            stmt.execute("CREATE TABLE IF NOT EXISTS playlist (" +
                    "playlist_name VARCHAR(255), " +
                    "username VARCHAR(255), " +
                    "song_title VARCHAR(255))");

            System.out.println("Database tables initialized successfully.");
            
            // Seed default songs if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM song")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    System.out.println("Seeding default songs into the database...");
                    try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO song VALUES(?, ?, ?, ?)")) {
                        Object[][] defaultSongs = {
                            {"Blinding Lights", "The Weeknd", 200.0, "Pop"},
                            {"Shape of You", "Ed Sheeran", 233.0, "Pop"},
                            {"Bohemian Rhapsody", "Queen", 354.0, "Rock"},
                            {"Stairway to Heaven", "Led Zeppelin", 482.0, "Rock"},
                            {"Levitating", "Dua Lipa", 203.0, "Pop"},
                            {"Hotel California", "Eagles", 390.0, "Rock"},
                            {"Take Five", "Dave Brubeck", 324.0, "Jazz"},
                            {"So What", "Miles Davis", 562.0, "Jazz"},
                            {"Cruel Summer", "Taylor Swift", 178.0, "Pop"},
                            {"Smells Like Teen Spirit", "Nirvana", 301.0, "Rock"}
                        };
                        for (Object[] s : defaultSongs) {
                            pstmt.setString(1, (String) s[0]);
                            pstmt.setString(2, (String) s[1]);
                            pstmt.setDouble(3, (Double) s[2]);
                            pstmt.setString(4, (String) s[3]);
                            pstmt.executeUpdate();
                        }
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println("Error initializing database: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!isMysql) {
            // Ensure connection URL has no query params that SQLite doesn't understand
            return DriverManager.getConnection("jdbc:sqlite:spotify.db");
        }
        return DriverManager.getConnection(dbUrl);
    }
}
