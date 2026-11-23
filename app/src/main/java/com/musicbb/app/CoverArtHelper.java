package com.musicbb.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.util.LruCache;

import java.io.File;

public class CoverArtHelper {

    private static LruCache<String, Bitmap> memoryCache;

    static {
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8; // Use 1/8th of available memory
        memoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024;
            }
        };
    }

    public static Bitmap getCoverArt(String filePath, int targetSize) {
        if (filePath == null || filePath.isEmpty()) return null;

        String cacheKey = filePath + "_" + targetSize;
        Bitmap cached = memoryCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Bitmap bitmap = null;
        MediaMetadataRetriever mmr = null;
        try {
            mmr = new MediaMetadataRetriever();
            mmr.setDataSource(filePath);
            byte[] art = mmr.getEmbeddedPicture();
            if (art != null) {
                bitmap = decodeSampledBitmap(art, targetSize, targetSize);
            }
        } catch (Exception ignored) {
        } finally {
            if (mmr != null) {
                try { mmr.release(); } catch (Exception ignored) {}
            }
        }

        // Fallback: check folder for cover.jpg / folder.jpg
        if (bitmap == null) {
            bitmap = findFolderArt(filePath, targetSize);
        }

        if (bitmap != null) {
            memoryCache.put(cacheKey, bitmap);
        }
        return bitmap;
    }

    private static Bitmap findFolderArt(String audioFilePath, int targetSize) {
        try {
            File audioFile = new File(audioFilePath);
            File parent = audioFile.getParentFile();
            if (parent != null && parent.exists()) {
                String[] candidates = {"cover.jpg", "folder.jpg", "album.jpg", "cover.png", "folder.png"};
                for (String c : candidates) {
                    File f = new File(parent, c);
                    if (f.exists() && f.length() > 0) {
                        return decodeSampledBitmapFromFile(f.getAbsolutePath(), targetSize, targetSize);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static Bitmap decodeSampledBitmap(byte[] data, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565; // save RAM on BB10
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static Bitmap decodeSampledBitmapFromFile(String path, int reqWidth, int reqHeight) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeFile(path, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
