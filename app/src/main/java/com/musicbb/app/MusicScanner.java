package com.musicbb.app;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class MusicScanner {

    public interface ScanCallback {
        void onProgress(int count, String currentFile);
        void onComplete(int totalFound);
    }

    public static void scanMedia(final Context context, final ScanCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                MusicDatabaseHelper db = new MusicDatabaseHelper(context);
                Set<String> scannedPaths = new HashSet<String>();
                int count = 0;

                // 1. Query Android MediaStore
                try {
                    Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
                    String[] projection = {
                            MediaStore.Audio.Media.DATA,
                            MediaStore.Audio.Media.TITLE,
                            MediaStore.Audio.Media.ARTIST,
                            MediaStore.Audio.Media.ALBUM,
                            MediaStore.Audio.Media.DURATION
                    };
                    Cursor c = context.getContentResolver().query(uri, projection, selection, null, null);
                    if (c != null) {
                        while (c.moveToNext()) {
                            String path = c.getString(0);
                            if (path != null && new File(path).exists()) {
                                String title = c.getString(1);
                                String artist = c.getString(2);
                                String album = c.getString(3);
                                long duration = c.getLong(4);

                                db.insertOrUpdateSong(path, title, artist, album, duration);
                                scannedPaths.add(path);
                                count++;
                                if (callback != null && count % 5 == 0) {
                                    callback.onProgress(count, title);
                                }
                            }
                        }
                        c.close();
                    }
                } catch (Exception ignored) {}

                // 2. Direct File Scanner on Storage Directories (critical for BB10 Android runtime)
                String[] rootPaths = {
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath(),
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath(),
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music",
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/Podcasts",
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                };

                for (String r : rootPaths) {
                    File rootDir = new File(r);
                    if (rootDir.exists() && rootDir.isDirectory()) {
                        count = scanDirectory(rootDir, db, scannedPaths, count, callback);
                    }
                }

                if (callback != null) {
                    callback.onComplete(count);
                }
            }
        }).start();
    }

    private static int scanDirectory(File dir, MusicDatabaseHelper db, Set<String> scanned, int count, ScanCallback cb) {
        File[] files = dir.listFiles();
        if (files == null) return count;

        for (File f : files) {
            if (f.isDirectory()) {
                // Avoid hidden dirs and Android system data
                if (!f.getName().startsWith(".") && !f.getName().equalsIgnoreCase("Android")) {
                    count = scanDirectory(f, db, scanned, count, cb);
                }
            } else if (isAudioFile(f.getName())) {
                String path = f.getAbsolutePath();
                if (!scanned.contains(path)) {
                    scanned.add(path);
                    readAndInsertFile(f, db);
                    count++;
                    if (cb != null) {
                        cb.onProgress(count, f.getName());
                    }
                }
            }
        }
        return count;
    }

    private static boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".flac") ||
                lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".aac");
    }

    private static void readAndInsertFile(File f, MusicDatabaseHelper db) {
        String title = f.getName();
        String artist = "Unknown Artist";
        String album = "Unknown Album";
        long duration = 0;

        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(f.getAbsolutePath());
            String metaTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            String metaArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            String metaAlbum = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            String metaDur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            if (metaTitle != null && !metaTitle.trim().isEmpty()) title = metaTitle.trim();
            if (metaArtist != null && !metaArtist.trim().isEmpty()) artist = metaArtist.trim();
            if (metaAlbum != null && !metaAlbum.trim().isEmpty()) album = metaAlbum.trim();
            if (metaDur != null) {
                try { duration = Long.parseLong(metaDur); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        } finally {
            if (mmr != null) {
                try { mmr.release(); } catch (Exception ignored) {}
            }
        }

        // Clean title if still filename
        if (title.endsWith(".mp3") || title.endsWith(".flac") || title.endsWith(".m4a")) {
            int dot = title.lastIndexOf('.');
            if (dot > 0) title = title.substring(0, dot);
        }

        db.insertOrUpdateSong(f.getAbsolutePath(), title, artist, album, duration);
    }
}
