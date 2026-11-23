package com.musicbb.app;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
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

    public static Bitmap getDefaultVinylArt(String title, String artist, int size) {
        String cacheKey = "default_vinyl_" + (title != null ? title : "") + "_" + size;
        Bitmap cached = memoryCache.get(cacheKey);
        if (cached != null) return cached;

        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setAntiAlias(true);

        float cx = size / 2.0f;
        float cy = size / 2.0f;
        float radius = size * 0.47f;

        // Vinyl outer disc
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF131418);
        canvas.drawCircle(cx, cy, radius, paint);

        // Vinyl outer rim stroke
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.015f);
        paint.setColor(0xFF272932);
        canvas.drawCircle(cx, cy, radius - size * 0.008f, paint);

        // Concentric audio groove rings
        paint.setColor(0xFF1B1D23);
        paint.setStrokeWidth(size * 0.004f);
        for (float r = radius * 0.50f; r < radius * 0.92f; r += size * 0.045f) {
            canvas.drawCircle(cx, cy, r, paint);
        }

        // Center Record Label
        float labelRadius = radius * 0.44f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF22242C);
        canvas.drawCircle(cx, cy, labelRadius, paint);

        // Label border ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.01f);
        paint.setColor(0xFF383C49);
        canvas.drawCircle(cx, cy, labelRadius, paint);

        // Center spindle hole
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0A0A0C);
        canvas.drawCircle(cx, cy, labelRadius * 0.20f, paint);

        // Center spindle ring
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(size * 0.006f);
        paint.setColor(0xFF4A4E5E);
        canvas.drawCircle(cx, cy, labelRadius * 0.20f, paint);

        // Label Text
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF4F4F5);
        paint.setTextSize(size * 0.055f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        canvas.drawText("MUSICBB", cx, cy - labelRadius * 0.42f, paint);

        paint.setColor(0xFFA1A1AA);
        paint.setTextSize(size * 0.040f);
        paint.setFakeBoldText(false);
        String display = (title != null && !title.isEmpty()) ? title : "HI-FI STEREO";
        if (display.length() > 14) {
            display = display.substring(0, 12) + "..";
        }
        canvas.drawText(display, cx, cy + labelRadius * 0.58f, paint);

        memoryCache.put(cacheKey, bitmap);
        return bitmap;
    }

    public static Bitmap getRoundedBitmap(Bitmap bitmap, float radiusPx) {
        if (bitmap == null) return null;
        try {
            Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(output);
            final Paint paint = new Paint();
            final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
            final RectF rectF = new RectF(rect);

            paint.setAntiAlias(true);
            canvas.drawARGB(0, 0, 0, 0);
            paint.setColor(0xFF000000);
            canvas.drawRoundRect(rectF, radiusPx, radiusPx, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
            canvas.drawBitmap(bitmap, rect, rect, paint);
            return output;
        } catch (Exception e) {
            return bitmap;
        }
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
