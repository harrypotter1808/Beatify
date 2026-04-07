import java.io.Serializable;

public class Song implements Serializable {
    private String title;
    private String artist;
    private String album;
    private String genre;
    private int durationInSeconds;

    public Song(String title, String artist, String album, String genre, int durationInSeconds) {
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.genre = genre;
        this.durationInSeconds = durationInSeconds;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public String getAlbum() { return album; }
    public String getGenre() { return genre; }
    public int getDurationInSeconds() { return durationInSeconds; }

    public String getFormattedDuration() {
        int minutes = durationInSeconds / 60;
        int seconds = durationInSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public String toString() {
        return title + " - " + artist + " [" + album + "] (" + getFormattedDuration() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Song song = (Song) obj;
        return title.equals(song.title) && artist.equals(song.artist);
    }

    @Override
    public int hashCode() {
        return title.hashCode() + artist.hashCode();
    }
}
