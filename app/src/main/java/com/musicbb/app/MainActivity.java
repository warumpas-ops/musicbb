package com.musicbb.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private MusicDatabaseHelper db;
    private AudioService audioService;
    private boolean isBound = false;

    // Active Tab State (0=Now, 1=Folders, 2=Lists, 3=Stats)
    private int currentTab = 0;
    private boolean isShowingVisualizer = false;

    // Header & Tabs
    private TextView tvLogo, btnTabNow, btnTabFolders, btnTabPlaylists, btnTabStats;
    private LinearLayout tabNowPlaying, tabFolders, tabPlaylists, tabStats;

    // Tab 1: Now Playing Views
    private TextView tvTrackBitrate, btnToggleCoverMode;
    private FrameLayout layoutArtContainer;
    private ImageView ivCoverArt;
    private VisualizerView visualizerView;
    private LinearLayout layoutCassetteFallback;
    private TextView tvTrackTitle, tvTrackArtist;
    private SeekBar sbProgress;
    private TextView tvCurrTime, tvTotTime;
    private TextView btnPrev, btnPlay, btnNext;
    private TextView btnShuffle, btnRepeat, btnFav, btnAddPlaylist;

    // Tab 2: Folders & Library Views
    private TextView tvDirBreadcrumb, btnScanMedia, tvEmptyLibrary;
    private EditText etSearch;
    private ListView lvLibrary;
    private List<Song> allSongs = new ArrayList<Song>();
    private List<Song> filteredSongs = new ArrayList<Song>();
    private SongAdapter songAdapter;

    // Tab 3: Playlists Views
    private TextView btnNewPlaylist;
    private ListView lvPlaylists;

    // Tab 4: Stats Views
    private TextView tvStatTotalPlays, tvStatTotalHours;
    private ListView lvTopTracks;

    // Timers & Observers
    private Handler progressHandler = new Handler();
    private boolean isUserSeeking = false;

    private Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (audioService != null && audioService.isPlaying() && !isUserSeeking) {
                int pos = audioService.getCurrentPosition();
                int dur = audioService.getDuration();
                if (dur > 0) {
                    int progress = (int) (((long) pos * 1000) / dur);
                    sbProgress.setProgress(progress);
                    tvCurrTime.setText(formatMs(pos));
                    tvTotTime.setText(formatMs(dur));
                }
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private BroadcastReceiver playbackReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNowPlayingUI();
            if (currentTab == 3) {
                loadStatsTab();
            }
        }
    };

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder) service;
            audioService = binder.getService();
            isBound = true;
            updateNowPlayingUI();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            audioService = null;
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = new MusicDatabaseHelper(this);

        // Start and bind AudioService
        Intent serviceIntent = new Intent(this, AudioService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        initViews();
        setupListeners();

        // Register broadcast receiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(AudioService.BROADCAST_STATE_CHANGED);
        filter.addAction(AudioService.BROADCAST_TRACK_CHANGED);
        registerReceiver(playbackReceiver, filter);

        switchTab(0);
        loadLibraryData();

        // Auto-scan on first launch if database is empty
        if (db.getTotalSongsCount() == 0) {
            performScan();
        }
    }

    private void initViews() {
        // Header & Tab Buttons
        tvLogo = (TextView) findViewById(R.id.tvLogo);
        btnTabNow = (TextView) findViewById(R.id.btnTabNow);
        btnTabFolders = (TextView) findViewById(R.id.btnTabFolders);
        btnTabPlaylists = (TextView) findViewById(R.id.btnTabPlaylists);
        btnTabStats = (TextView) findViewById(R.id.btnTabStats);

        tabNowPlaying = (LinearLayout) findViewById(R.id.tabNowPlaying);
        tabFolders = (LinearLayout) findViewById(R.id.tabFolders);
        tabPlaylists = (LinearLayout) findViewById(R.id.tabPlaylists);
        tabStats = (LinearLayout) findViewById(R.id.tabStats);

        // Tab 1: Now Playing
        tvTrackBitrate = (TextView) findViewById(R.id.tvTrackBitrate);
        btnToggleCoverMode = (TextView) findViewById(R.id.btnToggleCoverMode);
        layoutArtContainer = (FrameLayout) findViewById(R.id.layoutArtContainer);
        ivCoverArt = (ImageView) findViewById(R.id.ivCoverArt);
        visualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        layoutCassetteFallback = (LinearLayout) findViewById(R.id.layoutCassetteFallback);
        tvTrackTitle = (TextView) findViewById(R.id.tvTrackTitle);
        tvTrackArtist = (TextView) findViewById(R.id.tvTrackArtist);
        sbProgress = (SeekBar) findViewById(R.id.sbProgress);
        tvCurrTime = (TextView) findViewById(R.id.tvCurrTime);
        tvTotTime = (TextView) findViewById(R.id.tvTotTime);
        btnPrev = (TextView) findViewById(R.id.btnPrev);
        btnPlay = (TextView) findViewById(R.id.btnPlay);
        btnNext = (TextView) findViewById(R.id.btnNext);
        btnShuffle = (TextView) findViewById(R.id.btnShuffle);
        btnRepeat = (TextView) findViewById(R.id.btnRepeat);
        btnFav = (TextView) findViewById(R.id.btnFav);
        btnAddPlaylist = (TextView) findViewById(R.id.btnAddPlaylist);

        // Tab 2: Folders & Library
        tvDirBreadcrumb = (TextView) findViewById(R.id.tvDirBreadcrumb);
        btnScanMedia = (TextView) findViewById(R.id.btnScanMedia);
        etSearch = (EditText) findViewById(R.id.etSearch);
        lvLibrary = (ListView) findViewById(R.id.lvLibrary);
        tvEmptyLibrary = (TextView) findViewById(R.id.tvEmptyLibrary);

        // Tab 3: Playlists
        btnNewPlaylist = (TextView) findViewById(R.id.btnNewPlaylist);
        lvPlaylists = (ListView) findViewById(R.id.lvPlaylists);

        // Tab 4: Stats
        tvStatTotalPlays = (TextView) findViewById(R.id.tvStatTotalPlays);
        tvStatTotalHours = (TextView) findViewById(R.id.tvStatTotalHours);
        lvTopTracks = (ListView) findViewById(R.id.lvTopTracks);
    }

    private void setupListeners() {
        // Tab buttons
        btnTabNow.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(0); } });
        btnTabFolders.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(1); } });
        btnTabPlaylists.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(2); } });
        btnTabStats.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { switchTab(3); } });

        // Now Playing Controls
        btnPlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null) {
                    audioService.togglePlayPause();
                    updateNowPlayingUI();
                }
            }
        });

        btnPrev.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null) audioService.previous();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null) audioService.next();
            }
        });

        btnShuffle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null) {
                    audioService.toggleShuffle();
                    updateNowPlayingUI();
                }
            }
        });

        btnRepeat.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null) {
                    audioService.toggleRepeat();
                    updateNowPlayingUI();
                }
            }
        });

        btnFav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null && audioService.getCurrentSong() != null) {
                    Song s = audioService.getCurrentSong();
                    s.isFavorite = db.toggleFavorite(s.id);
                    btnFav.setText(s.isFavorite ? "★ [FAVORITE]" : "☆ [FAV]");
                    btnFav.setTextColor(s.isFavorite ? 0xFFA8D8A8 : 0xFF555A64);
                }
            }
        });

        btnAddPlaylist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (audioService != null && audioService.getCurrentSong() != null) {
                    showAddToPlaylistDialog(audioService.getCurrentSong());
                }
            }
        });

        // Cover / Visualizer Toggle
        View.OnClickListener toggleCoverListener = new View.OnClickListener() {
            public void onClick(View v) {
                isShowingVisualizer = !isShowingVisualizer;
                applyCoverMode();
            }
        };
        btnToggleCoverMode.setOnClickListener(toggleCoverListener);
        layoutArtContainer.setOnClickListener(toggleCoverListener);

        // Scrubber SeekBar
        sbProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && audioService != null && audioService.getDuration() > 0) {
                    int seekMs = (int) (((long) progress * audioService.getDuration()) / 1000);
                    tvCurrTime.setText(formatMs(seekMs));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isUserSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (audioService != null && audioService.getDuration() > 0) {
                    int seekMs = (int) (((long) seekBar.getProgress() * audioService.getDuration()) / 1000);
                    audioService.seekTo(seekMs);
                }
                isUserSeeking = false;
            }
        });

        // Folders Tab Listeners
        btnScanMedia.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                performScan();
            }
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { filterSongs(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });

        // Library Song Click
        lvLibrary.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (audioService != null && position >= 0 && position < filteredSongs.size()) {
                    audioService.playQueue(filteredSongs, position);
                    switchTab(0);
                }
            }
        });

        // Playlists Tab Listeners
        btnNewPlaylist.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showCreatePlaylistDialog();
            }
        });
    }

    // ==================== TAB SWITCHING ====================

    private void switchTab(int index) {
        currentTab = index;
        tabNowPlaying.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        tabFolders.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        tabPlaylists.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        tabStats.setVisibility(index == 3 ? View.VISIBLE : View.GONE);

        btnTabNow.setTextColor(index == 0 ? 0xFFA8D8A8 : 0xFF555A64);
        btnTabFolders.setTextColor(index == 1 ? 0xFFA8D8A8 : 0xFF555A64);
        btnTabPlaylists.setTextColor(index == 2 ? 0xFFA8D8A8 : 0xFF555A64);
        btnTabStats.setTextColor(index == 3 ? 0xFFA8D8A8 : 0xFF555A64);

        if (index == 0) {
            updateNowPlayingUI();
        } else if (index == 1) {
            loadLibraryData();
        } else if (index == 2) {
            loadPlaylistsTab();
        } else if (index == 3) {
            loadStatsTab();
        }
    }

    // ==================== NOW PLAYING UI ====================

    private void updateNowPlayingUI() {
        if (audioService == null) return;

        Song s = audioService.getCurrentSong();
        boolean isPlaying = audioService.isPlaying();

        if (s != null) {
            tvTrackTitle.setText(s.title);
            tvTrackArtist.setText(s.getArtistAlbumString());

            // Re-query song from DB for live play_count and favorite
            Song dbSong = db.getSongById(s.id);
            if (dbSong != null) {
                s.playCount = dbSong.playCount;
                s.isFavorite = dbSong.isFavorite;
            }

            tvTrackBitrate.setText(String.format(Locale.US, "320 KBPS · 🔥 %d PLAYS", s.playCount));
            btnFav.setText(s.isFavorite ? "★ [FAVORITE]" : "☆ [FAV]");
            btnFav.setTextColor(s.isFavorite ? 0xFFA8D8A8 : 0xFF555A64);

            int dur = audioService.getDuration();
            if (dur > 0) tvTotTime.setText(formatMs(dur));

            // Load Cover Art
            Bitmap art = CoverArtHelper.getCoverArt(s.filePath, 320);
            if (art != null) {
                ivCoverArt.setImageBitmap(art);
                ivCoverArt.setTag("has_art");
            } else {
                ivCoverArt.setImageBitmap(null);
                ivCoverArt.setTag(null);
            }
        } else {
            tvTrackTitle.setText("no track selected");
            tvTrackArtist.setText("select a song from folders");
            tvTrackBitrate.setText("STEREO AUDIO ENGINE");
            ivCoverArt.setTag(null);
        }

        applyCoverMode();

        // Play / Pause button
        if (isPlaying) {
            btnPlay.setText("⏸ PAUSE");
            btnPlay.setBackgroundResource(R.drawable.btn_accent);
            btnPlay.setTextColor(0xFF0D0F11);
            visualizerView.setPlaying(true);
            visualizerView.linkAudioSession(audioService.getAudioSessionId());
        } else {
            btnPlay.setText("▶ PLAY");
            btnPlay.setBackgroundResource(R.drawable.row_selector);
            btnPlay.setTextColor(0xFFA8D8A8);
            visualizerView.setPlaying(false);
        }

        // Shuffle button
        btnShuffle.setText(audioService.isShuffle() ? "[S] SHUF: ON" : "[S] SHUF: OFF");
        btnShuffle.setTextColor(audioService.isShuffle() ? 0xFFA8D8A8 : 0xFF555A64);

        // Repeat button
        int rep = audioService.getRepeatMode();
        String repStr = (rep == 0) ? "[R] REP: ALL" : (rep == 1 ? "[R] REP: ONE" : "[R] REP: OFF");
        btnRepeat.setText(repStr);
        btnRepeat.setTextColor(rep < 2 ? 0xFFA8D8A8 : 0xFF555A64);
    }

    private void applyCoverMode() {
        if (isShowingVisualizer) {
            ivCoverArt.setVisibility(View.GONE);
            layoutCassetteFallback.setVisibility(View.GONE);
            visualizerView.setVisibility(View.VISIBLE);
            btnToggleCoverMode.setText("[VU: SPECTRUM]");
        } else {
            visualizerView.setVisibility(View.GONE);
            if ("has_art".equals(ivCoverArt.getTag())) {
                ivCoverArt.setVisibility(View.VISIBLE);
                layoutCassetteFallback.setVisibility(View.GONE);
            } else {
                ivCoverArt.setVisibility(View.GONE);
                layoutCassetteFallback.setVisibility(View.VISIBLE);
            }
            btnToggleCoverMode.setText("[C: ART]");
        }
    }

    // ==================== LIBRARY & SCANNER ====================

    private void loadLibraryData() {
        allSongs = db.getAllSongs();
        filterSongs(etSearch.getText().toString());
    }

    private void filterSongs(String query) {
        filteredSongs.clear();
        String q = query != null ? query.trim().toLowerCase() : "";
        if (q.isEmpty()) {
            filteredSongs.addAll(allSongs);
        } else {
            for (Song s : allSongs) {
                if (s.title.toLowerCase().contains(q) || s.artist.toLowerCase().contains(q) || s.album.toLowerCase().contains(q)) {
                    filteredSongs.add(s);
                }
            }
        }

        if (filteredSongs.isEmpty()) {
            tvEmptyLibrary.setVisibility(View.VISIBLE);
            lvLibrary.setVisibility(View.GONE);
        } else {
            tvEmptyLibrary.setVisibility(View.GONE);
            lvLibrary.setVisibility(View.VISIBLE);
            songAdapter = new SongAdapter(this, filteredSongs);
            lvLibrary.setAdapter(songAdapter);
        }
    }

    private void performScan() {
        Toast.makeText(this, "scanning device for music...", Toast.LENGTH_SHORT).show();
        btnScanMedia.setText("[scanning...]");
        MusicScanner.scanMedia(this, new MusicScanner.ScanCallback() {
            @Override
            public void onProgress(final int count, final String currentFile) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btnScanMedia.setText(String.format(Locale.US, "[%d songs]", count));
                    }
                });
            }

            @Override
            public void onComplete(final int totalFound) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btnScanMedia.setText("[+ scan]");
                        Toast.makeText(MainActivity.this, String.format(Locale.US, "scan complete: %d songs found", totalFound), Toast.LENGTH_LONG).show();
                        loadLibraryData();
                    }
                });
            }
        });
    }

    // ==================== PLAYLISTS TAB ====================

    private void loadPlaylistsTab() {
        List<Playlist> smartAndCustom = new ArrayList<Playlist>();
        smartAndCustom.add(new Playlist(-1, "🔥 Most Played", 0, db.getMostPlayedSongs(25).size(), true));
        smartAndCustom.add(new Playlist(-2, "★ Favorites", 0, db.getFavoriteSongs().size(), true));
        smartAndCustom.add(new Playlist(-3, "🕒 Recently Played", 0, db.getRecentlyPlayedSongs(50).size(), true));
        smartAndCustom.addAll(db.getAllCustomPlaylists());

        lvPlaylists.setAdapter(new PlaylistAdapter(this, smartAndCustom));
        lvPlaylists.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Playlist p = (Playlist) parent.getItemAtPosition(position);
                openPlaylistSongs(p);
            }
        });
    }

    private void openPlaylistSongs(Playlist p) {
        List<Song> songs;
        if (p.id == -1) songs = db.getMostPlayedSongs(25);
        else if (p.id == -2) songs = db.getFavoriteSongs();
        else if (p.id == -3) songs = db.getRecentlyPlayedSongs(50);
        else songs = db.getPlaylistSongs(p.id);

        if (songs.isEmpty()) {
            Toast.makeText(this, "playlist is empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (audioService != null) {
            audioService.playQueue(songs, 0);
            switchTab(0);
        }
    }

    private void showCreatePlaylistDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. Gym Hype, Chill Mix");
        input.setTextColor(0xFFE0E2E6);
        input.setTypeface(android.graphics.Typeface.MONOSPACE);

        new AlertDialog.Builder(this)
                .setTitle("new playlist")
                .setView(input)
                .setPositiveButton("create", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (!name.isEmpty()) {
                            db.createPlaylist(name);
                            loadPlaylistsTab();
                        }
                    }
                })
                .setNegativeButton("cancel", null)
                .show();
    }

    private void showAddToPlaylistDialog(final Song song) {
        final List<Playlist> customLists = db.getAllCustomPlaylists();
        if (customLists.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle("no playlists")
                    .setMessage("create a playlist first?")
                    .setPositiveButton("create", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            showCreatePlaylistDialog();
                        }
                    })
                    .setNegativeButton("cancel", null)
                    .show();
            return;
        }

        String[] names = new String[customLists.size()];
        for (int i = 0; i < customLists.size(); i++) {
            names[i] = customLists.get(i).name;
        }

        new AlertDialog.Builder(this)
                .setTitle("add to playlist")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Playlist chosen = customLists.get(which);
                        boolean ok = db.addSongToPlaylist(chosen.id, song.id);
                        if (ok) {
                            Toast.makeText(MainActivity.this, "added to " + chosen.name, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "already in " + chosen.name, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    // ==================== STATS TAB ====================

    private void loadStatsTab() {
        int totalPlays = db.getTotalPlays();
        double hours = db.getTotalListeningHours();

        tvStatTotalPlays.setText(String.valueOf(totalPlays));
        tvStatTotalHours.setText(String.format(Locale.US, "%.1f hrs", hours));

        final List<Song> topSongs = db.getMostPlayedSongs(20);
        lvTopTracks.setAdapter(new ArrayAdapter<Song>(this, R.layout.item_top_song, topSongs) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_top_song, parent, false);
                }
                Song s = getItem(position);
                TextView tvRank = (TextView) convertView.findViewById(R.id.tvRank);
                TextView tvTitle = (TextView) convertView.findViewById(R.id.tvTopTitle);
                TextView tvArtist = (TextView) convertView.findViewById(R.id.tvTopArtist);
                TextView tvPlays = (TextView) convertView.findViewById(R.id.tvTopPlays);

                tvRank.setText("#" + (position + 1));
                tvTitle.setText(s.title);
                tvArtist.setText(s.getArtistAlbumString());
                tvPlays.setText(s.playCount + " plays");

                return convertView;
            }
        });

        lvTopTracks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (audioService != null && position >= 0 && position < topSongs.size()) {
                    audioService.playQueue(topSongs, position);
                    switchTab(0);
                }
            }
        });
    }

    // ==================== PHYSICAL KEYBOARD SHORTCUTS ====================

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SPACE) {
            if (audioService != null) {
                audioService.togglePlayPause();
                updateNowPlayingUI();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_N) {
            if (audioService != null) audioService.next();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_P) {
            if (audioService != null) audioService.previous();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_J) {
            if (audioService != null) audioService.seekTo(Math.max(0, audioService.getCurrentPosition() - 5000));
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_L) {
            if (audioService != null) audioService.seekTo(audioService.getCurrentPosition() + 5000);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_C) {
            isShowingVisualizer = !isShowingVisualizer;
            applyCoverMode();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_S) {
            if (audioService != null) audioService.toggleShuffle();
            updateNowPlayingUI();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_R) {
            if (audioService != null) audioService.toggleRepeat();
            updateNowPlayingUI();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_F) {
            switchTab(1);
            etSearch.requestFocus();
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_1) {
            switchTab(0);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_2) {
            switchTab(1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_3) {
            switchTab(2);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_4) {
            switchTab(3);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    // ==================== HELPERS & ADAPTERS ====================

    private String formatMs(int msec) {
        int sec = msec / 1000;
        int m = sec / 60;
        int s = sec % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateNowPlayingUI();
        progressHandler.post(progressRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        progressHandler.removeCallbacks(progressRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(playbackReceiver); } catch (Exception ignored) {}
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        visualizerView.releaseVisualizer();
    }

    // Inner Adapter for Song Rows
    private class SongAdapter extends ArrayAdapter<Song> {
        public SongAdapter(Context context, List<Song> list) {
            super(context, R.layout.item_song, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_song, parent, false);
            }
            final Song s = getItem(position);
            TextView title = (TextView) convertView.findViewById(R.id.tvSongTitle);
            TextView artist = (TextView) convertView.findViewById(R.id.tvSongArtist);
            TextView plays = (TextView) convertView.findViewById(R.id.tvSongPlays);
            ImageView thumb = (ImageView) convertView.findViewById(R.id.ivSongThumb);
            TextView btnOpt = (TextView) convertView.findViewById(R.id.btnSongOptions);

            title.setText(s.title);
            artist.setText(s.getArtistAlbumString() + " · " + s.getFormattedDuration());

            if (s.playCount > 0) {
                plays.setText("🔥 " + s.playCount);
                plays.setVisibility(View.VISIBLE);
            } else {
                plays.setVisibility(View.GONE);
            }

            // Load mini thumbnail
            Bitmap b = CoverArtHelper.getCoverArt(s.filePath, 80);
            if (b != null) {
                thumb.setImageBitmap(b);
            } else {
                thumb.setImageResource(R.mipmap.ic_launcher);
            }

            btnOpt.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    showAddToPlaylistDialog(s);
                }
            });

            return convertView;
        }
    }

    // Inner Adapter for Playlists
    private class PlaylistAdapter extends ArrayAdapter<Playlist> {
        public PlaylistAdapter(Context context, List<Playlist> list) {
            super(context, R.layout.item_playlist, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_playlist, parent, false);
            }
            final Playlist p = getItem(position);
            TextView name = (TextView) convertView.findViewById(R.id.tvPlaylistName);
            TextView count = (TextView) convertView.findViewById(R.id.tvPlaylistCount);
            TextView btnDel = (TextView) convertView.findViewById(R.id.btnDeletePlaylist);

            name.setText(p.name);
            count.setText(p.songCount + " tracks");

            if (p.isSmartPlaylist) {
                btnDel.setVisibility(View.GONE);
            } else {
                btnDel.setVisibility(View.VISIBLE);
                btnDel.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        db.deletePlaylist(p.id);
                        loadPlaylistsTab();
                    }
                });
            }

            return convertView;
        }
    }
}
