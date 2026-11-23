package com.musicbb.app;

import java.io.Serializable;

public class Playlist implements Serializable {
    public long id;
    public String name;
    public long createdMs;
    public int songCount;
    public boolean isSmartPlaylist;

    public Playlist() {}

    public Playlist(long id, String name, long createdMs, int songCount, boolean isSmartPlaylist) {
        this.id = id;
        this.name = name;
        this.createdMs = createdMs;
        this.songCount = songCount;
        this.isSmartPlaylist = isSmartPlaylist;
    }
}
