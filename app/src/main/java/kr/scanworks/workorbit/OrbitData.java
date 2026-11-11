package kr.scanworks.workorbit;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Summary data shown on the wallpaper: planets, open-task counts, next deadline. */
public final class OrbitData {

    /** Public summary file kept in the GitHub repository (updated by a scheduled task). */
    public static final String DATA_URL =
            "https://raw.githubusercontent.com/walkerkein/work-orbit-wallpaper/main/data/orbit.json";

    static final long FETCH_INTERVAL_MS = 60L * 60L * 1000L; // 1 hour

    private static final String PREFS = "orbit";
    private static final String KEY_JSON = "json";
    private static final String KEY_FETCHED = "fetched_at";

    /** Used until the first successful download. */
    static final String DEFAULT_JSON = "{\"updated\":\"\",\"projects\":["
            + "{\"name\":\"SCAN\",\"orbit\":0,\"a0\":3.4,\"open\":0},"
            + "{\"name\":\"The Scan\",\"orbit\":1,\"a0\":0.5,\"open\":0},"
            + "{\"name\":\"Work Orbit\",\"orbit\":1,\"a0\":3.6,\"open\":0},"
            + "{\"name\":\"New Ventures\",\"orbit\":2,\"a0\":5.2,\"open\":0},"
            + "{\"name\":\"The Pin\",\"orbit\":2,\"a0\":2.0,\"open\":0},"
            + "{\"name\":\"OfficeClear\",\"orbit\":3,\"a0\":4.3,\"open\":0},"
            + "{\"name\":\"Senior Portal\",\"orbit\":3,\"a0\":1.1,\"open\":0}"
            + "],\"next\":{\"title\":\"\",\"date\":\"\"}}";

    public static final class Planet {
        public final String name;
        public final int orbit;
        public final float a0;
        public final int open;

        Planet(String name, int orbit, float a0, int open) {
            this.name = name;
            this.orbit = orbit;
            this.a0 = a0;
            this.open = open;
        }
    }

    public static final class LogItem {
        public final String date;
        public final String text;
        public final String who;

        LogItem(String date, String text, String who) {
            this.date = date;
            this.text = text;
            this.who = who;
        }
    }

    public final List<Planet> planets = new ArrayList<>();
    public final List<String> briefing = new ArrayList<>();
    public final List<LogItem> log = new ArrayList<>();
    public String nextTitle = "";
    public LocalDate nextDate = null;
    public String updated = "";

    static OrbitData parse(String json) {
        OrbitData d = new OrbitData();
        try {
            JSONObject o = new JSONObject(json);
            d.updated = o.optString("updated", "");
            JSONArray arr = o.optJSONArray("projects");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject p = arr.getJSONObject(i);
                    int orbit = Math.max(0, Math.min(3, p.optInt("orbit", 0)));
                    d.planets.add(new Planet(p.optString("name", "?"), orbit,
                            (float) p.optDouble("a0", i), p.optInt("open", 0)));
                }
            }
            JSONArray br = o.optJSONArray("briefing");
            if (br != null) {
                for (int i = 0; i < br.length(); i++) {
                    d.briefing.add(br.optString(i, ""));
                }
            }
            JSONArray lg = o.optJSONArray("log");
            if (lg != null) {
                for (int i = 0; i < lg.length(); i++) {
                    JSONObject e = lg.getJSONObject(i);
                    d.log.add(new LogItem(e.optString("date", ""), e.optString("text", ""),
                            e.optString("who", "")));
                }
            }
            JSONObject next = o.optJSONObject("next");
            if (next != null) {
                d.nextTitle = next.optString("title", "");
                String date = next.optString("date", "");
                if (date.length() >= 10) {
                    d.nextDate = LocalDate.parse(date.substring(0, 10));
                }
            }
        } catch (Exception e) {
            if (!json.equals(DEFAULT_JSON)) {
                return parse(DEFAULT_JSON);
            }
        }
        return d;
    }

    static OrbitData load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return parse(sp.getString(KEY_JSON, DEFAULT_JSON));
    }

    static long lastFetched(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_FETCHED, 0L);
    }

    static boolean due(Context ctx) {
        return System.currentTimeMillis() - lastFetched(ctx) > FETCH_INTERVAL_MS;
    }

    /** Blocking download; call off the main thread. Returns true when new data was stored. */
    static boolean fetch(Context ctx) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(DATA_URL + "?t=" + (System.currentTimeMillis() / 60000L));
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setUseCaches(false);
            if (conn.getResponseCode() != 200) {
                return false;
            }
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            in.close();
            String json = new String(out.toByteArray(), StandardCharsets.UTF_8);
            new JSONObject(json); // validate before storing
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(KEY_JSON, json)
                    .putLong(KEY_FETCHED, System.currentTimeMillis())
                    .apply();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
