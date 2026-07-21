package com.User;

public class Song {
    private String title;
    private String artist;
    private double duration;
    private String genre;

    public Song(String title, String artist, double duration, String genre) {
        this.title = title;
        this.artist = artist;
        this.duration = duration;
        this.genre = genre;
    }

    public String getTitle() { return title; }
    public String getArtist() { return artist; }
    public double getDuration() { return duration; }
    public String getGenre() { return genre; }
}
