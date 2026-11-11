package kr.scanworks.workorbit;

import android.app.Activity;
import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

/** Launcher screen: one button that opens the system picker with this wallpaper selected. */
public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.rgb(4, 5, 10));
        int pad = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("WORK ORBIT");
        title.setTextColor(Color.rgb(255, 163, 107));
        title.setTextSize(22);
        title.setLetterSpacing(0.3f);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.rgb(185, 189, 208));
        status.setTextSize(14);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, pad, 0, pad);
        root.addView(status);

        Button set = new Button(this);
        set.setText("배경화면으로 설정");
        set.setOnClickListener(v -> openPicker());
        root.addView(set);

        Button refresh = new Button(this);
        refresh.setText("데이터 지금 받기");
        refresh.setOnClickListener(v -> refreshNow());
        root.addView(refresh);

        setContentView(root);
        showStatus();
    }

    private void openPicker() {
        try {
            Intent i = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER);
            i.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                    new ComponentName(this, OrbitWallpaperService.class));
            startActivity(i);
        } catch (Exception e) {
            try {
                startActivity(new Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER));
            } catch (Exception e2) {
                Toast.makeText(this, "배경화면 설정 화면을 열 수 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void refreshNow() {
        status.setText("받는 중…");
        new Thread(() -> {
            boolean ok = OrbitData.fetch(getApplicationContext());
            runOnUiThread(() -> {
                Toast.makeText(this, ok ? "새 데이터를 받았습니다." : "받지 못했습니다. 인터넷 연결을 확인해 주세요.",
                        Toast.LENGTH_SHORT).show();
                showStatus();
            });
        }).start();
    }

    private void showStatus() {
        long at = OrbitData.lastFetched(getApplicationContext());
        OrbitData d = OrbitData.load(getApplicationContext());
        String when = at == 0 ? "아직 받지 않음"
                : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(at));
        status.setText("프로젝트 " + d.planets.size() + "개 · 마지막 수신: " + when);
    }
}
