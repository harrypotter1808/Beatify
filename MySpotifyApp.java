import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MySpotifyApp {

    private Map<String, User> users;
    private User currentUser;
    private MusicLibrary library;
    private Scanner scanner;

    public MySpotifyApp() {
        users = new HashMap<>();
        library = new MusicLibrary();
        scanner = new Scanner(System.in);
        currentUser = null;
        loadData();
    }

    public static void main(String[] args) {
        MySpotifyApp app = new MySpotifyApp();
        app.run();
    }

    public void run() {
        System.out.println("Welcome to MySpotify - Console Player!");
        while (true) {
            if (currentUser == null) {
                showAuthMenu();
            } else {
                showUserMenu();
            }
        }
    }

    private void showAuthMenu() {
        System.out.println("\n--- Auth Menu ---");
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");
        System.out.print("Choose an option: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                registerUser();
                break;
            case "2":
                loginUser();
                break;
            case "3":
                System.out.println("Exiting MySpotify... Goodbye!");
                saveData();
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option. Try again.");
        }
    }

    private void registerUser() {
        System.out.print("Enter a new username: ");
        String username = scanner.nextLine();
        
        if (users.containsKey(username.toLowerCase())) {
            System.out.println("Username already exists!");
            return;
        }
        
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        
        users.put(username.toLowerCase(), new User(username, password));
        System.out.println("Registration successful! You can now log in.");
    }

    private void loginUser() {
        System.out.print("Username: ");
        String username = scanner.nextLine();
        System.out.print("Password: ");
        String password = scanner.nextLine();
        
        try {
            User user = users.get(username.toLowerCase());
            if (user == null || !user.getPassword().equals(password)) {
                throw new InvalidAuthenticationException("Incorrect username or password.");
            }
            currentUser = user;
            System.out.println("Login successful. Welcome, " + currentUser.getUsername() + "!");
        } catch (InvalidAuthenticationException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void showUserMenu() {
        System.out.println("\n--- Dashboard (" + currentUser.getUsername() + ") ---");
        System.out.println("1. Browse Global Music Library");
        System.out.println("2. Search Music Library");
        System.out.println("3. Filter Music Library (Duration)");
        System.out.println("4. Manage My Playlists");
        System.out.println("5. Logout");
        System.out.print("Choose an option: ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                library.displayLibrary();
                break;
            case "2":
                System.out.print("Enter search query (Artist/Title/Genre): ");
                String query = scanner.nextLine();
                printSongs(library.search(query));
                break;
            case "3":
                System.out.print("Enter maximum duration in seconds: ");
                try {
                    int maxSec = Integer.parseInt(scanner.nextLine());
                    printSongs(library.filterByMaxDuration(maxSec));
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input.");
                }
                break;
            case "4":
                managePlaylists();
                break;
            case "5":
                saveData();
                currentUser = null;
                System.out.println("Logged out successfully.");
                break;
            default:
                System.out.println("Invalid option. Try again.");
        }
    }

    private void printSongs(List<Song> songList) {
        if (songList.isEmpty()) {
            System.out.println("No matching songs found.");
            return;
        }
        System.out.println("\nSearch Results:");
        for (int i = 0; i < songList.size(); i++) {
            System.out.println((i + 1) + ". " + songList.get(i).toString());
        }
    }

    private void managePlaylists() {
        while (true) {
            System.out.println("\n--- My Playlists ---");
            System.out.println("1. View all playlists");
            System.out.println("2. Create a new playlist");
            System.out.println("3. Select a playlist");
            System.out.println("4. Delete a playlist");
            System.out.println("5. Go back");
            System.out.print("Choose an option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    Map<String, Playlist> playlists = currentUser.getAllPlaylists();
                    if (playlists.isEmpty()) {
                        System.out.println("You don't have any playlists yet.");
                    } else {
                        playlists.values().forEach(p -> System.out.println("- " + p.getName() + " (" + p.getSongs().size() + " songs)"));
                    }
                    break;
                case "2":
                    System.out.print("Enter new playlist name: ");
                    String pName = scanner.nextLine();
                    currentUser.createPlaylist(pName);
                    break;
                case "3":
                    System.out.print("Enter playlist name to select: ");
                    String selName = scanner.nextLine();
                    try {
                        Playlist p = currentUser.getPlaylist(selName);
                        manageSinglePlaylist(p);
                    } catch (PlaylistNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "4":
                    System.out.print("Enter playlist name to delete: ");
                    String delName = scanner.nextLine();
                    try {
                        currentUser.deletePlaylist(delName);
                        System.out.println("Playlist deleted.");
                    } catch (PlaylistNotFoundException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "5":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void manageSinglePlaylist(Playlist playlist) {
        while (true) {
            playlist.displayPlaylist();
            System.out.println("\n--- Playlist: " + playlist.getName() + " ---");
            System.out.println("1. Add a song from library");
            System.out.println("2. Remove a song");
            System.out.println("3. Play a song");
            System.out.println("4. Play entire playlist");
            System.out.println("5. Sort songs (Bonus)");
            System.out.println("6. Export to txt file (Bonus)");
            System.out.println("7. Go back");
            System.out.print("Choose an option: ");
            
            String choice = scanner.nextLine();
            switch (choice) {
                case "1":
                    library.displayLibrary();
                    System.out.print("Enter the number of the song to add: ");
                    try {
                        int num = Integer.parseInt(scanner.nextLine());
                        if (num > 0 && num <= library.getLibrary().size()) {
                            Song selected = library.getLibrary().get(num - 1);
                            playlist.addSong(selected);
                            System.out.println("Added: " + selected.getTitle());
                        } else {
                            System.out.println("Invalid song number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input.");
                    } catch (DuplicateSongException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "2":
                    System.out.print("Enter the number of the song to remove (from playlist view): ");
                    try {
                        int num = Integer.parseInt(scanner.nextLine());
                        if (num > 0 && num <= playlist.getSongs().size()) {
                            Song toRemove = playlist.getSongs().get(num - 1);
                            playlist.removeSong(toRemove);
                            System.out.println("Song removed.");
                        } else {
                            System.out.println("Invalid song number.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input.");
                    }
                    break;
                case "3":
                    System.out.print("Enter the number of the song to play: ");
                    try {
                        int num = Integer.parseInt(scanner.nextLine());
                        if (num > 0 && num <= playlist.getSongs().size()) {
                            playSong(playlist.getSongs().get(num - 1));
                        } else {
                            System.out.println("Invalid song number.");
                        }
                    } catch (NumberFormatException | InterruptedException e) {
                        System.out.println("Invalid input or playback interrupted.");
                    }
                    break;
                case "4":
                    try {
                        if (playlist.getSongs().isEmpty()) throw new EmptyPlaylistException("Cannot play an empty playlist.");
                        for (Song s : playlist.getSongs()) {
                            playSong(s);
                        }
                    } catch (EmptyPlaylistException | InterruptedException e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                    break;
                case "5":
                    sortPlaylist(playlist);
                    break;
                case "6":
                    exportPlaylist(playlist);
                    break;
                case "7":
                    return;
                default:
                    System.out.println("Invalid option.");
            }
        }
    }

    private void playSong(Song song) throws InterruptedException {
        System.out.println("\n========== NOW PLAYING ==========");
        System.out.println("Title : " + song.getTitle());
        System.out.println("Artist: " + song.getArtist());
        System.out.println("Album : " + song.getAlbum());
        System.out.println("=================================");
        System.out.println("Playing... (Simulating 3 seconds)");
        Thread.sleep(1000);
        System.out.println(". ");
        Thread.sleep(1000);
        System.out.println(".. ");
        Thread.sleep(1000);
        System.out.println("... Finished.\n");
    }

    private void sortPlaylist(Playlist playlist) {
        System.out.println("1. Sort alphabetically by Title");
        System.out.println("2. Sort by Duration");
        System.out.print("Choose mode: ");
        String mode = scanner.nextLine();
        
        if ("1".equals(mode)) {
            Collections.sort(playlist.getSongs(), new Comparator<Song>() {
                @Override
                public int compare(Song s1, Song s2) {
                    return s1.getTitle().compareToIgnoreCase(s2.getTitle());
                }
            });
            System.out.println("Sorted by Title.");
        } else if ("2".equals(mode)) {
            playlist.getSongs().sort((s1, s2) -> Integer.compare(s1.getDurationInSeconds(), s2.getDurationInSeconds()));
            System.out.println("Sorted by Duration.");
        } else {
            System.out.println("Invalid mode.");
        }
    }

    private void exportPlaylist(Playlist playlist) {
        String filename = playlist.getName().replaceAll("\\s+", "_") + "_export.txt";
        try (FileWriter writer = new FileWriter(filename)) {
            writer.write("Playlist: " + playlist.getName() + "\n");
            writer.write("User: " + currentUser.getUsername() + "\n");
            writer.write("Total Duration: " + playlist.getFormattedTotalDuration() + "\n");
            writer.write("------------------------------------------------\n");
            for (int i = 0; i < playlist.getSongs().size(); i++) {
                writer.write(String.format("%d. %s\n", (i+1), playlist.getSongs().get(i).toString()));
            }
            System.out.println("Playlist exported successfully to " + filename);
        } catch (IOException e) {
            System.out.println("Error saving file: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void loadData() {
        java.io.File file = new java.io.File("spotify_data.dat");
        if (file.exists()) {
            try (java.io.ObjectInputStream ois = new java.io.ObjectInputStream(new java.io.FileInputStream(file))) {
                users = (java.util.Map<String, User>) ois.readObject();
            } catch (Exception e) {
                System.out.println("Error loading data: " + e.getMessage());
            }
        }
    }

    private void saveData() {
        try (java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(new java.io.FileOutputStream("spotify_data.dat"))) {
            oos.writeObject(users);
            System.out.println("Data saved successfully.");
        } catch (Exception e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }
}
