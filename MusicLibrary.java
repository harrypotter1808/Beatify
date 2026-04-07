import java.util.ArrayList;
import java.util.List;

public class MusicLibrary {
    private List<Song> library;

    public MusicLibrary() {
        library = new ArrayList<>();
        populateLibrary();
    }

    private void populateLibrary() {
        library.add(new Song("Blinding Lights", "The Weeknd", "After Hours", "Pop", 200));
        library.add(new Song("Shape of You", "Ed Sheeran", "Divide", "Pop", 233));
        library.add(new Song("Bohemian Rhapsody", "Queen", "A Night at the Opera", "Rock", 354));
        library.add(new Song("Stairway to Heaven", "Led Zeppelin", "Led Zeppelin IV", "Rock", 482));
        library.add(new Song("Levitating", "Dua Lipa", "Future Nostalgia", "Pop", 203));
        library.add(new Song("Hotel California", "Eagles", "Hotel California", "Rock", 390));
        library.add(new Song("Take Five", "Dave Brubeck", "Time Out", "Jazz", 324));
        library.add(new Song("So What", "Miles Davis", "Kind of Blue", "Jazz", 562));
        library.add(new Song("Cruel Summer", "Taylor Swift", "Lover", "Pop", 178));
        library.add(new Song("Smells Like Teen Spirit", "Nirvana", "Nevermind", "Rock", 301));
    }

    public List<Song> getLibrary() {
        return library;
    }

    public void displayLibrary() {
        System.out.println("\n--- Global Music Library ---");
        for (int i = 0; i < library.size(); i++) {
            System.out.println((i + 1) + ". " + library.get(i).toString());
        }
    }

    public List<Song> search(String query) {
        List<Song> results = new ArrayList<>();
        String lowerQuery = query.toLowerCase();
        for (Song s : library) {
            if (s.getTitle().toLowerCase().contains(lowerQuery) ||
                s.getArtist().toLowerCase().contains(lowerQuery) ||
                s.getGenre().toLowerCase().contains(lowerQuery)) {
                results.add(s);
            }
        }
        return results;
    }

    public List<Song> filterByMaxDuration(int maxSeconds) {
        List<Song> results = new ArrayList<>();
        for (Song s : library) {
            if (s.getDurationInSeconds() <= maxSeconds) {
                results.add(s);
            }
        }
        return results;
    }
}
