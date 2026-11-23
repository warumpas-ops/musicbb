package com.musicbb.app;

import java.io.File;
import java.io.Serializable;
import java.util.Locale;

public class Song implements Serializable {
    public long id;
    public String filePath;
    public String title;
    public String artist;
    public String album;
    public long durationMs;
    public int playCount;
    public long lastPlayedMs;
    public boolean isFavorite;

    public Song() {}

    public Song(long id, String filePath, String title, String artist, String album,
                long durationMs, int playCount, long lastPlayedMs, boolean isFavorite) {
        this.id = id;
        this.filePath = filePath;
        this.title = title != null && !title.isEmpty() ? title : new File(filePath).getName();
        this.artist = artist != null && !artist.isEmpty() ? artist : "Unknown Artist";
        this.album = album != null && !album.isEmpty() ? album : "Unknown Album";
        this.durationMs = durationMs;
        this.playCount = playCount;
        this.lastPlayedMs = lastPlayedMs;
        this.isFavorite = isFavorite;
    }

    public String getFormattedDuration() {
        if (durationMs <= 0) return "00:00";
        long sec = durationMs / 1000;
        long m = sec / 60;
        long s = sec % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    public String getArtistAlbumString() {
        String a = artist != null ? artist : "Unknown";
        String alb = album != null && !album.isEmpty() && !album.equalsIgnoreCase("Unknown Album") ? " — " + album : "";
        return a + alb;
    }
}
