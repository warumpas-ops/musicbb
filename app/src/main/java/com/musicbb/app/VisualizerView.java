package com.musicbb.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.audiofx.Visualizer;
import android.util.AttributeSet;
import android.view.View;

import java.util.Random;

public class VisualizerView extends View {

    private static final int NUM_BARS = 18;
    private float[] barHeights = new float[NUM_BARS];
    private float[] targetHeights = new float[NUM_BARS];

    private Paint barPaint;
    private Paint peakPaint;
    private Visualizer visualizer;
    private boolean isPlaying = false;
    private Random random = new Random();

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPlaying) {
                // Smoothly interpolate towards target
                for (int i = 0; i < NUM_BARS; i++) {
                    if (Math.abs(barHeights[i] - targetHeights[i]) < 0.05f) {
                        targetHeights[i] = 0.15f + (random.nextFloat() * 0.80f);
                    }
                    barHeights[i] += (targetHeights[i] - barHeights[i]) * 0.25f;
                }
                invalidate();
                postDelayed(this, 35); // ~30 fps
            }
        }
    };

    public VisualizerView(Context context) {
        super(context);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public VisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFFA8D8A8); // Phosphor green
        barPaint.setStyle(Paint.Style.FILL);

        peakPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        peakPaint.setColor(0xFFE0E2E6);
        peakPaint.setStyle(Paint.Style.FILL);

        for (int i = 0; i < NUM_BARS; i++) {
            barHeights[i] = 0.1f;
            targetHeights[i] = 0.5f;
        }
    }

    public void linkAudioSession(int audioSessionId) {
        releaseVisualizer();
        try {
            if (audioSessionId > 0) {
                visualizer = new Visualizer(audioSessionId);
                visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[0]);
                visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                    @Override
                    public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int samplingRate) {
                        updateWaveform(waveform);
                    }

                    @Override
                    public void onFftDataCapture(Visualizer v, byte[] fft, int samplingRate) {
                        updateFft(fft);
                    }
                }, Visualizer.getMaxCaptureRate() / 2, false, true);
                visualizer.setEnabled(true);
            }
        } catch (Exception ignored) {
            // Visualizer not supported or permission denied on certain BB10 runtimes;
            // fall back to smooth simulated generator
        }
    }

    public void setPlaying(boolean playing) {
        this.isPlaying = playing;
        removeCallbacks(updateRunnable);
        if (playing) {
            post(updateRunnable);
        } else {
            for (int i = 0; i < NUM_BARS; i++) {
                barHeights[i] = 0.08f;
            }
            invalidate();
        }
    }

    private void updateWaveform(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return;
        int step = bytes.length / NUM_BARS;
        for (int i = 0; i < NUM_BARS; i++) {
            int index = i * step;
            byte val = bytes[index];
            targetHeights[i] = Math.min(1.0f, Math.abs((float) val / 128.0f));
        }
        invalidate();
    }

    private void updateFft(byte[] fft) {
        if (fft == null || fft.length == 0) return;
        int step = (fft.length / 2) / NUM_BARS;
        for (int i = 0; i < NUM_BARS; i++) {
            int index = i * step * 2;
            byte rfk = fft[index];
            byte ifk = fft[index + 1];
            float magnitude = (float) Math.hypot(rfk, ifk);
            targetHeights[i] = Math.min(1.0f, magnitude / 128.0f);
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        float totalSpacing = w * 0.20f;
        float spacing = totalSpacing / (NUM_BARS + 1);
        float barWidth = (w - totalSpacing) / NUM_BARS;

        for (int i = 0; i < NUM_BARS; i++) {
            float left = spacing + i * (barWidth + spacing);
            float right = left + barWidth;
            float barHeight = Math.max(8.0f, h * barHeights[i]);
            float top = h - barHeight;
            float bottom = h;

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(rect, 3.0f, 3.0f, barPaint);

            // Cap / peak indicator
            float capTop = Math.max(0, top - 4.0f);
            RectF capRect = new RectF(left, capTop, right, top - 1.0f);
            canvas.drawRect(capRect, peakPaint);
        }
    }

    public void releaseVisualizer() {
        if (visualizer != null) {
            try {
                visualizer.setEnabled(false);
                visualizer.release();
            } catch (Exception ignored) {}
            visualizer = null;
        }
        removeCallbacks(updateRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        releaseVisualizer();
    }
}
