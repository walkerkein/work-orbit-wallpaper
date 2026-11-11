package kr.scanworks.workorbit;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Live wallpaper: draws the orbit only while visible, about 20 frames per second. */
public class OrbitWallpaperService extends WallpaperService {

    private static final long FRAME_MS = 50L;
    private static final long RETRY_MS = 10L * 60L * 1000L;

    @Override
    public Engine onCreateEngine() {
        return new OrbitEngine();
    }

    private final class OrbitEngine extends Engine {
        private final Handler handler = new Handler(Looper.getMainLooper());
        private final ExecutorService io = Executors.newSingleThreadExecutor();
        private final OrbitRenderer renderer = new OrbitRenderer();
        private volatile OrbitData data;
        private boolean visible;
        private boolean fetching;
        private long lastAttempt;
        private int width, height;

        private final Runnable frame = this::drawFrame;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            data = OrbitData.load(getApplicationContext());
        }

        @Override
        public void onVisibilityChanged(boolean v) {
            visible = v;
            handler.removeCallbacks(frame);
            if (v) {
                maybeFetch();
                drawFrame();
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int w, int h) {
            super.onSurfaceChanged(holder, format, w, h);
            width = w;
            height = h;
            drawFrame();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            visible = false;
            handler.removeCallbacks(frame);
            super.onSurfaceDestroyed(holder);
        }

        @Override
        public void onDestroy() {
            handler.removeCallbacks(frame);
            io.shutdownNow();
            super.onDestroy();
        }

        private void maybeFetch() {
            long now = System.currentTimeMillis();
            // after a failed attempt (e.g. offline) wait 10 minutes before trying again
            if (fetching || now - lastAttempt < RETRY_MS || !OrbitData.due(getApplicationContext())) return;
            lastAttempt = now;
            fetching = true;
            io.execute(() -> {
                boolean ok = OrbitData.fetch(getApplicationContext());
                handler.post(() -> {
                    fetching = false;
                    if (ok) data = OrbitData.load(getApplicationContext());
                });
            });
        }

        private void drawFrame() {
            SurfaceHolder holder = getSurfaceHolder();
            if (width > 0 && height > 0) {
                Canvas c = null;
                try {
                    c = holder.lockHardwareCanvas();
                } catch (Exception e) {
                    c = null;
                }
                if (c == null) {
                    try {
                        c = holder.lockCanvas();
                    } catch (Exception e) {
                        c = null;
                    }
                }
                if (c != null) {
                    try {
                        renderer.draw(c, width, height, System.currentTimeMillis(), data);
                    } finally {
                        holder.unlockCanvasAndPost(c);
                    }
                }
            }
            handler.removeCallbacks(frame);
            if (visible) {
                maybeFetch();
                handler.postDelayed(frame, FRAME_MS);
            }
        }
    }
}
