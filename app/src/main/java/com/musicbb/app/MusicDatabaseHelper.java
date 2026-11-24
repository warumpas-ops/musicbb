package com.musicbb.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class MusicDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "musicbb.db";
    private static final int DB_VERSION = 1;

    public static final String TABLE_SONGS = "songs";
    public static final String TABLE_PLAYLISTS = "playlists";
    public static final String TABLE_PLAYLIST_SONGS = "playlist_songs";

    public MusicDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SONGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "file_path TEXT UNIQUE," +
                "title TEXT," +
                "artist TEXT," +
                "album TEXT," +
                "duration_ms INTEGER DEFAULT 0," +
                "date_added INTEGER DEFAULT 0," +
                "play_count INTEGER DEFAULT 0," +
                "last_played_ms INTEGER DEFAULT 0," +
                "listen_seconds INTEGER DEFAULT 0," +
                "is_favorite INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PLAYLISTS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT UNIQUE," +
                "created_ms INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_PLAYLIST_SONGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "playlist_id INTEGER," +
                "song_id INTEGER," +
                "sort_order INTEGER DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLIST_SONGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLAYLISTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SONGS);
        onCreate(db);
    }

    // ==================== SONG OPERATIONS ====================

    public long insertOrUpdateSong(String path, String title, String artist, String album, long durationMs) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_SONGS, new String[]{"id"}, "file_path=?", new String[]{path}, null, null, null);
        if (c.moveToFirst()) {
            long id = c.getLong(0);
            c.close();
            ContentValues cv = new ContentValues();
            cv.put("title", title);
            cv.put("artist", artist);
            cv.put("album", album);
            if (durationMs > 0) cv.put("duration_ms", durationMs);
            db.update(TABLE_SONGS, cv, "id=?", new String[]{String.valueOf(id)});
            return id;
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("file_path", path);
        cv.put("title", title);
        cv.put("artist", artist);
        cv.put("album", album);
        cv.put("duration_ms", durationMs);
        cv.put("date_added", System.currentTimeMillis());
        return db.insert(TABLE_SONGS, null, cv);
    }

    public List<Song> getAllSongs() {
        List<Song> list = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, null, null, null, null, "title COLLATE NOCASE ASC");
        while (c.moveToNext()) {
            list.add(cursorToSong(c));
        }
        c.close();
        return list;
    }

    public Song getSongById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, "id=?", new String[]{String.valueOf(id)}, null, null, null);
        Song s = null;
        if (c.moveToFirst()) {
            s = cursorToSong(c);
        }
        c.close();
        return s;
    }

    public Song getSongByPath(String path) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, "file_path=?", new String[]{path}, null, null, null);
        Song s = null;
        if (c.moveToFirst()) {
            s = cursorToSong(c);
        }
        c.close();
        return s;
    }

    public void incrementPlayCount(long songId, int listenSec) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_SONGS +
                " SET play_count = play_count + 1," +
                " last_played_ms = " + System.currentTimeMillis() + "," +
                " listen_seconds = listen_seconds + " + Math.max(0, listenSec) +
                " WHERE id = " + songId);
    }

    // Adds listened time to hours streamed WITHOUT counting it as a full play
    public void addListenTimeOnly(long songId, int listenSec) {
        if (listenSec <= 0) return;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("UPDATE " + TABLE_SONGS +
                " SET listen_seconds = listen_seconds + " + listenSec +
                " WHERE id = " + songId);
    }

    public boolean toggleFavorite(long songId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_SONGS, new String[]{"is_favorite"}, "id=?", new String[]{String.valueOf(songId)}, null, null, null);
        boolean newState = true;
        if (c.moveToFirst()) {
            int current = c.getInt(0);
            newState = current == 0;
            ContentValues cv = new ContentValues();
            cv.put("is_favorite", newState ? 1 : 0);
            db.update(TABLE_SONGS, cv, "id=?", new String[]{String.valueOf(songId)});
        }
        c.close();
        return newState;
    }

    public List<Song> getMostPlayedSongs(int limit) {
        List<Song> list = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, "play_count > 0", null, null, null, "play_count DESC, last_played_ms DESC", String.valueOf(limit));
        while (c.moveToNext()) {
            list.add(cursorToSong(c));
        }
        c.close();
        return list;
    }

    public List<Song> getFavoriteSongs() {
        List<Song> list = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, "is_favorite = 1", null, null, null, "title COLLATE NOCASE ASC");
        while (c.moveToNext()) {
            list.add(cursorToSong(c));
        }
        c.close();
        return list;
    }

    public List<Song> getRecentlyPlayedSongs(int limit) {
        List<Song> list = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_SONGS, null, "last_played_ms > 0", null, null, null, "last_played_ms DESC", String.valueOf(limit));
        while (c.moveToNext()) {
            list.add(cursorToSong(c));
        }
        c.close();
        return list;
    }

    // ==================== STATS OPERATIONS ====================

    public int getTotalPlays() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(play_count) FROM " + TABLE_SONGS, null);
        int total = 0;
        if (c.moveToFirst()) {
            total = c.getInt(0);
        }
        c.close();
        return total;
    }

    public double getTotalListeningHours() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(listen_seconds) FROM " + TABLE_SONGS, null);
        double sec = 0;
        if (c.moveToFirst()) {
            sec = c.getDouble(0);
        }
        c.close();
        return sec / 3600.0;
    }

    public int getTotalSongsCount() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_SONGS, null);
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    // ==================== PLAYLIST OPERATIONS ====================

    public long createPlaylist(String name) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", name.trim());
        cv.put("created_ms", System.currentTimeMillis());
        return db.insertWithOnConflict(TABLE_PLAYLISTS, null, cv, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public boolean addSongToPlaylist(long playlistId, long songId) {
        SQLiteDatabase db = getWritableDatabase();
        Cursor c = db.query(TABLE_PLAYLIST_SONGS, new String[]{"id"}, "playlist_id=? AND song_id=?",
                new String[]{String.valueOf(playlistId), String.valueOf(songId)}, null, null, null);
        if (c.moveToFirst()) {
            c.close();
            return false; // already in playlist
        }
        c.close();

        ContentValues cv = new ContentValues();
        cv.put("playlist_id", playlistId);
        cv.put("song_id", songId);
        db.insert(TABLE_PLAYLIST_SONGS, null, cv);
        return true;
    }

    public void removeSongFromPlaylist(long playlistId, long songId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS, "playlist_id=? AND song_id=?",
                new String[]{String.valueOf(playlistId), String.valueOf(songId)});
    }

    public List<Playlist> getAllCustomPlaylists() {
        List<Playlist> list = new ArrayList<Playlist>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PLAYLISTS, null, null, null, null, null, "name COLLATE NOCASE ASC");
        while (c.moveToNext()) {
            Playlist p = new Playlist();
            p.id = c.getLong(c.getColumnIndex("id"));
            p.name = c.getString(c.getColumnIndex("name"));
            p.createdMs = c.getLong(c.getColumnIndex("created_ms"));
            p.isSmartPlaylist = false;

            // Count songs
            Cursor countC = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PLAYLIST_SONGS + " WHERE playlist_id=?",
                    new String[]{String.valueOf(p.id)});
            if (countC.moveToFirst()) {
                p.songCount = countC.getInt(0);
            }
            countC.close();

            list.add(p);
        }
        c.close();
        return list;
    }

    public List<Song> getPlaylistSongs(long playlistId) {
        List<Song> list = new ArrayList<Song>();
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT s.* FROM " + TABLE_SONGS + " s " +
                "INNER JOIN " + TABLE_PLAYLIST_SONGS + " ps ON s.id = ps.song_id " +
                "WHERE ps.playlist_id = ? ORDER BY ps.id ASC";
        Cursor c = db.rawQuery(query, new String[]{String.valueOf(playlistId)});
        while (c.moveToNext()) {
            list.add(cursorToSong(c));
        }
        c.close();
        return list;
    }

    public void deletePlaylist(long playlistId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_PLAYLIST_SONGS, "playlist_id=?", new String[]{String.valueOf(playlistId)});
        db.delete(TABLE_PLAYLISTS, "id=?", new String[]{String.valueOf(playlistId)});
    }

    // Helper
    private Song cursorToSong(Cursor c) {
        return new Song(
                c.getLong(c.getColumnIndex("id")),
                c.getString(c.getColumnIndex("file_path")),
                c.getString(c.getColumnIndex("title")),
                c.getString(c.getColumnIndex("artist")),
                c.getString(c.getColumnIndex("album")),
                c.getLong(c.getColumnIndex("duration_ms")),
                c.getInt(c.getColumnIndex("play_count")),
                c.getLong(c.getColumnIndex("last_played_ms")),
                c.getInt(c.getColumnIndex("is_favorite")) == 1
        );
    }
}
