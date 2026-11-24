package com.musicbb.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AudioService extends Service implements
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final String ACTION_TOGGLE_PLAY = "com.musicbb.action.TOGGLE_PLAY";
    public static final String ACTION_NEXT = "com.musicbb.action.NEXT";
    public static final String ACTION_PREV = "com.musicbb.action.PREV";
    public static final String ACTION_STOP = "com.musicbb.action.STOP";

    public static final String BROADCAST_STATE_CHANGED = "com.musicbb.STATE_CHANGED";
    public static final String BROADCAST_TRACK_CHANGED = "com.musicbb.TRACK_CHANGED";

    private final IBinder binder = new AudioBinder();
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private PowerManager.WakeLock wakeLock;
    private MusicDatabaseHelper db;

    // Queue & State
    private List<Song> originalQueue = new ArrayList<Song>();
    private List<Song> playQueue = new ArrayList<Song>();
    private int currentIndex = -1;
    private Song currentSong = null;

    private boolean isShuffle = false;
    private int repeatMode = 0; // 0=all, 1=one, 2=off
    private long songStartTimeMs = 0;

    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        db = new MusicDatabaseHelper(this);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicBB:WakeLock");
        }

        initMediaPlayer();
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String act = intent.getAction();
            if (ACTION_TOGGLE_PLAY.equals(act)) {
                togglePlayPause();
            } else if (ACTION_NEXT.equals(act)) {
                next();
            } else if (ACTION_PREV.equals(act)) {
                previous();
            } else if (ACTION_STOP.equals(act)) {
                pause();
                stopForeground(true);
            }
        }
        return START_NOT_STICKY;
    }

    // ==================== PLAYBACK CONTROLS ====================

    public void playQueue(List<Song> songs, int startIndex) {
        if (songs == null || songs.isEmpty()) return;
        this.originalQueue = new ArrayList<Song>(songs);
        this.playQueue = new ArrayList<Song>(songs);

        if (isShuffle) {
            Song target = (startIndex >= 0 && startIndex < songs.size()) ? songs.get(startIndex) : null;
            Collections.shuffle(this.playQueue);
            if (target != null) {
                this.playQueue.remove(target);
                this.playQueue.add(0, target);
                this.currentIndex = 0;
            } else {
                this.currentIndex = 0;
            }
        } else {
            this.currentIndex = (startIndex >= 0 && startIndex < songs.size()) ? startIndex : 0;
        }

        playCurrent();
    }

    public void playSingle(Song song) {
        if (song == null) return;
        List<Song> singleList = new ArrayList<Song>();
        singleList.add(song);
        playQueue(singleList, 0);
    }

    private void playCurrent() {
        if (currentIndex < 0 || currentIndex >= playQueue.size()) return;

        recordCurrentListenStats(false); // switching track → time only, no play count

        currentSong = playQueue.get(currentIndex);
        songStartTimeMs = System.currentTimeMillis();

        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            return;
        }

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        try {
            initMediaPlayer();
            mediaPlayer.setDataSource(currentSong.filePath);
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

        broadcastTrackChanged();
        updateNotification();
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) return;
        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            resume();
        }
    }

    public void resume() {
        if (mediaPlayer == null) return;
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire();
            }
            mediaPlayer.start();
            broadcastStateChanged();
            updateNotification();
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        broadcastStateChanged();
        updateNotification();
    }

    public void next() {
        if (playQueue.isEmpty()) return;
        if (repeatMode == 1) { // Repeat one
            seekTo(0);
            resume();
            return;
        }

        currentIndex++;
        if (currentIndex >= playQueue.size()) {
            if (repeatMode == 0) { // Repeat all
                currentIndex = 0;
            } else {
                currentIndex = playQueue.size() - 1;
                pause();
                return;
            }
        }
        playCurrent();
    }

    public void previous() {
        if (playQueue.isEmpty()) return;
        if (mediaPlayer != null && mediaPlayer.getCurrentPosition() > 3000) {
            seekTo(0);
            return;
        }

        currentIndex--;
        if (currentIndex < 0) {
            if (repeatMode == 0) {
                currentIndex = playQueue.size() - 1;
            } else {
                currentIndex = 0;
            }
        }
        playCurrent();
    }

    public void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
        }
    }

    public void toggleShuffle() {
        isShuffle = !isShuffle;
        Song playing = getCurrentSong();
        if (isShuffle) {
            Collections.shuffle(playQueue);
            if (playing != null) {
                playQueue.remove(playing);
                playQueue.add(0, playing);
                currentIndex = 0;
            }
        } else {
            playQueue = new ArrayList<Song>(originalQueue);
            if (playing != null) {
                currentIndex = playQueue.indexOf(playing);
                if (currentIndex < 0) currentIndex = 0;
            }
        }
        broadcastStateChanged();
    }

    public void toggleRepeat() {
        repeatMode = (repeatMode + 1) % 3; // 0=all, 1=one, 2=off
        broadcastStateChanged();
    }

    // ==================== GETTERS ====================

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getCurrentPosition() {
        return (mediaPlayer != null) ? mediaPlayer.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return (mediaPlayer != null) ? mediaPlayer.getDuration() : 0;
    }

    public int getAudioSessionId() {
        return (mediaPlayer != null) ? mediaPlayer.getAudioSessionId() : 0;
    }

    // ==================== LISTEN STATS ====================

    private void recordCurrentListenStats(boolean completedFully) {
        if (currentSong != null && songStartTimeMs > 0) {
            long durationElapsedSec = (System.currentTimeMillis() - songStartTimeMs) / 1000;
            if (completedFully) {
                // Song played all the way through → count as a full listen + add time
                db.incrementPlayCount(currentSong.id, (int) durationElapsedSec);
            } else {
                // Song was skipped / interrupted → only add time streamed (no play count bump)
                // Only bother recording if they listened for at least 30 seconds
                if (durationElapsedSec >= 30) {
                    db.addListenTimeOnly(currentSong.id, (int) durationElapsedSec);
                }
            }
            songStartTimeMs = 0; // reset so we don't double-record
        }
    }

    // ==================== MEDIA PLAYER CALLBACKS ====================

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        broadcastStateChanged();
        updateNotification();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        recordCurrentListenStats(true); // fully finished → counts as a play
        if (repeatMode == 1) {
            seekTo(0);
            mp.start();
            broadcastStateChanged();
        } else {
            next();
        }
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        initMediaPlayer();
        return true;
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_LOSS:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pause();
                break;
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume if was playing
                break;
        }
    }

    // ==================== NOTIFICATION ====================

    @SuppressWarnings("deprecation")
    private void updateNotification() {
        if (currentSong == null) return;

        Intent mainIntent = new Intent(this, MainActivity.class);
        PendingIntent piMain = PendingIntent.getActivity(this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent playIntent = new Intent(this, AudioService.class);
        playIntent.setAction(ACTION_TOGGLE_PLAY);
        PendingIntent piPlay = PendingIntent.getService(this, 1, playIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent nextIntent = new Intent(this, AudioService.class);
        nextIntent.setAction(ACTION_NEXT);
        PendingIntent piNext = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent prevIntent = new Intent(this, AudioService.class);
        prevIntent.setAction(ACTION_PREV);
        PendingIntent piPrev = PendingIntent.getService(this, 3, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(currentSong.title)
                .setContentText(currentSong.getArtistAlbumString())
                .setContentIntent(piMain)
                .setOngoing(isPlaying());

        // Use standard Notification API 16+ actions
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            builder.addAction(android.R.drawable.ic_media_previous, "Prev", piPrev);
            if (isPlaying()) {
                builder.addAction(android.R.drawable.ic_media_pause, "Pause", piPlay);
            } else {
                builder.addAction(android.R.drawable.ic_media_play, "Play", piPlay);
            }
            builder.addAction(android.R.drawable.ic_media_next, "Next", piNext);
        }

        Notification notif = (android.os.Build.VERSION.SDK_INT >= 16) ? builder.build() : builder.getNotification();
        startForeground(1001, notif);
    }

    private void broadcastStateChanged() {
        sendBroadcast(new Intent(BROADCAST_STATE_CHANGED));
    }

    private void broadcastTrackChanged() {
        sendBroadcast(new Intent(BROADCAST_TRACK_CHANGED));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        recordCurrentListenStats(false); // app killed mid-song → time only, no play count
        if (mediaPlayer != null) {
            try { mediaPlayer.release(); } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        stopForeground(true);
        super.onDestroy();
    }
}
