import java.util.ArrayList;
import java.util.List;
import java.io.Serializable;

public class Playlist implements Serializable {
    private String name;
    private List<Song> songs;

    public Playlist(String name) {
        this.name = name;
        this.songs = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void rename(String newName) {
        this.name = newName;
    }

    public List<Song> getSongs() {
        return songs;
    }

    public void addSong(Song song) throws DuplicateSongException {
        if (songs.contains(song)) {
            throw new DuplicateSongException("Song is already in the playlist.");
        }
        songs.add(song);
    }

    public void removeSong(Song song) {
        songs.remove(song);
    }

    public int getTotalDuration() {
        int total = 0;
        for (Song s : songs) {
            total += s.getDurationInSeconds();
        }
        return total;
    }

    public String getFormattedTotalDuration() {
        int totalSeconds = getTotalDuration();
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public void displayPlaylist() {
        System.out.println("\nPlaylist: " + name + " (Total Time: " + getFormattedTotalDuration() + ")");
        if (songs.isEmpty()) {
            System.out.println("  [Empty]");
            return;
        }
        for (int i = 0; i < songs.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + songs.get(i).toString());
        }
    }
}
