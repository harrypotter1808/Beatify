import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private Map<String, Playlist> playlists;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.playlists = new HashMap<>();
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void createPlaylist(String name) {
        if (!playlists.containsKey(name.toLowerCase())) {
            playlists.put(name.toLowerCase(), new Playlist(name));
            System.out.println("Playlist '" + name + "' created successfully.");
        } else {
            System.out.println("Playlist with this name already exists.");
        }
    }

    public Playlist getPlaylist(String name) throws PlaylistNotFoundException {
        Playlist p = playlists.get(name.toLowerCase());
        if (p == null) {
            throw new PlaylistNotFoundException("Playlist '" + name + "' not found.");
        }
        return p;
    }

    public void deletePlaylist(String name) throws PlaylistNotFoundException {
        if (playlists.remove(name.toLowerCase()) == null) {
            throw new PlaylistNotFoundException("Playlist '" + name + "' not found.");
        }
    }

    public Map<String, Playlist> getAllPlaylists() {
        return playlists;
    }
}
